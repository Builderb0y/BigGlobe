package builderb0y.bigglobe.columns.scripted.entries;

import java.util.function.BiConsumer;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;

//todo: document this horrible mess I've created.
public abstract class ColumnEntry {

	public int flags;

	public void clear() {
		this.flags = 0;
	}

	public final boolean hasFlag(int flag) {
		return (this.flags & flag) != 0;
	}

	public final boolean hasFlags(int flags) {
		return (this.flags & flags) == flags;
	}

	public final boolean setFlags(int flags) {
		int oldFlags = this.flags;
		int newFlags = oldFlags | flags;
		if (oldFlags != newFlags) {
			this.flags = newFlags;
			return true;
		}
		else {
			return false;
		}
	}

	public static abstract class ColumnEntryRegistrable implements CoderRegistryTyped<ColumnEntryRegistrable> {

		public static final CoderRegistry<ColumnEntryRegistrable> CODER_REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("column_entry_registrable"));
		static {
			CODER_REGISTRY.registerAuto(BigGlobeMod.modID("int_script_2d"), IntScript2DColumnEntry.Registrable.class);
			CODER_REGISTRY.registerAuto(BigGlobeMod.modID("int_script_3d"), IntScript3DColumnEntry.Registrable.class);
		}

		public abstract boolean hasEntry();

		public abstract ColumnEntry createEntry();

		public abstract void createAccessors(String selfName, int slot, BiConsumer<String, ColumnEntryAccessor> accessors);
	}
}