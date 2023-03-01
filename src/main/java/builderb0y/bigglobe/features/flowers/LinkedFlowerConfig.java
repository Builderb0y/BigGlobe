package builderb0y.bigglobe.features.flowers;

import java.util.List;

import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.features.LinkedConfig;

/**
flower placement is handled by {@link BigGlobeOverworldChunkGenerator},
and is done in a very efficient multi-threaded way.
but in order to work, the chunk generator needs to know what flowers to place.
putting flowers directly in the preset json file is problematic
because every possible mod list would require its own preset file.
so, I instead opted to have every flower be a Feature,
located in the feature registry, and having its own dedicated file.
these are "dummy" entries in the registry;
{@link Feature#generate(FeatureContext)} will do nothing for them,
except log a warning, since these features are not designed to be generated or
placed in {@link Biome#generationSettings}.{@link GenerationSettings#features}.
the chunk generator will look for flower entries in the feature registry on construction,
and use them to place flowers using the previously mentioned efficient multi-threaded way.

flower features are split up into 2 parts: groups, and entries.
a group defines a set of related flowers, and entries define which flowers those are.
the benefit of this is that it allows modded flowers to be added to existing groups.
you'd probably want your own group for botania, for example,
but if a mod only adds a couple flowers that spawn mostly like vanilla,
then they can just be added to the vanilla_flowers group.
this class is responsible for "linking" the groups and the entries together.
in other words, determining which entries belong to which groups.
it also performs some sanity checking to ensure that:
	1: every entry has a group.
	2: every group has a json file to define the group's properties.
	3: every group has at least one entry.
	4: no group has more than one associated json file.
		this one should be impossible, but it never hurts to verify it anyway.
		not like this will be bad on performance or anything. it's a single null check.
*/
public class LinkedFlowerConfig extends LinkedConfig<
	FlowerGroupFeature.Config,
	FlowerEntryFeature.Config,
	FlowerEntryFeature.Entry
> {

	public static final LinkedConfig.Factory<
		LinkedFlowerConfig,
		FlowerGroupFeature.Config,
		FlowerEntryFeature.Config,
		FlowerEntryFeature.Entry
	>
	FACTORY = new Factory<>(
		LinkedFlowerConfig.class,
		FlowerGroupFeature.Config.class,
		FlowerEntryFeature.Config.class
	) {

		@Override
		public LinkedFlowerConfig newConfig(
			Identifier name,
			FlowerGroupFeature.Config groupConfig,
			List<FlowerEntryFeature.Entry> entries
		) {
			return new LinkedFlowerConfig(name, groupConfig, entries);
		}
	};

	public LinkedFlowerConfig(
		Identifier name,
		FlowerGroupFeature.Config group,
		List<FlowerEntryFeature.Entry> entries
	) {
		super(name, group, entries);
	}
}