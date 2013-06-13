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

import java.util.List;
import org.jboss.logging.Logger;
import org.junit.Test;
import org.openehr.rm.binding.DADLBinding;
import org.openehr.rm.common.archetyped.Locatable;
import org.hibernate.Session;
import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.testing.SkipForDialect;
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
public class ASTParserLoadingTest extends ASTParserLoadingTestBase {
	private static final Logger log = Logger
			.getLogger(ASTParserLoadingTest.class);

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
	public void testSimpleFrom() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
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
			String query = "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
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
			String query = "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
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

		s.close();
		
		cleanTestBaseData();
	}

	@Test
	public void testSimpleSelect() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "select o "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "where o#/uid/value = 'patient1'";
			List results = s
					.createAQLQuery(query)
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
	public void testSimpleSelectParameterized() throws Exception {
		createTestBaseData();

		Session s = openSession();

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

}
