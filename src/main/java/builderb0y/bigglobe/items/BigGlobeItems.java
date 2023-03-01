package builderb0y.bigglobe.items;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;

import net.minecraft.block.Block;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ToolMaterials;
import net.minecraft.util.registry.Registry;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BigGlobeBlockTags;
import builderb0y.bigglobe.blocks.BigGlobeBlocks;

public class BigGlobeItems {

	static { BigGlobeMod.LOGGER.debug("Registering items..."); }

	/*
	@TestOnly
	@Deprecated
	public static final TestItem
		TEST_ITEM                = register("test_item", new TestItem());
	//*/
	public static final BlockItem
		OVERGROWN_SAND           = registerBlockPlacer(BigGlobeBlocks.OVERGROWN_SAND),
		OVERGROWN_PODZOL         = registerBlockPlacer(BigGlobeBlocks.OVERGROWN_PODZOL),
		ROSE                     = registerDecoPlacer (BigGlobeBlocks.ROSE),
		SHORT_GRASS              = registerDecoPlacer (BigGlobeBlocks.SHORT_GRASS),
		MUSHROOM_SPORES          = registerDecoPlacer (BigGlobeBlocks.MUSHROOM_SPORES),
		ROPE_ANCHOR              = registerDecoPlacer (BigGlobeBlocks.ROPE_ANCHOR),
		SPELUNKING_ROPE          = registerPlacer     (BigGlobeBlocks.SPELUNKING_ROPE, ItemGroup.TOOLS),
		CRYSTALLINE_PRISMARINE   = registerBlockPlacer(BigGlobeBlocks.CRYSTALLINE_PRISMARINE),
		SLATED_PRISMARINE        = registerBlockPlacer(BigGlobeBlocks.SLATED_PRISMARINE),
		SLATED_PRISMARINE_SLAB   = registerBlockPlacer(BigGlobeBlocks.SLATED_PRISMARINE_SLAB),
		SLATED_PRISMARINE_STAIRS = registerBlockPlacer(BigGlobeBlocks.SLATED_PRISMARINE_STAIRS),
		FLOATSTONE               = registerBlockPlacer(BigGlobeBlocks.FLOATSTONE),
		FLOATSTONE_SLAB          = registerBlockPlacer(BigGlobeBlocks.FLOATSTONE_SLAB),
		FLOATSTONE_STAIRS        = registerBlockPlacer(BigGlobeBlocks.FLOATSTONE_STAIRS);
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

	static { BigGlobeMod.LOGGER.debug("Done registering items."); }

	public static BlockItem registerBlockPlacer(Block block) {
		return registerPlacer(block, ItemGroup.BUILDING_BLOCKS);
	}

	public static BlockItem registerDecoPlacer(Block block) {
		return registerPlacer(block, ItemGroup.DECORATIONS);
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
		//triggers static initializer.
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