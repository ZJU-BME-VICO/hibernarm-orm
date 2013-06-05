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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.junit.Test;
import org.openehr.am.archetype.Archetype;
import org.openehr.am.parser.ContentObject;
import org.openehr.am.parser.DADLParser;
import org.openehr.rm.binding.DADLBinding;
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
	protected String[] getAdlFiles() {
		return new String[] {
				"../../CDRDocument/knowledge/archetype/CKM/entry/observation/openEHR-EHR-OBSERVATION.blood_pressure.v1.adl",
				"../../CDRDocument/knowledge/archetype/ZJU/openEHR-DEMOGRAPHIC-PERSON.patient.v1.adl",
				"../../CDRDocument/knowledge/archetype/ZJU/openEHR-EHR-COMPOSITION.visit.v3.adl",
				"../../CDRDocument/knowledge/archetype/ZJU/ad/openEHR-EHR-OBSERVATION.adl.v1.adl",
				"../../CDRDocument/knowledge/archetype/ZJU/ad/openEHR-EHR-OBSERVATION.cdr.v1.adl",
				"../../CDRDocument/knowledge/archetype/ZJU/ad/openEHR-EHR-OBSERVATION.gds.v1.adl",
				"../../CDRDocument/knowledge/archetype/ZJU/ad/openEHR-EHR-OBSERVATION.mmse.v1.adl", 
				"../../CDRDocument/knowledge/archetype/ZJU/ad/openEHR-EHR-OBSERVATION.other_cognitions_scale_exams.v1.adl", };
	}

	@Override
	protected String[] getArmFiles() {
		return new String[] {
				"../../CDRDocument/knowledge/archetype/CKM/entry/observation/openEHR-EHR-OBSERVATION.blood_pressure.v1.arm.xml",
				"../../CDRDocument/knowledge/archetype/ZJU/openEHR-DEMOGRAPHIC-PERSON.patient.v1.arm.xml",
				"../../CDRDocument/knowledge/archetype/ZJU/openEHR-EHR-COMPOSITION.visit.v3.arm.xml",
				"../../CDRDocument/knowledge/archetype/ZJU/ad/openEHR-EHR-OBSERVATION.adl.v1.arm.xml",
				"../../CDRDocument/knowledge/archetype/ZJU/ad/openEHR-EHR-OBSERVATION.cdr.v1.arm.xml",
				"../../CDRDocument/knowledge/archetype/ZJU/ad/openEHR-EHR-OBSERVATION.gds.v1.arm.xml",
				"../../CDRDocument/knowledge/archetype/ZJU/ad/openEHR-EHR-OBSERVATION.mmse.v1.arm.xml", 
				"../../CDRDocument/knowledge/archetype/ZJU/ad/openEHR-EHR-OBSERVATION.other_cognitions_scale_exams.v1.arm.xml", };
	}

	protected String[] getArchetypeIds() {
		return new String[] {
				"openEHR-EHR-OBSERVATION.blood_pressure.v1",
				"openEHR-DEMOGRAPHIC-PERSON.patient.v1",
				"openEHR-EHR-COMPOSITION.visit.v3",
				"openEHR-EHR-OBSERVATION.adl.v1",
				"openEHR-EHR-OBSERVATION.cdr.v1",
				"openEHR-EHR-OBSERVATION.gds.v1",
				"openEHR-EHR-OBSERVATION.mmse.v1", 
				"openEHR-EHR-OBSERVATION.other_cognitions_scale_exams.v1", };
	}

	protected String[] getDadlFiles() {
		return new String[] {
				"../../CDRDocument/knowledge/archetype/CKM/entry/observation/openEHR-EHR-OBSERVATION.blood_pressure.v1.1.dadl",
				"../../CDRDocument/knowledge/archetype/CKM/entry/observation/openEHR-EHR-OBSERVATION.blood_pressure.v1.2.dadl", };
	}

	protected Map<HashMap<String, Object>, String> getArchetypeValues() {
		Map<HashMap<String, Object>, String> results = new HashMap<HashMap<String, Object>, String>();

		{
			HashMap<String, Object> patient1 = new HashMap<String, Object>();
			patient1.put("/uid/value", "patient1");
			patient1.put("/details[at0001]/items[at0003]/value/value", "M");
			patient1.put("/details[at0001]/items[at0004]/value/value",
					"1984-08-11T19:20:30+08:00");
			patient1.put("/details[at0001]/items[at0009]/value/value", "zhangsan");
			results.put(patient1, "openEHR-DEMOGRAPHIC-PERSON.patient.v1");			
		}

		{
			HashMap<String, Object> patient2 = new HashMap<String, Object>();
			patient2.put("/uid/value", "patient2");
			patient2.put("/details[at0001]/items[at0003]/value/value", "F");
			patient2.put("/details[at0001]/items[at0004]/value/value",
					"1986-08-11T19:20:30+08:00");
			patient2.put("/details[at0001]/items[at0009]/value/value", "lisi");
			results.put(patient2, "openEHR-DEMOGRAPHIC-PERSON.patient.v1");		
		}

		{
			HashMap<String, Object> patient3 = new HashMap<String, Object>();
			patient3.put("/uid/value", "patient3");
			patient3.put("/details[at0001]/items[at0003]/value/value", "O");
			patient3.put("/details[at0001]/items[at0004]/value/value",
					"1988-08-11T19:20:30+08:00");
			patient3.put("/details[at0001]/items[at0009]/value/value", "wangwu");
			results.put(patient3, "openEHR-DEMOGRAPHIC-PERSON.patient.v1");			
		}

		{
			HashMap<String, Object> visit1 = new HashMap<String, Object>();
			visit1.put("/uid/value", "visit1");
			visit1.put("/context/other_context[at0001]/items[at0007]/value/value",
					"2010-01-15T19:20:30+08:00");
			visit1.put("/context/other_context[at0001]/items[at0015]/value/value",
					"patient1");
			results.put(visit1, "openEHR-EHR-COMPOSITION.visit.v3");			
		}

		{
			HashMap<String, Object> visit2 = new HashMap<String, Object>();
			visit2.put("/uid/value", "visit2");
			visit2.put("/context/other_context[at0001]/items[at0007]/value/value",
					"2010-01-25T19:20:30+08:00");
			visit2.put("/context/other_context[at0001]/items[at0015]/value/value",
					"patient1");
			results.put(visit2, "openEHR-EHR-COMPOSITION.visit.v3");			
		}

		{
			HashMap<String, Object> visit3 = new HashMap<String, Object>();
			visit3.put("/uid/value", "visit3");
			visit3.put("/context/other_context[at0001]/items[at0007]/value/value",
					"2011-02-05T19:20:30+08:00");
			visit3.put("/context/other_context[at0001]/items[at0015]/value/value",
					"patient2");
			results.put(visit3, "openEHR-EHR-COMPOSITION.visit.v3");			
		}

		{
			HashMap<String, Object> other_cognitions_scale_exams1 = new HashMap<String, Object>();
			other_cognitions_scale_exams1.put("/uid/value", "other_cognitions_scale_exams1");
			other_cognitions_scale_exams1.put("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0005]/value/magnitude", 1);
			other_cognitions_scale_exams1.put("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0006]/value/magnitude", 2);
			other_cognitions_scale_exams1.put("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0007]/value/magnitude", 3);
			other_cognitions_scale_exams1.put("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0008]/value/magnitude", 6);
			results.put(other_cognitions_scale_exams1, "openEHR-EHR-OBSERVATION.other_cognitions_scale_exams.v1");			
		}

		{
			HashMap<String, Object> mmse1 = new HashMap<String, Object>();
			mmse1.put("/uid/value", "mmse1");
			mmse1.put("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0005]/value/value", false);
			mmse1.put("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0009]/value/value", false);
			mmse1.put("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0012]/value/value", false);
			results.put(mmse1, "openEHR-EHR-OBSERVATION.mmse.v1");			
		}

		return results;
	}

	protected void createTestBaseData() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();

		for (String dadl : getDadlFiles()) {
			File file = new File(dadl);
			InputStream is = new FileInputStream(file);
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
			Archetype archetype = ArchetypeRepository
					.getArchetype(archetypeValues.get(values));
			Object result = generator.create(archetype,
					GenerationStrategy.MAXIMUM_EMPTY);
			if (result instanceof Locatable) {
				Locatable loc = (Locatable) result;
				ReflectHelper.setArchetypeValue(loc, values);
				s.save(loc);
			}
		}

		s.flush();
		txn.commit();
		s.close();
	}
	
	protected void cleanTestBaseData() {
		Session s = openSession();
		Transaction txn = s.beginTransaction();

		for (String str : getArchetypeIds()) {
			String aql = String.format("delete from %s as o", str);
			s.createAQLQuery(aql).executeUpdateAQL();
		}

		s.flush();
		txn.commit();
		s.close();
	}

	@Test
	public void testSimpleSelectProperty() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "select "
					+ "o#/uid/value, "
					+ "o#/details[at0001]/items[at0003]/value/value, "
					+ "o#/details[at0001]/items[at0004]/value/value, "
					+ "o#/details[at0001]/items[at0009]/value/value "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "order by o#/uid/value asc";
			List results = s
					.createAQLQuery(query)
					.listAQL();

			assertEquals(results.size(), 3);
			Object[] loc1 = (Object[]) results.get(0);
			assertEquals(loc1[0], "patient1");
			assertEquals(loc1[1], "M");
			assertEquals(loc1[2], "1984-08-11T19:20:30+08:00");
			assertEquals(loc1[3], "zhangsan");
			Object[] loc2 = (Object[]) results.get(1);
			assertEquals(loc2[0], "patient2");
			assertEquals(loc2[1], "F");
			assertEquals(loc2[2], "1986-08-11T19:20:30+08:00");
			assertEquals(loc2[3], "lisi");
			Object[] loc3 = (Object[]) results.get(2);
			assertEquals(loc3[0], "patient3");
			assertEquals(loc3[1], "O");
			assertEquals(loc3[2], "1988-08-11T19:20:30+08:00");
			assertEquals(loc3[3], "wangwu");
		}

		{
			String query = "select "
					+ "o#/uid/value, "
					+ "o#/details[at0001]/items[at0003]/value/value, "
					+ "o#/details[at0001]/items[at0004]/value/value, "
					+ "o#/details[at0001]/items[at0009]/value/value "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "where o#/details[at0001]/items[at0009]/value/value = :name";
			List results = s
					.createAQLQuery(query)
					.setParameter("name", "lisi")
					.listAQL();

			assertEquals(results.size(), 1);
			Object[] loc2 = (Object[]) results.get(0);
			assertEquals(loc2[0], "patient2");
			assertEquals(loc2[1], "F");
			assertEquals(loc2[2], "1986-08-11T19:20:30+08:00");
			assertEquals(loc2[3], "lisi");
		}

		{
			String query = "select "
					+ "o#/uid/value, "
					+ "o#/details[at0001]/items[at0003]/value/value, "
					+ "o#/details[at0001]/items[at0004]/value/value, "
					+ "o#/details[at0001]/items[at0009]/value/value "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "where o#/uid/value = :name";
			List results = s
					.createAQLQuery(query)
					.setParameter("name", "patient1")
					.listAQL();

			assertEquals(results.size(), 1);
			Object[] loc1 = (Object[]) results.get(0);
			assertEquals(loc1[0], "patient1");
			assertEquals(loc1[1], "M");
			assertEquals(loc1[2], "1984-08-11T19:20:30+08:00");
			assertEquals(loc1[3], "zhangsan");
		}
		
		s.close();
		
		cleanTestBaseData();
	}

	@Test
	public void testSimpleSelect() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "select o "
					+ "from openEHR-EHR-OBSERVATION.blood_pressure.v1 as o "
					+ "order by o#/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude asc";
			List results = s
					.createAQLQuery(query)
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 2);
			Locatable loc1 = (Locatable) results.get(0);
			Double d1 = (Double) loc1
					.itemAtPath("/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude");
			Double d2 = (Double) loc1
					.itemAtPath("/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude");
			assertEquals(d1.doubleValue(), 120, 0.1);
			assertEquals(d2.doubleValue(), 80, 0.1);
			Locatable loc2 = (Locatable) results.get(1);
			Double d3 = (Double) loc2
					.itemAtPath("/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude");
			Double d4 = (Double) loc2
					.itemAtPath("/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude");
			assertEquals(d3.doubleValue(), 125, 0.1);
			assertEquals(d4.doubleValue(), 85, 0.1);
		}

		{
			String query = "select o "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "order by o#/uid/value asc";
			List results = s
					.createAQLQuery(query)
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 3);
			Locatable loc1 = (Locatable) results.get(0);
			String d1 = (String) loc1.itemAtPath("/uid/value");
			String d2 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0003]/value/value");
			String d3 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0004]/value/value");
			String d4 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0009]/value/value");
			assertEquals(d1, "patient1");
			assertEquals(d2, "M");
			assertEquals(d3, "1984-08-11T19:20:30+08:00");
			assertEquals(d4, "zhangsan");
			Locatable loc2 = (Locatable) results.get(1);
			String d5 = (String) loc2.itemAtPath("/uid/value");
			String d6 = (String) loc2
					.itemAtPath("/details[at0001]/items[at0003]/value/value");
			String d7 = (String) loc2
					.itemAtPath("/details[at0001]/items[at0004]/value/value");
			String d8 = (String) loc2
					.itemAtPath("/details[at0001]/items[at0009]/value/value");
			assertEquals(d5, "patient2");
			assertEquals(d6, "F");
			assertEquals(d7, "1986-08-11T19:20:30+08:00");
			assertEquals(d8, "lisi");
			Locatable loc3 = (Locatable) results.get(2);
			String d9 = (String) loc3.itemAtPath("/uid/value");
			String d10 = (String) loc3
					.itemAtPath("/details[at0001]/items[at0003]/value/value");
			String d11 = (String) loc3
					.itemAtPath("/details[at0001]/items[at0004]/value/value");
			String d12 = (String) loc3
					.itemAtPath("/details[at0001]/items[at0009]/value/value");
			assertEquals(d9, "patient3");
			assertEquals(d10, "O");
			assertEquals(d11, "1988-08-11T19:20:30+08:00");
			assertEquals(d12, "wangwu");
		}

		{
			String query = "select o "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "where o#/details[at0001]/items[at0009]/value/value = :name";
			List results = s
					.createAQLQuery(query)
					.setParameter("name", "lisi")
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 1);
			Locatable loc2 = (Locatable) results.get(0);
			String d5 = (String) loc2.itemAtPath("/uid/value");
			String d6 = (String) loc2
					.itemAtPath("/details[at0001]/items[at0003]/value/value");
			String d7 = (String) loc2
					.itemAtPath("/details[at0001]/items[at0004]/value/value");
			String d8 = (String) loc2
					.itemAtPath("/details[at0001]/items[at0009]/value/value");
			assertEquals(d5, "patient2");
			assertEquals(d6, "F");
			assertEquals(d7, "1986-08-11T19:20:30+08:00");
			assertEquals(d8, "lisi");
		}

		{
			String query = "select o "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "where o#/uid/value = :name";
			List results = s
					.createAQLQuery(query)
					.setParameter("name", "patient1")
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 1);
			Locatable loc1 = (Locatable) results.get(0);
			String d1 = (String) loc1.itemAtPath("/uid/value");
			String d2 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0003]/value/value");
			String d3 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0004]/value/value");
			String d4 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0009]/value/value");
			assertEquals(d1, "patient1");
			assertEquals(d2, "M");
			assertEquals(d3, "1984-08-11T19:20:30+08:00");
			assertEquals(d4, "zhangsan");
		}

		{
			String query = "select o "
					+ "from openEHR-EHR-OBSERVATION.other_cognitions_scale_exams.v1 as o "
					+ "order by o#/uid/value asc";
			List results = s
					.createAQLQuery(query)
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 1);
			Locatable loc1 = (Locatable) results.get(0);
			Integer d1 = (Integer) loc1
					.itemAtPath("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0005]/value/magnitude");
			Integer d2 = (Integer) loc1
					.itemAtPath("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0006]/value/magnitude");
			Integer d3 = (Integer) loc1
					.itemAtPath("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0007]/value/magnitude");
			Integer d4 = (Integer) loc1
					.itemAtPath("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0008]/value/magnitude");
			assertEquals(d1.intValue(), 1);
			assertEquals(d2.intValue(), 2);
			assertEquals(d3.intValue(), 3);
			assertEquals(d4.intValue(), 6);
		}

		{
			String query = "select o "
					+ "from openEHR-EHR-OBSERVATION.mmse.v1 as o "
					+ "order by o#/uid/value asc";
			List results = s
					.createAQLQuery(query)
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 1);
			Locatable loc1 = (Locatable) results.get(0);
			Boolean d1 = (Boolean) loc1
					.itemAtPath("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0005]/value/value");
			Boolean d2 = (Boolean) loc1
					.itemAtPath("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0009]/value/value");
			Boolean d3 = (Boolean) loc1
					.itemAtPath("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0015]/value/value");
			Boolean d4 = (Boolean) loc1
					.itemAtPath("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0016]/value/value");
			assertEquals(d1.booleanValue(), false);
			assertEquals(d2.booleanValue(), false);
			assertEquals(d3.booleanValue(), true);
			assertEquals(d4.booleanValue(), true);
		}

		s.close();
		
		cleanTestBaseData();
	}

	@Test
	public void testSimpleSelectAs() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "select "
					+ "o#/uid/value as /uid/value, "
					+ "o#/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude as /data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude, "
					+ "o#/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude as /data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude "
					+ "from openEHR-EHR-OBSERVATION.blood_pressure.v1 as o "
					+ "order by o#/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude asc";
			String archetypeId = "openEHR-EHR-OBSERVATION.blood_pressure.v1";
			List results = s
					.createAQLQuery(query)
					.setResultTransformer(
							Transformers.aliasToArchetype(archetypeId))
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 2);
			Locatable loc1 = (Locatable) results.get(0);
			Double d1 = (Double) loc1
					.itemAtPath("/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude");
			Double d2 = (Double) loc1
					.itemAtPath("/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude");
			assertEquals(d1.doubleValue(), 120, 0.1);
			assertEquals(d2.doubleValue(), 80, 0.1);
			Locatable loc2 = (Locatable) results.get(1);
			Double d3 = (Double) loc2
					.itemAtPath("/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude");
			Double d4 = (Double) loc2
					.itemAtPath("/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude");
			assertEquals(d3.doubleValue(), 125, 0.1);
			assertEquals(d4.doubleValue(), 85, 0.1);
		}

		{
			String query = "select "
					+ "o#/uid/value as /uid/value, "
					+ "o#/details[at0001]/items[at0003]/value/value as /details[at0001]/items[at0003]/value/value, "
					+ "o#/details[at0001]/items[at0004]/value/value as /details[at0001]/items[at0004]/value/value, "
					+ "o#/details[at0001]/items[at0009]/value/value as /details[at0001]/items[at0009]/value/value "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "order by o#/uid/value asc";
			String archetypeId = "openEHR-DEMOGRAPHIC-PERSON.patient.v1";
			List results = s
					.createAQLQuery(query)
					.setResultTransformer(
							Transformers.aliasToArchetype(archetypeId))
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 3);
			Locatable loc1 = (Locatable) results.get(0);
			String d1 = (String) loc1.itemAtPath("/uid/value");
			String d2 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0003]/value/value");
			String d3 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0004]/value/value");
			String d4 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0009]/value/value");
			assertEquals(d1, "patient1");
			assertEquals(d2, "M");
			assertEquals(d3, "1984-08-11T19:20:30+08:00");
			assertEquals(d4, "zhangsan");
			Locatable loc2 = (Locatable) results.get(1);
			String d5 = (String) loc2.itemAtPath("/uid/value");
			String d6 = (String) loc2
					.itemAtPath("/details[at0001]/items[at0003]/value/value");
			String d7 = (String) loc2
					.itemAtPath("/details[at0001]/items[at0004]/value/value");
			String d8 = (String) loc2
					.itemAtPath("/details[at0001]/items[at0009]/value/value");
			assertEquals(d5, "patient2");
			assertEquals(d6, "F");
			assertEquals(d7, "1986-08-11T19:20:30+08:00");
			assertEquals(d8, "lisi");
			Locatable loc3 = (Locatable) results.get(2);
			String d9 = (String) loc3.itemAtPath("/uid/value");
			String d10 = (String) loc3
					.itemAtPath("/details[at0001]/items[at0003]/value/value");
			String d11 = (String) loc3
					.itemAtPath("/details[at0001]/items[at0004]/value/value");
			String d12 = (String) loc3
					.itemAtPath("/details[at0001]/items[at0009]/value/value");
			assertEquals(d9, "patient3");
			assertEquals(d10, "O");
			assertEquals(d11, "1988-08-11T19:20:30+08:00");
			assertEquals(d12, "wangwu");
		}

		{
			String query = "select "
					+ "o#/uid/value as /uid/value, "
					+ "o#/details[at0001]/items[at0003]/value/value as /details[at0001]/items[at0003]/value/value, "
					+ "o#/details[at0001]/items[at0004]/value/value as /details[at0001]/items[at0004]/value/value, "
					+ "o#/details[at0001]/items[at0009]/value/value as /details[at0001]/items[at0009]/value/value "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "where o#/details[at0001]/items[at0009]/value/value = :name";
			String archetypeId = "openEHR-DEMOGRAPHIC-PERSON.patient.v1";
			List results = s
					.createAQLQuery(query)
					.setParameter("name", "lisi")
					.setResultTransformer(
							Transformers.aliasToArchetype(archetypeId))
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 1);
			Locatable loc2 = (Locatable) results.get(0);
			String d5 = (String) loc2.itemAtPath("/uid/value");
			String d6 = (String) loc2
					.itemAtPath("/details[at0001]/items[at0003]/value/value");
			String d7 = (String) loc2
					.itemAtPath("/details[at0001]/items[at0004]/value/value");
			String d8 = (String) loc2
					.itemAtPath("/details[at0001]/items[at0009]/value/value");
			assertEquals(d5, "patient2");
			assertEquals(d6, "F");
			assertEquals(d7, "1986-08-11T19:20:30+08:00");
			assertEquals(d8, "lisi");
		}

		{
			String query = "select "
					+ "o#/uid/value as /uid/value, "
					+ "o#/details[at0001]/items[at0003]/value/value as /details[at0001]/items[at0003]/value/value, "
					+ "o#/details[at0001]/items[at0004]/value/value as /details[at0001]/items[at0004]/value/value, "
					+ "o#/details[at0001]/items[at0009]/value/value as /details[at0001]/items[at0009]/value/value "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "where o#/uid/value = :name";
			String archetypeId = "openEHR-DEMOGRAPHIC-PERSON.patient.v1";
			List results = s
					.createAQLQuery(query)
					.setParameter("name", "patient1")
					.setResultTransformer(
							Transformers.aliasToArchetype(archetypeId))
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 1);
			Locatable loc1 = (Locatable) results.get(0);
			String d1 = (String) loc1.itemAtPath("/uid/value");
			String d2 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0003]/value/value");
			String d3 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0004]/value/value");
			String d4 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0009]/value/value");
			assertEquals(d1, "patient1");
			assertEquals(d2, "M");
			assertEquals(d3, "1984-08-11T19:20:30+08:00");
			assertEquals(d4, "zhangsan");
		}

		{
			String query = "select "
					+ "o#/uid/value as /uid/value, "
					+ "o#/context/other_context[at0001]/items[at0007]/value/value as /context/other_context[at0001]/items[at0007]/value/value, "
					+ "o#/context/other_context[at0001]/items[at0015]/value/value as /context/other_context[at0001]/items[at0015]/value/value "
					+ "from openEHR-EHR-COMPOSITION.visit.v3 as o "
					+ "where o#/context/other_context[at0001]/items[at0015]/value/value = :pid "
					+ "order by o#/uid/value asc";
			String archetypeId = "openEHR-EHR-COMPOSITION.visit.v3";
			List results = s
					.createAQLQuery(query)
					.setParameter("pid", "patient1")
					.setResultTransformer(
							Transformers.aliasToArchetype(archetypeId))
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 2);
		}

		{
			String query = "select "
					+ "o#/uid/value as /uid/value, "
					+ "o#/context/other_context[at0001]/items[at0007]/value/value as /context/other_context[at0001]/items[at0007]/value/value, "
					+ "o#/context/other_context[at0001]/items[at0015]/value/value as /context/other_context[at0001]/items[at0015]/value/value "
					+ "from openEHR-EHR-COMPOSITION.visit.v3 as o "
					+ "where o#/uid/value = :name and o#/context/other_context[at0001]/items[at0015]/value/value = :pid";
			String archetypeId = "openEHR-EHR-COMPOSITION.visit.v3";
			List results = s
					.createAQLQuery(query)
					.setParameter("name", "visit1")
					.setParameter("pid", "patient1")
					.setResultTransformer(
							Transformers.aliasToArchetype(archetypeId))
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 1);
		}

		s.close();
		
		cleanTestBaseData();
	}

	@Test
	public void testSimpleDelete() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "select "
					+ "o#/uid/value as /uid/value, "
					+ "o#/details[at0001]/items[at0003]/value/value as /details[at0001]/items[at0003]/value/value, "
					+ "o#/details[at0001]/items[at0004]/value/value as /details[at0001]/items[at0004]/value/value, "
					+ "o#/details[at0001]/items[at0009]/value/value as /details[at0001]/items[at0009]/value/value "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o ";
			String archetypeId = "openEHR-DEMOGRAPHIC-PERSON.patient.v1";
			List results = s
					.createAQLQuery(query)
					.setResultTransformer(
							Transformers.aliasToArchetype(archetypeId))
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 3);
		}

		{
			String query = "delete "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "where o#/details[at0001]/items[at0009]/value/value = :name";
			int ret = s.createAQLQuery(query).setParameter("name", "lisi")
					.executeUpdateAQL();

			assertEquals(ret, 1);
		}

		{
			String query = "select "
					+ "o#/uid/value as /uid/value, "
					+ "o#/details[at0001]/items[at0003]/value/value as /details[at0001]/items[at0003]/value/value, "
					+ "o#/details[at0001]/items[at0004]/value/value as /details[at0001]/items[at0004]/value/value, "
					+ "o#/details[at0001]/items[at0009]/value/value as /details[at0001]/items[at0009]/value/value "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o ";
			String archetypeId = "openEHR-DEMOGRAPHIC-PERSON.patient.v1";
			List results = s
					.createAQLQuery(query)
					.setResultTransformer(
							Transformers.aliasToArchetype(archetypeId))
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 2);
		}

		{
			String query = "delete "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o ";
			int ret = s.createAQLQuery(query).executeUpdateAQL();

			assertEquals(ret, 2);
		}

		{
			String query = "select "
					+ "o#/uid/value as /uid/value, "
					+ "o#/details[at0001]/items[at0003]/value/value as /details[at0001]/items[at0003]/value/value, "
					+ "o#/details[at0001]/items[at0004]/value/value as /details[at0001]/items[at0004]/value/value, "
					+ "o#/details[at0001]/items[at0009]/value/value as /details[at0001]/items[at0009]/value/value "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o ";
			String archetypeId = "openEHR-DEMOGRAPHIC-PERSON.patient.v1";
			List results = s
					.createAQLQuery(query)
					.setResultTransformer(
							Transformers.aliasToArchetype(archetypeId))
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 0);
		}

		s.close();
		
		cleanTestBaseData();
	}

	@Test
	public void testSimpleUpdate() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "select "
					+ "o#/uid/value as /uid/value, "
					+ "o#/details[at0001]/items[at0003]/value/value as /details[at0001]/items[at0003]/value/value, "
					+ "o#/details[at0001]/items[at0004]/value/value as /details[at0001]/items[at0004]/value/value, "
					+ "o#/details[at0001]/items[at0009]/value/value as /details[at0001]/items[at0009]/value/value "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "where o#/uid/value = :name";
			String archetypeId = "openEHR-DEMOGRAPHIC-PERSON.patient.v1";
			List results = s
					.createAQLQuery(query)
					.setParameter("name", "patient1")
					.setResultTransformer(
							Transformers.aliasToArchetype(archetypeId))
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 1);
			Locatable loc1 = (Locatable) results.get(0);
			String d1 = (String) loc1.itemAtPath("/uid/value");
			String d2 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0003]/value/value");
			String d3 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0004]/value/value");
			String d4 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0009]/value/value");
			assertEquals(d1, "patient1");
			assertEquals(d2, "M");
			assertEquals(d3, "1984-08-11T19:20:30+08:00");
			assertEquals(d4, "zhangsan");
		}

		{
			String query = "update openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o set "
					+ "o#/details[at0001]/items[at0009]/value/value = :name, "
					+ "o#/details[at0001]/items[at0004]/value/value = :birthday "
					+ "where " + "o#/uid/value = :pid ";
			int ret = s.createAQLQuery(query).setParameter("name", "lisi")
					.setParameter("birthday", "1994-08-11T19:20:30+08:00")
					.setParameter("pid", "patient1").executeUpdateAQL();

			assertEquals(ret, 1);
		}

		{
			String query = "select "
					+ "o#/uid/value as /uid/value, "
					+ "o#/details[at0001]/items[at0003]/value/value as /details[at0001]/items[at0003]/value/value, "
					+ "o#/details[at0001]/items[at0004]/value/value as /details[at0001]/items[at0004]/value/value, "
					+ "o#/details[at0001]/items[at0009]/value/value as /details[at0001]/items[at0009]/value/value "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "where o#/uid/value = :name";
			String archetypeId = "openEHR-DEMOGRAPHIC-PERSON.patient.v1";
			List results = s
					.createAQLQuery(query)
					.setParameter("name", "patient1")
					.setResultTransformer(
							Transformers.aliasToArchetype(archetypeId))
					.listAQL();

			DADLBinding binding = new DADLBinding();
			for (Object obj : results) {
				System.out.println(binding.toDADL(obj));
			}

			assertEquals(results.size(), 1);
			Locatable loc1 = (Locatable) results.get(0);
			String d1 = (String) loc1.itemAtPath("/uid/value");
			String d2 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0003]/value/value");
			String d3 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0004]/value/value");
			String d4 = (String) loc1
					.itemAtPath("/details[at0001]/items[at0009]/value/value");
			assertEquals(d1, "patient1");
			assertEquals(d2, "M");
			assertEquals(d3, "1994-08-11T19:20:30+08:00");
			assertEquals(d4, "lisi");
		}

		s.close();
		
		cleanTestBaseData();
	}
	
	@Test
	public void testSimpleSelectPerformance() throws Exception {
		createTestBaseData();
		Session s = openSession();
		long start=System.currentTimeMillis();
		for (int i=0;i<10000;i++) {
			{
				String query = "select "
						+ "o#/uid/value as /uid/value, "
						+ "o#/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude as /data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude, "
						+ "o#/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude as /data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude "
						+ "from openEHR-EHR-OBSERVATION.blood_pressure.v1 as o "
						+ "order by o#/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude asc";
				String archetypeId = "openEHR-EHR-OBSERVATION.blood_pressure.v1";
				List results = s
						.createAQLQuery(query)
						.setResultTransformer(
								Transformers.aliasToArchetype(archetypeId))
						.listAQL();

				DADLBinding binding = new DADLBinding();
				for (Object obj : results) {
					System.out.println(binding.toDADL(obj));
				}

				assertEquals(results.size(), 2);
				Locatable loc1 = (Locatable) results.get(0);
				Double d1 = (Double) loc1
						.itemAtPath("/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude");
				Double d2 = (Double) loc1
						.itemAtPath("/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude");
				assertEquals(d1.doubleValue(), 120, 0.1);
				assertEquals(d2.doubleValue(), 80, 0.1);
				Locatable loc2 = (Locatable) results.get(1);
				Double d3 = (Double) loc2
						.itemAtPath("/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude");
				Double d4 = (Double) loc2
						.itemAtPath("/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value/magnitude");
				assertEquals(d3.doubleValue(), 125, 0.1);
				assertEquals(d4.doubleValue(), 85, 0.1);
			}
		}
		System.out.println(start);
		System.out.println(System.currentTimeMillis()-start);
		
		s.close();
		
		cleanTestBaseData();
	}

}
