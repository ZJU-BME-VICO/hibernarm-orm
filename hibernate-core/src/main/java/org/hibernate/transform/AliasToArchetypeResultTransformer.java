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
 */
package org.hibernate.transform;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;
import org.hibernate.archetype.ArchetypeRepository;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.ChainedPropertyAccessor;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.openehr.am.archetype.Archetype;
import org.openehr.am.archetype.constraintmodel.CObject;
import org.openehr.rm.common.archetyped.Locatable;
import org.openehr.rm.util.GenerationStrategy;
import org.openehr.rm.util.SkeletonGenerator;

/**
 * Result transformer that allows to transform a result to
 * a user specified class which will be populated via setter
 * methods or fields matching the alias names.
 * <p/>
 * <pre>
 * List resultWithAliasedBean = s.createCriteria(Enrolment.class)
 * 			.createAlias("student", "st")
 * 			.createAlias("course", "co")
 * 			.setProjection( Projections.projectionList()
 * 					.add( Projections.property("co.description"), "courseDescription" )
 * 			)
 * 			.setResultTransformer( new AliasToBeanResultTransformer(StudentDTO.class) )
 * 			.list();
 * <p/>
 *  StudentDTO dto = (StudentDTO)resultWithAliasedBean.get(0);
 * 	</pre>
 *
 * @author max
 */
public class AliasToArchetypeResultTransformer extends AliasedTupleSubsetResultTransformer {

	// IMPL NOTE : due to the delayed population of setters (setters cached
	// 		for performance), we really cannot properly define equality for
	// 		this transformer

	private final String archetypeId;
	private boolean isInitialized;
	private String[] aliases;

	public AliasToArchetypeResultTransformer(String archetypeId) {
		isInitialized = false;
		this.archetypeId = archetypeId;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isTransformedValueATupleElement(String[] aliases, int tupleLength) {
		return false;
	}	

	public Object transformTuple(Object[] tuple, String[] aliases) {
		try {
			SkeletonGenerator generator = SkeletonGenerator.getInstance();
			Archetype archetype = ArchetypeRepository.getArchetype(archetypeId);
			Object result = generator.create(archetype, GenerationStrategy.MAXIMUM_EMPTY);
			if (result instanceof Locatable) {
				Locatable loc = (Locatable) result;
				if ( ! isInitialized ) {
					initialize( aliases );
				}
				else {
					check( aliases );
				}

				for ( int i = 0; i < aliases.length; i++ ) {
					Map<String, CObject> patheNodeMap = archetype.getPathNodeMap();
					String nodePath = ReflectHelper.getArchetypeNodePath(archetype, aliases[i]);
					CObject node = patheNodeMap.get(nodePath);
					Object target = loc.itemAtPath(nodePath);
					if (target == null) {
//						target = ArchetypeRepository.getRMBuilder().construct(node.getRmTypeName(), new HashMap<String, Object>());
						Class klass = ArchetypeRepository.getRMBuilder().retrieveRMType(node.getRmTypeName());
						target = klass.newInstance();
					}
					
					String attributePath = aliases[i].substring(nodePath.length());
					String[] attributePathSegments = attributePath.split("/");
					Object tempTarget = target;
					for (String pathSegment : attributePathSegments) {
						if (!pathSegment.isEmpty()) {
							Class klass = ReflectHelper.getter(tempTarget.getClass(), pathSegment).getReturnType();
							PropertyAccessor propertyAccessor = new ChainedPropertyAccessor(
									new PropertyAccessor[] {
											PropertyAccessorFactory.getPropertyAccessor(tempTarget.getClass(), null),
											PropertyAccessorFactory.getPropertyAccessor("field")
									}
							);
							Setter setter = propertyAccessor.getSetter(tempTarget.getClass(), pathSegment);
							if (klass.isPrimitive() || ClassUtils.wrapperToPrimitive(klass) != null) {
								setter.set(tempTarget, tuple[i], null);
							} 
							else {
								Object value = klass.newInstance();
								setter.set(tempTarget, value, null);
								tempTarget = value;								
							}
						}
					}
					
					loc.set(nodePath, target);
				}

				return loc;				
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	private void initialize(String[] aliases) {
		this.aliases = new String[ aliases.length ];
		for ( int i = 0; i < aliases.length; i++ ) {
			String alias = aliases[ i ];
			if ( alias != null ) {
				this.aliases[ i ] = alias;
			}
		}
		isInitialized = true;
	}

	private void check(String[] aliases) {
		if ( ! Arrays.equals( aliases, this.aliases ) ) {
			throw new IllegalStateException(
					"aliases are different from what is cached; aliases=" + Arrays.asList( aliases ) +
							" cached=" + Arrays.asList( this.aliases ) );
		}
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		AliasToArchetypeResultTransformer that = ( AliasToArchetypeResultTransformer ) o;

		if ( ! archetypeId.equals( that.archetypeId ) ) {
			return false;
		}
		if ( ! Arrays.equals( aliases, that.aliases ) ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = archetypeId.hashCode();
		result = 31 * result + ( aliases != null ? Arrays.hashCode( aliases ) : 0 );
		return result;
	}
}
