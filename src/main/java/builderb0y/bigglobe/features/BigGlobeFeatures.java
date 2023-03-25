package builderb0y.bigglobe.features;

import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.feature.Feature;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.features.flowers.FlowerEntryFeature;
import builderb0y.bigglobe.features.flowers.FlowerGroupFeature;
import builderb0y.bigglobe.features.ores.NetherOreFeature;
import builderb0y.bigglobe.features.ores.OverworldOreFeature;
import builderb0y.bigglobe.features.overriders.*;
import builderb0y.bigglobe.features.rockLayers.RockLayerEntryFeature;
import builderb0y.bigglobe.features.rockLayers.RockLayerGroupFeature;

public class BigGlobeFeatures {

	static { BigGlobeMod.LOGGER.debug("Registering features..."); }

	public static final BigGlobeRandomFeature       RANDOM                        = register("random",                        new       BigGlobeRandomFeature());
	public static final SingleBlockFeature          SINGLE_BLOCK                  = register("single_block",                  new          SingleBlockFeature());
	public static final NaturalTreeFeature          NATURAL_TREE                  = register("natural_tree",                  new          NaturalTreeFeature());
	public static final ArtificialTreeFeature       ARTIFICIAL_TREE               = register("artificial_tree",               new       ArtificialTreeFeature());
	public static final BushFeature                 BUSH                          = register("bush",                          new                 BushFeature());
	public static final OverworldOreFeature         OVERWORLD_ORE                 = register("overworld_ore",                 new         OverworldOreFeature());
	public static final NetherOreFeature            NETHER_ORE                    = register("nether_ore",                    new            NetherOreFeature());
	public static final FlowerGroupFeature          FLOWER_GROUP                  = register("overworld_flower_group",        new          FlowerGroupFeature());
	public static final FlowerEntryFeature          FLOWER_ENTRY                  = register("overworld_flower_entry",        new          FlowerEntryFeature());
	public static final RockLayerGroupFeature       ROCK_LAYER_GROUP              = register("overworld_rock_layer_group",    new       RockLayerGroupFeature());
	public static final RockLayerEntryFeature       ROCK_LAYER_ENTRY              = register("overworld_rock_layer_entry",    new       RockLayerEntryFeature());
	public static final HeightOverrideFeature       OVERWORLD_HEIGHT_OVERRIDER    = register("overworld_height_overrider",    new       HeightOverrideFeature());
	public static final FoliageOverrideFeature      OVERWORLD_FOLIAGE_OVERRIDER   = register("overworld_foliage_overrider",   new      FoliageOverrideFeature());
	public static final CaveOverrideFeature         OVERWORLD_CAVE_OVERRIDER      = register("overworld_cave_overrider",      new         CaveOverrideFeature());
	public static final CavernOverrideFeature       OVERWORLD_CAVERN_OVERRIDER    = register("overworld_cavern_overrider",    new       CavernOverrideFeature());
	public static final StructureOverrideFeature    OVERWORLD_STRUCTURE_OVERRIDER = register("overworld_structure_overrider", new    StructureOverrideFeature());
	public static final StructureOverrideFeature    NETHER_STRUCTURE_OVERRIDER    = register("nether_structure_overrider",    new    StructureOverrideFeature());
	public static final ScriptedFeature             SCRIPT                        = register("script",                        new             ScriptedFeature());
	public static final DefineScriptTemplateFeature DEFINE_SCRIPT_TEMPLATE        = register("define_script_template",        new DefineScriptTemplateFeature());
	public static final UseScriptTemplateFeature    USE_SCRIPT_TEMPLATE           = register("use_script_template",           new    UseScriptTemplateFeature());

	static { BigGlobeMod.LOGGER.debug("Done registering features."); }

	public static <F extends Feature<?>> F register(String name, F feature) {
		return Registry.register(Registry.FEATURE, BigGlobeMod.modID(name), feature);
	}

	public static void init() {}
}