package builderb0y.bigglobe.features.rockLayers;

import java.util.List;

import net.minecraft.util.Identifier;

import builderb0y.bigglobe.features.LinkedConfig;
import builderb0y.bigglobe.noise.Permuter;

public class LinkedRockLayerConfig extends LinkedConfig<
	RockLayerGroupFeature.Config,
	RockLayerEntryFeature.Config,
	RockLayerEntryFeature.Entry
>  {

	public static final LinkedConfig.Factory<
		LinkedRockLayerConfig,
		RockLayerGroupFeature.Config,
		RockLayerEntryFeature.Config,
		RockLayerEntryFeature.Entry
	>
	FACTORY = new Factory<>(
		LinkedRockLayerConfig.class,
		RockLayerGroupFeature.Config.class,
		RockLayerEntryFeature.Config.class
	) {

		@Override
		public LinkedRockLayerConfig newConfig(
			Identifier name,
			RockLayerGroupFeature.Config groupConfig,
			List<RockLayerEntryFeature.Entry> entries
		) {
			return new LinkedRockLayerConfig(name, groupConfig, entries);
		}
	};

	public final long nameHash;
	public final double minWindow, maxWindow;

	public LinkedRockLayerConfig(
		Identifier name,
		RockLayerGroupFeature.Config group,
		List<RockLayerEntryFeature.Entry> entries
	) {
		super(name, group, entries);
		this.nameHash = Permuter.permute(0x1185E30383BD341CL, name);
		this.minWindow = entries.stream().mapToDouble(entry -> entry.center.minValue() - entry.thickness.maxValue()).min().orElse(0.0D);
		this.maxWindow = entries.stream().mapToDouble(entry -> entry.center.maxValue() + entry.thickness.maxValue()).max().orElse(0.0D);
	}
}