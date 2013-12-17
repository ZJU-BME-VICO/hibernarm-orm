/*
 * daowangli@gmail.com
 */

package org.hibernate.archetype;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openehr.am.archetype.Archetype;
import org.openehr.build.RMObjectBuilder;
import org.openehr.build.SystemValue;
import org.openehr.rm.datatypes.text.CodePhrase;
import org.openehr.rm.support.measurement.MeasurementService;
import org.openehr.rm.support.measurement.SimpleMeasurementService;
import org.openehr.rm.support.terminology.TerminologyService;
import org.openehr.terminology.SimpleTerminologyService;

public enum ArchetypeRepository {

	INSTANCE;

	private Map<String, Archetype> archetypes = new HashMap<String, Archetype>();
	private RMObjectBuilder rmBuilder = null;

	protected CodePhrase lang = new CodePhrase("ISO_639-1", "en");
	protected CodePhrase charset = new CodePhrase("IANA_character-sets",
			"UTF-8");
	protected TerminologyService ts = null;
	protected MeasurementService ms = null;

	private ArchetypeRepository() {
		try {
			ts = SimpleTerminologyService.getInstance();
			ms = SimpleMeasurementService.getInstance();

			Map<SystemValue, Object> values = new HashMap<SystemValue, Object>();
			values.put(SystemValue.LANGUAGE, lang);
			values.put(SystemValue.CHARSET, charset);
			values.put(SystemValue.ENCODING, charset);
			values.put(SystemValue.TERMINOLOGY_SERVICE, ts);
			values.put(SystemValue.MEASUREMENT_SERVICE, ms);

			rmBuilder = new RMObjectBuilder(values);
		} catch (Exception e) {
			throw new RuntimeException(
					"failed to start terminology or measure service");
		}
	}

	public Archetype getArchetype(String key) {
		return archetypes.get(key);
	}

	public void addArchetype(String key, Archetype value) {
		archetypes.put(key, value);
	}

	public void addArchetype(Archetype value) {
		if (value != null) {
			String key = value.getArchetypeId().getValue();
			archetypes.put(key, value);
		}
	}

	public RMObjectBuilder getRMBuilder() {
		return rmBuilder;
	}
	
	public Set<String> getArchetypeIds() {
		return archetypes.keySet();
	}

}
