package builderb0y.bigglobe.columns.scripted.classes;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.classes.ConstructorSpec.ConstructorContext;
import builderb0y.bigglobe.columns.scripted.classes.BaseMethodSpec.MethodSpecDesc;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.bigglobe.util.UnregisteredObjectException.getID;

public class OverrideTracker {

	public final ClassHierarchy hierarchy;
	public final RegistryEntry<ElementSpec> owner;
	public final Object2ObjectOpenHashMap<String, TrackedField> fields = new Object2ObjectOpenHashMap<>();
	public final Object2ObjectOpenHashMap<MethodSpecDesc, TrackedMethod> methods = new Object2ObjectOpenHashMap<>();
	public final Object2ObjectOpenHashMap<String, TrackedProperty> properties = new Object2ObjectOpenHashMap<>();
	public final Object2ObjectOpenHashMap<String, TrackedEnumValue> enumValues = new Object2ObjectOpenHashMap<>();

	public OverrideTracker(ClassHierarchy hierarchy, RegistryEntry<ElementSpec> owner) {
		this.hierarchy = hierarchy;
		this.owner = owner;
	}

	public OverrideTracker(ClassHierarchy hierarchy, RegistryEntry<ElementSpec> owner, OverrideTracker from) {
		this(hierarchy, owner);
		this.fields.putAll(from.fields);
		this.methods.putAll(from.methods);
	}

	public boolean hasAnyAbstractMethods() {
		for (TrackedMethod method : this.methods.values()) {
			if (method.type == TrackedMethod.Type.ABSTRACT) return true;
		}
		for (TrackedProperty property : this.properties.values()) {
			if (property.type == TrackedProperty.Type.ABSTRACT) return true;
		}
		return false;
	}

	public Stream<RegistryEntry<ElementSpec>> getAbstractMembers() {
		return Stream.concat(
			this.methods.values().stream().filter((TrackedMethod method) -> method.type == TrackedMethod.Type.ABSTRACT).map(TrackedMethod::declaration),
			this.properties.values().stream().filter((TrackedProperty property) -> property.type == TrackedProperty.Type.ABSTRACT).map(TrackedProperty::declaration)
		);
	}

	public void addReservedField(String name) {
		this.fields.put(name, new TrackedField(this.owner, null, TrackedField.Type.RESERVED));
	}

	public void addReservedMethod(MethodSpecDesc desc) {
		this.methods.put(desc, new TrackedMethod(this.owner, null, TrackedMethod.Type.RESERVED));
	}

	public void addReservedMethod(String name, TypeInfo... parameters) {
		this.addReservedMethod(new MethodSpecDesc(name, Arrays.asList(parameters)));
	}

	public void addNormalField(FieldSpec field) throws CustomClassFormatException {
		RegistryEntry<ElementSpec> entry = this.hierarchy.entryOf(field);
		TrackedProperty existingProperty = this.properties.get(field.name);
		if (existingProperty != null) {
			throw new CustomClassFormatException("Field " + getID(entry) + " in class " + getID(this.owner) + " conflicts with property " + getID(existingProperty.declaration) + " in class " + getID(existingProperty.owner));
		}

		TrackedField existing = this.fields.get(field.name);
		if (existing != null) {
			if (existing.type == TrackedField.Type.RESERVED) {
				throw new CustomClassFormatException("Field " + field.name + " is reserved.");
			}
			else if (existing.owner == this.owner) {
				throw new CustomClassFormatException("Multiple fields named " + field.name + " in class " + getID(this.owner));
			}
			else {
				throw new CustomClassFormatException("Field " + field.name + " in class " + getID(this.owner) + " shadows another field with the same name in class " + getID(existing.owner));
			}
		}

		this.fields.put(field.name, new TrackedField(this.owner, entry, TrackedField.Type.NORMAL));
	}

