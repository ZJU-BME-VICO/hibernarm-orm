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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.junit.Test;
import org.openehr.am.archetype.Archetype;
import org.openehr.am.parser.ContentObject;
import org.openehr.am.parser.DADLParser;
import org.openehr.am.parser.ParseException;
import org.openehr.build.RMObjectBuildingException;
import org.openehr.rm.binding.DADLBinding;
import org.openehr.rm.binding.DADLBindingException;
import org.openehr.rm.common.archetyped.Locatable;
import org.openehr.rm.composition.content.entry.Observation;
import org.openehr.rm.support.identification.HierObjectID;
import org.openehr.rm.util.GenerationStrategy;
import org.openehr.rm.util.SkeletonGenerator;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.aql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.archetype.ArchetypeRepository;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.transform.Transformers;

/**
 * Tests the integration of the new AST parser into the loading of query results
 * using the Hibernate persisters and loaders.
 * <p/>
 * Also used to test the syntax of the resulting sql against the underlying
 * database, specifically for functionality not supported by the classic parser.
 * 
 * @author Steve
 */
@SkipForDialect(value = CUBRIDDialect.class, comment = "As of verion 8.4.1 CUBRID doesn't support temporary tables. This test fails with"
		+ "HibernateException: cannot doAfterTransactionCompletion multi-table deletes using dialect not supporting temp tables")
public class ASTParserLoadingTest extends BaseCoreFunctionalTestCase {
	private static final Logger log = Logger
			.getLogger(ASTParserLoadingTest.class);

	@Override
	protected boolean isCleanupTestDataRequired() {
		return false;
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure(cfg);
		cfg.setProperty(Environment.USE_QUERY_CACHE, "true");
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
		cfg.setProperty(Environment.AQL_QUERY_TRANSLATOR,
				ASTQueryTranslatorFactory.class.getName());
	}

	@Override
	protected String[] getXmlFiles() {
		return new String[] {
				"org/hibernate/test/annotations/xml/arm/openEHR-EHR-OBSERVATION.blood_pressure.v1.arm.xml",
				"org/hibernate/test/annotations/xml/arm/openEHR-DEMOGRAPHIC-PERSON.patient.v1.arm.xml",
				"org/hibernate/test/annotations/xml/arm/openEHR-EHR-COMPOSITION.visit.v3.arm.xml",
				"org/hibernate/test/annotations/xml/arm/ad/openEHR-EHR-OBSERVATION.adl.v1.arm.xml",
				"org/hibernate/test/annotations/xml/arm/ad/openEHR-EHR-OBSERVATION.cdr.v1.arm.xml",
				"org/hibernate/test/annotations/xml/arm/ad/openEHR-EHR-OBSERVATION.gds.v1.arm.xml",
				"org/hibernate/test/annotations/xml/arm/ad/openEHR-EHR-OBSERVATION.mmse.v1.arm.xml", };
	}

	@Override
	protected String[] getAdlFiles() {
		return new String[] {
				"org/hibernate/test/annotations/xml/arm/openEHR-EHR-OBSERVATION.blood_pressure.v1.adl",
				"org/hibernate/test/annotations/xml/arm/openEHR-DEMOGRAPHIC-PERSON.patient.v1.adl",
				"org/hibernate/test/annotations/xml/arm/openEHR-EHR-COMPOSITION.visit.v3.adl",
				"org/hibernate/test/annotations/xml/arm/ad/openEHR-EHR-OBSERVATION.adl.v1.adl",
				"org/hibernate/test/annotations/xml/arm/ad/openEHR-EHR-OBSERVATION.cdr.v1.adl",
				"org/hibernate/test/annotations/xml/arm/ad/openEHR-EHR-OBSERVATION.gds.v1.adl",
				"org/hibernate/test/annotations/xml/arm/ad/openEHR-EHR-OBSERVATION.mmse.v1.adl", };
	}

	protected String[] getDadlFiles() {
		return new String[] {
				"org/hibernate/test/annotations/xml/arm/openEHR-EHR-OBSERVATION.blood_pressure.v1.1.dadl",
				"org/hibernate/test/annotations/xml/arm/openEHR-EHR-OBSERVATION.blood_pressure.v1.2.dadl", };
	}

