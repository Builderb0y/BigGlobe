package builderb0y.bigglobe.features;

import net.minecraft.registry.Registry;
import net.minecraft.world.gen.feature.Feature;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.features.flowers.FlowerEntryFeature;
import builderb0y.bigglobe.features.flowers.FlowerGroupFeature;
import builderb0y.bigglobe.features.flowers.NetherFlowerFeature;
import builderb0y.bigglobe.features.ores.NetherOreFeature;
import builderb0y.bigglobe.features.ores.OverworldOreFeature;
import builderb0y.bigglobe.features.rockLayers.NetherRockLayerEntryFeature;
import builderb0y.bigglobe.features.rockLayers.OverworldRockLayerEntryFeature;
import builderb0y.bigglobe.features.rockLayers.RockLayerGroupFeature;
import builderb0y.bigglobe.overriders.ScriptStructureOverrider;
import builderb0y.bigglobe.overriders.end.EndFoliageOverrider;
import builderb0y.bigglobe.overriders.end.EndHeightOverrider;
import builderb0y.bigglobe.overriders.end.EndVolumetricOverrider;
import builderb0y.bigglobe.overriders.nether.NetherVolumetricOverrider;
import builderb0y.bigglobe.overriders.overworld.*;
import builderb0y.bigglobe.versions.RegistryVersions;

public class BigGlobeFeatures {

	static { BigGlobeMod.LOGGER.debug("Registering features..."); }

	public static final BigGlobeRandomFeature                                   RANDOM                             = register("random",                             new           BigGlobeRandomFeature());
	public static final SingleBlockFeature                                      SINGLE_BLOCK                       = register("single_block",                       new              SingleBlockFeature());
	public static final NaturalTreeFeature                                      NATURAL_TREE                       = register("natural_tree",                       new              NaturalTreeFeature());
	public static final ArtificialTreeFeature                                   ARTIFICIAL_TREE                    = register("artificial_tree",                    new           ArtificialTreeFeature());
	public static final EndSpikeReplacementFeature                              END_SPIKE                          = register("end_spike",                          new      EndSpikeReplacementFeature());

	public static final OverworldOreFeature                                     OVERWORLD_ORE                      = register("overworld_ore",                      new             OverworldOreFeature());
	public static final NetherOreFeature                                        NETHER_ORE                         = register("nether_ore",                         new                NetherOreFeature());

	public static final FlowerGroupFeature                                      FLOWER_GROUP                       = register("overworld_flower_group",             new              FlowerGroupFeature());
	public static final FlowerEntryFeature                                      FLOWER_ENTRY                       = register("overworld_flower_entry",             new              FlowerEntryFeature());
	public static final NetherFlowerFeature                                     NETHER_FLOWER                      = register("nether_flower",                      new             NetherFlowerFeature());

	public static final RockLayerGroupFeature                                   OVERWORLD_ROCK_LAYER_GROUP         = register("overworld_rock_layer_group",         new           RockLayerGroupFeature());
	public static final OverworldRockLayerEntryFeature                          OVERWORLD_ROCK_LAYER_ENTRY         = register("overworld_rock_layer_entry",         new  OverworldRockLayerEntryFeature());
	public static final RockLayerGroupFeature                                   NETHER_ROCK_LAYER_GROUP            = register("nether_rock_layer_group",            new           RockLayerGroupFeature());
	public static final NetherRockLayerEntryFeature                             NETHER_ROCK_LAYER_ENTRY            = register("nether_rock_layer_entry",            new     NetherRockLayerEntryFeature());