	public void addEnumField(EnumValueSpec value) throws CustomClassFormatException {
		RegistryEntry<ElementSpec> entry = this.hierarchy.entryOf(value);
		TrackedField existingField = this.fields.get(value.name);
		if (existingField != null) {
			throw new CustomClassFormatException("Enum field " + getID(entry) + " in class " + getID(this.owner) + " conflicts with field " + getID(existingField.declaration) + " in class " + getID(existingField.owner));
		}
		TrackedEnumValue existingValue = this.enumValues.get(value.name);
		if (existingValue != null) {
			if (existingValue.owner == this.owner) {
				throw new CustomClassFormatException("Multiple enum values named " + value.name + " in class " + getID(this.owner));
			}
			else {
				throw new CustomClassFormatException("Enum value " + value.name + " in class " + getID(this.owner) + " shadows another field with the same name in class " + existingValue.owner);
			}
		}

		this.enumValues.put(value.name, new TrackedEnumValue(this.owner, entry));
	}

	public void checkPropertyConflicts(BasePropertySpec property) throws CustomClassFormatException {
		RegistryEntry<ElementSpec> entry = this.hierarchy.entryOf(property);
		TrackedField existingField = this.fields.get(property.name());
		if (existingField != null) {
			if (existingField.type == TrackedField.Type.RESERVED) {
				throw new CustomClassFormatException("Property name " + property.name() + " used by " + getID(entry) + " is reserved.");
			}
			else {
				throw new CustomClassFormatException("Property " + getID(entry) + " in class " + getID(this.owner) + " conflicts with field " + getID(existingField.declaration) + " in class " + getID(existingField.owner));
			}
		}

		TrackedMethod existingGetter = this.methods.get(new MethodSpecDesc(property.name(), Collections.emptyList()));
		if (existingGetter != null) {
			if (existingGetter.type == TrackedMethod.Type.RESERVED) {
				throw new CustomClassFormatException("Property name " + property.name() + " used by " + getID(entry) + " is reserved.");
			}
			else {
				throw new CustomClassFormatException("Internal getter method of property " + getID(entry) + " in class " + getID(this.owner) + " conflicts with method " + getID(existingGetter.declaration) + " in class " + getID(existingGetter.owner));
			}
		}

		TypeInfo typeInfo = ElementSpec.asType(property.getPropertyType()).getTypeInfo();
		TrackedMethod existingSetter = this.methods.get(new MethodSpecDesc(property.name(), Collections.singletonList(typeInfo)));
		if (existingSetter != null) {
			if (existingSetter.type == TrackedMethod.Type.RESERVED) {
				throw new CustomClassFormatException("Property name " + property.name() + " used by " + getID(entry) + " is reserved.");
			}
			else {
				throw new CustomClassFormatException("Internal setter method of property " + getID(entry) + " in class " + getID(this.owner) + " conflicts with method " + getID(existingSetter.declaration) + " in class " + getID(existingSetter.owner));
			}
		}
	}

	public void addNormalProperty(NormalPropertySpec property) throws CustomClassFormatException {
		this.checkPropertyConflicts(property);
		RegistryEntry<ElementSpec> entry = this.hierarchy.entryOf(property);
		TrackedProperty existingProperty = this.properties.get(property.name);
		if (existingProperty != null) {
			if (existingProperty.owner == this.owner) {
				throw new CustomClassFormatException("Multiple properties named " + property.name + " in class " + getID(this.owner) + ": [" + getID(entry) + ", " + getID(existingProperty.declaration) + ']');
			}
			else {
				throw new CustomClassFormatException("Property " + getID(entry) + " in class " + getID(this.owner) + " overrides property " + getID(existingProperty.declaration) + " in class " + getID(existingProperty.owner) + ", but its type was " + ElementSpecTypes.PROPERTY_NORMAL + ". If overriding is intentional, its type should be " + ElementSpecTypes.PROPERTY_OVERRIDE);
			}
		}

		this.properties.put(property.name, new TrackedProperty(this.owner, entry, TrackedProperty.Type.NORMAL, property.isSettable()));
	}

	public void addOverrideProperty(OverridePropertySpec property) throws CustomClassFormatException {
		this.checkPropertyConflicts(property);
		RegistryEntry<ElementSpec> entry = this.hierarchy.entryOf(property);
		TrackedProperty existingProperty = this.properties.get(property.name());
		if (existingProperty == null) {
			throw new CustomClassFormatException("Property " + getID(entry) + " overrides property " + getID(property.override) + " but that property is not inherited by class " + getID(this.owner));
		}
		else if (existingProperty.owner == this.owner) {
			throw new CustomClassFormatException("Multiple properties named " + property.name() + " in class " + getID(this.owner) + ": [" + getID(entry) + ", " + getID(existingProperty.declaration) + ']');
		}
		else if (property.override != existingProperty.declaration) {
			throw new CustomClassFormatException("Property " + getID(entry) + " overrides property " + getID(property.override) + " but that property is already overridden by " + getID(existingProperty.declaration) + " in class " + getID(existingProperty.owner));
		}

		this.properties.put(property.name(), new TrackedProperty(this.owner, entry, TrackedProperty.Type.NORMAL, property.isSettable()));
	}

