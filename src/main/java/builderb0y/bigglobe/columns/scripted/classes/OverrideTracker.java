package builderb0y.bigglobe.columns.scripted.classes;

import java.util.Arrays;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.columns.scripted.classes.ConstructorSpec.ConstructorContext;
import builderb0y.bigglobe.columns.scripted.classes.MethodSpec.MethodSpecDesc;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.TypeInfo;

public class OverrideTracker {

	public final RegistryEntry<ElementSpec> owner;
	public final Object2ObjectOpenHashMap<String, TrackedField> fields = new Object2ObjectOpenHashMap<>();
	public final Object2ObjectOpenHashMap<MethodSpecDesc, TrackedMethod> methods = new Object2ObjectOpenHashMap<>();

	public OverrideTracker(RegistryEntry<ElementSpec> owner) {
		this.owner = owner;
	}

	public OverrideTracker(RegistryEntry<ElementSpec> owner, OverrideTracker from) {
		this(owner);
		this.fields.putAll(from.fields);
		this.methods.putAll(from.methods);
	}

	public boolean hasAnyAbstractMethods() {
		for (TrackedMethod method : this.methods.values()) {
			if (method.type == TrackedMethod.Type.ABSTRACT) return true;
		}
		return false;
	}

	public void addField(FieldSpec field) throws CustomClassFormatException {
		this.fields.compute(field.name, (String name, TrackedField existing) -> {
			if (existing != null) {
				if (existing.type == TrackedField.Type.RESERVED) {
					throw AutoCodecUtil.rethrow(new CustomClassFormatException(name + " is a reserved field name."));
				}
				else if (existing.source == this.owner) {
					throw AutoCodecUtil.rethrow(new CustomClassFormatException("Multiple fields named " + name + " in class " + UnregisteredObjectException.getID(this.owner)));
				}
				else {
					throw AutoCodecUtil.rethrow(new CustomClassFormatException("Field " + name + " in class " + UnregisteredObjectException.getID(this.owner) + " shadows another field with the same name in a class being extended!"));
				}
			}
			return new TrackedField(this.owner, TrackedField.Type.NORMAL);
		});
	}

	public void addReservedField(String name) {
		this.fields.put(name, new TrackedField(this.owner, TrackedField.Type.RESERVED));
	}

	public void addReservedMethod(MethodSpecDesc desc) {
		this.methods.put(desc, new TrackedMethod(this.owner, TrackedMethod.Type.RESERVED));
	}

	public void addReservedMethod(String name, TypeInfo... parameters) {
		this.addReservedMethod(new MethodSpecDesc(name, Arrays.asList(parameters)));
	}

	public void addStaticMethod(StaticMethodSpec method) throws CustomClassFormatException {
		this.methods.compute(method.getDescriptor(), (MethodSpecDesc desc, TrackedMethod existing) -> {
			if (existing != null && existing.source == this.owner) {
				throw AutoCodecUtil.rethrow(new CustomClassFormatException("Multiple methods named " + desc.name() + " in class " + UnregisteredObjectException.getID(this.owner) + " with parameters " + desc.parameters()));
			}
			return new TrackedMethod(this.owner, TrackedMethod.Type.STATIC);
		});
	}

	public void addConstructor(ConstructorSpec constructor) throws CustomClassFormatException {
		ConstructorContext context = ((ClassSpec)(this.owner.value())).getCompileContext(constructor);
		this.methods.compute(context.descriptor, (MethodSpecDesc desc, TrackedMethod existing) -> {
			if (existing != null && existing.source == this.owner) {
				throw AutoCodecUtil.rethrow(new CustomClassFormatException("Multiple constructors named " + desc.name() + " in class " + UnregisteredObjectException.getID(this.owner) + " with parameters " + desc.parameters()));
			}
			return new TrackedMethod(this.owner, TrackedMethod.Type.STATIC);
		});
	}

	public void addInstanceMethod(InstanceMethodSpec method) throws CustomClassFormatException {
		this.methods.compute(method.getDescriptor(), (MethodSpecDesc desc, TrackedMethod existing) -> {
			if (existing != null) {
				if (existing.type == TrackedMethod.Type.RESERVED) {
					throw AutoCodecUtil.rethrow(new CustomClassFormatException("Method " + desc + " is reserved."));
				}
				else if (existing.source == this.owner) {
					throw AutoCodecUtil.rethrow(new CustomClassFormatException("Multiple methods named " + desc.name() + " in class " + UnregisteredObjectException.getID(this.owner) + " with parameters " + desc.parameters()));
				}
				else if (existing.type != TrackedMethod.Type.STATIC) {
					throw AutoCodecUtil.rethrow(new CustomClassFormatException("Method " + desc.name() + " in " + UnregisteredObjectException.getID(this.owner) + " overrides method from " + UnregisteredObjectException.getID(existing.source) + " but has a type of '" + ElementSpecTypes.METHOD_NORMAL + "'. If this override is intentional, the type should be '" + ElementSpecTypes.METHOD_OVERRIDE + "'."));
				}
			}
			return new TrackedMethod(this.owner, TrackedMethod.Type.NORMAL);
		});
	}

	public void addAbstractMethod(AbstractMethodSpec method) throws CustomClassFormatException {
		this.methods.compute(method.getDescriptor(), (MethodSpecDesc desc, TrackedMethod existing) -> {
			if (existing != null) {
				if (existing.type == TrackedMethod.Type.RESERVED) {
					throw AutoCodecUtil.rethrow(new CustomClassFormatException("Method " + desc + " is reserved."));
				}
				else if (existing.source == this.owner) {
					throw AutoCodecUtil.rethrow(new CustomClassFormatException("Multiple methods named " + desc.name() + " in class " + UnregisteredObjectException.getID(this.owner) + " with parameters " + desc.parameters()));
				}
				else if (existing.type != TrackedMethod.Type.STATIC) {
					throw AutoCodecUtil.rethrow(new CustomClassFormatException("Method " + desc.name() + " in " + UnregisteredObjectException.getID(this.owner) + " overrides method from " + UnregisteredObjectException.getID(existing.source) + " but has a type of '" + ElementSpecTypes.METHOD_ABSTRACT + "'. Abstract methods cannot (currently) override other methods."));
				}
			}
			return new TrackedMethod(this.owner, TrackedMethod.Type.ABSTRACT);
		});
	}

	public void addOverrideMethod(OverrideMethodSpec method) throws CustomClassFormatException {
		this.methods.compute(method.getDescriptor(), (MethodSpecDesc desc, TrackedMethod existing) -> {
			if (existing != null) {
				if (existing.type == TrackedMethod.Type.RESERVED) {
					throw AutoCodecUtil.rethrow(new CustomClassFormatException("Method " + desc + " is reserved."));
				}
				else if (existing.source == this.owner) {
					throw AutoCodecUtil.rethrow(new CustomClassFormatException("Multiple methods named " + desc.name() + " in class " + UnregisteredObjectException.getID(this.owner) + " with parameters " + desc.parameters()));
				}
			}
			return new TrackedMethod(this.owner, TrackedMethod.Type.NORMAL);
		});
	}

	public static record TrackedField(RegistryEntry<ElementSpec> source, Type type) {

		public static enum Type {
			NORMAL,
			RESERVED;
		}
	}

	public static record TrackedMethod(RegistryEntry<ElementSpec> source, Type type) {

		public static enum Type {
			NORMAL,
			ABSTRACT,
			STATIC,
			//override counts as normal.
			RESERVED;
		}
	}
}