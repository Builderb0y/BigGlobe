package builderb0y.bigglobe.columns.scripted.entries;

import java.util.HashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.FieldCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.parsing.ScriptParsingException;

import static org.objectweb.asm.Opcodes.*;

@UseCoder(name = "REGISTRY", in = ColumnEntry.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface ColumnEntry extends CoderRegistryTyped<ColumnEntry> {

	public static final CoderRegistry<ColumnEntry> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("column_value"));
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("int_script_2d"), IntScript2DColumnEntry.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("long_script_2d"), LongScript2DColumnEntry.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("float_script_2d"), FloatScript2DColumnEntry.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("double_script_2d"), DoubleScript2DColumnEntry.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("boolean_script_2d"), BooleanScript2DColumnEntry.class);

		REGISTRY.registerAuto(BigGlobeMod.modID("int_script_3d"), IntScript3DColumnEntry.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("long_script_3d"), LongScript3DColumnEntry.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("float_script_3d"), FloatScript3DColumnEntry.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("double_script_3d"), DoubleScript3DColumnEntry.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("boolean_script_3d"), BooleanScript3DColumnEntry.class);

		REGISTRY.registerAuto(BigGlobeMod.modID("float_noise_2d"), FloatNoise2DColumnEntry.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("double_noise_2d"), DoubleNoise2DColumnEntry.class);
	}};

	public abstract AccessSchema getAccessSchema();

	public abstract boolean hasField();

	public default void populateField(ColumnEntryMemory memory, DataCompileContext context, FieldCompileContext getterMethod) {}

	public abstract void populateGetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod);

	public abstract void populateSetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext setterMethod);

	public default void emitFieldGetterAndSetter(ColumnEntryMemory memory, DataCompileContext context) {
		int uniqueIndex = context.mainClass.memberUniquifier++;
		int flagIndex = context.flagsIndex++;
		memory.putTyped(ColumnEntryMemory.FLAGS_INDEX, flagIndex);
		Identifier accessID = memory.getTyped(ColumnEntryMemory.ACCESSOR_ID);
		TypeContext type = context.getSchemaType(this.getAccessSchema());
		memory.putTyped(ColumnEntryMemory.TYPE, type);
		String internalName = DataCompileContext.internalName(accessID, uniqueIndex);
		memory.putTyped(ColumnEntryMemory.INTERNAL_NAME, internalName);

		if (this.hasField()) {
			FieldCompileContext valueField = context.mainClass.newField(ACC_PUBLIC, internalName, type.type());
			memory.putTyped(ColumnEntryMemory.FIELD, valueField);
			MethodCompileContext getterMethod = context.mainClass.newMethod(this.getAccessSchema().getterDescriptor(ACC_PUBLIC, "get_" + internalName, context));
			memory.putTyped(ColumnEntryMemory.GETTER, getterMethod);
			MethodCompileContext setterMethod = context.mainClass.newMethod(this.getAccessSchema().setterDescriptor(ACC_PUBLIC, "set_" + internalName, context));
			memory.putTyped(ColumnEntryMemory.SETTER, setterMethod);

			this.populateField(memory, context, valueField);
			this.populateGetter(memory, context, getterMethod);
			this.populateSetter(memory, context, setterMethod);
		}
		else {
			MethodCompileContext getterMethod = context.mainClass.newMethod(this.getAccessSchema().getterDescriptor(ACC_PUBLIC, "get_" + internalName, context));
			memory.putTyped(ColumnEntryMemory.GETTER, getterMethod);

			this.populateGetter(memory, context, getterMethod);
		}
	}

	public default void setupEnvironment(ColumnEntryMemory memory, DataCompileContext context) {
		if (this.getAccessSchema().requiresYLevel()) {
			context.environment.addFunctionInvoke(memory.getTyped(ColumnEntryMemory.ACCESSOR_ID).toString(), context.loadSelf(), memory.getTyped(ColumnEntryMemory.GETTER).info);
		}
		else {
			context.environment.addVariableRenamedInvoke(context.loadSelf(), memory.getTyped(ColumnEntryMemory.ACCESSOR_ID).toString(), memory.getTyped(ColumnEntryMemory.GETTER).info);
		}
	}

	public abstract void emitComputer(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException;

	/**
	a quick-and-dirty way of transferring information between
	{@link #emitFieldGetterAndSetter(ColumnEntryMemory, DataCompileContext)},
	{@link #setupEnvironment(ColumnEntryMemory, DataCompileContext)},
	and {@link #emitComputer(ColumnEntryMemory, DataCompileContext)}.
	*/
	public static class ColumnEntryMemory extends HashMap<ColumnEntryMemory.Key<?>, Object> {

		public static final Key<ColumnEntry>
			ENTRY = new Key<>("entry");
		public static final Key<Identifier>
			ACCESSOR_ID = new Key<>("accessorID");
		public static final Key<Integer>
			FLAGS_INDEX = new Key<>("flagsIndex");
		public static final Key<String>
			INTERNAL_NAME = new Key<>("internalName");
		public static final Key<FieldCompileContext>
			FIELD = new Key<>("field");
		public static final Key<MethodCompileContext>
			GETTER = new Key<>("getter"),
			SETTER = new Key<>("setter"),
			COMPUTER = new Key<>("computer"),
			VALID_WHERE = new Key<>("validWhere");
		public static final Key<TypeContext>
			TYPE = new Key<>("type");

		public ColumnEntryMemory(RegistryEntry<ColumnEntry> entry) {
			this.putTyped(ACCESSOR_ID, UnregisteredObjectException.getID(entry));
			this.putTyped(ENTRY, entry.value());
		}

		public <T> T getTyped(Key<T> key) {
			@SuppressWarnings("unchecked")
			T result = (T)(this.get(key));
			if (result != null) return result;
			else throw new IllegalArgumentException("Key " + key + " not in " + this);
		}

		public <T> void putTyped(Key<T> key, T value) {
			if (this.putIfAbsent(key, value) != null) {
				throw new IllegalStateException(key + " already present in " + this);
			}
		}

		public <T> void replaceTyped(Key<T> key, T oldValue, T newValue) {
			if (!this.replace(key, oldValue, newValue)) {
				throw new IllegalStateException(key + " was bound to " + this.get(key) + " when trying to replace " + oldValue + " with " + newValue + " in " + this);
			}
		}

		public static record Key<T>(String name) {

			@Override
			public boolean equals(Object obj) {
				return this == obj;
			}

			@Override
			public int hashCode() {
				return System.identityHashCode(this);
			}

			@Override
			public String toString() {
				return this.name;
			}
		}
	}

	public static record TypeContext(@NotNull TypeInfo type, @Nullable DataCompileContext context) {}
}