/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 * daowangli@gmail.com
 */
package org.hibernate.tuple;
import java.io.Serializable;
import org.hibernate.archetype.ArchetypeRepository;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.binding.EntityBinding;
import org.openehr.am.archetype.Archetype;
import org.openehr.rm.common.archetyped.Locatable;
import org.openehr.rm.util.GenerationStrategy;
import org.openehr.rm.util.SkeletonGenerator;


public class ArchetypeInstantiator implements Instantiator {

	private String entityName;

	public ArchetypeInstantiator() {
		this.entityName = null;
	}

	public ArchetypeInstantiator(PersistentClass mappingInfo) {
		this.entityName = mappingInfo.getEntityName();
	}

	public ArchetypeInstantiator(EntityBinding mappingInfo) {
		this.entityName = mappingInfo.getEntity().getName();
	}

	public final Object instantiate(Serializable id) {
		return instantiate();
	}

	public final Object instantiate() {
		Object result = null;
		try {
			SkeletonGenerator generator = SkeletonGenerator.getInstance();
			Archetype archetype = ArchetypeRepository.INSTANCE.getArchetype(entityName);
			result = generator.create(archetype, GenerationStrategy.MAXIMUM_EMPTY);			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return result;
	}

	public final boolean isInstance(Object object) {
		if ( object instanceof Locatable ) {
			if ( entityName == null ) {
				return true;
			}

			Locatable loc = (Locatable) object;
			return entityName.equals(loc.getArchetypeNodeId());
		}
		else {
			return false;
		}
	}
}