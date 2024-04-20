package builderb0y.bigglobe.features;

import net.minecraft.registry.Registry;
import net.minecraft.world.gen.feature.Feature;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.RegistryVersions;

public class BigGlobeFeatures {

	static { BigGlobeMod.LOGGER.debug("Registering features..."); }

	public static final BigGlobeRandomFeature                                   RANDOM                             = register("random",                             new           BigGlobeRandomFeature());
	public static final SingleBlockFeature                                      SINGLE_BLOCK                       = register("single_block",                       new              SingleBlockFeature());
	public static final NaturalTreeFeature                                      NATURAL_TREE                       = register("natural_tree",                       new              NaturalTreeFeature());
	public static final ArtificialTreeFeature                                   ARTIFICIAL_TREE                    = register("artificial_tree",                    new           ArtificialTreeFeature());
	public static final EndSpikeRespawnFeature                                  END_SPIKE_RESPAWN                  = register("end_spike_respawn",                  new          EndSpikeRespawnFeature());
	public static final EndSpikeWorldgenFeature                                 END_SPIKE_WORLDGEN                 = register("end_spike_worldgen",                 new         EndSpikeWorldgenFeature());
	public static final GenericOreFeature                                       GENERIC_ORE                        = register("generic_ore",                        new               GenericOreFeature());
	public static final FlowerFeature                                           FLOWER                             = register("flower",                             new                   FlowerFeature());
	public static final FlowerControllerFeature                                 FLOWER_CONTROlLER                  = register("flower_controller",                  new         FlowerControllerFeature());
	public static final NetherFlowerFeature                                     NETHER_FLOWER                      = register("nether_flower",                      new             NetherFlowerFeature());

	public static final ChunkSprinkleFeature                                    CHUNK_SPRINKLE                     = register("chunk_sprinkle",                     new            ChunkSprinkleFeature());
	public static final MoltenRockReplacerFeature                               MOLTEN_ROCK_FEATURE                = register("molten_rock",                        new       MoltenRockReplacerFeature());
	public static final BedrockFeature                                          BEDROCK                            = register("bedrock",                            new                  BedrockFeature());
	public static final RockLayerFeature                                        ROCK_LAYER                         = register("rock_layer",                         new                RockLayerFeature());
	public static final OreFeature                                              ORE                                = register("ore",                                new                      OreFeature());

	public static final ScriptedFeature                                         SCRIPT                             = register("script",                             new                 ScriptedFeature());

	static { BigGlobeMod.LOGGER.debug("Done registering features."); }

	public static <F extends Feature<?>> F register(String name, F feature) {
		return Registry.register(RegistryVersions.feature(), BigGlobeMod.modID(name), feature);
	}

	public static void init() {}
}