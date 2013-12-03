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
package org.hibernate.internal.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ClassUtils;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.archetype.ArchetypeRepository;
import org.hibernate.property.BasicPropertyAccessor;
import org.hibernate.property.ChainedPropertyAccessor;
import org.hibernate.property.DirectPropertyAccessor;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.proxy.archetype.ArchetypeProxy;
import org.hibernate.proxy.map.MapProxy;
import org.hibernate.type.PrimitiveType;
import org.hibernate.type.Type;
import org.openehr.am.archetype.Archetype;
import org.openehr.am.archetype.constraintmodel.CObject;
import org.openehr.am.archetype.ontology.ArchetypeOntology;
import org.openehr.am.archetype.ontology.ArchetypeTerm;
import org.openehr.rm.common.archetyped.Locatable;

/**
 * Utility class for various reflection operations.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked")
public final class ReflectHelper {

	//TODO: this dependency is kinda Bad
	private static final PropertyAccessor BASIC_PROPERTY_ACCESSOR = new BasicPropertyAccessor();
	private static final PropertyAccessor DIRECT_PROPERTY_ACCESSOR = new DirectPropertyAccessor();

	public static final Class[] NO_PARAM_SIGNATURE = new Class[0];
	public static final Object[] NO_PARAMS = new Object[0];

	public static final Class[] SINGLE_OBJECT_PARAM_SIGNATURE = new Class[] { Object.class };

	private static final Method OBJECT_EQUALS;
	private static final Method OBJECT_HASHCODE;

	static {
		Method eq;
		Method hash;
		try {
			eq = extractEqualsMethod( Object.class );
			hash = extractHashCodeMethod( Object.class );
		}
		catch ( Exception e ) {
			throw new AssertionFailure( "Could not find Object.equals() or Object.hashCode()", e );
		}
		OBJECT_EQUALS = eq;
		OBJECT_HASHCODE = hash;
	}

	/**
	 * Disallow instantiation of ReflectHelper.
	 */
	private ReflectHelper() {
	}

	/**
	 * Encapsulation of getting hold of a class's {@link Object#equals equals}  method.
	 *
	 * @param clazz The class from which to extract the equals method.
	 * @return The equals method reference
	 * @throws NoSuchMethodException Should indicate an attempt to extract equals method from interface.
	 */
	public static Method extractEqualsMethod(Class clazz) throws NoSuchMethodException {
		return clazz.getMethod( "equals", SINGLE_OBJECT_PARAM_SIGNATURE );
	}

	/**
	 * Encapsulation of getting hold of a class's {@link Object#hashCode hashCode} method.
	 *
	 * @param clazz The class from which to extract the hashCode method.
	 * @return The hashCode method reference
	 * @throws NoSuchMethodException Should indicate an attempt to extract hashCode method from interface.
	 */
	public static Method extractHashCodeMethod(Class clazz) throws NoSuchMethodException {
		return clazz.getMethod( "hashCode", NO_PARAM_SIGNATURE );
	}

	/**
	 * Determine if the given class defines an {@link Object#equals} override.
	 *
	 * @param clazz The class to check
	 * @return True if clazz defines an equals override.
	 */
	public static boolean overridesEquals(Class clazz) {
		Method equals;
		try {
			equals = extractEqualsMethod( clazz );
		}
		catch ( NoSuchMethodException nsme ) {
			return false; //its an interface so we can't really tell anything...
		}
		return !OBJECT_EQUALS.equals( equals );
	}

	/**
	 * Determine if the given class defines a {@link Object#hashCode} override.
	 *
	 * @param clazz The class to check
	 * @return True if clazz defines an hashCode override.
	 */
	public static boolean overridesHashCode(Class clazz) {
		Method hashCode;
		try {
			hashCode = extractHashCodeMethod( clazz );
		}
		catch ( NoSuchMethodException nsme ) {
			return false; //its an interface so we can't really tell anything...
		}
		return !OBJECT_HASHCODE.equals( hashCode );
	}

	/**
	 * Determine if the given class implements the given interface.
	 *
	 * @param clazz The class to check
	 * @param intf The interface to check it against.
	 * @return True if the class does implement the interface, false otherwise.
	 */
	public static boolean implementsInterface(Class clazz, Class intf) {
		assert intf.isInterface() : "Interface to check was not an interface";
		return intf.isAssignableFrom( clazz );
	}

	/**
	 * Perform resolution of a class name.
	 * <p/>
	 * Here we first check the context classloader, if one, before delegating to
	 * {@link Class#forName(String, boolean, ClassLoader)} using the caller's classloader
	 *
	 * @param name The class name
	 * @param caller The class from which this call originated (in order to access that class's loader).
	 * @return The class reference.
	 * @throws ClassNotFoundException From {@link Class#forName(String, boolean, ClassLoader)}.
	 */
	public static Class classForName(String name, Class caller) throws ClassNotFoundException {
		try {
			ClassLoader classLoader = ClassLoaderHelper.getContextClassLoader();
			if ( classLoader != null ) {
				return classLoader.loadClass( name );
			}
		}
		catch ( Throwable ignore ) {
		}
		return Class.forName( name, true, caller.getClassLoader() );
	}

	/**
	 * Perform resolution of a class name.
	 * <p/>
	 * Same as {@link #classForName(String, Class)} except that here we delegate to
	 * {@link Class#forName(String)} if the context classloader lookup is unsuccessful.
	 *
	 * @param name The class name
	 * @return The class reference.
	 * @throws ClassNotFoundException From {@link Class#forName(String)}.
	 */
	public static Class classForName(String name) throws ClassNotFoundException {
		try {
			ClassLoader classLoader = ClassLoaderHelper.getContextClassLoader();
			if ( classLoader != null ) {
				return classLoader.loadClass(name);
			}
		}
		catch ( Throwable ignore ) {
		}
		return Class.forName( name );
	}

	/**
	 * Is this member publicly accessible.
	 * <p/>
	 * Short-hand for {@link #isPublic(Class, Member)} passing the member + {@link Member#getDeclaringClass()}
	 *
	 * @param member The member to check
	 * @return True if the member is publicly accessible.
	 */
	public static boolean isPublic(Member member) {
		return isPublic( member.getDeclaringClass(), member );
	}

	/**
	 * Is this member publicly accessible.
	 *
	 * @param clazz The class which defines the member
	 * @param member The memeber.
	 * @return True if the member is publicly accessible, false otherwise.
	 */
	public static boolean isPublic(Class clazz, Member member) {
		return Modifier.isPublic( member.getModifiers() ) && Modifier.isPublic( clazz.getModifiers() );
	}

	/**
	 * Attempt to resolve the specified property type through reflection.
	 *
	 * @param className The name of the class owning the property.
	 * @param name The name of the property.
	 * @return The type of the property.
	 * @throws MappingException Indicates we were unable to locate the property.
	 */
	public static Class reflectedPropertyClass(String className, String name) throws MappingException {
		try {
			Class clazz = ReflectHelper.classForName( className );
			return getter( clazz, name ).getReturnType();
		}
		catch ( ClassNotFoundException cnfe ) {
			throw new MappingException( "class " + className + " not found while looking for property: " + name, cnfe );
		}
	}

	/**
	 * Attempt to resolve the specified property type through reflection.
	 *
	 * @param archetypeName The name of the class owning the property.
	 * @param name The name of the property.
	 * @return The type of the property.
	 * @throws MappingException Indicates we were unable to locate the property.
	 */
	public static Class reflectedPropertyArchetype(Archetype archetype, String name) throws MappingException {
		Map<String, CObject> patheNodeMap = archetype.getPathNodeMap();
		String nodePath = getArchetypeNodePath(archetype, name);
		CObject node = patheNodeMap.get(nodePath);
		String attributePath = name.substring(nodePath.length());
		String[] attributePathSegments = attributePath.split("/");
		Class klass = ArchetypeRepository.getRMBuilder().retrieveRMType(node.getRmTypeName());			
		if (klass != null) {
			for (String pathSegment : attributePathSegments) {
				if (!pathSegment.isEmpty()) {
					klass = getter( klass, pathSegment ).getReturnType();					
				}
			}				
		}
		
		return klass;
	}

	public static String getArchetypeNodePath(Archetype archetype, String name) {
		Map<String, CObject> patheNodeMap = archetype.getPathNodeMap();
		Set<String> pathSet = patheNodeMap.keySet();
		String nodePath = "";
		for (String path : pathSet) {
			if (name.startsWith(path)) {
				if (path.length() > nodePath.length()) {
					nodePath = path;
				}
			}
		}			
		
		return nodePath;
	}

	/**
	 * Attempt to resolve the specified property type through reflection.
	 *
	 * @param clazz The class owning the property.
	 * @param name The name of the property.
	 * @return The type of the property.
	 * @throws MappingException Indicates we were unable to locate the property.
	 */
	public static Class reflectedPropertyClass(Class clazz, String name) throws MappingException {
		return getter( clazz, name ).getReturnType();
	}

	private static Getter getter(Class clazz, String name) throws MappingException {
		try {
			return BASIC_PROPERTY_ACCESSOR.getGetter( clazz, name );
		}
		catch ( PropertyNotFoundException pnfe ) {
			return DIRECT_PROPERTY_ACCESSOR.getGetter( clazz, name );
		}
	}

	/**
	 * Directly retrieve the {@link Getter} reference via the {@link BasicPropertyAccessor}.
	 *
	 * @param theClass The class owning the property
	 * @param name The name of the property
	 * @return The getter.
	 * @throws MappingException Indicates we were unable to locate the property.
	 */
	public static Getter getGetter(Class theClass, String name) throws MappingException {
		return BASIC_PROPERTY_ACCESSOR.getGetter( theClass, name );
	}

	/**
	 * Resolve a constant to its actual value.
	 *
	 * @param name The name
	 * @return The value
	 */
	public static Object getConstantValue(String name) {
		Class clazz;
		try {
			clazz = classForName( StringHelper.qualifier( name ) );
		}
		catch ( Throwable t ) {
			return null;
		}
		try {
			return clazz.getField( StringHelper.unqualify( name ) ).get( null );
		}
		catch ( Throwable t ) {
			return null;
		}
	}

	/**
	 * Retrieve the default (no arg) constructor from the given class.
	 *
	 * @param clazz The class for which to retrieve the default ctor.
	 * @return The default constructor.
	 * @throws PropertyNotFoundException Indicates there was not publicly accessible, no-arg constructor (todo : why PropertyNotFoundException???)
	 */
	public static Constructor getDefaultConstructor(Class clazz) throws PropertyNotFoundException {
		if ( isAbstractClass( clazz ) ) {
			return null;
		}

		try {
			Constructor constructor = clazz.getDeclaredConstructor( NO_PARAM_SIGNATURE );
			if ( !isPublic( clazz, constructor ) ) {
				constructor.setAccessible( true );
			}
			return constructor;
		}
		catch ( NoSuchMethodException nme ) {
			throw new PropertyNotFoundException(
					"Object class [" + clazz.getName() + "] must declare a default (no-argument) constructor"
			);
		}
	}

	/**
	 * Determine if the given class is declared abstract.
	 *
	 * @param clazz The class to check.
	 * @return True if the class is abstract, false otherwise.
	 */
	public static boolean isAbstractClass(Class clazz) {
		int modifier = clazz.getModifiers();
		return Modifier.isAbstract(modifier) || Modifier.isInterface(modifier);
	}

	/**
	 * Determine is the given class is declared final.
	 *
	 * @param clazz The class to check.
	 * @return True if the class is final, flase otherwise.
	 */
	public static boolean isFinalClass(Class clazz) {
		return Modifier.isFinal( clazz.getModifiers() );
	}

	/**
	 * Retrieve a constructor for the given class, with arguments matching the specified Hibernate mapping
	 * {@link Type types}.
	 *
	 * @param clazz The class needing instantiation
	 * @param types The types representing the required ctor param signature
	 * @return The matching constructor.
	 * @throws PropertyNotFoundException Indicates we could not locate an appropriate constructor (todo : again with PropertyNotFoundException???)
	 */
	public static Constructor getConstructor(Class clazz, Type[] types) throws PropertyNotFoundException {
		final Constructor[] candidates = clazz.getConstructors();
		for ( int i = 0; i < candidates.length; i++ ) {
			final Constructor constructor = candidates[i];
			final Class[] params = constructor.getParameterTypes();
			if ( params.length == types.length ) {
				boolean found = true;
				for ( int j = 0; j < params.length; j++ ) {
					final boolean ok = params[j].isAssignableFrom( types[j].getReturnedClass() ) || (
							types[j] instanceof PrimitiveType &&
									params[j] == ( ( PrimitiveType ) types[j] ).getPrimitiveClass()
					);
					if ( !ok ) {
						found = false;
						break;
					}
				}
				if ( found ) {
					if ( !isPublic( clazz, constructor ) ) {
						constructor.setAccessible( true );
					}
					return constructor;
				}
			}
		}
		throw new PropertyNotFoundException( "no appropriate constructor in class: " + clazz.getName() );
	}
	
	public static Method getMethod(Class clazz, Method method) {
		try {
			return clazz.getMethod( method.getName(), method.getParameterTypes() );
		}
		catch (Exception e) {
			return null;
		}
	}
	
	public static void setArchetypeValue(Locatable loc, Map<String, Object> values) 
			throws InstantiationException, IllegalAccessException {
		for (String path : values.keySet()) {
			setArchetypeValue(loc, path, values.get(path));
		}
	}
	
	public static void setArchetypeValue(Locatable loc, String propertyPath, Object propertyValue) 
			throws InstantiationException, IllegalAccessException {

		Archetype archetype = ArchetypeRepository.getArchetype(loc.getArchetypeNodeId());
		Map<String, CObject> pathNodeMap = archetype.getPathNodeMap();
		String nodePath = ReflectHelper.getArchetypeNodePath(archetype, propertyPath);
		if (nodePath.compareTo(propertyPath) == 0) {
			loc.set(nodePath, propertyValue);
		}
		else {
			CObject node = pathNodeMap.get(nodePath);
			Object target = loc.itemAtPath(nodePath);
			if (target == null) {
				Class klass = ArchetypeRepository.getRMBuilder().retrieveRMType(node.getRmTypeName());
				target = klass.newInstance();
			}
			
			String attributePath = propertyPath.substring(nodePath.length());
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
					Getter getter = propertyAccessor.getGetter(tempTarget.getClass(), pathSegment);
					if (klass.isPrimitive() || 
							ClassUtils.wrapperToPrimitive(klass) != null || 
							String.class.isAssignableFrom(klass) ||
							Set.class.isAssignableFrom(klass)) {						
						if (propertyValue instanceof Locatable) {
							String uid = ((Locatable) propertyValue).getUid().getValue();
							setter.set(tempTarget, uid, null);
							loc.getAssociatedObjects().put(uid, propertyValue);
						}
						else if (propertyValue instanceof ArchetypeProxy) {
							LazyInitializer li = ((ArchetypeProxy) propertyValue).getHibernateLazyInitializer();
							setter.set(tempTarget, li.getIdentifier(), null);
						}
						else {
							setter.set(tempTarget, propertyValue, null);
						}						
					} 
					else {
						Object value = klass.newInstance();
						setter.set(tempTarget, value, null);
						tempTarget = value;								
					}
				}
			}
		}
	}

}
