package builderb0y.bigglobe.blockdefs;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class DarkslateBlocks {

	public static void init() {}

	public static final RotatedPillarBlock
		DARKSLATE = BigGlobeBlocks.register(
			new RotatedPillarBlock(
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.DEEPSLATE)
				.setId(BigGlobeBlocks.key("darkslate"))
				.strength(4.5F, 6.0F)
			)
		),
		INFESTED_DARKSLATE = BigGlobeBlocks.register(
			new RotatedPillarBlock(
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.INFESTED_DEEPSLATE)
				.setId(BigGlobeBlocks.key("infested_darkslate"))
			)
		);
	public static final Block
		CHISELED_DARKSLATE = BigGlobeBlocks.register(
			new Block(
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.CHISELED_DEEPSLATE)
				.strength(4.5F, 6.0F)
				.setId(BigGlobeBlocks.key("chiseled_darkslate"))
			)
		),
		COBBLED_DARKSLATE = BigGlobeBlocks.register(
			new Block(
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.COBBLED_DEEPSLATE)
				.strength(5.0F, 6.0F)
				.setId(BigGlobeBlocks.key("cobbled_darkslate"))
			)
		),
		POLISHED_DARKSLATE = BigGlobeBlocks.register(
			new Block(
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.POLISHED_DEEPSLATE)
				.strength(4.5F, 6.0F)
				.setId(BigGlobeBlocks.key("polished_darkslate"))
			)
		),
		DARKSLATE_BRICKS = BigGlobeBlocks.register(
			new Block(
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.DEEPSLATE_BRICKS)
				.strength(4.5F, 6.0F)
				.setId(BigGlobeBlocks.key("darkslate_bricks"))
			)
		),
		DARKSLATE_TILES = BigGlobeBlocks.register(
			new Block(
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.DEEPSLATE_TILES)
				.strength(4.5F, 6.0F)
				.setId(BigGlobeBlocks.key("darkslate_tiles"))
			)
		);
	public static final StairBlock
		COBBLED_DARKSLATE_STAIRS = BigGlobeBlocks.register(
			new StairBlock(
				COBBLED_DARKSLATE.defaultBlockState(),
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.COBBLED_DEEPSLATE_STAIRS)
				.strength(5.0F, 6.0F)
				.setId(BigGlobeBlocks.key("cobbled_darkslate_stairs"))
			)
		),
		POLISHED_DARKSLATE_STAIRS = BigGlobeBlocks.register(
			new StairBlock(
				POLISHED_DARKSLATE.defaultBlockState(),
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.POLISHED_DEEPSLATE_STAIRS)
				.strength(4.5F, 6.0F)
				.setId(BigGlobeBlocks.key("polished_darkslate_stairs"))
			)
		),
		DARKSLATE_BRICK_STAIRS = BigGlobeBlocks.register(
			new StairBlock(
				DARKSLATE_BRICKS.defaultBlockState(),
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.DEEPSLATE_BRICK_STAIRS)
				.strength(4.5F, 6.0F)
				.setId(BigGlobeBlocks.key("darkslate_brick_stairs"))
			)
		),
		DARKSLATE_TILE_STAIRS = BigGlobeBlocks.register(
			new StairBlock(
				DARKSLATE_TILES.defaultBlockState(),
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.DEEPSLATE_TILE_STAIRS)
				.strength(4.5F, 6.0F)
				.setId(BigGlobeBlocks.key("darkslate_tile_stairs"))
			)
		);
	public static final SlabBlock
		COBBLED_DARKSLATE_SLAB = BigGlobeBlocks.register(
			new SlabBlock(
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.COBBLED_DEEPSLATE_SLAB)
				.strength(5.0F, 6.0F)
				.setId(BigGlobeBlocks.key("cobbled_darkslate_slab"))
			)
		),
		POLISHED_DARKSLATE_SLAB = BigGlobeBlocks.register(
			new SlabBlock(
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.POLISHED_DEEPSLATE_SLAB)
				.strength(4.5F, 6.0F)
				.setId(BigGlobeBlocks.key("polished_darkslate_slab"))
			)
		),
		DARKSLATE_BRICK_SLAB = BigGlobeBlocks.register(
			new SlabBlock(
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.DEEPSLATE_BRICK_SLAB)
				.strength(4.5F, 6.0F)
				.setId(BigGlobeBlocks.key("darkslate_brick_slab"))
			)
		),
		DARKSLATE_TILE_SLAB = BigGlobeBlocks.register(
			new SlabBlock(
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.DEEPSLATE_TILE_SLAB)
				.strength(4.5F, 6.0F)
				.setId(BigGlobeBlocks.key("darkslate_tile_slab"))
			)
		);
	public static final WallBlock
		COBBLED_DARKSLATE_WALL = BigGlobeBlocks.register(
			new WallBlock(
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.COBBLED_DEEPSLATE_WALL)
				.strength(5.0F, 6.0F)
				.setId(BigGlobeBlocks.key("cobbled_darkslate_wall"))
			)
		),
		POLISHED_DARKSLATE_WALL = BigGlobeBlocks.register(
			new WallBlock(
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.POLISHED_DEEPSLATE_WALL)
				.strength(4.5F, 6.0F)
				.setId(BigGlobeBlocks.key("polished_darkslate_wall"))
			)
		),
		DARKSLATE_BRICK_WALL = BigGlobeBlocks.register(
			new WallBlock(
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.DEEPSLATE_BRICK_WALL)
				.strength(4.5F, 6.0F)
				.setId(BigGlobeBlocks.key("darkslate_brick_wall"))
			)
		),
		DARKSLATE_TILE_WALL = BigGlobeBlocks.register(
			new WallBlock(
				BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.DEEPSLATE_TILE_WALL)
				.strength(4.5F, 6.0F)
				.setId(BigGlobeBlocks.key("darkslate_tile_wall"))
			)
		);
}