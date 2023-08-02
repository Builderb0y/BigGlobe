package builderb0y.bigglobe.scripting.wrappers;

import java.util.random.RandomGenerator;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;

import builderb0y.bigglobe.settings.OverworldCaveSettings.LocalOverworldCaveSettings;

public record OverworldCaveSettingsTag(TagKey<LocalOverworldCaveSettings> key) implements TagWrapper<LocalOverworldCaveSettings, OverworldCaveSettingsEntry> {

	@Override
	public OverworldCaveSettingsEntry wrap(RegistryEntry<LocalOverworldCaveSettings> entry) {
		return new OverworldCaveSettingsEntry(entry);
	}

	@Override
	public OverworldCaveSettingsEntry random(RandomGenerator random) {
		return this.randomImpl(random);
	}

	@Override
	public OverworldCaveSettingsEntry random(long seed) {
		return this.randomImpl(seed);
	}
}