package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class NetherGrassBlock extends PlantBlock {

	public static final VoxelShape SHAPE = Block.createCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 8.0D, 14.0D);

	#if MC_VERSION >= MC_1_20_3
		public static final MapCodec<NetherGrassBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(NetherGrassBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			return CODEC;
		}
	#endif

	public NetherGrassBlock(Settings settings) {
		super(settings);
	}

	@Override
	public boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return floor.isOpaqueFullCube(world, pos);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}
}