	public void addAbstractProperty(AbstractPropertySpec property) throws CustomClassFormatException {
		this.checkPropertyConflicts(property);
		RegistryEntry<ElementSpec> entry = this.hierarchy.entryOf(property);
		TrackedProperty existingProperty = this.properties.get(property.name);
		if (existingProperty != null) {
			if (existingProperty.owner == this.owner) {
				throw new CustomClassFormatException("Multiple properties named " + property.name + " in class " + getID(this.owner) + ": [" + getID(entry) + ", " + getID(existingProperty.declaration) + ']');
			}
			else {
				throw new CustomClassFormatException("Property " + getID(entry) + " in class " + getID(this.owner) + " overrides property " + getID(existingProperty.declaration) + " in class " + getID(existingProperty.owner) + ", but its type was " + ElementSpecTypes.PROPERTY_ABSTRACT + ". Abstract properties cannot (currently) override other properties.");
			}
		}

		this.properties.put(property.name, new TrackedProperty(this.owner, entry, TrackedProperty.Type.NORMAL, property.isSettable()));
	}

	public void addConstructor(ConstructorSpec constructor) throws CustomClassFormatException {
		RegistryEntry<ElementSpec> entry = this.hierarchy.entryOf(constructor);
		ConstructorContext context = ((BaseClassSpec)(this.owner.value())).getCompileContext(constructor);
		TrackedMethod existing = this.methods.get(context.descriptor);
		if (existing != null) {
			if (existing.type == TrackedMethod.Type.RESERVED) {
				throw new CustomClassFormatException("Constructor " + context.descriptor + " is reserved.");
			}
			else if (existing.owner == this.owner) {
				throw new CustomClassFormatException("Constructor " + getID(entry) + " in class " + getID(this.owner) + " conflicts with " + getID(existing.declaration));
			}
		}
		this.methods.put(context.descriptor, new TrackedMethod(this.owner, entry, TrackedMethod.Type.CONSTRUCTOR));
	}

	public void checkPropertyConflicts(BaseMethodSpec method) throws CustomClassFormatException {
		RegistryEntry<ElementSpec> entry = this.hierarchy.entryOf(method);
		MethodSpecDesc desc = method.getDescriptor();
		switch (desc.parameters().size()) {
			case 0 -> {
				TrackedProperty existingProperty = this.properties.get(method.name());
				if (existingProperty != null) {
					throw new CustomClassFormatException("Method " + getID(entry) + " in class " + getID(this.owner) + " conflicts with internal getter of property " + getID(existingProperty.declaration) + " in class " + getID(existingProperty.owner));
				}
			}
			case 1 -> {
				TrackedProperty existingProperty = this.properties.get(method.name());
				if (existingProperty != null) {
					throw new CustomClassFormatException("Method " + getID(entry) + " in class " + getID(this.owner) + " conflicts with internal setter of property " + getID(existingProperty.declaration) + " in class " + getID(existingProperty.owner));
				}
			}
		}
	}

	public void addInstanceMethod(NormalMethodSpec method) throws CustomClassFormatException {
		RegistryEntry<ElementSpec> entry = this.hierarchy.entryOf(method);
		MethodSpecDesc desc = method.getDescriptor();
		TrackedMethod existingMethod = this.methods.get(desc);
		if (existingMethod != null) {
			if (existingMethod.type == TrackedMethod.Type.RESERVED) {
				throw new CustomClassFormatException("Method " + desc + " is reserved.");
			}
			else if (existingMethod.owner == this.owner) {
				throw new CustomClassFormatException("Multiple methods named " + method.name() + " in class " + getID(this.owner) + " with parameters " + desc.parameters() + ": [" + getID(entry) + ", " + getID(existingMethod.declaration) + ']');
			}
			else if (existingMethod.type != TrackedMethod.Type.CONSTRUCTOR) {
				throw new CustomClassFormatException("Method " + getID(entry) + " in class " + getID(this.owner) + " conflicts with method " + getID(existingMethod.declaration) + " in class " + getID(existingMethod.owner));
			}
		}
		this.checkPropertyConflicts(method);
		this.methods.put(desc, new TrackedMethod(this.owner, entry, TrackedMethod.Type.NORMAL));
	}