	public static final OverrideFeature<OverworldHeightOverrider.Holder>        OVERWORLD_HEIGHT_OVERRIDER         = register("overworld_height_overrider",         new                 OverrideFeature<>(       OverworldHeightOverrider.Holder.class));
	public static final OverrideFeature<OverworldGlacierHeightOverrider.Holder> OVERWORLD_GLACIER_HEIGHT_OVERRIDER = register("overworld_glacier_height_overrider", new                 OverrideFeature<>(OverworldGlacierHeightOverrider.Holder.class));
	public static final OverrideFeature<OverworldFoliageOverrider.Holder>       OVERWORLD_FOLIAGE_OVERRIDER        = register("overworld_foliage_overrider",        new                 OverrideFeature<>(      OverworldFoliageOverrider.Holder.class));
	public static final OverrideFeature<OverworldVolumetricOverrider.Holder>    OVERWORLD_CAVE_OVERRIDER           = register("overworld_cave_overrider",           new                 OverrideFeature<>(   OverworldVolumetricOverrider.Holder.class));
	public static final OverrideFeature<OverworldCavernOverrider.Holder>        OVERWORLD_CAVERN_OVERRIDER         = register("overworld_cavern_overrider",         new                 OverrideFeature<>(       OverworldCavernOverrider.Holder.class));
	public static final OverrideFeature<OverworldSkylandOverrider.Holder>       OVERWORLD_SKYLAND_OVERRIDER        = register("overworld_skyland_overrider",        new                 OverrideFeature<>(      OverworldSkylandOverrider.Holder.class));
	public static final OverrideFeature<ScriptStructureOverrider.Holder>        OVERWORLD_STRUCTURE_OVERRIDER      = register("overworld_structure_overrider",      new                 OverrideFeature<>(       ScriptStructureOverrider.Holder.class));

	public static final OverrideFeature<NetherVolumetricOverrider.Holder>       NETHER_CAVE_OVERRIDER              = register("nether_cave_overrider",              new                 OverrideFeature<>(      NetherVolumetricOverrider.Holder.class));
	public static final OverrideFeature<NetherVolumetricOverrider.Holder>       NETHER_CAVERN_OVERRIDER            = register("nether_cavern_overrider",            new                 OverrideFeature<>(      NetherVolumetricOverrider.Holder.class));
	public static final OverrideFeature<ScriptStructureOverrider.Holder>        NETHER_STRUCTURE_OVERRIDER         = register("nether_structure_overrider",         new                 OverrideFeature<>(       ScriptStructureOverrider.Holder.class));

	public static final OverrideFeature<EndHeightOverrider.Holder>              END_HEIGHT_OVERRIDER               = register("end_height_overrider",               new                 OverrideFeature<>(             EndHeightOverrider.Holder.class));
	public static final OverrideFeature<EndFoliageOverrider.Holder>             END_FOLIAGE_OVERRIDER              = register("end_foliage_overrider",              new                 OverrideFeature<>(            EndFoliageOverrider.Holder.class));
	public static final OverrideFeature<EndVolumetricOverrider.Holder>          END_LOWER_RING_CLOUD_OVERRIDER     = register("end_lower_ring_cloud_overrider",     new                 OverrideFeature<>(         EndVolumetricOverrider.Holder.class));
	public static final OverrideFeature<EndVolumetricOverrider.Holder>          END_UPPER_RING_CLOUD_OVERRIDER     = register("end_upper_ring_cloud_overrider",     new                 OverrideFeature<>(         EndVolumetricOverrider.Holder.class));
	public static final OverrideFeature<EndVolumetricOverrider.Holder>          END_LOWER_BRIDGE_CLOUD_OVERRIDER   = register("end_lower_bridge_cloud_overrider",   new                 OverrideFeature<>(         EndVolumetricOverrider.Holder.class));
	public static final OverrideFeature<EndVolumetricOverrider.Holder>          END_UPPER_BRIDGE_CLOUD_OVERRIDER   = register("end_upper_bridge_cloud_overrider",   new                 OverrideFeature<>(         EndVolumetricOverrider.Holder.class));
	public static final OverrideFeature<ScriptStructureOverrider.Holder>        END_STRUCTURE_OVERRIDER            = register("end_structure_overrider",            new                 OverrideFeature<>(       ScriptStructureOverrider.Holder.class));

	public static final ScriptedFeature                                         SCRIPT                             = register("script",                             new                 ScriptedFeature());

	static { BigGlobeMod.LOGGER.debug("Done registering features."); }

	public static <F extends Feature<?>> F register(String name, F feature) {
		return Registry.register(RegistryVersions.feature(), BigGlobeMod.modID(name), feature);
	}

	public static void init() {}
}