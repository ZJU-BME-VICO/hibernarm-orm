/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg;

import java.util.Map;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.EntityMode;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.NullPrecedence;
import org.hibernate.cache.spi.QueryCacheFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.tuple.entity.EntityTuplizerFactory;

/**
 * Settings that affect the behaviour of Hibernate at runtime.
 *
 * @author Gavin King
 */
public final class Settings {

	private Integer maximumFetchDepth;
	private Map querySubstitutions;
	private int jdbcBatchSize;
	private int defaultBatchFetchSize;
	private boolean scrollableResultSetsEnabled;
	private boolean getGeneratedKeysEnabled;
	private String defaultSchemaName;
	private String defaultCatalogName;
	private Integer jdbcFetchSize;
	private String sessionFactoryName;
	private boolean sessionFactoryNameAlsoJndiName;
	private boolean autoCreateSchema;
	private boolean autoDropSchema;
	private boolean autoUpdateSchema;
	private boolean autoValidateSchema;
	private boolean queryCacheEnabled;
	private boolean structuredCacheEntriesEnabled;
	private boolean secondLevelCacheEnabled;
	private String cacheRegionPrefix;
	private boolean minimalPutsEnabled;
	private boolean commentsEnabled;
	private boolean statisticsEnabled;
	private boolean jdbcBatchVersionedData;
	private boolean identifierRollbackEnabled;
	private boolean flushBeforeCompletionEnabled;
	private boolean autoCloseSessionEnabled;
	private ConnectionReleaseMode connectionReleaseMode;
	private RegionFactory regionFactory;
	private QueryCacheFactory queryCacheFactory;
	private org.hibernate.hql.spi.QueryTranslatorFactory queryTranslatorFactory;
	private org.hibernate.aql.spi.QueryTranslatorFactory aqlQueryTranslatorFactory;
	private boolean wrapResultSetsEnabled;
	private boolean orderUpdatesEnabled;
	private boolean orderInsertsEnabled;
	private EntityMode defaultEntityMode;
	private boolean dataDefinitionImplicitCommit;
	private boolean dataDefinitionInTransactionSupported;
	private boolean strictJPAQLCompliance;
	private boolean namedQueryStartupCheckingEnabled;
	private EntityTuplizerFactory entityTuplizerFactory;
	private boolean checkNullability;
	private NullPrecedence defaultNullPrecedence;
	private boolean initializeLazyStateOutsideTransactions;
//	private ComponentTuplizerFactory componentTuplizerFactory; todo : HHH-3517 and HHH-1907
//	private BytecodeProvider bytecodeProvider;
	private String importFiles;
	private MultiTenancyStrategy multiTenancyStrategy;

	private JtaPlatform jtaPlatform;

	private org.hibernate.hql.spi.MultiTableBulkIdStrategy multiTableBulkIdStrategy;
	private org.hibernate.aql.spi.MultiTableBulkIdStrategy aqlMultiTableBulkIdStrategy;
	private BatchFetchStyle batchFetchStyle;
	private boolean directReferenceCacheEntriesEnabled;
	
	private boolean jtaTrackByThread;


	/**
	 * Package protected constructor
	 */
	Settings() {
	}