	protected Map<HashMap<String, Object>, String> getArchetypeValues() {
		Map<HashMap<String, Object>, String> results = new HashMap<HashMap<String, Object>, String>();
		
		HashMap<String, Object> patient1 = new HashMap<String, Object>();
		patient1.put("/uid/value", "patient1");
		patient1.put("/details[at0001]/items[at0003]/value/value", "M");
		patient1.put("/details[at0001]/items[at0004]/value/value", "1984-08-11T19:20:30+08:00");
		patient1.put("/details[at0001]/items[at0009]/value/value", "zhangsan");
		results.put(patient1, "openEHR-DEMOGRAPHIC-PERSON.patient.v1");
		
		HashMap<String, Object> patient2 = new HashMap<String, Object>();
		patient2.put("/uid/value", "patient2");
		patient2.put("/details[at0001]/items[at0003]/value/value", "F");
		patient2.put("/details[at0001]/items[at0004]/value/value", "1986-08-11T19:20:30+08:00");
		patient2.put("/details[at0001]/items[at0009]/value/value", "lisi");
		results.put(patient2, "openEHR-DEMOGRAPHIC-PERSON.patient.v1");
		
		HashMap<String, Object> patient3 = new HashMap<String, Object>();
		patient3.put("/uid/value", "patient3");
		patient3.put("/details[at0001]/items[at0003]/value/value", "O");
		patient3.put("/details[at0001]/items[at0004]/value/value", "1988-08-11T19:20:30+08:00");
		patient3.put("/details[at0001]/items[at0009]/value/value", "wangwu");
		results.put(patient3, "openEHR-DEMOGRAPHIC-PERSON.patient.v1");
		
		return results;
	}

	private void createTestBaseData() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();

		for (String dadl : getDadlFiles()) {
			InputStream is = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(dadl);
			DADLParser parser = new DADLParser(is);
			ContentObject contentObj = parser.parse();
			DADLBinding binding = new DADLBinding();
			Observation bp = (Observation) binding.bind(contentObj);
			UUID uuid = UUID.randomUUID();
			HierObjectID uid = new HierObjectID(uuid.toString());
			bp.setUid(uid);
			s.save(bp);
		}

		Map<HashMap<String, Object>, String> archetypeValues = getArchetypeValues();
		for (HashMap<String, Object> values : archetypeValues.keySet()) {
			SkeletonGenerator generator = SkeletonGenerator.getInstance();
			Archetype archetype = ArchetypeRepository.getArchetype(archetypeValues.get(values));
			Object result = generator.create(archetype,
					GenerationStrategy.MAXIMUM_EMPTY);
			if (result instanceof Locatable) {
				Locatable loc = (Locatable) result;
				ReflectHelper.setArchetypeValue(archetype, loc, values);
				s.save(loc);
			}
		}

		s.flush();
		txn.commit();
		s.close();
	}

	private void destroyTestBaseData() {
		// Session session = openSession();
		// Transaction txn = session.beginTransaction();
		//
		// for ( Long createdAnimalId : createdAnimalIds ) {
		// Animal animal = (Animal) session.load( Animal.class, createdAnimalId
		// );
		// session.delete( animal );
		// }
		//
		// txn.commit();
		// session.close();
		//
		// createdAnimalIds.clear();
	}

	@Test
	public void testSimpleSelect() throws Exception {
		// createTestBaseData();
		// Session session = openSession();
		// List results = session.createAQLQuery( "select a from Animal as a"
		// ).list();
		// assertEquals( "Incorrect result size", 2, results.size() );
		// assertTrue( "Incorrect result return type", results.get( 0 )
		// instanceof Animal );
		// session.close();
		// destroyTestBaseData();
	}

	@Test
	public void testEntityPropertySelect() throws Exception {
		createTestBaseData();
		
		Session s = openSession();
		
		{
			String query = "select "
					+ "o#/uid/value as /uid/value, "
					+ "o#/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude as /data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude, "
					+ "o#/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude as /data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude "
					+ "from openEHR-EHR-OBSERVATION.blood_pressure.v1 as o";
			String archetypeId = "openEHR-EHR-OBSERVATION.blood_pressure.v1";
			List results = s
					.createAQLQuery(query)
					.setResultTransformer(
							Transformers.aliasToArchetype(archetypeId)).listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}			
		}
		
		{
			String query = "select "
					+ "o#/uid/value as /uid/value, "
					+ "o#/details[at0001]/items[at0003]/value/value as /details[at0001]/items[at0003]/value/value, "
					+ "o#/details[at0001]/items[at0004]/value/value as /details[at0001]/items[at0004]/value/value, "
					+ "o#/details[at0001]/items[at0009]/value/value as /details[at0001]/items[at0009]/value/value "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o";
			String archetypeId = "openEHR-DEMOGRAPHIC-PERSON.patient.v1";
			List results = s
					.createAQLQuery(query)
					.setResultTransformer(
							Transformers.aliasToArchetype(archetypeId)).listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}			
		}
		
		s.close();

		destroyTestBaseData();
	}
}
