/*
 * daowangli@gmail.com
 */

package org.hibernate.archetype;

import java.util.HashMap;
import java.util.Map;

import org.openehr.am.archetype.Archetype;
import org.openehr.build.RMObjectBuilder;
import org.openehr.build.SystemValue;
import org.openehr.rm.datatypes.text.CodePhrase;
import org.openehr.rm.support.measurement.MeasurementService;
import org.openehr.rm.support.measurement.SimpleMeasurementService;
import org.openehr.rm.support.terminology.TerminologyService;
import org.openehr.terminology.SimpleTerminologyService;

public class ArchetypeRepository {

	private static Map<String, Archetype> archetypes = new HashMap<String, Archetype>();
	private static RMObjectBuilder rmBuilder = null;

	protected static CodePhrase lang = new CodePhrase("ISO_639-1", "en");
	protected static CodePhrase charset = new CodePhrase("IANA_character-sets",
			"UTF-8");
	protected static TerminologyService ts;
	protected static MeasurementService ms;

	static {
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

	public static Archetype getArchetype(String key) {
		return archetypes.get(key);
	}

	public static void addArchetype(String key, Archetype value) {
		ArchetypeRepository.archetypes.put(key, value);
	}

	public static void addArchetype(Archetype value) {
		if (value != null) {
			String key = value.getArchetypeId().getValue();
			ArchetypeRepository.archetypes.put(key, value);
		}
	}

	public static RMObjectBuilder getRMBuilder() {
		return rmBuilder;
	}

}
