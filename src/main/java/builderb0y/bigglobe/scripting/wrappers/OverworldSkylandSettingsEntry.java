package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.settings.OverworldSkylandSettings.LocalSkylandSettings;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record OverworldSkylandSettingsEntry(RegistryEntry<LocalSkylandSettings> entry) implements EntryWrapper<LocalSkylandSettings, OverworldSkylandSettingsTag> {

	public static final TypeInfo TYPE = type(OverworldSkylandSettingsEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static OverworldSkylandSettingsEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static OverworldSkylandSettingsEntry of(String id) {
		if (id == null) return null;
		return new OverworldSkylandSettingsEntry(
			BigGlobeMod
			.getCurrentServer()
			.getRegistryManager()
			.get(BigGlobeDynamicRegistries.LOCAL_SKYLAND_SETTINGS_REGISTRY_KEY)
			.entryOf(RegistryKey.of(BigGlobeDynamicRegistries.LOCAL_SKYLAND_SETTINGS_REGISTRY_KEY, new Identifier(id)))
		);
	}

	@Override
	public boolean isIn(OverworldSkylandSettingsTag tag) {
		return this.entry.isIn(tag.key());
	}

	@Override
	public int hashCode() {
		return this.entry.getKey().hashCode();
	}

	@Override
	public boolean equals(Object object) {
		return this == object || (
			object instanceof OverworldSkylandSettingsEntry that &&
			this.entry.getKey().equals(that.entry.getKey())
		);
	}

	public double weight() {
		return this.entry.value().weight;
	}
}