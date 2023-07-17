package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.settings.OverworldSkylandSettings.LocalSkylandSettings;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record OverworldSkylandSettingsTag(TagKey<LocalSkylandSettings> key) implements TagWrapper<LocalSkylandSettings, OverworldSkylandSettingsEntry> {

	public static final TypeInfo TYPE = type(OverworldSkylandSettingsTag.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static OverworldSkylandSettingsTag of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static OverworldSkylandSettingsTag of(String id) {
		if (id == null) return null;
		return new OverworldSkylandSettingsTag(TagKey.of(BigGlobeDynamicRegistries.LOCAL_SKYLAND_SETTINGS_REGISTRY_KEY, new Identifier(id)));
	}

	@Override
	public OverworldSkylandSettingsEntry wrap(RegistryEntry<LocalSkylandSettings> entry) {
		return new OverworldSkylandSettingsEntry(entry);
	}

	@Override
	public OverworldSkylandSettingsEntry random(RandomGenerator random) {
		return this.randomImpl(random);
	}

	@Override
	public OverworldSkylandSettingsEntry random(long seed) {
		return this.randomImpl(seed);
	}
}