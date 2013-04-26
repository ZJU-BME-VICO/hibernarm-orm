/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.xml.arm;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.openehr.am.parser.ContentObject;
import org.openehr.am.parser.DADLParser;
import org.openehr.build.RMObjectBuilder;
import org.openehr.rm.binding.DADLBinding;
import org.openehr.rm.composition.content.entry.Observation;
import org.openehr.rm.datatypes.text.DvText;
import org.openehr.rm.support.identification.HierObjectID;
import org.openehr.rm.support.identification.UID;
import org.openehr.rm.support.identification.UIDBasedID;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Emmanuel Bernard
 */
public class ArmTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testManyToOne() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
//		Government gov = new Government();
//		gov.setName( "Liberals" );
//		s.save( gov );
//		PrimeMinister pm = new PrimeMinister();
//		pm.setName( "Murray" );
//		pm.setCurrentGovernment( gov );
//		s.save( pm );
//		s.getTransaction().rollback();
//		s.close();
	}

	@Test
	public void testOneToMany() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
//		Government gov = new Government();
//		gov.setName( "Liberals" );
//		Government gov2 = new Government();
//		gov2.setName( "Liberals2" );
//		s.save( gov );
//		s.save( gov2 );
//		PrimeMinister pm = new PrimeMinister();
//		pm.setName( "Murray" );
//		pm.setCurrentGovernment( gov );
//		pm.setGovernments( new HashSet() );
//		pm.getGovernments().add( gov2 );
//		pm.getGovernments().add( gov );
//		gov.setPrimeMinister( pm );
//		gov2.setPrimeMinister( pm );
//		s.save( pm );
//		s.flush();
//		s.getTransaction().rollback();
//		s.close();
	}

	@Test
	public void testManyToMany() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		
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
		
		
//		RMObjectBuilder builder = configuration().getRMObjectBuilder();
//		Map<String, Object> values = new HashMap<String, Object>();
//        DvText name = new DvText("test observation", lang, charset, ts);
//        String node = "at0001";
//        Archetyped archetypeDetails = new Archetyped(
//                new ArchetypeID("openehr-ehr_rm-observation.physical_examination.v3"), "v1.0");
//        History<ItemStructure> data = event();
//        values.put("archetypeNodeId", node);
//        values.put("archetypeDetails", archetypeDetails);
//        values.put("name", name);
//        values.put("language", lang);
//        values.put("encoding", charset);
//        values.put("subject", subject());
//        values.put("provider", provider());
//        values.put("data", data);
//        RMObject obj = builder.construct("Observation", values);
		
		s.flush();
		s.getTransaction().rollback();
		s.close();
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
}
