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
public class ASTParserLoadingPerformanceTest extends ASTParserLoadingTestBase {
	private static final Logger log = Logger
			.getLogger(ASTParserLoadingPerformanceTest.class);
	
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