	// public getters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public String getImportFiles() {
		return importFiles;
	}

	public void setImportFiles(String importFiles) {
		this.importFiles = importFiles;
	}

	public String getDefaultSchemaName() {
		return defaultSchemaName;
	}

	public String getDefaultCatalogName() {
		return defaultCatalogName;
	}

	public int getJdbcBatchSize() {
		return jdbcBatchSize;
	}

	public int getDefaultBatchFetchSize() {
		return defaultBatchFetchSize;
	}

	public Map getQuerySubstitutions() {
		return querySubstitutions;
	}

	public boolean isIdentifierRollbackEnabled() {
		return identifierRollbackEnabled;
	}

	public boolean isScrollableResultSetsEnabled() {
		return scrollableResultSetsEnabled;
	}

	public boolean isGetGeneratedKeysEnabled() {
		return getGeneratedKeysEnabled;
	}

	public boolean isMinimalPutsEnabled() {
		return minimalPutsEnabled;
	}

	public Integer getJdbcFetchSize() {
		return jdbcFetchSize;
	}

	public String getSessionFactoryName() {
		return sessionFactoryName;
	}

	public boolean isSessionFactoryNameAlsoJndiName() {
		return sessionFactoryNameAlsoJndiName;
	}

	public boolean isAutoCreateSchema() {
		return autoCreateSchema;
	}

	public boolean isAutoDropSchema() {
		return autoDropSchema;
	}

	public boolean isAutoUpdateSchema() {
		return autoUpdateSchema;
	}

	public Integer getMaximumFetchDepth() {
		return maximumFetchDepth;
	}

	public RegionFactory getRegionFactory() {
		return regionFactory;
	}

	public boolean isQueryCacheEnabled() {
		return queryCacheEnabled;
	}

	public boolean isCommentsEnabled() {
		return commentsEnabled;
	}

	public boolean isSecondLevelCacheEnabled() {
		return secondLevelCacheEnabled;
	}

	public String getCacheRegionPrefix() {
		return cacheRegionPrefix;
	}

	public QueryCacheFactory getQueryCacheFactory() {
		return queryCacheFactory;
	}

	public boolean isStatisticsEnabled() {
		return statisticsEnabled;
	}

	public boolean isJdbcBatchVersionedData() {
		return jdbcBatchVersionedData;
	}

	public boolean isFlushBeforeCompletionEnabled() {
		return flushBeforeCompletionEnabled;
	}

	public boolean isAutoCloseSessionEnabled() {
		return autoCloseSessionEnabled;
	}

	public ConnectionReleaseMode getConnectionReleaseMode() {
		return connectionReleaseMode;
	}

	public org.hibernate.hql.spi.QueryTranslatorFactory getQueryTranslatorFactory() {
		return queryTranslatorFactory;
	}

	public org.hibernate.aql.spi.QueryTranslatorFactory getAqlQueryTranslatorFactory() {
		return aqlQueryTranslatorFactory;
	}

	public boolean isWrapResultSetsEnabled() {
		return wrapResultSetsEnabled;
	}

	public boolean isOrderUpdatesEnabled() {
		return orderUpdatesEnabled;
	}

	public boolean isOrderInsertsEnabled() {
		return orderInsertsEnabled;
	}

	public boolean isStructuredCacheEntriesEnabled() {
		return structuredCacheEntriesEnabled;
	}

	public boolean isDirectReferenceCacheEntriesEnabled() {
		return directReferenceCacheEntriesEnabled;
	}

	public EntityMode getDefaultEntityMode() {
		return defaultEntityMode;
	}

	public boolean isAutoValidateSchema() {
		return autoValidateSchema;
	}

	public boolean isDataDefinitionImplicitCommit() {
		return dataDefinitionImplicitCommit;
	}

	public boolean isDataDefinitionInTransactionSupported() {
		return dataDefinitionInTransactionSupported;
	}

	public boolean isStrictJPAQLCompliance() {
		return strictJPAQLCompliance;
	}

	public boolean isNamedQueryStartupCheckingEnabled() {
		return namedQueryStartupCheckingEnabled;
	}

	public EntityTuplizerFactory getEntityTuplizerFactory() {
		return entityTuplizerFactory;
	}