	public void addAbstractMethod(AbstractMethodSpec method) throws CustomClassFormatException {
		RegistryEntry<ElementSpec> entry = this.hierarchy.entryOf(method);
		MethodSpecDesc desc = method.getDescriptor();
		TrackedMethod existingMethod = this.methods.get(desc);
		if (existingMethod != null) {
			if (existingMethod.type == TrackedMethod.Type.RESERVED) {
				throw new CustomClassFormatException("Method " + desc + " is reserved.");
			}
			else if (existingMethod.owner == this.owner) {
				throw new CustomClassFormatException("Multiple methods named " + desc.name() + " in class " + getID(this.owner) + " with parameters " + desc.parameters() + ": [" + getID(entry) + ", " + getID(existingMethod.declaration) + ']');
			}
			else if (existingMethod.type != TrackedMethod.Type.CONSTRUCTOR) {
				throw new CustomClassFormatException("Method " + getID(entry) + " in class " + getID(this.owner) + " overrides method " + getID(existingMethod.declaration) + " in class " + getID(existingMethod.owner) + " but has a type of " + ElementSpecTypes.METHOD_ABSTRACT + ". Abstract methods cannot (currently) override other methods.");
			}
		}
		this.checkPropertyConflicts(method);
		this.methods.put(desc, new TrackedMethod(this.owner, entry, TrackedMethod.Type.ABSTRACT));
	}

	public void addOverrideMethod(OverrideMethodSpec method) throws CustomClassFormatException {
		RegistryEntry<ElementSpec> entry = this.hierarchy.entryOf(method);
		MethodSpecDesc desc = method.getDescriptor();
		TrackedMethod existingMethod = this.methods.get(desc);
		if (existingMethod == null) {
			throw new CustomClassFormatException("Method " + getID(entry) + " overrides " + getID(method.override) + " but that method is not inherited by class " + getID(this.owner));
		}
		else if (existingMethod.type == TrackedMethod.Type.RESERVED) {
			throw new CustomClassFormatException("Method " + desc + " is reserved.");
		}
		else if (existingMethod.owner == this.owner) {
			throw new CustomClassFormatException("Multiple methods named " + desc.name() + " in class " + getID(this.owner) + " with parameters " + desc.parameters() + ": [" + getID(entry) + ", " + getID(existingMethod.declaration) + ']');
		}
		else if (method.override != existingMethod.declaration) {
			throw new CustomClassFormatException("Method " + getID(entry) + " overrides " + getID(method.override) + " but that method is already overridden by " + getID(existingMethod.declaration) + " in class " + getID(existingMethod.owner));
		}
		this.checkPropertyConflicts(method);
		this.methods.put(desc, new TrackedMethod(this.owner, entry, TrackedMethod.Type.NORMAL));
	}

	public static record TrackedField(
		RegistryEntry<ElementSpec> owner,
		RegistryEntry<ElementSpec> declaration,
		Type type
	) {

		public static enum Type {
			NORMAL,
			RESERVED;
		}
	}

	public static record TrackedMethod(
		RegistryEntry<ElementSpec> owner,
		RegistryEntry<ElementSpec> declaration,
		Type type
	) {

		public static enum Type {
			NORMAL,
			ABSTRACT,
			CONSTRUCTOR,
			//override counts as normal.
			RESERVED;
		}
	}

	public static record TrackedProperty(
		RegistryEntry<ElementSpec> owner,
		RegistryEntry<ElementSpec> declaration,
		Type type,
		boolean hasSetter
	) {

		public static enum Type {
			NORMAL,
			ABSTRACT;
			//override counts as normal.
			//reservation is handled by the backing getter and setter methods, along with the field name.
		}
	}

	public static record TrackedEnumValue(
		RegistryEntry<ElementSpec> owner,
		RegistryEntry<ElementSpec> declaration
	) {}
}