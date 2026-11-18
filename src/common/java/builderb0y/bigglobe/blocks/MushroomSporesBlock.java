package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.versions.BlockStateVersions;

public class MushroomSporesBlock extends VegetationBlock {

	public static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D);

	#if MC_VERSION < MC_26_3_0

		public static final MapCodec<MushroomSporesBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(MushroomSporesBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec codec() {
			return CODEC;
		}

	#endif

	public MushroomSporesBlock(Properties settings) {
		super(settings);
	}

	@Override
	public boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
		return BlockStateVersions.isOpaqueFullCube(floor, world, pos); //match logic from MushroomPlantBlock.canPlantOnTop().
	}

	@Override
	@Deprecated
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}