//	public ComponentTuplizerFactory getComponentTuplizerFactory() {
//		return componentTuplizerFactory;
//	}

	public NullPrecedence getDefaultNullPrecedence() {
		return defaultNullPrecedence;
	}

	// package protected setters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	void setDefaultSchemaName(String string) {
		defaultSchemaName = string;
	}

	void setDefaultCatalogName(String string) {
		defaultCatalogName = string;
	}

	void setJdbcBatchSize(int i) {
		jdbcBatchSize = i;
	}

	void setDefaultBatchFetchSize(int i) {
		defaultBatchFetchSize = i;
	}

	void setQuerySubstitutions(Map map) {
		querySubstitutions = map;
	}

	void setIdentifierRollbackEnabled(boolean b) {
		identifierRollbackEnabled = b;
	}

	void setMinimalPutsEnabled(boolean b) {
		minimalPutsEnabled = b;
	}

	void setScrollableResultSetsEnabled(boolean b) {
		scrollableResultSetsEnabled = b;
	}

	void setGetGeneratedKeysEnabled(boolean b) {
		getGeneratedKeysEnabled = b;
	}

	void setJdbcFetchSize(Integer integer) {
		jdbcFetchSize = integer;
	}

	void setSessionFactoryName(String string) {
		sessionFactoryName = string;
	}

	void setSessionFactoryNameAlsoJndiName(boolean sessionFactoryNameAlsoJndiName) {
		this.sessionFactoryNameAlsoJndiName = sessionFactoryNameAlsoJndiName;
	}

	void setAutoCreateSchema(boolean b) {
		autoCreateSchema = b;
	}

	void setAutoDropSchema(boolean b) {
		autoDropSchema = b;
	}

	void setAutoUpdateSchema(boolean b) {
		autoUpdateSchema = b;
	}

	void setMaximumFetchDepth(Integer i) {
		maximumFetchDepth = i;
	}

	void setRegionFactory(RegionFactory regionFactory) {
		this.regionFactory = regionFactory;
	}

	void setQueryCacheEnabled(boolean b) {
		queryCacheEnabled = b;
	}

	void setCommentsEnabled(boolean commentsEnabled) {
		this.commentsEnabled = commentsEnabled;
	}

	void setSecondLevelCacheEnabled(boolean secondLevelCacheEnabled) {
		this.secondLevelCacheEnabled = secondLevelCacheEnabled;
	}

	void setCacheRegionPrefix(String cacheRegionPrefix) {
		this.cacheRegionPrefix = cacheRegionPrefix;
	}

	void setQueryCacheFactory(QueryCacheFactory queryCacheFactory) {
		this.queryCacheFactory = queryCacheFactory;
	}

	void setStatisticsEnabled(boolean statisticsEnabled) {
		this.statisticsEnabled = statisticsEnabled;
	}

	void setJdbcBatchVersionedData(boolean jdbcBatchVersionedData) {
		this.jdbcBatchVersionedData = jdbcBatchVersionedData;
	}

	void setFlushBeforeCompletionEnabled(boolean flushBeforeCompletionEnabled) {
		this.flushBeforeCompletionEnabled = flushBeforeCompletionEnabled;
	}

	void setAutoCloseSessionEnabled(boolean autoCloseSessionEnabled) {
		this.autoCloseSessionEnabled = autoCloseSessionEnabled;
	}

	void setConnectionReleaseMode(ConnectionReleaseMode connectionReleaseMode) {
		this.connectionReleaseMode = connectionReleaseMode;
	}

	void setQueryTranslatorFactory(org.hibernate.hql.spi.QueryTranslatorFactory queryTranslatorFactory) {
		this.queryTranslatorFactory = queryTranslatorFactory;
	}

	void setAqlQueryTranslatorFactory(org.hibernate.aql.spi.QueryTranslatorFactory queryTranslatorFactory) {
		this.aqlQueryTranslatorFactory = queryTranslatorFactory;
	}

	void setWrapResultSetsEnabled(boolean wrapResultSetsEnabled) {
		this.wrapResultSetsEnabled = wrapResultSetsEnabled;
	}

	void setOrderUpdatesEnabled(boolean orderUpdatesEnabled) {
		this.orderUpdatesEnabled = orderUpdatesEnabled;
	}

	void setOrderInsertsEnabled(boolean orderInsertsEnabled) {
		this.orderInsertsEnabled = orderInsertsEnabled;
	}

	void setStructuredCacheEntriesEnabled(boolean structuredCacheEntriesEnabled) {
		this.structuredCacheEntriesEnabled = structuredCacheEntriesEnabled;
	}

	void setDefaultEntityMode(EntityMode defaultEntityMode) {
		this.defaultEntityMode = defaultEntityMode;
	}

	void setAutoValidateSchema(boolean autoValidateSchema) {
		this.autoValidateSchema = autoValidateSchema;
	}

	void setDataDefinitionImplicitCommit(boolean dataDefinitionImplicitCommit) {
		this.dataDefinitionImplicitCommit = dataDefinitionImplicitCommit;
	}

	void setDataDefinitionInTransactionSupported(boolean dataDefinitionInTransactionSupported) {
		this.dataDefinitionInTransactionSupported = dataDefinitionInTransactionSupported;
	}

	void setStrictJPAQLCompliance(boolean strictJPAQLCompliance) {
		this.strictJPAQLCompliance = strictJPAQLCompliance;
	}

	void setNamedQueryStartupCheckingEnabled(boolean namedQueryStartupCheckingEnabled) {
		this.namedQueryStartupCheckingEnabled = namedQueryStartupCheckingEnabled;
	}

	void setEntityTuplizerFactory(EntityTuplizerFactory entityTuplizerFactory) {
		this.entityTuplizerFactory = entityTuplizerFactory;
	}

	public boolean isCheckNullability() {
		return checkNullability;
	}

	public void setCheckNullability(boolean checkNullability) {
		this.checkNullability = checkNullability;
	}

	//	void setComponentTuplizerFactory(ComponentTuplizerFactory componentTuplizerFactory) {
