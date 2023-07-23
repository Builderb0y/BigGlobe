package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.settings.OverworldCavernSettings.LocalOverworldCavernSettings;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record OverworldCavernSettingsEntry(RegistryEntry<LocalOverworldCavernSettings> entry) implements EntryWrapper<LocalOverworldCavernSettings, OverworldCavernSettingsTag> {

	public static final TypeInfo TYPE = type(OverworldCavernSettingsEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static OverworldCavernSettingsEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static OverworldCavernSettingsEntry of(String id) {
		if (id == null) return null;
		return new OverworldCavernSettingsEntry(
			BigGlobeMod
			.getCurrentServer()
			.getRegistryManager()
			.get(BigGlobeDynamicRegistries.LOCAL_OVERWORLD_CAVERN_SETTINGS_REGISTRY_KEY)
			.entryOf(RegistryKey.of(BigGlobeDynamicRegistries.LOCAL_OVERWORLD_CAVERN_SETTINGS_REGISTRY_KEY, new Identifier(id)))
		);
	}

	@Override
	public boolean isIn(OverworldCavernSettingsTag tag) {
		return this.entry.isIn(tag.key());
	}

	@Override
	public int hashCode() {
		return this.entry.getKey().hashCode();
	}

	@Override
	public boolean equals(Object object) {
		return this == object || (
			object instanceof OverworldCavernSettingsEntry that &&
			this.entry.getKey().equals(that.entry.getKey())
		);
	}

	public double weight() {
		return this.entry.value().weight;
	}

	public double padding() {
		return this.entry.value().padding;
	}

	public BlockState fluid() {
		return this.entry.value().fluid;
	}

	public boolean has_ancient_cities() {
		return this.entry.value().has_ancient_cities;
	}
}