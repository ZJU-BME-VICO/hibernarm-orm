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
import org.hibernate.archetype.ArchetypeRepository;
import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.internal.util.ReflectHelper;
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
	
	@Test
	public void testSimpleInsertPerformance() throws Exception {
		long start = System.currentTimeMillis();
		long s3=0;
		for (int i =0;i <1000;i++){
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

			long s1=System.currentTimeMillis();
			s.flush();
			long s2=System.currentTimeMillis()-s1;
			s3=s3+s2;
			txn.commit();
			s.close();
			cleanTestBaseData();
		}
		long end = System.currentTimeMillis();
		System.out.println(s3);	
		System.out.println(end-start);
		System.out.println("done");

	}

}
