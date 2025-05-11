package builderb0y.scripting.bytecode;

import java.lang.StackWalker.Option;
import java.lang.invoke.MethodHandles;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryOwner;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.util.FakeRegistry.RegistryEntryImpl;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConstantFactory extends AbstractConstantFactory {

	public static final StackWalker STACK_WALKER = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);

	public final MethodInfo constantMethod, variableMethod;
	public final boolean hasFlags;

	public ConstantFactory(Class<?> owner, String name, Class<?> inType, Class<?> outType) {
		super(type(inType), type(outType));
		this.constantMethod = MethodInfo.findMethod(owner, name, outType, MethodHandles.Lookup.class, String.class, Class.class, inType, int.class);
		this.variableMethod = MethodInfo.findMethod(owner, name, outType, inType, int.class);
		this.hasFlags = true;
	}

	public ConstantFactory(MethodInfo constantMethod, MethodInfo variableMethod, TypeInfo inType, TypeInfo outType, boolean hasFlags) {
		super(inType, outType);
		this.constantMethod = constantMethod;
		this.variableMethod = variableMethod;
		this.hasFlags = hasFlags;
	}

	public static <T> RegistryEntry<T> getEntry(RegistryKey<Registry<T>> registryKey, String id, int flags) {
		if (id == null) return null;
		try {
			return BigGlobeMod.getSidedRegistry(registryKey, (flags & CLIENT) != 0).requireByName(id);
		}
		catch (RuntimeException exception) {
			if ((flags & NULLABLE) != 0) return null;
			else throw exception;
		}
	}

	public static <T> RegistryEntry<T> getEntryServerOnly(RegistryKey<Registry<T>> registryKey, String id, int flags, T defaultValue) {
		if (id == null) return null;
		try {
			if ((flags & CLIENT) != 0) {
				return new RegistryEntryImpl<>(
					new RegistryEntryOwner<>() {},
					registryKey,
					IdentifierVersions.create(id),
					defaultValue
				);
			}
			else {
				return BigGlobeMod.getRegistry(registryKey).requireByName(id);
			}
		}
		catch (RuntimeException exception) {
			if ((flags & NULLABLE) != 0) return null;
			else throw exception;
		}
	}

	/**
	factory method for the most common case,
	where the owner is the caller class, the name is "of",
	inType is String.class, and outType is also the caller class.
	*/
	public static ConstantFactory autoOfString() {
		Class<?> caller = STACK_WALKER.getCallerClass();
		return new ConstantFactory(caller, "of", String.class, caller);
	}

	@Override
	public InsnTree createConstant(ConstantValue constant, int flags) {
		if (this.hasFlags) {
			return ldc(this.constantMethod, constant, constant(flags));
		}
		else {
			return ldc(this.constantMethod, constant);
		}
	}

	@Override
	public InsnTree createNonConstant(InsnTree tree, int flags) {
		if (this.hasFlags) {
			return invokeStatic(this.variableMethod, tree, ldc(flags));
		}
		else {
			return invokeStatic(this.variableMethod, tree);
		}
	}
}