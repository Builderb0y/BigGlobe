package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.settings.NetherSettings.LocalNetherSettings;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record NetherBiomeSettingsTag(TagKey<LocalNetherSettings> key) implements TagWrapper<LocalNetherSettings, NetherBiomeSettingsEntry> {

	public static final TypeInfo TYPE = type(NetherBiomeSettingsTag.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static NetherBiomeSettingsTag of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static NetherBiomeSettingsTag of(String id) {
		if (id == null) return null;
		return new NetherBiomeSettingsTag(TagKey.of(BigGlobeDynamicRegistries.LOCAL_NETHER_SETTINGS_REGISTRY_KEY, new Identifier(id)));
	}

	@Override
	public NetherBiomeSettingsEntry wrap(RegistryEntry<LocalNetherSettings> entry) {
		return new NetherBiomeSettingsEntry(entry);
	}

	@Override
	public NetherBiomeSettingsEntry random(RandomGenerator random) {
		return this.randomImpl(random);
	}

	@Override
	public NetherBiomeSettingsEntry random(long seed) {
		return this.randomImpl(seed);
	}
}