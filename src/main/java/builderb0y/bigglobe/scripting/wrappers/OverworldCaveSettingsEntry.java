package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.settings.OverworldCaveSettings.LocalOverworldCaveSettings;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record OverworldCaveSettingsEntry(RegistryEntry<LocalOverworldCaveSettings> entry) implements EntryWrapper<LocalOverworldCaveSettings, OverworldCaveSettingsTag> {

	public static final TypeInfo TYPE = type(OverworldCaveSettingsEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static OverworldCaveSettingsEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static OverworldCaveSettingsEntry of(String id) {
		if (id == null) return null;
		return new OverworldCaveSettingsEntry(
			BigGlobeMod
			.getCurrentServer()
			.getRegistryManager()
			.get(BigGlobeDynamicRegistries.LOCAL_OVERWORLD_CAVE_SETTINGS_REGISTRY_KEY)
			.entryOf(RegistryKey.of(BigGlobeDynamicRegistries.LOCAL_OVERWORLD_CAVE_SETTINGS_REGISTRY_KEY, new Identifier(id)))
		);
	}

	@Override
	public boolean isIn(OverworldCaveSettingsTag tag) {
		return this.entry.isIn(tag.key());
	}

	public double weight() {
		return this.entry.value().weight;
	}

	public int depth() {
		return this.entry.value().depth;
	}
}