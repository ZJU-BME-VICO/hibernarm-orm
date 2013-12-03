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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.expr.Instanceof;

import org.jboss.logging.Logger;
import org.junit.Test;
import org.openehr.am.parser.ContentObject;
import org.openehr.am.parser.DADLParser;
import org.openehr.rm.binding.DADLBinding;
import org.openehr.rm.common.archetyped.Locatable;
import org.hibernate.Session;
import org.hibernate.Transaction;
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
	public void testDelete() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "from openEHR-EHR-COMPOSITION.visit.v3 as o ";
			List results = s.createQuery(query).list();

			assertEquals(results.size(), 3);
		}

		{
			String query = "delete "
					+ "from openEHR-EHR-COMPOSITION.visit.v3 as o "
					+ "where o#/uid/value = 'visit2'";
			int ret = s.createQuery(query).executeUpdate();

			assertEquals(ret, 1);
		}

		{
			String query = "from openEHR-EHR-COMPOSITION.visit.v3 as o ";
			List results = s.createQuery(query).list();

			assertEquals(results.size(), 2);
		}

		{
			String query = "delete "
					+ "from openEHR-EHR-COMPOSITION.visit.v3 as o ";
			int ret = s.createQuery(query).executeUpdate();

			assertEquals(ret, 2);
		}

		{
			String query = "from openEHR-EHR-COMPOSITION.visit.v3 as o ";
			List results = s.createQuery(query).list();

			assertEquals(results.size(), 0);
		}

		s.close();
		
		cleanTestBaseData();
	}

	@Test
	public void testDeleteParameterized() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "from openEHR-EHR-COMPOSITION.visit.v3 as o ";
			List results = s.createQuery(query).list();

			assertEquals(results.size(), 3);
		}

		{
			String query = "delete "
					+ "from openEHR-EHR-COMPOSITION.visit.v3 as o "
					+ "where o#/uid/value = :name";
			int ret = s.createQuery(query).setParameter("name", "visit2").executeUpdate();

			assertEquals(ret, 1);
		}

		{
			String query = "from openEHR-EHR-COMPOSITION.visit.v3 as o ";
			List results = s.createQuery(query).list();

			assertEquals(results.size(), 2);
		}

		{
			String query = "delete "
					+ "from openEHR-EHR-COMPOSITION.visit.v3 as o ";
			int ret = s.createQuery(query).executeUpdate();

			assertEquals(ret, 2);
		}

		{
			String query = "from openEHR-EHR-COMPOSITION.visit.v3 as o ";
			List results = s.createQuery(query).list();

			assertEquals(results.size(), 0);
		}

		s.close();
		
		cleanTestBaseData();
	}

	@Test
	public void testSelect() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "select o "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "order by o#/uid/value asc";
			List results = s
					.createQuery(query)
					.list();

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
					+ "where o#/details[at0001]/items[at0009]/value/value = 'lisi'";
			List results = s
					.createQuery(query)
					.list();

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
					+ "where o#/uid/value = 'patient1'";
			List results = s
					.createQuery(query)
					.list();

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
					+ "from openEHR-EHR-COMPOSITION.visit.v3 as o "
					+ "where o#/context/other_context[at0001]/items[at0015]/value/value = 'patient1'";
			List results = s
					.createQuery(query)
					.list();

			assertEquals(results.size(), 2);
		}

		{
			String query = "select o "
					+ "from openEHR-EHR-COMPOSITION.visit.v3 as o "
					+ "where o#/uid/value = 'visit1' and o#/context/other_context[at0001]/items[at0015]/value/value = 'patient1'";
			List results = s
					.createQuery(query)
					.list();

			assertEquals(results.size(), 1);
		}

		s.close();
		
		cleanTestBaseData();
	}

	@Test
	public void testSelectAs() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "select "
					+ "o#/uid/value as /uid/value, "
					+ "o#/details[at0001]/items[at0003]/value/value as /details[at0001]/items[at0003]/value/value, "
					+ "o#/details[at0001]/items[at0004]/value/value as /details[at0001]/items[at0004]/value/value, "
					+ "o#/details[at0001]/items[at0009]/value/value as /details[at0001]/items[at0009]/value/value "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "where o#/details[at0001]/items[at0009]/value/value = 'lisi'";
			String archetypeId = "openEHR-DEMOGRAPHIC-PERSON.patient.v1";
			List results = s
					.createQuery(query)
					.setResultTransformer(
							Transformers.aliasToArchetype(archetypeId))
					.list();

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

		s.close();
		
		cleanTestBaseData();
	}

	@Test
	public void testSelectColumn() throws Exception {
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
					.createQuery(query)
					.list();

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
					.createQuery(query)
					.setParameter("name", "lisi")
					.list();

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
					.createQuery(query)
					.setParameter("name", "patient1")
					.list();

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
	public void testSelectJoinCartesian() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "select p, v " +
					"from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as p, openEHR-EHR-COMPOSITION.visit.v3 as v ";
			List results = s
					.createQuery(query)
					.list();

			assertEquals(results.size(), 9);

			List<Locatable> patients = new ArrayList<Locatable>();
			List<Locatable> visits = new ArrayList<Locatable>();
			for (Object arr : results) {
				if (arr.getClass().isArray()) {
					for (int i = 0; i < Array.getLength(arr); i++)
					{
						Object obj = Array.get(arr, i);
						if (obj instanceof Locatable) {
							Locatable loc = (Locatable) obj;			
							if (loc.getArchetypeNodeId().compareToIgnoreCase("openEHR-DEMOGRAPHIC-PERSON.patient.v1") == 0) {
								if (!patients.contains(loc)) {
									patients.add(loc);									
								}
							}
							
							if (loc.getArchetypeNodeId().compareToIgnoreCase("openEHR-EHR-COMPOSITION.visit.v3") == 0) {
								if (!visits.contains(loc)) {
									visits.add(loc);									
								}
							}
						}						
					}					
				}
			}

			assertEquals(patients.size(), 3);
			assertEquals(visits.size(), 3);
		}

		{
			String query = "select p, v " +
					"from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as p, openEHR-EHR-COMPOSITION.visit.v3 as v " +
					"where p#/uid/value = v#/context/other_context[at0001]/items[at0015]/value/value ";
			List results = s
					.createQuery(query)
					.list();

			assertEquals(results.size(), 3);

			List<Locatable> patients = new ArrayList<Locatable>();
			List<Locatable> visits = new ArrayList<Locatable>();
			for (Object arr : results) {
				if (arr.getClass().isArray()) {
					for (int i = 0; i < Array.getLength(arr); i++)
					{
						Object obj = Array.get(arr, i);
						if (obj instanceof Locatable) {
							Locatable loc = (Locatable) obj;			
							if (loc.getArchetypeNodeId().compareToIgnoreCase("openEHR-DEMOGRAPHIC-PERSON.patient.v1") == 0) {
								if (!patients.contains(loc)) {
									patients.add(loc);									
								}
							}
							
							if (loc.getArchetypeNodeId().compareToIgnoreCase("openEHR-EHR-COMPOSITION.visit.v3") == 0) {
								if (!visits.contains(loc)) {
									visits.add(loc);									
								}
							}
						}						
					}					
				}
			}

			assertEquals(patients.size(), 2);
			assertEquals(visits.size(), 3);
		}

		s.close();
		
		cleanTestBaseData();
	}

	@Test
	public void testSelectJoinFetchManyToOne() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "select v " +
					"from openEHR-EHR-COMPOSITION.visit.v3 as v " +
					"join fetch v#/context/other_context[at0001]/items[at0015]/value/value as p ";
			List results = s
					.createQuery(query)
					.list();

			assertEquals(results.size(), 3);

			List<Locatable> patients = new ArrayList<Locatable>();
			List<Locatable> visits = new ArrayList<Locatable>();
			for (Object obj1 : results) {
				if (obj1 instanceof Locatable) {
					Locatable loc1 = (Locatable) obj1;	
					if (!visits.contains(loc1)) {
						visits.add(loc1);																
					}				
					for (Object obj2 : loc1.getAssociatedObjects().values()) {
						if (obj2 instanceof Locatable) {
							Locatable loc2 = (Locatable) obj2;
							if (!patients.contains(loc2)) {
								patients.add(loc2);																
							}
						}
					}
				}
			}

			assertEquals(patients.size(), 2);
			assertEquals(visits.size(), 3);
		}

		s.close();
		
		cleanTestBaseData();
	}

	@Test
	public void testSelectJoinFetchOneToMany() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "select p " +
					"from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as p " +
					"join fetch p#/details[at0001]/items[at0032]/onetomany as v ";
			List results = s
					.createQuery(query)
					.list();

			assertEquals(results.size(), 3);

			List<Locatable> patients = new ArrayList<Locatable>();
			List<Locatable> visits = new ArrayList<Locatable>();
			for (Object obj1 : results) {
				if (obj1 instanceof Locatable) {
					Locatable loc1 = (Locatable) obj1;	
					if (!patients.contains(loc1)) {
						patients.add(loc1);																
					}		
					for (Object obj2 : loc1.getAssociatedObjects().values()) {
						if (obj2 instanceof Locatable) {
							Locatable loc2 = (Locatable) obj2;
							if (!visits.contains(loc2)) {
								visits.add(loc2);																
							}
						}
					}
				}
			}

			assertEquals(patients.size(), 2);
			assertEquals(visits.size(), 3);
		}

		{
			String query = "select distinct p " +
					"from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as p " +
					"join fetch p#/details[at0001]/items[at0032]/onetomany as v ";
			List results = s
					.createQuery(query)
					.list();

			assertEquals(results.size(), 2);

			List<Locatable> patients = new ArrayList<Locatable>();
			List<Locatable> visits = new ArrayList<Locatable>();
			for (Object obj1 : results) {
				if (obj1 instanceof Locatable) {
					Locatable loc1 = (Locatable) obj1;	
					if (!patients.contains(loc1)) {
						patients.add(loc1);																
					}		
					for (Object obj2 : loc1.getAssociatedObjects().values()) {
						if (obj2 instanceof Locatable) {
							Locatable loc2 = (Locatable) obj2;
							if (!visits.contains(loc2)) {
								visits.add(loc2);																
							}
						}
					}
				}
			}

			assertEquals(patients.size(), 2);
			assertEquals(visits.size(), 3);
		}

		s.close();
		
		cleanTestBaseData();
	}

	@Test
	public void testSelectJoinManyToOne() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "select v " +
					"from openEHR-EHR-COMPOSITION.visit.v3 as v " +
					"join v#/context/other_context[at0001]/items[at0015]/value/value as p ";
			List results = s
					.createQuery(query)
					.list();

			assertEquals(results.size(), 3);

			List<Locatable> patients = new ArrayList<Locatable>();
			List<Locatable> visits = new ArrayList<Locatable>();
			for (Object obj1 : results) {
				if (obj1 instanceof Locatable) {
					Locatable loc1 = (Locatable) obj1;	
					if (!visits.contains(loc1)) {
						visits.add(loc1);																
					}				
					for (Object obj2 : loc1.getAssociatedObjects().values()) {
						if (obj2 instanceof Locatable) {
							Locatable loc2 = (Locatable) obj2;
							if (!patients.contains(loc2)) {
								patients.add(loc2);																
							}
						}
					}
				}
			}

			assertEquals(patients.size(), 0);
			assertEquals(visits.size(), 3);
		}

		{
			String query = "select p, v " +
					"from openEHR-EHR-COMPOSITION.visit.v3 as v " +
					"join v#/context/other_context[at0001]/items[at0015]/value/value as p ";
			List results = s
					.createQuery(query)
					.list();

			assertEquals(results.size(), 3);

			List<Locatable> patients = new ArrayList<Locatable>();
			List<Locatable> visits = new ArrayList<Locatable>();
			for (Object arr : results) {
				if (arr.getClass().isArray()) {
					for (int i = 0; i < Array.getLength(arr); i++)
					{
						Object obj = Array.get(arr, i);
						if (obj instanceof Locatable) {
							Locatable loc = (Locatable) obj;			
							if (loc.getArchetypeNodeId().compareToIgnoreCase("openEHR-DEMOGRAPHIC-PERSON.patient.v1") == 0) {
								if (!patients.contains(loc)) {
									patients.add(loc);									
								}
							}
							
							if (loc.getArchetypeNodeId().compareToIgnoreCase("openEHR-EHR-COMPOSITION.visit.v3") == 0) {
								if (!visits.contains(loc)) {
									visits.add(loc);									
								}
							}
						}						
					}					
				}
			}

			assertEquals(patients.size(), 2);
			assertEquals(visits.size(), 3);
		}

		s.close();
		
		cleanTestBaseData();
	}

	@Test
	public void testSelectJoinOneToMany() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "select p " +
					"from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as p " +
					"join p#/details[at0001]/items[at0032]/onetomany as v ";
			List results = s
					.createQuery(query)
					.list();

			assertEquals(results.size(), 3);

			List<Locatable> patients = new ArrayList<Locatable>();
			List<Locatable> visits = new ArrayList<Locatable>();
			for (Object obj1 : results) {
				if (obj1 instanceof Locatable) {
					Locatable loc1 = (Locatable) obj1;	
					if (!patients.contains(loc1)) {
						patients.add(loc1);																
					}		
					for (Object obj2 : loc1.getAssociatedObjects().values()) {
						if (obj2 instanceof Locatable) {
							Locatable loc2 = (Locatable) obj2;
							if (!visits.contains(loc2)) {
								visits.add(loc2);																
							}
						}
					}
				}
			}

			assertEquals(patients.size(), 2);
			assertEquals(visits.size(), 0);
		}

		{
			String query = "select p, v " +
					"from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as p " +
					"join p#/details[at0001]/items[at0032]/onetomany as v ";
			List results = s
					.createQuery(query)
					.list();

			assertEquals(results.size(), 3);

			List<Locatable> patients = new ArrayList<Locatable>();
			List<Locatable> visits = new ArrayList<Locatable>();
			for (Object arr : results) {
				if (arr.getClass().isArray()) {
					for (int i = 0; i < Array.getLength(arr); i++)
					{
						Object obj = Array.get(arr, i);
						if (obj instanceof Locatable) {
							Locatable loc = (Locatable) obj;			
							if (loc.getArchetypeNodeId().compareToIgnoreCase("openEHR-DEMOGRAPHIC-PERSON.patient.v1") == 0) {
								if (!patients.contains(loc)) {
									patients.add(loc);									
								}
							}
							
							if (loc.getArchetypeNodeId().compareToIgnoreCase("openEHR-EHR-COMPOSITION.visit.v3") == 0) {
								if (!visits.contains(loc)) {
									visits.add(loc);									
								}
							}
						}						
					}					
				}
			}

			assertEquals(patients.size(), 2);
			assertEquals(visits.size(), 3);
		}

		{
			String query = "select p " +
					"from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as p " +
					"left join p#/details[at0001]/items[at0032]/onetomany as v ";
			List results = s
					.createQuery(query)
					.list();

			assertEquals(results.size(), 4);

			List<Locatable> patients = new ArrayList<Locatable>();
			List<Locatable> visits = new ArrayList<Locatable>();
			for (Object obj1 : results) {
				if (obj1 instanceof Locatable) {
					Locatable loc1 = (Locatable) obj1;	
					if (!patients.contains(loc1)) {
						patients.add(loc1);																
					}		
					for (Object obj2 : loc1.getAssociatedObjects().values()) {
						if (obj2 instanceof Locatable) {
							Locatable loc2 = (Locatable) obj2;
							if (!visits.contains(loc2)) {
								visits.add(loc2);																
							}
						}
					}
				}
			}

			assertEquals(patients.size(), 3);
			assertEquals(visits.size(), 0);
		}

		{
			String query = "select p, v " +
					"from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as p " +
					"left join p#/details[at0001]/items[at0032]/onetomany as v ";
			List results = s
					.createQuery(query)
					.list();

			assertEquals(results.size(), 4);

			List<Locatable> patients = new ArrayList<Locatable>();
			List<Locatable> visits = new ArrayList<Locatable>();
			for (Object arr : results) {
				if (arr.getClass().isArray()) {
					for (int i = 0; i < Array.getLength(arr); i++)
					{
						Object obj = Array.get(arr, i);
						if (obj instanceof Locatable) {
							Locatable loc = (Locatable) obj;			
							if (loc.getArchetypeNodeId().compareToIgnoreCase("openEHR-DEMOGRAPHIC-PERSON.patient.v1") == 0) {
								if (!patients.contains(loc)) {
									patients.add(loc);									
								}
							}
							
							if (loc.getArchetypeNodeId().compareToIgnoreCase("openEHR-EHR-COMPOSITION.visit.v3") == 0) {
								if (!visits.contains(loc)) {
									visits.add(loc);									
								}
							}
						}						
					}					
				}
			}

			assertEquals(patients.size(), 3);
			assertEquals(visits.size(), 3);
		}

		s.close();
		
		cleanTestBaseData();
	}

	@Test
	public void testSelectParameterized() throws Exception {
		createTestBaseData();

		Session s = openSession();

		{
			String query = "select o "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "where o#/details[at0001]/items[at0009]/value/value = :name";
			List results = s
					.createQuery(query)
					.setParameter("name", "lisi")
					.list();

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
					+ "from openEHR-EHR-COMPOSITION.visit.v3 as o "
					+ "where o#/uid/value = :VisitId and o#/context/other_context[at0001]/items[at0015]/value/value = :PatientId";
			List results = s
					.createQuery(query)
					.setParameter("VisitId", "visit1")
					.setParameter("PatientId", "patient1")
					.list();

			assertEquals(results.size(), 1);
		}

		s.close();
		
		cleanTestBaseData();
	}

	@Test
	public void testUpdate() throws Exception {
		createTestBaseData();

		{
			String query = "select o "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "where o#/uid/value = 'patient1'";
			Session s = openSession();
			List results = s
					.createQuery(query)
					.list();
			s.close();

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
					+ "o#/details[at0001]/items[at0009]/value/value = 'lisi', "
					+ "o#/details[at0001]/items[at0004]/value/value = '1994-08-11T19:20:30+08:00' "
					+ "where o#/uid/value = 'patient1'";
			Session s = openSession();
			int ret = s.createQuery(query).executeUpdate();
			s.close();

			assertEquals(ret, 1);
		}

		{
			String query = "select o "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "where o#/uid/value = 'patient1'";
			Session s = openSession();
			List results = s
					.createQuery(query)
					.list();
			s.close();

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
		
		cleanTestBaseData();
	}

	@Test
	public void testUpdateParameterized() throws Exception {
		createTestBaseData();

		{
			String query = "select o "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "where o#/uid/value = 'patient1'";
			Session s = openSession();
			List results = s
					.createQuery(query)
					.list();
			s.close();

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
			Session s = openSession();
			int ret = s.createQuery(query).setParameter("name", "lisi")
					.setParameter("birthday", "1994-08-11T19:20:30+08:00")
					.setParameter("pid", "patient1").executeUpdate();
			s.close();

			assertEquals(ret, 1);
		}

		{
			String query = "select o "
					+ "from openEHR-DEMOGRAPHIC-PERSON.patient.v1 as o "
					+ "where o#/uid/value = 'patient1'";
			Session s = openSession();
			List results = s
					.createQuery(query)
					.list();
			s.close();

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
		
		cleanTestBaseData();
	}

}
