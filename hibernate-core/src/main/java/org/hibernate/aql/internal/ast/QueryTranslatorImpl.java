/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.aql.internal.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import antlr.ANTLRException;
import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;
import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.ScrollableResults;
import org.hibernate.aql.internal.QueryExecutionRequestException;
import org.hibernate.aql.internal.antlr.AqlTokenTypes;
import org.hibernate.aql.internal.ast.exec.BasicExecutor;
import org.hibernate.aql.internal.ast.exec.MultiTableDeleteExecutor;
import org.hibernate.aql.internal.ast.exec.MultiTableUpdateExecutor;
import org.hibernate.aql.internal.ast.exec.StatementExecutor;
import org.hibernate.aql.internal.ast.tree.AggregatedSelectExpression;
import org.hibernate.aql.internal.ast.tree.FromElement;
import org.hibernate.aql.internal.ast.tree.InsertStatement;
import org.hibernate.aql.internal.ast.tree.QueryNode;
import org.hibernate.aql.internal.ast.tree.Statement;
import org.hibernate.aql.internal.ast.util.ASTPrinter;
import org.hibernate.aql.internal.ast.util.ASTUtil;
import org.hibernate.aql.internal.ast.util.NodeTraverser;
import org.hibernate.aql.spi.FilterTranslator;
import org.hibernate.aql.spi.ParameterTranslations;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.antlr.HqlTokenTypes;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.loader.aql.QueryLoader;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.type.Type;

/**
 * A QueryTranslator that uses an Antlr-based parser.
 *
 * @author Joshua Davis (pgmjsd@sourceforge.net)
 */
