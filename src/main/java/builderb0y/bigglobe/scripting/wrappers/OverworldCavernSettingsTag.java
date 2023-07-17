package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.settings.OverworldCavernSettings.LocalOverworldCavernSettings;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record OverworldCavernSettingsTag(TagKey<LocalOverworldCavernSettings> key) implements TagWrapper<LocalOverworldCavernSettings, OverworldCavernSettingsEntry> {

	public static final TypeInfo TYPE = type(OverworldCavernSettingsTag.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static OverworldCavernSettingsTag of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static OverworldCavernSettingsTag of(String id) {
		if (id == null) return null;
		return new OverworldCavernSettingsTag(TagKey.of(BigGlobeDynamicRegistries.LOCAL_OVERWORLD_CAVERN_SETTINGS_REGISTRY_KEY, new Identifier(id)));
	}

	@Override
	public OverworldCavernSettingsEntry wrap(RegistryEntry<LocalOverworldCavernSettings> entry) {
		return new OverworldCavernSettingsEntry(entry);
	}

	@Override
	public OverworldCavernSettingsEntry random(RandomGenerator random) {
		return this.randomImpl(random);
	}

	@Override
	public OverworldCavernSettingsEntry random(long seed) {
		return this.randomImpl(seed);
	}
}