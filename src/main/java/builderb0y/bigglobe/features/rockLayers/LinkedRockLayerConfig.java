package builderb0y.bigglobe.features.rockLayers;

import java.util.List;

import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.Feature;

import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.bigglobe.features.BigGlobeFeatures;
import builderb0y.bigglobe.features.LinkedConfig;
import builderb0y.bigglobe.noise.Permuter;

public class LinkedRockLayerConfig<T_Entry extends RockLayerEntryFeature.Entry> extends LinkedConfig<
	RockLayerGroupFeature.Config,
	RockLayerEntryFeature.Config<T_Entry>,
	T_Entry
> {

	public static final LinkedRockLayerConfigFactory<OverworldRockLayerEntryFeature.Entry>
		OVERWORLD_FACTORY = new LinkedRockLayerConfigFactory<>(
		new ObjectArrayFactory<>(LinkedRockLayerConfig.class).generic(),
			BigGlobeFeatures.OVERWORLD_ROCK_LAYER_GROUP,
			BigGlobeFeatures.OVERWORLD_ROCK_LAYER_ENTRY
		);
	public static final LinkedRockLayerConfigFactory<NetherRockLayerEntryFeature.Entry>
		NETHER_FACTORY = new LinkedRockLayerConfigFactory<>(
		new ObjectArrayFactory<>(LinkedRockLayerConfig.class).generic(),
			BigGlobeFeatures.NETHER_ROCK_LAYER_GROUP,
			BigGlobeFeatures.NETHER_ROCK_LAYER_ENTRY
		);

	public final long nameHash;
	public final double minWindow, maxWindow;

	public LinkedRockLayerConfig(
		Identifier name,
		RockLayerGroupFeature.Config group,
		List<T_Entry> entries
	) {
		super(name, group, entries);
		this.nameHash = Permuter.permute(0x1185E30383BD341CL, name);
		this.minWindow = entries.stream().mapToDouble(entry -> entry.center.minValue() - entry.thickness.maxValue()).min().orElse(0.0D);
		this.maxWindow = entries.stream().mapToDouble(entry -> entry.center.maxValue() + entry.thickness.maxValue()).max().orElse(0.0D);
	}

	public static class LinkedRockLayerConfigFactory<T_Entry extends RockLayerEntryFeature.Entry> extends Factory<LinkedRockLayerConfig<T_Entry>, RockLayerGroupFeature.Config, RockLayerEntryFeature.Config<T_Entry>, T_Entry> {

		public LinkedRockLayerConfigFactory(ObjectArrayFactory<LinkedRockLayerConfig<T_Entry>> linkedConfigClass, Feature<RockLayerGroupFeature.Config> groupFeature, Feature<RockLayerEntryFeature.Config<T_Entry>> entryFeature) {
			super(linkedConfigClass, groupFeature, entryFeature);
		}

		@Override
		public LinkedRockLayerConfig<T_Entry> newConfig(
			Identifier name,
			RockLayerGroupFeature.Config groupConfig,
			List<T_Entry> entries
		) {
			return new LinkedRockLayerConfig<>(name, groupConfig, entries);
		}
	}
}