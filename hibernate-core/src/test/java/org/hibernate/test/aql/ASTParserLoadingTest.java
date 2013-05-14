/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.aql;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.junit.Test;
import org.openehr.am.parser.ContentObject;
import org.openehr.am.parser.DADLParser;
import org.openehr.am.parser.ParseException;
import org.openehr.build.RMObjectBuildingException;
import org.openehr.rm.binding.DADLBinding;
import org.openehr.rm.binding.DADLBindingException;
import org.openehr.rm.composition.content.entry.Observation;
import org.openehr.rm.support.identification.HierObjectID;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.aql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.transform.Transformers;

/**
 * Tests the integration of the new AST parser into the loading of query results using
 * the Hibernate persisters and loaders.
 * <p/>
 * Also used to test the syntax of the resulting sql against the underlying
 * database, specifically for functionality not supported by the classic
 * parser.
 *
 * @author Steve
 */
@SkipForDialect(
        value = CUBRIDDialect.class,
        comment = "As of verion 8.4.1 CUBRID doesn't support temporary tables. This test fails with" +
                "HibernateException: cannot doAfterTransactionCompletion multi-table deletes using dialect not supporting temp tables"
)
public class ASTParserLoadingTest extends BaseCoreFunctionalTestCase {
	private static final Logger log = Logger.getLogger( ASTParserLoadingTest.class );
	
	@Override
	protected boolean isCleanupTestDataRequired() {
		return false;
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.AQL_QUERY_TRANSLATOR, ASTQueryTranslatorFactory.class.getName() );
	}

	@Override
	protected String[] getXmlFiles() {
		return new String[]{
				"org/hibernate/test/annotations/xml/arm/openEHR-EHR-OBSERVATION.blood_pressure.v1.arm.xml",
		};
	}
	
	@Override
	protected String[] getAdlFiles() {
		return new String[]{
				"org/hibernate/test/annotations/xml/arm/openEHR-EHR-OBSERVATION.blood_pressure.v1.adl",
		};
	}
	
	protected String[] getDadlFiles() {
		return new String[]{
				"org/hibernate/test/annotations/xml/arm/openEHR-EHR-OBSERVATION.blood_pressure.v1.dadl",
		};
	}

	private void createTestBaseData() throws ParseException, DADLBindingException, RMObjectBuildingException {
		Session s = openSession();
		Transaction txn = session.beginTransaction();
		
		for (String dadl : getDadlFiles()) {
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(dadl);
			DADLParser parser = new DADLParser(is);
			ContentObject contentObj = parser.parse();
			DADLBinding binding = new DADLBinding();
			Observation bp = (Observation) binding.bind(contentObj);
			UUID uuid = UUID.randomUUID();
			HierObjectID uid = new HierObjectID(uuid.toString());
			bp.setUid(uid);
			s.save(bp);
		}
		
		s.flush();
		txn.commit();
		s.close();
	}

	private void destroyTestBaseData() {
//		Session session = openSession();
//		Transaction txn = session.beginTransaction();
//
//		for ( Long createdAnimalId : createdAnimalIds ) {
//			Animal animal = (Animal) session.load( Animal.class, createdAnimalId );
//			session.delete( animal );
//		}
//
//		txn.commit();
//		session.close();
//
//		createdAnimalIds.clear();
	}

	@Test
	public void testSimpleSelect() throws Exception {
//		createTestBaseData();
//		Session session = openSession();
//		List results = session.createAQLQuery( "select a from Animal as a" ).list();
//		assertEquals( "Incorrect result size", 2, results.size() );
//		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Animal );
//		session.close();
//		destroyTestBaseData();
	}

	@Test
	public void testEntityPropertySelect() throws Exception {
		createTestBaseData();
		Session session = openSession();
		String query = "select " + 
				"o#/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude as /data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude, " + 
				"o#/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude as /data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude " + 
				"from openEHR-EHR-OBSERVATION.blood_pressure.v1 as o";
		String archetypeId = "openEHR-EHR-OBSERVATION.blood_pressure.v1";
		List results = session.createAQLQuery(query)
				.setResultTransformer(Transformers.aliasToArchetype(archetypeId))
				.listAQL();
		session.close();
		destroyTestBaseData();

		DADLBinding binding = new DADLBinding();
		for (Object obj : results) {
			System.out.println(binding.toDADL(obj));
		}
	}
}
