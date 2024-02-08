package builderb0y.bigglobe.structures;

import java.util.Optional;
import java.util.random.RandomGenerator;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import net.minecraft.block.*;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.block.enums.StairShape;
import net.minecraft.block.enums.WallShape;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.mixins.StructurePiece_DirectRotationSetter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.structures.LabyrinthLayout.HallPiece;
import builderb0y.bigglobe.structures.LabyrinthLayout.LabyrinthPiece;
import builderb0y.bigglobe.structures.LabyrinthLayout.RoomPiece;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.util.coordinators.Coordinator;

public class BiggerDesertPyramidStructure extends BigGlobeStructure {

	public static final Codec<BiggerDesertPyramidStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(BiggerDesertPyramidStructure.class);

	public BiggerDesertPyramidStructure(Config config) {
		super(config);
	}

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		BlockPos pos = randomBlockInSurface(context, 0);
		if (pos == null) return Optional.empty();

		long seed = chunkSeed(context, 0xEA158B72242CC93EL);
		return Optional.of(
			new StructurePosition(
				pos,
				(StructurePiecesCollector collector) -> {
					Permuter permuter = new Permuter(seed);
					BlockRotation rotation = Directions.ROTATIONS[permuter.nextInt() & 3];
					MainPiece mainPiece = new MainPiece(BigGlobeStructures.BIGGER_DESERT_PYRAMID_PIECE_TYPE, pos.getX(), pos.getY(), pos.getZ(), rotation);
					collector.addPiece(mainPiece);
					UndergroundLayout layout = new UndergroundLayout(permuter, mainPiece);
					layout.generate();
					layout.addTo(collector);
				}
			)
		);
	}

	@Override
	public StructureType<?> getType() {
		return BigGlobeStructures.BIGGER_DESERT_PYRAMID_TYPE;
	}

	public static class MainPiece extends StructurePiece {

		public MainPiece(StructurePieceType type, int x, int y, int z, BlockRotation rotation) {
			super(
				type,
				0,
				switch (rotation) {
					case NONE                -> new BlockBox(x - 21, y - 16, z - 36, x + 21, y + 27, z + 21);
					case CLOCKWISE_90        -> new BlockBox(x - 21, y - 16, z - 21, x + 36, y + 27, z + 21);
					case CLOCKWISE_180       -> new BlockBox(x - 21, y - 16, z - 21, x + 21, y + 27, z + 36);
					case COUNTERCLOCKWISE_90 -> new BlockBox(x - 36, y - 16, z - 21, x + 21, y + 27, z + 21);
				}
			);
			((StructurePiece_DirectRotationSetter)(this)).bigglobe_setRotationDirect(rotation);
		}

		public MainPiece(StructurePieceType type, StructureContext context, NbtCompound nbt) {
			super(type, nbt);
			((StructurePiece_DirectRotationSetter)(this)).bigglobe_setRotationDirect(Directions.ROTATIONS[nbt.getByte("rot")]);
		}

		@Override
		public void writeNbt(StructureContext context, NbtCompound nbt) {
			nbt.putByte("rot", (byte)(this.getRotation().ordinal()));
		}

		public Coordinator coordinator(StructureWorldAccess world, BlockBox chunkBox) {
			return (
				Coordinator
				.forWorld(world, Block.NOTIFY_LISTENERS | Block.FORCE_STATE)
				.inBox(chunkBox, false)
				.translate(this.x(), this.y(), this.z())
				.rotate1x(this.getRotation())
			);
		}

		public int x() {
			int minX = this.boundingBox.getMinX();
			return minX + (this.getRotation() == BlockRotation.COUNTERCLOCKWISE_90 ? 36 : 21);
		}

		public int y() {
			return this.boundingBox.getMinY() + 16;
		}

		public int z() {
			int minZ = this.boundingBox.getMinZ();
			return minZ + (this.getRotation() == BlockRotation.NONE ? 36 : 21);
		}

		@Override
		public void generate(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			Coordinator root = this.coordinator(world, chunkBox);
			Coordinator flipX = root.flip2X();
			Coordinator flipXZ = flipX.flip2Z();
			Coordinator rot4 = root.rotate4x90();
			//base
			root.setBlockStateCuboid(-21, -16, -21, 21, -1, 21, BlockStates.SANDSTONE);
			root.setBlockStateCuboid(-20, 0, -20, 20, 0, 20, BlockStates.SANDSTONE);
			root.setBlockState(0, -1, 0, Blocks.BLUE_TERRACOTTA.getDefaultState());
			rot4.flip2X().setBlockStateLine(1, -1, 2, 0, 0, 1, 2, Blocks.ORANGE_TERRACOTTA.getDefaultState());
			rot4.flip2X().setBlockStateLine(2, -1, 4, 0, 0, 1, 2, Blocks.ORANGE_TERRACOTTA.getDefaultState());
			rot4.setBlockState(0, -1, 5, Blocks.CYAN_TERRACOTTA.getDefaultState());
			rot4.setBlockState(4, -1, 4, Blocks.CYAN_TERRACOTTA.getDefaultState());
			root.setBlockStateCuboid(-10, 0, -10, 10, 0, 10, BlockStates.AIR);
			root.setBlockStateCuboid(-1, 0, 11, 1, 0, 19, BlockStates.AIR);
			root.setBlockStateLine(-1, 0, 20, 1, 0, 0, 3, smoothBottomStairs(Direction.SOUTH));
			flipX.setBlockStateLine(2, 0, 14, 0, 0, -1, 3, smoothBottomStairs(Direction.EAST));
			flipX.setBlockState(2, 0, 11, smoothStairs(Direction.SOUTH, BlockHalf.BOTTOM, StairShape.OUTER_LEFT));
			flipX.setBlockStateLine(3, 0, 11, 1, 0, 0, 8, smoothBottomStairs(Direction.SOUTH));
			flipX.setBlockState(11, 0, 11, smoothStairs(Direction.SOUTH, BlockHalf.BOTTOM, StairShape.INNER_LEFT));
			root.flip4XZ().setBlockStateLine(11, 0, 10, 0, 0, -1, 8, smoothBottomStairs(Direction.EAST));
			flipX.setBlockState(11, 0, -11, smoothStairs(Direction.NORTH, BlockHalf.BOTTOM, StairShape.INNER_RIGHT));
			root.setBlockStateLine(-10, 0, -11, 1, 0, 0, 21, smoothBottomStairs(Direction.NORTH));
			//pyramid shape
			for (int y = 1; y <= 15; y++) {
				rot4.setBlockStateLine(-20 + y, y, -20 + y, 1, 0, 0, (20 - y) << 1, BlockStates.SANDSTONE);
				root.setBlockStateCuboid(-19 + y, y, -19 + y, 19 - y, y, 19 - y, BlockStates.AIR);
			}
			root.setBlockStateCuboid(-4, 16, -4, 4, 16, 4, BlockStates.SANDSTONE);
			flipX.setBlockStateCuboid(16, 1, -18, 18, 1, -2, BlockStates.SANDSTONE);
			flipX.setBlockStateCuboid(16, 2, -17, 17, 2, -2, BlockStates.SANDSTONE);
			flipX.setBlockStateLine(16, 3, -16, 0, 0, 1, 15, BlockStates.SANDSTONE);
			root.setBlockStateCuboid(-15, 1, -18, 15, 1, -16, BlockStates.SANDSTONE);
			root.setBlockStateCuboid(-15, 2, -17, 15, 2, -16, BlockStates.SANDSTONE);
			root.setBlockStateLine(-15, 3, -16, 1, 0, 0, 31, BlockStates.SANDSTONE);
			//towers
			Coordinator tower = flipX.translate(17, 0, 17);
			Coordinator rotatedTower = tower.rotate4x90();
			rotatedTower.setBlockStateCuboid(-3, 1, 3, 2, 16, 3, BlockStates.SANDSTONE);
			rotatedTower.setBlockStateLine(-3, 1, 3, 0, 1, 0, 9, BlockStates.CUT_SANDSTONE);
			rotatedTower.setBlockStateLine(-3, 15, 3, 0, 1, 0, 2, BlockStates.CUT_SANDSTONE);
			rotatedTower.setBlockState(0, 12, 3, Blocks.BLUE_TERRACOTTA.getDefaultState());
			rotatedTower.flip2X().setBlockStateLine(1, 9, 3, 0, 1, 0, 2, Blocks.ORANGE_TERRACOTTA.getDefaultState());
			rotatedTower.flip2X().setBlockStateLine(2, 11, 3, 0, 1, 0, 3, Blocks.ORANGE_TERRACOTTA.getDefaultState());
			rotatedTower.flip2X().setBlockStateLine(1, 14, 3, 0, 1, 0, 2, Blocks.ORANGE_TERRACOTTA.getDefaultState());
			tower.setBlockStateCuboid(-2, 10, -2, 2, 16, 2, BlockStates.SANDSTONE);
			tower.setBlockStateCuboid(-2, 1, -2, 2, 9, 2, BlockStates.AIR);
			rotatedTower.setBlockStateLine(-1, 9, -2, 1, 0, 0, 3, plainTopStairs(Direction.NORTH));
			rotatedTower.setBlockState(-2, 9, -2, plainStairs(Direction.NORTH, BlockHalf.TOP, StairShape.INNER_LEFT));
			tower.setBlockStateCuboid(-1, 9, -1, 1, 9, 1, Blocks.SMOOTH_SANDSTONE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
			flipX.setBlockStateLine(14, 6, 17, 0, 1, 0, 2, BlockStates.AIR);
			flipX.setBlockStateLine(17, 6, 14, 0, 1, 0, 2, BlockStates.AIR);
			//tower decorations
			flipX.setBlockStateLine(15, 5, 20, 1, 0, 0, 5, BlockStates.CUT_SANDSTONE);
			flipX.setBlockStateLine(20, 5, 15, 0, 0, 1, 5, BlockStates.CUT_SANDSTONE);
			flipX.setBlockStateLine(16, 7, 20, 1, 0, 0, 3, BlockStates.CHISELED_SANDSTONE);
			flipX.setBlockStateLine(20, 7, 16, 0, 0, 1, 3, BlockStates.CHISELED_SANDSTONE);
			flipX.stack(2, 0, 0, 2).setBlockStateLine(16, 0, 21, 0, 1, 0, BlockStates.CUT_SANDSTONE, BlockStates.CUT_SANDSTONE, BlockStates.CUT_SANDSTONE, smoothBottomStairs(Direction.NORTH));
			flipX.stack(0, 0, 2, 2).setBlockStateLine(21, 0, 16, 0, 1, 0, BlockStates.CUT_SANDSTONE, BlockStates.CUT_SANDSTONE, BlockStates.CUT_SANDSTONE, smoothBottomStairs(Direction.WEST));
			//pointy tower top
			for (int y = 0; y < 3; y++) {
				rotatedTower.setBlockStateLine(-2 + y, 17 + y, 3 - y, 1, 0, 0, 5 - (y << 1), bottomStairs(Blocks.SMOOTH_QUARTZ_STAIRS, Direction.NORTH));
				rotatedTower.setBlockState(3 - y, 17 + y, 3 - y, stairs(Blocks.SMOOTH_QUARTZ_STAIRS, Direction.NORTH, BlockHalf.BOTTOM, StairShape.OUTER_LEFT));
				tower.setBlockStateCuboid(-2 + y, 17 + y, -2 + y, 2 - y, 17 + y, 2 - y, Blocks.SMOOTH_QUARTZ.getDefaultState());
			}
			tower.setBlockState(0, 20, 0, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
			//tower staircases
			flipX.setBlockStateLine(19, 1, 17, 0, 1, 1, 2, plainBottomStairs(Direction.SOUTH));
			flipX.setBlockState(19, 1, 18, BlockStates.SANDSTONE);
			flipX.setBlockState(19, 1, 19, BlockStates.SANDSTONE);
			flipX.setBlockState(19, 2, 19, BlockStates.SANDSTONE);
			flipX.stack(-1, 1, 0, 3).setBlockStateLine(18, 2, 19, 0, 1, 0, plainTopStairs(Direction.EAST), plainBottomStairs(Direction.WEST));
			flipX.setBlockStateLine(15, 5, 19, 0, 0, -1, 5, BlockStates.CUT_SANDSTONE);
			flipX.setBlockStateLine(16, 5, 15, 1, 0, 0, 4, BlockStates.CUT_SANDSTONE);
			flipX.setBlockStateLine(16, 5, 18, 0, 0, -1, 2, smoothTopStairs(Direction.WEST));
			flipX.setBlockState(16, 5, 16, smoothStairs(Direction.NORTH, BlockHalf.TOP, StairShape.INNER_LEFT));
			flipX.setBlockStateLine(17, 5, 16, 1, 0, 0, 3, smoothTopStairs(Direction.NORTH));
			flipX.setBlockState(16, 6, 18, Blocks.SANDSTONE_WALL.getDefaultState().with(WallBlock.NORTH_SHAPE, WallShape.LOW));
			flipX.setBlockState(16, 6, 17, Blocks.SANDSTONE_WALL.getDefaultState().with(WallBlock.NORTH_SHAPE, WallShape.LOW).with(WallBlock.SOUTH_SHAPE, WallShape.LOW).with(WallBlock.UP, Boolean.FALSE));
			flipX.setBlockState(16, 6, 16, Blocks.SANDSTONE_WALL.getDefaultState().with(WallBlock.EAST_SHAPE, WallShape.LOW).with(WallBlock.SOUTH_SHAPE, WallShape.LOW));
			flipX.setBlockStateLine(17, 6, 16, 1, 0, 0, 3, Blocks.SANDSTONE_WALL.getDefaultState().with(WallBlock.EAST_SHAPE, WallShape.LOW).with(WallBlock.WEST_SHAPE, WallShape.LOW).with(WallBlock.UP, Boolean.FALSE));
			//walkways
			flipX.setBlockStateCuboid(2, 1, 16, 13, 5, 18, BlockStates.SANDSTONE);
			flipX.setBlockStateCuboid(16, 1, 2, 18, 5, 13, BlockStates.SANDSTONE);
			flipX.setBlockStateCuboid(8, 0, -1, 18, 5, 1, BlockStates.SANDSTONE);
			root.setBlockStateCuboid(-1, 5, 16, 1, 5, 19, BlockStates.SANDSTONE);
			//corridors
			flipX.setBlockStateCuboid(4, 1, 17, 14, 3, 17, BlockStates.AIR);
			flipX.setBlockStateCuboid(17, 1, 4, 17, 3, 14, BlockStates.AIR);
			flipX.stack(4, 0, 0, 3).setBlockStateLine(4, 1, 16, 0, 1, 0, BlockStates.AIR, BlockStates.AIR, plainTopStairs(Direction.NORTH));
			flipX.stack(0, 0, 4, 3).setBlockStateLine(16, 1, 4, 0, 1, 0, BlockStates.AIR, BlockStates.AIR, plainTopStairs(Direction.WEST));
			//arches
			//front entrance
			root.setBlockStateCuboid(-1, 1, 16, 1, 4, 20, BlockStates.AIR);
			flipX.setBlockStateLine(2, 1, 19, 0, 1, 0, 4, BlockStates.SANDSTONE);
			flipX.setBlockStateLine(2, 0, 20, 0, 1, 0, 7, BlockStates.CUT_SANDSTONE);
			root.stack(0, 2, 0, 2).setBlockStateLine(-1, 5, 20, 1, 0, 0, 3, BlockStates.CUT_SANDSTONE);
			root.setBlockStateLine(-1, 6, 20, 1, 0, 0, BlockStates.CHISELED_SANDSTONE, Blocks.BLUE_TERRACOTTA.getDefaultState(), BlockStates.CHISELED_SANDSTONE);
			flipX.stack(0, 0, -5, 2).setBlockState(1, 4, 20, smoothTopStairs(Direction.EAST));
			flipX.setBlockStateLine(1, 4, 19, 0, 0, -1, 4, plainTopStairs(Direction.EAST));
			//outside of front walkway
			flipX.stack(4, 0, 0, 2).setBlockStateLine(6, 1, 19, 0, 1, 0, 4, BlockStates.CUT_SANDSTONE);
			flipX.setBlockStateLine(2, 5, 19, 1, 0, 0, 12, BlockStates.CUT_SANDSTONE);
			flipX.stack(4, 0, 0, 3).setBlockStateCuboid(3, 1, 19, 5, 3, 19, BlockStates.AIR);
			flipX.stack(4, 0, 0, 3).setBlockStateLine(3, 4, 19, 1, 0, 0, smoothTopStairs(Direction.WEST), BlockStates.AIR, smoothTopStairs(Direction.EAST));
			root.setBlockStateLine(-13, 6, 19, 1, 0, 0, 27, Blocks.SANDSTONE_WALL.getDefaultState().with(WallBlock.EAST_SHAPE, WallShape.LOW).with(WallBlock.WEST_SHAPE, WallShape.LOW).with(WallBlock.UP, Boolean.FALSE));
			//inside of front walkway
			flipX.setBlockStateLine(2, 0, 15, 0, 1, 0, 5, BlockStates.CUT_SANDSTONE);
			flipX.stack(4, 0, 0, 2).setBlockStateLine(6, 1, 15, 0, 1, 0, 4, BlockStates.CUT_SANDSTONE);
			flipX.stack(4, 0, 0, 3).setBlockStateLine(3, 4, 15, 1, 0, 0, smoothTopStairs(Direction.WEST), null, smoothTopStairs(Direction.EAST));
			root.setBlockStateLine(-14, 5, 15, 1, 0, 0, 29, BlockStates.CUT_SANDSTONE);
			//side entrance
			flipX.setBlockStateCuboid(11, 6, -1,14, 9, 1, BlockStates.AIR);
			flipX.stack(0, 0, -1, 3).setBlockStateLine(14, 6, 1, -1, 1, 0, 4, BlockStates.AIR);
			flipXZ.setBlockStateLine(14, 6, 2, 0, 1, 0, 5, BlockStates.CUT_SANDSTONE);
			flipX.setBlockStateCuboid(14, 10, -1, 14, 11, 1, BlockStates.CUT_SANDSTONE);
			flipXZ.setBlockState(13, 8, 2, BlockStates.SANDSTONE);
			flipXZ.setBlockState(13, 9, 2, BlockStates.SANDSTONE);
			flipXZ.setBlockState(12, 9, 2, BlockStates.SANDSTONE);
			flipX.setBlockStateCuboid(11, 10, -1, 13, 10, 1, BlockStates.SANDSTONE);
			flipXZ.setBlockState(14, 9, 1, smoothTopStairs(Direction.SOUTH));
			flipXZ.setBlockStateLine(13, 9, 1, -1, 0, 0, 3, plainTopStairs(Direction.SOUTH));
			//outside of side walkway
			flipX.stack(0, 0, -4, 4).setBlockStateLine(19, 1, 10, 0, 1, 0, 4, BlockStates.CUT_SANDSTONE);
			flipX.setBlockStateLine(19, 5, 13, 0, 0, -1, 16, BlockStates.CUT_SANDSTONE);
			flipX.stack(0, 0, -4, 4).setBlockStateCuboid(19, 1, 11, 19, 3, 13, BlockStates.AIR);
			flipX.stack(0, 0, -4, 4).setBlockStateLine(19, 4, 13, 0, 0, -1, smoothTopStairs(Direction.SOUTH), null, smoothTopStairs(Direction.NORTH));
			flipX.setBlockState(18, 4, -2, smoothTopStairs(Direction.EAST));
			flipX.setBlockStateLine(18, 5, -2, -1, 0, 0, 12, BlockStates.CUT_SANDSTONE);
			flipX.setBlockStateLine(19, 6, 13, 0, 0, -1, 15, Blocks.SANDSTONE_WALL.getDefaultState().with(WallBlock.NORTH_SHAPE, WallShape.LOW).with(WallBlock.SOUTH_SHAPE, WallShape.LOW).with(WallBlock.UP, Boolean.FALSE));
			flipX.setBlockState(19, 6, -2, Blocks.SANDSTONE_WALL.getDefaultState().with(WallBlock.SOUTH_SHAPE, WallShape.LOW).with(WallBlock.WEST_SHAPE, WallShape.LOW));
			flipX.setBlockStateLine(18, 6, -2, -1, 0, 0, 4, Blocks.SANDSTONE_WALL.getDefaultState().with(WallBlock.EAST_SHAPE, WallShape.LOW).with(WallBlock.WEST_SHAPE, WallShape.LOW).with(WallBlock.UP, Boolean.FALSE));
			//inside of side hallway
			flipX.setBlockStateLine(7, 5, -1, 0, 0, 1, 3, BlockStates.CUT_SANDSTONE);
			flipX.setBlockStateLine(7, 5, 2, 1, 0, 0, 9, BlockStates.CUT_SANDSTONE);
			flipX.setBlockStateLine(15, 5, 3, 0, 0, 1, 11, BlockStates.CUT_SANDSTONE);
			flipXZ.stack(4, 0, 0, 2).setBlockStateLine(7, 0, -2, 0, 1, 0, 5, BlockStates.CUT_SANDSTONE);
			flipXZ.stack(0, 0, 4, 3).setBlockStateLine(15, 1, 2, 0, 1, 0, 4, BlockStates.CUT_SANDSTONE);
			flipXZ.stack(0, 0, -4, 3).setBlockStateLine(15, 4, 13, 0, 0, -1, smoothTopStairs(Direction.SOUTH), null, smoothTopStairs(Direction.NORTH));
			flipXZ.stack(-4, 0, 0, 2).setBlockStateLine(14, 4, 2, -1, 0, 0, smoothTopStairs(Direction.EAST), null, smoothTopStairs(Direction.WEST));
			flipXZ.setBlockState(7, 4, 1, smoothTopStairs(Direction.SOUTH));
			flipXZ.setBlockStateLine(13, 6, -2, -1, 0, 0, 6, Blocks.SANDSTONE_WALL.getDefaultState().with(WallBlock.EAST_SHAPE, WallShape.LOW).with(WallBlock.WEST_SHAPE, WallShape.LOW).with(WallBlock.UP, Boolean.FALSE));
			flipXZ.setBlockState(7, 6, -2, Blocks.SANDSTONE_WALL.getDefaultState().with(WallBlock.EAST_SHAPE, WallShape.LOW).with(WallBlock.SOUTH_SHAPE, WallShape.LOW));
			flipX.setBlockStateLine(7, 6, -1, 0, 0, 1, 3, Blocks.SANDSTONE_WALL.getDefaultState().with(WallBlock.NORTH_SHAPE, WallShape.LOW).with(WallBlock.SOUTH_SHAPE, WallShape.LOW).with(WallBlock.UP, Boolean.FALSE));
			//back interior
			flipX.multiTranslate(
				14, 1, -15,
				15, 1, -15,
				15, 1, -14
			)
			.setBlockStateLine(0, 0, 0, 0, 1, 0, 4, BlockStates.SANDSTONE);
			flipX.setBlockStateLine(14, 1, -14, 0, 1, 0, 5, BlockStates.CUT_SANDSTONE);
			root.stack(4, 0, 0, 6).setBlockStateLine(-10, 1, -15, 0, 1, 0, 4, BlockStates.CUT_SANDSTONE);
			root.stack(4, 0, 0, 7).setBlockStateLine(-13, 4, -15, 1, 0, 0, smoothTopStairs(Direction.WEST), null, smoothTopStairs(Direction.EAST));
			//pillars
			flipXZ.setBlockStateLine(7, 0, 7, 0, 1, 0, 13, BlockStates.CUT_SANDSTONE);
			flipXZ.setBlockStateLine(7, 0, 8, 0, 1, 0, 12, BlockStates.SANDSTONE);
			flipXZ.setBlockStateLine(7, 0, 9, 0, 1, 0, 11, BlockStates.CUT_SANDSTONE);
			flipXZ.setBlockStateLine(8, 0, 7, 0, 1, 0, 12, BlockStates.SANDSTONE);
			flipXZ.setBlockStateLine(8, 0, 8, 0, 1, 0, 12, BlockStates.SANDSTONE);
			flipXZ.setBlockStateLine(8, 0, 9, 0, 1, 0, 11, BlockStates.SANDSTONE);
			flipXZ.setBlockStateLine(9, 0, 7, 0, 1, 0, 11, BlockStates.CUT_SANDSTONE);
			flipXZ.setBlockStateLine(9, 0, 8, 0, 1, 0, 11, BlockStates.SANDSTONE);
			flipXZ.setBlockStateLine(9, 0, 9, 0, 1, 0, 11, BlockStates.CUT_SANDSTONE);
			//tip top
			rot4.stack(4, 0, 0, 2).setBlockStateLine(0, 17, 4, 0, 1, 0, 5, BlockStates.CUT_SANDSTONE);
			Coordinator.combine(
				root,
				root.rotate1x(BlockRotation.CLOCKWISE_90),
				root.rotate1x(BlockRotation.COUNTERCLOCKWISE_90)
			)
			.flip2X()
			.setBlockStateLine(1, 17, 4, 1, 0, 0, 3, Blocks.SANDSTONE_WALL.getDefaultState().with(WallBlock.EAST_SHAPE, WallShape.LOW).with(WallBlock.WEST_SHAPE, WallShape.LOW).with(WallBlock.UP, Boolean.FALSE));
			flipX.setBlockStateLine(1, 17, -4, 1, 0, 0, 3, BlockStates.AIR);
			rot4.flip2X().setBlockStateCuboid(1, 18, 4, 3, 20, 4, BlockStates.AIR);
			rot4.setBlockStateLine(-3, 22, 4, 1, 0, 0, 8, BlockStates.CUT_SANDSTONE);
			rot4.flip2X().setBlockStateLine(1, 21, 4, 1, 0, 0, smoothTopStairs(Direction.WEST), BlockStates.AIR, smoothTopStairs(Direction.EAST));
			rot4.setBlockStateLine(-2, 22, -3, 1, 0, 0, 5, smoothTopStairs(Direction.NORTH));
			rot4.setBlockState(-3, 22, -3, smoothStairs(Direction.NORTH, BlockHalf.TOP, StairShape.INNER_LEFT));
			root.setBlockStateCuboid(-2, 22, -2, 2, 22, 2, Blocks.SMOOTH_SANDSTONE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
			for (int y = 0; y < 4; y++) {
				rot4.setBlockStateLine(-3 + y, 23 + y, 4 - y, 1, 0, 0, 7 - (y << 1), bottomStairs(Blocks.SMOOTH_QUARTZ_STAIRS, Direction.NORTH));
				rot4.setBlockState(4 - y, 23 + y, 4 - y, stairs(Blocks.SMOOTH_QUARTZ_STAIRS, Direction.NORTH, BlockHalf.BOTTOM, StairShape.OUTER_LEFT));
				root.setBlockStateCuboid(-3 + y, 23 + y, -3 + y, 3 - y, 23 + y, 3 - y, Blocks.SMOOTH_QUARTZ.getDefaultState());
			}
			root.setBlockState(0, 27, 0, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
			//back ramp
			for (int z = -36; z <= -5; z++) {
				for (int x = -4; x <= 4; x++) {
					int height = z + 37;
					if (Math.abs(x) == 4) height++;
					int y = height >> 1;
					root.setBlockState(x, y, z, (height & 1) == 0 ? Blocks.SANDSTONE_SLAB.getDefaultState() : BlockStates.SANDSTONE);
					int minY = Math.max(z + 21, 0);
					while (--y >= minY) {
						root.setBlockState(x, y, z, BlockStates.SANDSTONE);
					}
				}
			}
		}
	}

	public static class UndergroundLayout extends LabyrinthLayout {

		public int centerX, centerY, centerZ;

		public UndergroundLayout(RandomGenerator random, MainPiece mainPiece) {
			super(random, 363); //theoretical maximum number of rooms that can fit in the underground area.
			this.centerX = mainPiece.x();
			this.centerY = mainPiece.y();
			this.centerZ = mainPiece.z();
			BlockPos center = new BlockPos(this.centerX, this.centerY, this.centerZ);
			BlockRotation rotation = mainPiece.getRotation();

			UndergroundRoomPiece topLeftRoom     = this.newRoom();
			UndergroundRoomPiece topRightRoom    = this.newRoom();
			UndergroundRoomPiece middleLeftRoom  = this.newRoom();
			UndergroundRoomPiece middleRightRoom = this.newRoom();
			UndergroundRoomPiece bottomLeftRoom  = this.newRoom();
			UndergroundRoomPiece bottomRightRoom = this.newRoom();

			topLeftRoom    .setPos(new BlockPos( 16, -2, 0).rotate(rotation).add(center));
			topRightRoom   .setPos(new BlockPos(-16, -2, 0).rotate(rotation).add(center));
			middleLeftRoom .setPos(new BlockPos( 12, -4, 0).rotate(rotation).add(center));
			middleRightRoom.setPos(new BlockPos(-12, -4, 0).rotate(rotation).add(center));
			bottomLeftRoom .setPos(new BlockPos(  8, -6, 0).rotate(rotation).add(center));
			bottomRightRoom.setPos(new BlockPos( -8, -6, 0).rotate(rotation).add(center));

			topLeftRoom    .setConnectedRoom(rotation.rotate(Direction.WEST), middleLeftRoom);
			middleLeftRoom .setConnectedRoom(rotation.rotate(Direction.EAST), topLeftRoom);
			middleLeftRoom .setConnectedRoom(rotation.rotate(Direction.WEST), bottomLeftRoom);
			bottomLeftRoom .setConnectedRoom(rotation.rotate(Direction.EAST), middleLeftRoom);

			topRightRoom   .setConnectedRoom(rotation.rotate(Direction.EAST), middleRightRoom);
			middleRightRoom.setConnectedRoom(rotation.rotate(Direction.WEST), topRightRoom);
			middleRightRoom.setConnectedRoom(rotation.rotate(Direction.EAST), bottomRightRoom);
			bottomRightRoom.setConnectedRoom(rotation.rotate(Direction.WEST), middleRightRoom);

			this.rooms.add(topLeftRoom);
			this.rooms.add(topRightRoom);
			this.rooms.add(middleLeftRoom);
			this.rooms.add(middleRightRoom);
			this.rooms.add(bottomLeftRoom);
			this.rooms.add(bottomRightRoom);
			this.activeRooms.add(bottomLeftRoom);
			this.activeRooms.add(bottomRightRoom);

			UndergroundHallPiece topLeftHall  = hall(new BlockPos( 17, -2, 1).rotate(rotation).add(center), rotation.rotate(Direction.SOUTH));
			UndergroundHallPiece topRightHall = hall(new BlockPos(-17, -2, 1).rotate(rotation).add(center), rotation.rotate(Direction.SOUTH));

			this.halls.add(topLeftHall);
			this.halls.add(topRightHall);

			//all other halls will be created automatically.
		}

		public static UndergroundHallPiece hall(BlockPos pos, Direction direction) {
			return new UndergroundHall2Piece(BigGlobeStructures.BIGGER_DESERT_PYRAMID_UNDERGROUND_HALL2_PIECE_TYPE, pos.getX(), pos.getY(), pos.getZ(), direction);
		}

		@Override
		public void generate() {
			super.generate();
			for (RoomPiece room : this.rooms) {
				((UndergroundRoomPiece)(room)).maybeAddChest(this.random);
			}
		}

		@Override
		public UndergroundRoomPiece newRoom() {
			return new UndergroundRoomPiece(BigGlobeStructures.BIGGER_DESERT_PYRAMID_UNDERGROUND_ROOM_PIECE_TYPE);
		}

		@Override
		public UndergroundHallPiece newHall(RoomPiece from, RoomPiece to, Direction direction) {
			int x = (from.x() + to.x()) >> 1;
			int y = Math.min(from.y(), to.y());
			int z = (from.z() + to.z()) >> 1;
			return switch (to.y() - from.y()) {
				case -2 -> new UndergroundHall2Piece(BigGlobeStructures.BIGGER_DESERT_PYRAMID_UNDERGROUND_HALL2_PIECE_TYPE, x, y, z, direction.getOpposite());
				case -1 -> new UndergroundHall1Piece(BigGlobeStructures.BIGGER_DESERT_PYRAMID_UNDERGROUND_HALL1_PIECE_TYPE, x, y, z, direction.getOpposite());
				case  0 -> new UndergroundHall0Piece(BigGlobeStructures.BIGGER_DESERT_PYRAMID_UNDERGROUND_HALL0_PIECE_TYPE, x, y, z, direction);
				case  1 -> new UndergroundHall1Piece(BigGlobeStructures.BIGGER_DESERT_PYRAMID_UNDERGROUND_HALL1_PIECE_TYPE, x, y, z, direction);
				case  2 -> new UndergroundHall2Piece(BigGlobeStructures.BIGGER_DESERT_PYRAMID_UNDERGROUND_HALL2_PIECE_TYPE, x, y, z, direction);
				default -> throw new IllegalArgumentException(Integer.toString(to.y() - from.y()));
			};
		}

		@Override
		public int distanceBetweenRooms() {
			return 4;
		}

		@Override
		public boolean isValidPosition(RoomPiece next) {
			return (
				//bounds check
				next.x() >= this.centerX - 20 &&
				next.x() <= this.centerX + 20 &&
				next.y() >= this.centerY - 16 &&
				next.y() <= this.centerY -  6 &&
				next.z() >= this.centerZ - 20 &&
				next.z() <= this.centerZ + 20 &&
				//don't place anything in the center,
				//cause I don't want players to dig down in the center.
				(
					next.x() != this.centerX ||
					next.z() != this.centerZ
				)
			);
		}

		@Override
		public int maxHeightDifference() {
			return 2;
		}

		@Override
		public double mergeChance() {
			return 0.25D;
		}

		public void addTo(StructurePiecesCollector collector) {
			for (LabyrinthPiece room : this.rooms) {
				collector.addPiece((StructurePiece)(room));
			}
			for (LabyrinthPiece hall : this.halls) {
				collector.addPiece((StructurePiece)(hall));
			}
		}
	}

	public static abstract class UndergroundPiece extends StructurePiece implements LabyrinthPiece {

		public UndergroundPiece(StructurePieceType type, int length, BlockBox boundingBox) {
			super(type, length, boundingBox);
		}

		public UndergroundPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		public Coordinator coordinator(StructureWorldAccess world, BlockBox limit) {
			return Coordinator.forWorld(world, Block.NOTIFY_LISTENERS).inBox(limit, false).translate(this.x(), this.y(), this.z());
		}

		@Override
		public BlockBox boundingBox() {
			return this.boundingBox;
		}
	}

	public static class UndergroundRoomPiece extends UndergroundPiece implements RoomPiece {

		public final UndergroundRoomPiece[] connections = new UndergroundRoomPiece[4];
		public long chestSeed;

		public UndergroundRoomPiece(StructurePieceType type) {
			super(type, 0, null);
			this.setPos(0, 0, 0);
		}

		public UndergroundRoomPiece(StructurePieceType type, StructureContext context, NbtCompound nbt) {
			super(type, nbt);
			this.chestSeed = nbt.getLong("chestSeed");
		}

		@Override
		@MustBeInvokedByOverriders
		public void writeNbt(StructureContext context, NbtCompound nbt) {
			if (this.hasChest()) nbt.putLong("chestSeed", this.chestSeed);
		}

		@Override
		public void generate(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			Coordinator root = this.coordinator(world, chunkBox);
			root.setBlockStateLine(0, 1, 0, 0, 1, 0, 3, BlockStates.AIR);
			Direction chestDirection = this.getFacing();
			if (chestDirection != null) {
				long chestSeed = this.chestSeed;
				root.setBlockStateAndBlockEntity(
					chestDirection.getOffsetX(),
					1,
					chestDirection.getOffsetZ(),
					Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, chestDirection.getOpposite()),
					ChestBlockEntity.class,
					(BlockPos.Mutable pos, ChestBlockEntity chest) -> {
						chest.setLootTable(LootTables.DESERT_PYRAMID_CHEST, chestSeed);
					}
				);
				root.setBlockState(
					chestDirection.getOffsetX(),
					2,
					chestDirection.getOffsetZ(),
					plainTopStairs(chestDirection)
				);
			}
		}

		public void maybeAddChest(RandomGenerator random) {
			int bits = random.nextInt();
			if ((bits & 0x111100) == 0) {
				Direction direction = Directions.HORIZONTAL[bits & 3];
				if (this.getConnectedRoom(direction) == null) {
					this.setChest(direction, random.nextLong());
				}
			}
		}

		@Override
		public void setPos(int x, int y, int z) {
			this.boundingBox = new BlockBox(x - 1, y, z - 1, x + 1, y + 4, z + 1);
		}

		public boolean hasChest() {
			return this.getFacing() != null;
		}

		public void setChest(Direction direction, long chestSeed) {
			this.setOrientation(direction);
			this.chestSeed = chestSeed;
		}

		@Override
		public RoomPiece getConnectedRoom(Direction direction) {
			return this.connections[direction.getHorizontal()];
		}

		@Override
		public void setConnectedRoom(Direction direction, RoomPiece connection) {
			this.connections[direction.getHorizontal()] = (UndergroundRoomPiece)(connection);
		}
	}

	public static abstract class UndergroundHallPiece extends UndergroundPiece implements HallPiece {

		public UndergroundHallPiece(StructurePieceType type, int length, BlockBox boundingBox) {
			super(type, length, boundingBox);
		}

		public UndergroundHallPiece(StructurePieceType type, StructureContext context, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		@MustBeInvokedByOverriders
		public void writeNbt(StructureContext context, NbtCompound nbt) {}

		@Override
		public Coordinator coordinator(StructureWorldAccess world, BlockBox limit) {
			return super.coordinator(world, limit).rotate1x(Directions.rotationOf(Directions.POSITIVE_X, this.getFacing()));
		}
	}

	public static class UndergroundHall0Piece extends UndergroundHallPiece {

		public UndergroundHall0Piece(StructurePieceType type, int x, int y, int z, Direction direction) {
			super(type, 0, new BlockBox(x - 1, y, z - 1, x + 1, y + 4, z + 1));
			this.setOrientation(direction);
		}

		public UndergroundHall0Piece(StructurePieceType type, StructureContext context, NbtCompound nbt) {
			super(type, context, nbt);
		}

		@Override
		public void generate(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			this.coordinator(world, chunkBox).setBlockStateCuboid(-1, 1, 0, 1, 3, 0, BlockStates.AIR);
		}
	}

	public static class UndergroundHall1Piece extends UndergroundHallPiece {

		public UndergroundHall1Piece(StructurePieceType type, int x, int y, int z, Direction direction) {
			super(type, 0, new BlockBox(x - 1, y, z - 1, x + 1, y + 5, z + 1));
			this.setOrientation(direction);
		}

		public UndergroundHall1Piece(StructurePieceType type, StructureContext context, NbtCompound nbt) {
			super(type, context, nbt);
		}

		@Override
		public void generate(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			Coordinator root = this.coordinator(world, chunkBox);
			root.stack(2, 1, 0, 2).setBlockStateLine(-1, 1, 0, 0, 1, 0, 3, BlockStates.AIR);
			root.setBlockStateLine(0, 1, 0, 0, 1, 0, Blocks.SANDSTONE_SLAB.getDefaultState(), BlockStates.AIR, BlockStates.AIR, Blocks.SANDSTONE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
		}
	}

	public static class UndergroundHall2Piece extends UndergroundHallPiece {

		public UndergroundHall2Piece(StructurePieceType type, int x, int y, int z, Direction direction) {
			super(type, 0, new BlockBox(x - 1, y, z - 1, x + 1, y + 6, z + 1));
			this.setOrientation(direction);
		}

		public UndergroundHall2Piece(StructurePieceType type, StructureContext context, NbtCompound nbt) {
			super(type, context, nbt);
		}

		@Override
		public void generate(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			Coordinator root = this.coordinator(world, chunkBox);
			root.stack(2, 1, 0, 2).setBlockStateLine(-1, 1, 0, 0, 1, 0, Blocks.SANDSTONE_SLAB.getDefaultState(), BlockStates.AIR, BlockStates.AIR, Blocks.SANDSTONE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
			root.setBlockStateLine(0, 2, 0, 0, 1, 0, 3, BlockStates.AIR);
		}
	}

	public static BlockState smoothBottomStairs(Direction direction) {
		return stairs(Blocks.SMOOTH_SANDSTONE_STAIRS, direction, BlockHalf.BOTTOM);
	}

	public static BlockState smoothTopStairs(Direction direction) {
		return stairs(Blocks.SMOOTH_SANDSTONE_STAIRS, direction, BlockHalf.TOP);
	}

	public static BlockState smoothStairs(Direction direction, BlockHalf half, StairShape shape) {
		return stairs(Blocks.SMOOTH_SANDSTONE_STAIRS, direction, half, shape);
	}

	public static BlockState plainBottomStairs(Direction direction) {
		return stairs(Blocks.SANDSTONE_STAIRS, direction, BlockHalf.BOTTOM);
	}

	public static BlockState plainTopStairs(Direction direction) {
		return stairs(Blocks.SANDSTONE_STAIRS, direction, BlockHalf.TOP);
	}

	public static BlockState plainStairs(Direction direction, BlockHalf half, StairShape shape) {
		return stairs(Blocks.SANDSTONE_STAIRS, direction, half, shape);
	}

	public static BlockState bottomStairs(Block stairs, Direction direction) {
		return stairs(stairs, direction, BlockHalf.BOTTOM, StairShape.STRAIGHT);
	}

	public static BlockState topStairs(Block stairs, Direction direction) {
		return stairs(stairs, direction, BlockHalf.TOP, StairShape.STRAIGHT);
	}

	public static BlockState stairs(Block stairs, Direction direction, BlockHalf half) {
		return stairs(stairs, direction, half, StairShape.STRAIGHT);
	}

	public static BlockState stairs(Block stairs, Direction facing, BlockHalf half, StairShape shape) {
		return (
			stairs
			.getDefaultState()
			.with(StairsBlock.FACING, facing)
			.with(StairsBlock.HALF, half)
			.with(StairsBlock.SHAPE, shape)
		);
	}
}