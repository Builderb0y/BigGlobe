package builderb0y.bigglobe.items;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;

import net.minecraft.block.Block;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.item.*;
import net.minecraft.util.DyeColor;
import net.minecraft.util.registry.Registry;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BigGlobeBlockTags;
import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import builderb0y.bigglobe.fluids.BigGlobeFluids;

public class BigGlobeItems {

	static { BigGlobeMod.LOGGER.debug("Registering items..."); }

	/*
	@TestOnly
	@Deprecated
	public static final TestItem
		TEST_ITEM                = register("test_item", new TestItem());
	//*/
	public static final BlockItem
		OVERGROWN_SAND           = registerBlockPlacer   (BigGlobeBlocks.OVERGROWN_SAND),
		OVERGROWN_PODZOL         = registerBlockPlacer   (BigGlobeBlocks.OVERGROWN_PODZOL),
		ROSE                     = registerDecoPlacer    (BigGlobeBlocks.ROSE),
		SHORT_GRASS              = registerDecoPlacer    (BigGlobeBlocks.SHORT_GRASS),
		MUSHROOM_SPORES          = registerDecoPlacer    (BigGlobeBlocks.MUSHROOM_SPORES),
		ROPE_ANCHOR              = registerDecoPlacer    (BigGlobeBlocks.ROPE_ANCHOR),
		SPELUNKING_ROPE          = registerPlacer        (BigGlobeBlocks.SPELUNKING_ROPE, ItemGroup.TOOLS),
		CRYSTALLINE_PRISMARINE   = registerBlockPlacer   (BigGlobeBlocks.CRYSTALLINE_PRISMARINE),
		SLATED_PRISMARINE        = registerBlockPlacer   (BigGlobeBlocks.SLATED_PRISMARINE),
		SLATED_PRISMARINE_SLAB   = registerBlockPlacer   (BigGlobeBlocks.SLATED_PRISMARINE_SLAB),
		SLATED_PRISMARINE_STAIRS = registerBlockPlacer   (BigGlobeBlocks.SLATED_PRISMARINE_STAIRS),
		FLOATSTONE               = registerBlockPlacer   (BigGlobeBlocks.FLOATSTONE),
		FLOATSTONE_SLAB          = registerBlockPlacer   (BigGlobeBlocks.FLOATSTONE_SLAB),
		FLOATSTONE_STAIRS        = registerBlockPlacer   (BigGlobeBlocks.FLOATSTONE_STAIRS),
		ASHEN_NETHERRACK         = registerBlockPlacer   (BigGlobeBlocks.ASHEN_NETHERRACK),
		SULFUR_ORE               = registerBlockPlacer   (BigGlobeBlocks.SULFUR_ORE),
		SULFUR_BLOCK             = registerBlockPlacer   (BigGlobeBlocks.SULFUR_BLOCK),
		WART_WEED                = registerDecoPlacer    (BigGlobeBlocks.WART_WEED),
		CHARRED_GRASS            = registerDecoPlacer    (BigGlobeBlocks.CHARRED_GRASS),
		BLAZING_BLOSSOM          = registerDecoPlacer    (BigGlobeBlocks.BLAZING_BLOSSOM),
		GLOWING_GOLDENROD        = registerDecoPlacer    (BigGlobeBlocks.GLOWING_GOLDENROD),
		CHARRED_PLANKS           = registerBlockPlacer   (BigGlobeBlocks.CHARRED_PLANKS),
		CHARRED_SAPLING          = registerDecoPlacer    (BigGlobeBlocks.CHARRED_SAPLING),
		CHARRED_LOG              = registerBlockPlacer   (BigGlobeBlocks.CHARRED_LOG),
		STRIPPED_CHARRED_LOG     = registerBlockPlacer   (BigGlobeBlocks.STRIPPED_CHARRED_LOG),
		CHARRED_WOOD             = registerBlockPlacer   (BigGlobeBlocks.CHARRED_WOOD),
		STRIPPED_CHARRED_WOOD    = registerBlockPlacer   (BigGlobeBlocks.STRIPPED_CHARRED_WOOD),
		CHARRED_LEAVES           = registerDecoPlacer    (BigGlobeBlocks.CHARRED_LEAVES),
		CHARRED_SIGN             = register              ("charred_sign", new ColoredSignItem(new Item.Settings().maxCount(16).group(ItemGroup.DECORATIONS), BigGlobeBlocks.CHARRED_SIGN, BigGlobeBlocks.CHARRED_WALL_SIGN, DyeColor.LIGHT_GRAY)),
		CHARRED_PRESSURE_PLATE   = registerRedstonePlacer(BigGlobeBlocks.CHARRED_PRESSURE_PLATE),
		CHARRED_TRAPDOOR         = registerRedstonePlacer(BigGlobeBlocks.CHARRED_TRAPDOOR),
		CHARRED_STAIRS           = registerBlockPlacer   (BigGlobeBlocks.CHARRED_STAIRS),
		CHARRED_BUTTON           = registerRedstonePlacer(BigGlobeBlocks.CHARRED_BUTTON),
		CHARRED_SLAB             = registerBlockPlacer   (BigGlobeBlocks.CHARRED_SLAB),
		CHARRED_FENCE_GATE       = registerRedstonePlacer(BigGlobeBlocks.CHARRED_FENCE_GATE),
		CHARRED_FENCE            = registerDecoPlacer    (BigGlobeBlocks.CHARRED_FENCE),
		CHARRED_DOOR             = register              ("charred_door", new TallBlockItem(BigGlobeBlocks.CHARRED_DOOR, new Item.Settings().group(ItemGroup.REDSTONE)));

