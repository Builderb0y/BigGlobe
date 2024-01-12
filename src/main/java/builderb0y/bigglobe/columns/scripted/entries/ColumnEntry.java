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
import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.DataCompileContext.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.AccessSchema.TypeContext;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

@UseCoder(name = "REGISTRY", in = ColumnEntry.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface ColumnEntry extends CoderRegistryTyped<ColumnEntry> {

	public static final CoderRegistry<ColumnEntry> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("column_value"));
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID(   "int_script_2d"), IntScript2DColumnEntry.class);
	}};

	public abstract AccessSchema getAccessSchema();

	public abstract void emitFieldGetterAndSetter(ColumnEntryMemory memory, DataCompileContext context);

	public abstract void setupEnvironment(ColumnEntryMemory memory, DataCompileContext context);

	public abstract void emitComputer(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException;

	public static class ColumnEntryMemory extends HashMap<ColumnEntryMemory.Key<?>, Object> {

		public static final Key<ColumnEntry>
			ENTRY = new Key<>("entry");
		public static final Key<Identifier>
			ACCESSOR_ID = new Key<>("accessorID");
		public static final Key<FieldInfo>
			FIELD = new Key<>("field");
		public static final Key<MethodCompileContext>
			GETTER = new Key<>("getter"),
			SETTER = new Key<>("setter"),
			COMPUTER = new Key<>("computer");
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

		@SuppressWarnings("unchecked")
		public <T> T putTyped(Key<T> key, T value) {
			return (T)(this.put(key, value));
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

	@UseCoder(name = "REGISTRY", in = AccessSchema.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
	public static interface AccessSchema extends CoderRegistryTyped<AccessSchema> {

		public static final CoderRegistry<AccessSchema> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("column_entry_access_schema"));

		public abstract TypeContext createType(ColumnCompileContext context);

		public abstract boolean requiresYLevel();

		public default MethodInfo descriptor(int flags, String name, DataCompileContext context) {
			return new MethodInfo(
				flags,
				context.selfType(),
				name,
				context.getSchemaType(this).type(),
				this.requiresYLevel() ? new TypeInfo[] { TypeInfos.INT } : TypeInfo.ARRAY_FACTORY.empty()
			);
		}

		@Override
		public abstract boolean equals(Object other);

		@Override
		public abstract int hashCode();
	}

	public static record TypeContext(@NotNull TypeInfo type, @Nullable DataCompileContext context) {}
}