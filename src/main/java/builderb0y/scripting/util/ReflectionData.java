package builderb0y.scripting.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ReflectionData {

	public static final ClassValue<ReflectionData> LOOKUP = new ClassValue<ReflectionData>() {

		@Override
		protected ReflectionData computeValue(Class<?> type) {
			return new ReflectionData(type);
		}
	};

	public final Class<?> clazz;
	public final List<Field> declaredFields, derivedFields;
	public final List<Method> declaredMethods, derivedMethods;
	public final Map<String, List<Field>> declaredFieldsByName, derivedFieldsByName;
	public final Map<String, List<Method>> declaredMethodsByName, derivedMethodsByName;

	public ReflectionData(Class<?> clazz) {
		this.clazz = clazz;
		this.declaredFields = Arrays.asList(clazz.getDeclaredFields());
		this.declaredMethods = Arrays.asList(clazz.getDeclaredMethods());
		this.declaredFieldsByName = this.declaredFields.stream().collect(Collectors.groupingBy(Field::getName));
		this.declaredMethodsByName = this.declaredMethods.stream().collect(Collectors.groupingBy(Method::getName));
		this.derivedFields = new ArrayList<>(16);
		this.derivedMethods = new ArrayList<>(64);
		this.derivedFieldsByName = new HashMap<>(16);
		this.derivedMethodsByName = new HashMap<>(64);
		this.recursiveAddDerived(this, new HashSet<>(16));
	}

	public static ReflectionData forClass(Class<?> clazz) {
		return LOOKUP.get(clazz);
	}

	public List<Field> getDeclaredFields() {
		return this.declaredFields;
	}

	public List<Field> getDerivedFields() {
		return this.derivedFields;
	}

	public List<Method> getDeclaredMethods() {
		return this.declaredMethods;
	}

	public List<Method> getDerivedMethods() {
		return this.derivedMethods;
	}

	public List<Field> getDeclaredFields(String name) {
		return this.declaredFieldsByName.getOrDefault(name, Collections.emptyList());
	}

	public List<Field> getDerivedFields(String name) {
		return this.derivedFieldsByName.getOrDefault(name, Collections.emptyList());
	}

	public List<Method> getDeclaredMethods(String name) {
		return this.declaredMethodsByName.getOrDefault(name, Collections.emptyList());
	}

	public List<Method> getDerivedMethods(String name) {
		return this.derivedMethodsByName.getOrDefault(name, Collections.emptyList());
	}

	public Field getDeclaredField(String name) {
		return this.checkSingleton(this.getDeclaredFields(name), name, "field");
	}

	public Field getDerivedField(String name) {
		return this.checkSingleton(this.getDerivedFields(name), name, "field");
	}

	public Method getDeclaredMethod(String name) {
		return this.checkSingleton(this.getDeclaredMethods(name), name, "method");
	}

	public Method getDerivedMethod(String name) {
		return this.checkSingleton(this.getDerivedMethods(name), name, "method");
	}

	public Field findDeclaredField(String name, Class<?> type) {
		for (Field field : this.getDeclaredFields(name)) {
			if (field.getType() == type) return field;
		}
		throw new IllegalArgumentException("No such field with name " + name + " of type " + type + " in " + this.clazz);
	}

	public Field findDerivedField(String name, Class<?> type) {
		for (Field field : this.getDerivedFields(name)) {
			if (field.getType() == type) return field;
		}
		throw new IllegalArgumentException("No such field with name " + name + " of type " + type + " in " + this.clazz);
	}

	public Method findDeclaredMethod(String name, Class<?> returnType, Class<?>... parameterTypes) {
		for (Method method : this.getDeclaredMethods(name)) {
			if (method.getReturnType() == returnType && Arrays.equals(method.getParameterTypes(), parameterTypes)) return method;
		}
		throw new IllegalArgumentException("No such method with name " + name + " of type " + returnType.getName() + Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.joining(", ", "(", ")")) + " in " + this.clazz);
	}

	public Method findDerivedMethod(String name, Class<?> returnType, Class<?>... parameterTypes) {
		for (Method method : this.getDerivedMethods(name)) {
			if (method.getReturnType() == returnType && Arrays.equals(method.getParameterTypes(), parameterTypes)) return method;
		}
		throw new IllegalArgumentException("No such method with name " + name + " of type " + returnType.getName() + Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.joining(", ", "(", ")")) + " in " + this.clazz);
	}

	public Field findDeclaredField(String name, Predicate<Field> predicate) {
		return this.find(this.getDeclaredFields(name), predicate, name, "field");
	}

	public Field findDerivedField(String name, Predicate<Field> predicate) {
		return this.find(this.getDerivedFields(name), predicate, name, "field");
	}

	public Method findDeclaredMethod(String name, Predicate<Method> predicate) {
		return this.find(this.getDeclaredMethods(name), predicate, name, "method");
	}

	public Method findDerivedMethod(String name, Predicate<Method> predicate) {
		return this.find(this.getDerivedMethods(name), predicate, name, "method");
	}

	public <T> T find(List<T> list, Predicate<T> predicate, String name, String type) {
		T found = null;
		for (T element : list) {
			if (predicate.test(element)) {
				if (found == null) found = element;
				else throw new IllegalArgumentException("More than one " + type + " with name " + name + " which matches " + predicate + " in " + this.clazz);
			}
		}
		if (found == null) throw new IllegalArgumentException("No " + type + "s with name " + name + " which matches " + predicate + " in " + this.clazz);
		return found;
	}

	public <T> T checkSingleton(List<T> list, String name, String type) {
		if (list.isEmpty()) {
			throw new IllegalArgumentException("No " + type + "s with name " + name + " in " + this.clazz);
		}
		if (list.size() > 1) {
			throw new IllegalArgumentException("More than one " + type + " with name " + name + " in " + this.clazz);
		}
		return list.get(0);
	}

	public void recursiveAddDerived(ReflectionData data, Set<Class<?>> seen) {
		if (!seen.add(data.clazz)) return;
		this.derivedFields.addAll(data.declaredFields);
		this.derivedMethods.addAll(data.derivedMethods);
		for (Map.Entry<String, List<Field>> entry : data.derivedFieldsByName.entrySet()) {
			//can't rely on merge() because if the list is not already in the map,
			//then it needs to be copied before adding it to the map.
			List<Field> fields = this.derivedFieldsByName.get(entry.getKey());
			if (fields != null) fields.addAll(entry.getValue());
			else this.derivedFieldsByName.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		for (Map.Entry<String, List<Method>> entry : data.derivedMethodsByName.entrySet()) {
			List<Method> methods = this.derivedMethodsByName.get(entry.getKey());
			if (methods != null) methods.addAll(entry.getValue());
			else this.derivedMethodsByName.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		Class<?> superClass = data.clazz.getSuperclass();
		if (superClass != null) {
			this.recursiveAddDerived(forClass(superClass), seen);
		}
		for (Class<?> superInterface : data.clazz.getInterfaces()) {
			this.recursiveAddDerived(forClass(superInterface), seen);
		}
	}
}