//		this.componentTuplizerFactory = componentTuplizerFactory;
//	}

	//	public BytecodeProvider getBytecodeProvider() {
//		return bytecodeProvider;
//	}
//
//	void setBytecodeProvider(BytecodeProvider bytecodeProvider) {
//		this.bytecodeProvider = bytecodeProvider;
//	}


	public JtaPlatform getJtaPlatform() {
		return jtaPlatform;
	}

	void setJtaPlatform(JtaPlatform jtaPlatform) {
		this.jtaPlatform = jtaPlatform;
	}

	public MultiTenancyStrategy getMultiTenancyStrategy() {
		return multiTenancyStrategy;
	}

	void setMultiTenancyStrategy(MultiTenancyStrategy multiTenancyStrategy) {
		this.multiTenancyStrategy = multiTenancyStrategy;
	}

	public boolean isInitializeLazyStateOutsideTransactionsEnabled() {
		return initializeLazyStateOutsideTransactions;
	}

	void setInitializeLazyStateOutsideTransactions(boolean initializeLazyStateOutsideTransactions) {
		this.initializeLazyStateOutsideTransactions = initializeLazyStateOutsideTransactions;
	}

	public org.hibernate.hql.spi.MultiTableBulkIdStrategy getMultiTableBulkIdStrategy() {
		return multiTableBulkIdStrategy;
	}

	public org.hibernate.aql.spi.MultiTableBulkIdStrategy getAqlMultiTableBulkIdStrategy() {
		return aqlMultiTableBulkIdStrategy;
	}

	void setMultiTableBulkIdStrategy(org.hibernate.hql.spi.MultiTableBulkIdStrategy multiTableBulkIdStrategy) {
		this.multiTableBulkIdStrategy = multiTableBulkIdStrategy;
	}

	void setAqlMultiTableBulkIdStrategy(org.hibernate.aql.spi.MultiTableBulkIdStrategy multiTableBulkIdStrategy) {
		this.aqlMultiTableBulkIdStrategy = multiTableBulkIdStrategy;
	}

	public BatchFetchStyle getBatchFetchStyle() {
		return batchFetchStyle;
	}

	void setBatchFetchStyle(BatchFetchStyle batchFetchStyle) {
		this.batchFetchStyle = batchFetchStyle;
	}

	public void setDirectReferenceCacheEntriesEnabled(boolean directReferenceCacheEntriesEnabled) {
		this.directReferenceCacheEntriesEnabled = directReferenceCacheEntriesEnabled;
	}

	void setDefaultNullPrecedence(NullPrecedence defaultNullPrecedence) {
		this.defaultNullPrecedence = defaultNullPrecedence;
	}

	public boolean isJtaTrackByThread() {
		return jtaTrackByThread;
	}

	public void setJtaTrackByThread(boolean jtaTrackByThread) {
		this.jtaTrackByThread = jtaTrackByThread;
	}
}