	public static final TorchArrowItem TORCH_ARROW = register(
		"torch_arrow",
		new TorchArrowItem(
			new Item.Settings().group(ItemGroup.COMBAT)
		)
	);
	public static final PercussiveHammerItem PERCUSSIVE_HAMMER = register(
		"percussive_hammer",
		new PercussiveHammerItem(
			2.0F,
			-2.8F,
			ToolMaterials.IRON,
			BigGlobeBlockTags.MINEABLE_PERCUSSIVE_HAMMER,
			new Item.Settings().group(ItemGroup.TOOLS)
		)
	);
	public static final Item ASH = register(
		"ash",
		new Item(
			new Item.Settings().group(ItemGroup.BREWING)
		)
	);
	public static final Item SULFUR = register(
		"sulfur",
		new Item(
			new Item.Settings().group(ItemGroup.MISC)
		)
	);
	public static final BucketItem SOUL_LAVA_BUCKET = register(
		"soul_lava_bucket",
		new BucketItem(
			BigGlobeFluids.SOUL_LAVA,
			new Item.Settings().recipeRemainder(Items.BUCKET).maxCount(1).group(ItemGroup.MISC)
		)
	);

	static { BigGlobeMod.LOGGER.debug("Done registering items."); }

	public static BlockItem registerBlockPlacer(Block block) {
		return registerPlacer(block, ItemGroup.BUILDING_BLOCKS);
	}

	public static BlockItem registerDecoPlacer(Block block) {
		return registerPlacer(block, ItemGroup.DECORATIONS);
	}

	public static BlockItem registerRedstonePlacer(Block block) {
		return registerPlacer(block, ItemGroup.REDSTONE);
	}

	public static BlockItem registerPlacer(Block block, ItemGroup group) {
		return Registry.register(
			Registry.ITEM,
			Registry.BLOCK.getId(block),
			new BlockItem(block, new Item.Settings().group(group))
		);
	}

	public static <I extends Item> I register(String name, I item) {
		return Registry.register(Registry.ITEM, BigGlobeMod.modID(name), item);
	}

	public static void init() {
		FuelRegistry.INSTANCE.add(SOUL_LAVA_BUCKET, 20000);
		FuelRegistry.INSTANCE.add(SULFUR, 1200);
		FuelRegistry.INSTANCE.add(SULFUR_BLOCK, 12000);
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		ColorProviderRegistry.ITEM.register(
			(stack, tintIndex) -> GrassColors.getColor(0.5D, 1.0D),
			OVERGROWN_PODZOL,
			SHORT_GRASS
		);
	}
}