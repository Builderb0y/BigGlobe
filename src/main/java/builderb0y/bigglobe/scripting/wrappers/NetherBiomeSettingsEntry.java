package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.settings.NetherSettings.LocalNetherSettings;
import builderb0y.bigglobe.settings.NetherSettings.NetherCaveSettings;
import builderb0y.bigglobe.settings.NetherSettings.NetherCavernSettings;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record NetherBiomeSettingsEntry(RegistryEntry<LocalNetherSettings> entry) implements EntryWrapper<LocalNetherSettings, NetherBiomeSettingsTag> {

	public static final TypeInfo TYPE = type(NetherBiomeSettingsEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static NetherBiomeSettingsEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static NetherBiomeSettingsEntry of(String id) {
		if (id == null) return null;
		return new NetherBiomeSettingsEntry(
			BigGlobeMod
			.getCurrentServer()
			.getRegistryManager()
			.get(BigGlobeDynamicRegistries.LOCAL_NETHER_SETTINGS_REGISTRY_KEY)
			.entryOf(RegistryKey.of(BigGlobeDynamicRegistries.LOCAL_NETHER_SETTINGS_REGISTRY_KEY, new Identifier(id)))
		);
	}

	@Override
	public boolean isIn(NetherBiomeSettingsTag tag) {
		return this.entry.isIn(tag.key());
	}

	@Override
	public int hashCode() {
		return this.entry.getKey().hashCode();
	}

	@Override
	public boolean equals(Object object) {
		return this == object || (
			object instanceof NetherBiomeSettingsEntry that &&
			this.entry.getKey().equals(that.entry.getKey())
		);
	}

	public double weight() {
		return this.entry.value().weight;
	}

	public BiomeEntry biome() {
		return new BiomeEntry(this.entry.value().biome);
	}

	public NetherCaveSettings caves() {
		return this.entry.value().caves;
	}

	public NetherCavernSettings caverns() {
		return this.entry.value().caverns;
	}
}