public class QueryTranslatorImpl implements FilterTranslator {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			QueryTranslatorImpl.class.getName()
	);

	private SessionFactoryImplementor factory;

	private final String queryIdentifier;
	private String hql;
	private boolean shallowQuery;
	private Map tokenReplacements;

	//TODO:this is only needed during compilation .. can we eliminate the instvar?
	private Map enabledFilters;

	private boolean compiled;
	private QueryLoader queryLoader;
	private StatementExecutor statementExecutor;

	private Statement sqlAst;
	private String sql;

	private ParameterTranslations paramTranslations;
	private List collectedParameterSpecifications;


	/**
	 * Creates a new AST-based query translator.
	 *
	 * @param queryIdentifier The query-identifier (used in stats collection)
	 * @param query The hql query to translate
	 * @param enabledFilters Currently enabled filters
	 * @param factory The session factory constructing this translator instance.
	 */
	public QueryTranslatorImpl(
			String queryIdentifier,
			String query,
			Map enabledFilters,
			SessionFactoryImplementor factory) {
		this.queryIdentifier = queryIdentifier;
		this.hql = query;
		this.compiled = false;
		this.shallowQuery = false;
		this.enabledFilters = enabledFilters;
		this.factory = factory;
	}

	/**
	 * Compile a "normal" query. This method may be called multiple
	 * times. Subsequent invocations are no-ops.
	 *
	 * @param replacements Defined query substitutions.
	 * @param shallow      Does this represent a shallow (scalar or entity-id) select?
	 * @throws QueryException   There was a problem parsing the query string.
	 * @throws MappingException There was a problem querying defined mappings.
	 */
	public void compile(
			Map replacements,
			boolean shallow) throws QueryException, MappingException {
		doCompile( replacements, shallow, null );
	}

	/**
	 * Compile a filter. This method may be called multiple
	 * times. Subsequent invocations are no-ops.
	 *
	 * @param collectionRole the role name of the collection used as the basis for the filter.
	 * @param replacements   Defined query substitutions.
	 * @param shallow        Does this represent a shallow (scalar or entity-id) select?
	 * @throws QueryException   There was a problem parsing the query string.
	 * @throws MappingException There was a problem querying defined mappings.
	 */
	public void compile(
			String collectionRole,
			Map replacements,
			boolean shallow) throws QueryException, MappingException {
		doCompile( replacements, shallow, collectionRole );
	}

	/**
	 * Performs both filter and non-filter compiling.
	 *
	 * @param replacements   Defined query substitutions.
	 * @param shallow        Does this represent a shallow (scalar or entity-id) select?
	 * @param collectionRole the role name of the collection used as the basis for the filter, NULL if this
	 *                       is not a filter.
	 */
	private synchronized void doCompile(Map replacements, boolean shallow, String collectionRole) {
		// If the query is already compiled, skip the compilation.
		if ( compiled ) {
			LOG.debug( "compile() : The query is already compiled, skipping..." );
			return;
		}

		// Remember the parameters for the compilation.
		this.tokenReplacements = replacements;
		if ( tokenReplacements == null ) {
			tokenReplacements = new HashMap();
		}
		this.shallowQuery = shallow;

		try {
			// PHASE 1 : Parse the HQL into an AST.
			final AqlParser parser = parse( true );

			// PHASE 2 : Analyze the HQL AST, and produce an SQL AST.
			final AqlSqlWalker w = analyze( parser, collectionRole );

			sqlAst = (Statement) w.getAST();

			// at some point the generate phase needs to be moved out of here,
			// because a single object-level DML might spawn multiple SQL DML
			// command executions.
			//
			// Possible to just move the sql generation for dml stuff, but for
			// consistency-sake probably best to just move responsiblity for
			// the generation phase completely into the delegates
			// (QueryLoader/StatementExecutor) themselves.  Also, not sure why
			// QueryLoader currently even has a dependency on this at all; does
			// it need it?  Ideally like to see the walker itself given to the delegates directly...

			if ( sqlAst.needsExecutor() ) {
				statementExecutor = buildAppropriateStatementExecutor( w );
			}
			else {
				// PHASE 3 : Generate the SQL.
				generate( (QueryNode) sqlAst );
				queryLoader = new QueryLoader( this, factory, w.getSelectClause() );
			}

			compiled = true;
		}
		catch ( QueryException qe ) {
			if ( qe.getQueryString() == null ) {
				throw qe.wrapWithQueryString( hql );
			}
			else {
				throw qe;
			}
		}
		catch ( RecognitionException e ) {
			// we do not actually propagate ANTLRExceptions as a cause, so
			// log it here for diagnostic purposes
			LOG.trace( "Converted antlr.RecognitionException", e );
			throw QuerySyntaxException.convert( e, hql );
		}
		catch ( ANTLRException e ) {
			// we do not actually propagate ANTLRExceptions as a cause, so
			// log it here for diagnostic purposes
			LOG.trace( "Converted antlr.ANTLRException", e );
			throw new QueryException( e.getMessage(), hql );
		}

		//only needed during compilation phase...
		this.enabledFilters = null;
	}

	private void generate(AST sqlAst) throws QueryException, RecognitionException {
		if ( sql == null ) {
			final SqlGenerator gen = new SqlGenerator( factory );
			gen.statement( sqlAst );
			sql = gen.getSQL();
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "HQL: %s", hql );
				LOG.debugf( "SQL: %s", sql );
			}
			gen.getParseErrorHandler().throwQueryException();
			collectedParameterSpecifications = gen.getCollectedParameters();
		}
	}

	private static final ASTPrinter SQL_TOKEN_PRINTER = new ASTPrinter( SqlTokenTypes.class );

	private AqlSqlWalker analyze(AqlParser parser, String collectionRole) throws QueryException, RecognitionException {
		final AqlSqlWalker w = new AqlSqlWalker( this, factory, parser, tokenReplacements, collectionRole );
		final AST hqlAst = parser.getAST();

		// Transform the tree.
		w.statement( hqlAst );

		if ( LOG.isDebugEnabled() ) {
			LOG.debug( SQL_TOKEN_PRINTER.showAsString( w.getAST(), "--- SQL AST ---" ) );
		}

		w.getParseErrorHandler().throwQueryException();

		return w;
	}

	private AqlParser parse(boolean filter) throws TokenStreamException, RecognitionException {
		// Parse the query string into an HQL AST.
		final AqlParser parser = AqlParser.getInstance( hql );
		parser.setFilter( filter );

		LOG.debugf( "parse() - HQL: %s", hql );
		parser.statement();

		final AST hqlAst = parser.getAST();

		final NodeTraverser walker = new NodeTraverser( new JavaConstantConverter() );
		walker.traverseDepthFirst( hqlAst );

		showHqlAst( hqlAst );

		parser.getParseErrorHandler().throwQueryException();
		return parser;
	}

	private static final ASTPrinter AQL_TOKEN_PRINTER = new ASTPrinter( AqlTokenTypes.class );

	void showHqlAst(AST hqlAst) {
		if ( LOG.isDebugEnabled() ) {
			LOG.debug( AQL_TOKEN_PRINTER.showAsString( hqlAst, "--- AQL AST ---" ) );
		}
	}

	private void errorIfDML() throws HibernateException {
		if ( sqlAst.needsExecutor() ) {
			throw new QueryExecutionRequestException( "Not supported for DML operations", hql );
		}
	}

	private void errorIfSelect() throws HibernateException {
		if ( !sqlAst.needsExecutor() ) {
			throw new QueryExecutionRequestException( "Not supported for select queries", hql );
		}
	}

	public String getQueryIdentifier() {
		return queryIdentifier;
	}

	public Statement getSqlAST() {
		return sqlAst;
	}

	private AqlSqlWalker getWalker() {
		return sqlAst.getWalker();
	}

	/**
	 * Types of the return values of an <tt>iterate()</tt> style query.
	 *
	 * @return an array of <tt>Type</tt>s.
	 */
	public Type[] getReturnTypes() {
		errorIfDML();
		return getWalker().getReturnTypes();
	}

	public String[] getReturnAliases() {
		errorIfDML();
		return getWalker().getReturnAliases();
	}

	public String[][] getColumnNames() {
		errorIfDML();
		return getWalker().getSelectClause().getColumnNames();
	}

	public Set getQuerySpaces() {
		return getWalker().getQuerySpaces();
	}

	public List list(SessionImplementor session, QueryParameters queryParameters)
			throws HibernateException {
		// Delegate to the QueryLoader...
		errorIfDML();
		QueryNode query = ( QueryNode ) sqlAst;
		boolean hasLimit = queryParameters.getRowSelection() != null && queryParameters.getRowSelection().definesLimits();
		boolean needsDistincting = ( query.getSelectClause().isDistinct() || hasLimit ) && containsCollectionFetches();

		QueryParameters queryParametersToUse;
		if ( hasLimit && containsCollectionFetches() ) {
			LOG.firstOrMaxResultsSpecifiedWithCollectionFetch();
			RowSelection selection = new RowSelection();
			selection.setFetchSize( queryParameters.getRowSelection().getFetchSize() );
			selection.setTimeout( queryParameters.getRowSelection().getTimeout() );
			queryParametersToUse = queryParameters.createCopyUsing( selection );
		}
		else {
			queryParametersToUse = queryParameters;
		}

		List results = queryLoader.list( session, queryParametersToUse );

		if ( needsDistincting ) {
			int includedCount = -1;
			// NOTE : firstRow is zero-based
			int first = !hasLimit || queryParameters.getRowSelection().getFirstRow() == null
						? 0
						: queryParameters.getRowSelection().getFirstRow();
			int max = !hasLimit || queryParameters.getRowSelection().getMaxRows() == null
						? -1
						: queryParameters.getRowSelection().getMaxRows();
			List tmp = new ArrayList();
			IdentitySet distinction = new IdentitySet();
			for ( final Object result : results ) {
				if ( !distinction.add( result ) ) {
					continue;
				}
				includedCount++;
				if ( includedCount < first ) {
					continue;
				}
				tmp.add( result );
				// NOTE : ( max - 1 ) because first is zero-based while max is not...
				if ( max >= 0 && ( includedCount - first ) >= ( max - 1 ) ) {
					break;
				}
			}
			results = tmp;
		}

		return results;
	}

	/**
	 * Return the query results as an iterator
	 */
	public Iterator iterate(QueryParameters queryParameters, EventSource session)
			throws HibernateException {
		// Delegate to the QueryLoader...
		errorIfDML();
		return queryLoader.iterate( queryParameters, session );
	}

	/**
	 * Return the query results, as an instance of <tt>ScrollableResults</tt>
	 */
	public ScrollableResults scroll(QueryParameters queryParameters, SessionImplementor session)
			throws HibernateException {
		// Delegate to the QueryLoader...
		errorIfDML();
		return queryLoader.scroll( queryParameters, session );
	}

	public int executeUpdate(QueryParameters queryParameters, SessionImplementor session)
			throws HibernateException {
		errorIfSelect();
		return statementExecutor.execute( queryParameters, session );
	}

	/**
	 * The SQL query string to be called; implemented by all subclasses
	 */
	public String getSQLString() {
		return sql;
	}

	public List<String> collectSqlStrings() {
		ArrayList<String> list = new ArrayList<String>();
		if ( isManipulationStatement() ) {
			String[] sqlStatements = statementExecutor.getSqlStatements();
			for ( int i = 0; i < sqlStatements.length; i++ ) {
				list.add( sqlStatements[i] );
			}
		}
		else {
			list.add( sql );
		}
		return list;
	}

	// -- Package local methods for the QueryLoader delegate --

	public boolean isShallowQuery() {
		return shallowQuery;
	}

	public String getQueryString() {
		return hql;
	}

	public Map getEnabledFilters() {
		return enabledFilters;
	}

	public int[] getNamedParameterLocs(String name) {
		return getWalker().getNamedParameterLocations( name );
	}

	public boolean containsCollectionFetches() {
		errorIfDML();
		List collectionFetches = ( ( QueryNode ) sqlAst ).getFromClause().getCollectionFetches();
		return collectionFetches != null && collectionFetches.size() > 0;
	}

	public boolean isManipulationStatement() {
		return sqlAst.needsExecutor();
	}

	public void validateScrollability() throws HibernateException {
		// Impl Note: allows multiple collection fetches as long as the
		// entire fecthed graph still "points back" to a single
		// root entity for return

		errorIfDML();

		QueryNode query = ( QueryNode ) sqlAst;

		// If there are no collection fetches, then no further checks are needed
		List collectionFetches = query.getFromClause().getCollectionFetches();
		if ( collectionFetches.isEmpty() ) {
			return;
		}

		// A shallow query is ok (although technically there should be no fetching here...)
		if ( isShallowQuery() ) {
			return;
		}

		// Otherwise, we have a non-scalar select with defined collection fetch(es).
		// Make sure that there is only a single root entity in the return (no tuples)
		if ( getReturnTypes().length > 1 ) {
			throw new HibernateException( "cannot scroll with collection fetches and returned tuples" );
		}

		FromElement owner = null;
		Iterator itr = query.getSelectClause().getFromElementsForLoad().iterator();
		while ( itr.hasNext() ) {
			// should be the first, but just to be safe...
			final FromElement fromElement = ( FromElement ) itr.next();
			if ( fromElement.getOrigin() == null ) {
				owner = fromElement;
				break;
			}
		}

		if ( owner == null ) {
			throw new HibernateException( "unable to locate collection fetch(es) owner for scrollability checks" );
		}

		// This is not strictly true.  We actually just need to make sure that
		// it is ordered by root-entity PK and that that order-by comes before
		// any non-root-entity ordering...

		AST primaryOrdering = query.getOrderByClause().getFirstChild();
		if ( primaryOrdering != null ) {
			// TODO : this is a bit dodgy, come up with a better way to check this (plus see above comment)
			String [] idColNames = owner.getQueryable().getIdentifierColumnNames();
			String expectedPrimaryOrderSeq = StringHelper.join(
			        ", ",
			        StringHelper.qualify( owner.getTableAlias(), idColNames )
			);
			if (  !primaryOrdering.getText().startsWith( expectedPrimaryOrderSeq ) ) {
				throw new HibernateException( "cannot scroll results with collection fetches which are not ordered primarily by the root entity's PK" );
			}
		}
	}

	private StatementExecutor buildAppropriateStatementExecutor(AqlSqlWalker walker) {
		Statement statement = ( Statement ) walker.getAST();
		if ( walker.getStatementType() == HqlSqlTokenTypes.DELETE ) {
			FromElement fromElement = walker.getFinalFromClause().getFromElement();
			Queryable persister = fromElement.getQueryable();
			if ( persister.isMultiTable() ) {
				return new MultiTableDeleteExecutor( walker );
			}
			else {
				return new BasicExecutor( walker, persister );
			}
		}
		else if ( walker.getStatementType() == HqlSqlTokenTypes.UPDATE ) {
			FromElement fromElement = walker.getFinalFromClause().getFromElement();
			Queryable persister = fromElement.getQueryable();
			if ( persister.isMultiTable() ) {
				// even here, if only properties mapped to the "base table" are referenced
				// in the set and where clauses, this could be handled by the BasicDelegate.
				// TODO : decide if it is better performance-wise to doAfterTransactionCompletion that check, or to simply use the MultiTableUpdateDelegate
				return new MultiTableUpdateExecutor( walker );
			}
			else {
				return new BasicExecutor( walker, persister );
			}
		}
		else if ( walker.getStatementType() == HqlSqlTokenTypes.INSERT ) {
			return new BasicExecutor( walker, ( ( InsertStatement ) statement ).getIntoClause().getQueryable() );
		}
		else {
			throw new QueryException( "Unexpected statement type" );
		}
	}

	public ParameterTranslations getParameterTranslations() {
		if ( paramTranslations == null ) {
			paramTranslations = new ParameterTranslationsImpl( getWalker().getParameters() );
//			paramTranslations = new ParameterTranslationsImpl( collectedParameterSpecifications );
		}
		return paramTranslations;
	}

	public List getCollectedParameterSpecifications() {
		return collectedParameterSpecifications;
	}

	@Override
	public Class getDynamicInstantiationResultType() {
		AggregatedSelectExpression aggregation = queryLoader.getAggregatedSelectExpression();
		return aggregation == null ? null : aggregation.getAggregationResultType();
	}

	public static class JavaConstantConverter implements NodeTraverser.VisitationStrategy {
		private AST dotRoot;
		public void visit(AST node) {
			if ( dotRoot != null ) {
				// we are already processing a dot-structure
                if (ASTUtil.isSubtreeChild(dotRoot, node)) return;
                // we are now at a new tree level
                dotRoot = null;
			}

			if ( dotRoot == null && node.getType() == HqlTokenTypes.DOT ) {
				dotRoot = node;
				handleDotStructure( dotRoot );
			}
		}
		private void handleDotStructure(AST dotStructureRoot) {
			String expression = ASTUtil.getPathText( dotStructureRoot );
			Object constant = ReflectHelper.getConstantValue( expression );
			if ( constant != null ) {
				dotStructureRoot.setFirstChild( null );
				dotStructureRoot.setType( HqlTokenTypes.JAVA_CONSTANT );
				dotStructureRoot.setText( expression );
			}
		}
	}
}
