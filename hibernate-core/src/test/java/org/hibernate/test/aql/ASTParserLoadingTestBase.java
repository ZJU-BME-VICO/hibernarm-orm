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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;
import org.openehr.am.archetype.Archetype;
import org.openehr.rm.common.archetyped.Locatable;
import org.openehr.rm.util.GenerationStrategy;
import org.openehr.rm.util.SkeletonGenerator;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.archetype.ArchetypeRepository;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

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
public class ASTParserLoadingTestBase extends BaseCoreFunctionalTestCase {
	private static final Logger log = Logger
			.getLogger(ASTParserLoadingTestBase.class);

	@Override
	protected boolean isCleanupTestDataRequired() {
		return false;
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure(cfg);
		cfg.setProperty(Environment.USE_QUERY_CACHE, "true");
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
		cfg.setProperty( Environment.QUERY_TRANSLATOR, ASTQueryTranslatorFactory.class.getName() );
	}

	@Override
	protected String[] getAdlFiles() {
		return new String[] {
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
				"openEHR-EHR-OBSERVATION.adl.v1",
				"openEHR-EHR-OBSERVATION.cdr.v1",
				"openEHR-EHR-OBSERVATION.gds.v1",
				"openEHR-EHR-OBSERVATION.mmse.v1", 
				"openEHR-EHR-OBSERVATION.other_cognitions_scale_exams.v1",
				"openEHR-EHR-COMPOSITION.visit.v3",
				"openEHR-DEMOGRAPHIC-PERSON.patient.v1", };
	}

	protected List<Map<HashMap<String, Object>, String>> getArchetypeValues() {		
		Map<HashMap<String, Object>, String> patients = new HashMap<HashMap<String, Object>, String>();

		{
			HashMap<String, Object> patient1 = new HashMap<String, Object>();
			patient1.put("/uid/value", "patient1");
			patient1.put("/details[at0001]/items[at0003]/value/value", "M");
			patient1.put("/details[at0001]/items[at0004]/value/value",
					"1984-08-11T19:20:30+08:00");
			patient1.put("/details[at0001]/items[at0009]/value/value",
					"zhangsan");
			patients.put(patient1, "openEHR-DEMOGRAPHIC-PERSON.patient.v1");
		}

		{
			HashMap<String, Object> patient2 = new HashMap<String, Object>();
			patient2.put("/uid/value", "patient2");
			patient2.put("/details[at0001]/items[at0003]/value/value", "F");
			patient2.put("/details[at0001]/items[at0004]/value/value",
					"1986-08-11T19:20:30+08:00");
			patient2.put("/details[at0001]/items[at0009]/value/value", "lisi");
			patients.put(patient2, "openEHR-DEMOGRAPHIC-PERSON.patient.v1");
		}

		{
			HashMap<String, Object> patient3 = new HashMap<String, Object>();
			patient3.put("/uid/value", "patient3");
			patient3.put("/details[at0001]/items[at0003]/value/value", "O");
			patient3.put("/details[at0001]/items[at0004]/value/value",
					"1988-08-11T19:20:30+08:00");
			patient3.put("/details[at0001]/items[at0009]/value/value", "wangwu");
			patients.put(patient3, "openEHR-DEMOGRAPHIC-PERSON.patient.v1");
		}

		Map<HashMap<String, Object>, String> visits = new HashMap<HashMap<String, Object>, String>();
		
		{
			HashMap<String, Object> visit1 = new HashMap<String, Object>();
			visit1.put("/uid/value", "visit1");
			visit1.put(
					"/context/other_context[at0001]/items[at0007]/value/value",
					"2010-01-15T19:20:30+08:00");
			visit1.put(
					"/context/other_context[at0001]/items[at0015]/value/value",
					"patient1");
			visits.put(visit1, "openEHR-EHR-COMPOSITION.visit.v3");
		}

		{
			HashMap<String, Object> visit2 = new HashMap<String, Object>();
			visit2.put("/uid/value", "visit2");
			visit2.put(
					"/context/other_context[at0001]/items[at0007]/value/value",
					"2010-01-25T19:20:30+08:00");
			visit2.put(
					"/context/other_context[at0001]/items[at0015]/value/value",
					"patient1");
			visits.put(visit2, "openEHR-EHR-COMPOSITION.visit.v3");
		}

		{
			HashMap<String, Object> visit3 = new HashMap<String, Object>();
			visit3.put("/uid/value", "visit3");
			visit3.put(
					"/context/other_context[at0001]/items[at0007]/value/value",
					"2011-02-05T19:20:30+08:00");
			visit3.put(
					"/context/other_context[at0001]/items[at0015]/value/value",
					"patient2");
			visits.put(visit3, "openEHR-EHR-COMPOSITION.visit.v3");
		}

		Map<HashMap<String, Object>, String> others = new HashMap<HashMap<String, Object>, String>();

		{
			HashMap<String, Object> other_cognitions_scale_exams1 = new HashMap<String, Object>();
			other_cognitions_scale_exams1.put("/uid/value",
					"other_cognitions_scale_exams1");
			other_cognitions_scale_exams1
					.put("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0005]/value/magnitude",
							1);
			other_cognitions_scale_exams1
					.put("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0006]/value/magnitude",
							2);
			other_cognitions_scale_exams1
					.put("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0007]/value/magnitude",
							3);
			other_cognitions_scale_exams1
					.put("/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0096]/value/magnitude",
							6);
			others.put(other_cognitions_scale_exams1,
					"openEHR-EHR-OBSERVATION.other_cognitions_scale_exams.v1");
		}

		{
			HashMap<String, Object> mmse1 = new HashMap<String, Object>();
			mmse1.put("/uid/value", "mmse1");
			mmse1.put(
					"/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0005]/value/value",
					false);
			mmse1.put(
					"/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0009]/value/value",
					false);
			mmse1.put(
					"/data[at0001]/events[at0002]/data[at0003]/items[at0004]/items[at0012]/value/value",
					false);
			others.put(mmse1, "openEHR-EHR-OBSERVATION.mmse.v1");
		}

		List<Map<HashMap<String, Object>, String>> results = new ArrayList<Map<HashMap<String, Object>, String>>();
		results.add(patients);
		results.add(visits);
		results.add(others);
		
		return results;
	}

	protected void createTestBaseData() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();

		List<Map<HashMap<String, Object>, String>> list = getArchetypeValues();
		for (Map<HashMap<String, Object>, String> archetypeValues : list) {
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
			s.createQuery(aql).executeUpdate();
		}

		s.flush();
		txn.commit();
		s.close();
	}

}
