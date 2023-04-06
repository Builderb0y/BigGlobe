package builderb0y.bigglobe.structures.dungeons;

import java.util.List;
import java.util.random.RandomGenerator;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.block.enums.StairShape;
import net.minecraft.block.enums.WallShape;
import net.minecraft.entity.EntityType;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePieceType;
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
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.mixins.MobSpawnerLogic_GettersAndSettersForEverything;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.structures.BigGlobeStructures;
import builderb0y.bigglobe.structures.LabyrinthLayout;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.util.coordinators.Coordinator;

public class LargeDungeonStructure extends AbstractDungeonStructure {

	public static final Codec<LargeDungeonStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(LargeDungeonStructure.class);

	public LargeDungeonStructure(Config config, RandomList<EntityType<?>> spawner_entries, List<Palette> palettes) {
		super(config, spawner_entries, palettes);
	}

	@Override
	public DungeonLayout layout(WorldColumn column, int y, RandomGenerator random) {
		return new Layout(column, y, random, this.spawner_entries, this.palettes);
	}

	@Override
	public StructureType<?> getType() {
		return BigGlobeStructures.LARGE_DUNGEON_TYPE;
	}

	public static class Layout extends DungeonLayout {

		public Layout(WorldColumn column, int y, RandomGenerator random, IRandomList<EntityType<?>> spawnerEntries, List<Palette> palettes) {
			super(column, y, random, (random.nextInt() & 127) + 64, spawnerEntries, palettes);
		}

		@Override
		public int distanceBetweenRooms() {
			return 12;
		}

		@Override
		public boolean isValidPosition(RoomPiece next) {
			return (
				BigGlobeMath.squareI(
					next.x() - this.centerX,
					next.z() - this.centerZ
				)
				< (this.random.nextInt(3072))
			);
		}

		@Override
		public int maxHeightDifference() {
			return 3;
		}

		@Override
		public RoomDungeonPiece newRoom() {
			return new Room(BigGlobeStructures.LARGE_DUNGEON_ROOM_TYPE, this.palette, this.random);
		}

		@Override
		public HallDungeonPiece newHall(RoomPiece room1, RoomPiece room2, Direction direction) {
			return Hall.create((Room)(room1), (Room)(room2), direction, this.random);
		}
	}

	public static class Room extends RoomDungeonPiece {

		public Room(StructurePieceType type, Palette palette, RandomGenerator random) {
			super(type, 0, null, palette);
			this.setPit((random.nextInt() & 7) == 0);
			this.setPos(0, 0, 0);
		}

		public Room(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public void setPos(int x, int y, int z) {
			this.boundingBox = new BlockBox(x - 4, this.hasPit() ? y - 2 : y, z - 4, x + 4, y + 6, z + 4);
		}

		@Override
		public void addDecorations(LabyrinthLayout layout) {
			Direction orientation;
			if (this.hasPit()) {
				layout.decorations.add(new PitDungeonPiece(BigGlobeStructures.DUNGEON_PIT_TYPE, this.x(), this.y(), this.z(), this.palette, layout.random.nextInt(4), layout.random));
			}
			else if ((orientation = this.getHallwayDirection()) != null) {
				layout.decorations.add(new TrapPiece(BigGlobeStructures.LARGE_DUNGEON_TRAP_TYPE, this.x(), this.y() + 1, this.z(), this.palette, orientation));
			}
			else if ((orientation = this.getDeadEndDirection()) != null) {
				if (layout.random.nextBoolean()) {
					layout.decorations.add(new ChestPiece(BigGlobeStructures.LARGE_DUNGEON_CHEST_TYPE, this.x(), this.y() + 1, this.z(), this.palette, orientation, layout.random.nextLong()));
				}
			}
			else {
				if ((layout.random.nextInt() & 7) == 0) {
					layout.decorations.add(new SpawnerPiece(BigGlobeStructures.LARGE_DUNGEON_SPAWNER_TYPE, this.x(), this.y() + 1, this.z(), this.palette, ((Layout)(layout)).spawnerEntries.getRandomElement(layout.random), layout.random));
				}
			}
		}

		public @Nullable Direction getHallwayDirection() {
			RoomDungeonPiece
				north = this.getConnectedRoom(Direction.NORTH),
				south = this.getConnectedRoom(Direction.SOUTH),
				east  = this.getConnectedRoom(Direction.EAST),
				west  = this.getConnectedRoom(Direction.WEST);
			if (north != null && south != null && east == null && west == null) {
				return Direction.EAST;
			}
			else if (north == null && south == null && east != null && west != null) {
				return Direction.NORTH;
			}
			else {
				return null;
			}
		}
	}

	public static class ChestPiece extends ChestDungeonPiece {

		public ChestPiece(StructurePieceType type, int x, int y, int z, Palette palette, Direction facing, long seed) {
			super(type, 0, new BlockBox(x - 2, y, z - 2, x + 2, y + 1, z + 2), palette, facing, seed);
		}

		public ChestPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
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
			Palette palette = this.palette();
			Coordinator root = this.coordinator(world, chunkBox);
			root.setBlockState(0, 0, 0, palette.mainSupplier());
			root.setBlockStateAndBlockEntity(0, 1, 0, Blocks.CHEST.getDefaultState(), ChestBlockEntity.class, this::initChest);
			root.flip2X().setBlockStateLine(1, 0, 0, 0, 1, 0, palette.mainSupplier(), palette.slabSupplier(SlabType.BOTTOM));
			root.flip2Z().setBlockState(0, 0, 1, palette.stairsSupplier(BlockHalf.BOTTOM, Direction.NORTH, StairShape.STRAIGHT));
			root.flip4XZ().setBlockState(1, 0, 1, palette.stairsSupplier(BlockHalf.BOTTOM, Direction.WEST, StairShape.OUTER_RIGHT));
			root.flip2X().setBlockState(2, 0, 0, palette.stairsSupplier(BlockHalf.BOTTOM, Direction.WEST, StairShape.STRAIGHT));
		}
	}

	public static class SpawnerPiece extends SpawnerDungeonPiece {

		public static final int BARS_BIT = 1 << 1;

		public SpawnerPiece(StructurePieceType type, int x, int y, int z, Palette palette, EntityType<?> spawnerType, RandomGenerator random) {
			super(type, 0, new BlockBox(x - 1, y, z - 1, x + 1, y + 3, z + 1), palette, spawnerType);
			this.setBars(random.nextBoolean());
		}

		public SpawnerPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		public boolean hasBars() {
			return (this.variant & BARS_BIT) != 0;
		}

		public void setBars(boolean bars) {
			this.variant = (byte)(bars ? (this.variant | BARS_BIT) : (this.variant & ~BARS_BIT));
		}

		@Override
		@SuppressWarnings("unchecked") //generic varargs.
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
			Palette palette = this.palette();
			root.setBlockState(0, 0, 0, palette.mainSupplier());
			root.setBlockStateAndBlockEntity(0, 1, 0, Blocks.SPAWNER.getDefaultState(), MobSpawnerBlockEntity.class, this::initSpawner);
			root.setBlockState(0, 2, 0, palette.mainSupplier());
			root.setBlockState(0, 3, 0, palette.slabSupplier(SlabType.BOTTOM));
			if (this.hasBars()) {
				root.rotate4x90().setBlockStateLine(0, 0, 1, 0, 1, 0,
					palette.stairsSupplier(BlockHalf.BOTTOM, Direction.NORTH, StairShape.STRAIGHT),
					palette.barsSupplier(false, true, false, true),
					palette.stairsSupplier(BlockHalf.TOP, Direction.NORTH, StairShape.STRAIGHT)
				);
				root.rotate4x90().setBlockStateLine(1, 0, 1, 0, 1, 0,
					palette.mainSupplier(),
					palette.wallSupplier(WallShape.TALL, WallShape.NONE, WallShape.NONE, WallShape.TALL, true),
					palette.stairsSupplier(BlockHalf.BOTTOM, Direction.NORTH, StairShape.OUTER_LEFT)
				);
			}
			else {
				root.rotate4x90().setBlockStateLine(0, 0, 1, 0, 1, 0,
					palette.stairsSupplier(BlockHalf.BOTTOM, Direction.NORTH, StairShape.STRAIGHT),
					null,
					palette.stairsSupplier(BlockHalf.TOP, Direction.NORTH, StairShape.STRAIGHT)
				);
				root.rotate4x90().setBlockStateLine(1, 0, 1, 0, 1, 0,
					palette.mainSupplier(),
					palette.wallSupplier(WallShape.NONE, WallShape.NONE, WallShape.NONE, WallShape.NONE, true),
					palette.stairsSupplier(BlockHalf.BOTTOM, Direction.NORTH, StairShape.OUTER_LEFT)
				);
			}
		}

		@Override
		public void initSpawner(BlockPos pos, MobSpawnerBlockEntity spawner) {
			super.initSpawner(pos, spawner);
			MobSpawnerLogic_GettersAndSettersForEverything logic = (MobSpawnerLogic_GettersAndSettersForEverything)(spawner.getLogic());
			logic.bigglobe_setRequiredPlayerRange(32);
			logic.bigglobe_setMaxNearbyEntities(2);
			logic.bigglobe_setSpawnCount(2);
		}
	}

	public static class TrapPiece extends DecorationDungeonPiece {

		public TrapPiece(StructurePieceType type, int x, int y, int z, Palette palette, Direction orientation) {
			super(type, 0, new BlockBox(x - 4, y, z - 4, x + 4, y + 1, z + 4), palette);
			this.setOrientation(orientation);
		}

		public TrapPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public Coordinator coordinator(StructureWorldAccess world, BlockBox limit) {
			return super.coordinator(world, limit).rotate1x(Directions.rotationOf(Direction.NORTH, this.getFacing()));
		}

		public static final BlockState
			HOOK_STATE     = BlockStates.of("minecraft:tripwire_hook[attached=true,facing=north,powered=false]"),
			TRIPWIRE_STATE = BlockStates.of("minecraft:tripwire[attached=true,disarmed=false,east=false,north=true,powered=false,south=true,west=false]");

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
			long seed = Permuter.permute(0x5FD6491E727A8E44L, this.x(), this.y(), this.z());
			Coordinator root = this.coordinator(world, chunkBox);
			for (int row = -3; row <= 3; row += 2) {
				Coordinator translated = root.translate(row, 0, 0);
				translated.flip2Z().setBlockState(0, 0, 3, HOOK_STATE);
				translated.setBlockStateLine(0, 0, 2, 0, 0, -1, 5, TRIPWIRE_STATE);
				(Permuter.toBoolean(Permuter.permute(seed, row)) ? translated.flip1Z() : translated)
				.setBlockStateAndBlockEntity(0, 1, 4, Blocks.DISPENSER.getDefaultState(), DispenserBlockEntity.class, this::initDispenser);
			}
		}

		public void initDispenser(BlockPos.Mutable pos, DispenserBlockEntity dispenser) {
			dispenser.setLootTable(LootTables.JUNGLE_TEMPLE_DISPENSER_CHEST, Permuter.permute(0x80E3F919EFE5D3AAL, pos));
		}
	}

	public static abstract class Hall extends HallDungeonPiece {

		public Hall(StructurePieceType type, int x, int y, int z, Palette palette, Direction direction, RandomGenerator random) {
			super(type, 0, new BlockBox(x - 4, y, z - 4, x + 4, y + 7, z + 4), palette);
			this.setOrientation(direction);
			this.setBars((random.nextInt() & 3) == 0);
			int width = random.nextInt(7) + 1;
			int position = random.nextInt(8 - width) - 3;
			this.setLeft(position);
			this.setRight(position + width - 1);
		}

		public Hall(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		public static Hall create(Room from, Room to, Direction direction, RandomGenerator random) {
			return create(
				(from.x() + to.x()) >> 1,
				Math.min(from.y(), to.y()),
				(from.z() + to.z()) >> 1,
				from.palette,
				direction,
				to.y() - from.y(),
				random
			);
		}

		public static Hall create(int x, int y, int z, Palette palette, Direction direction, int step, RandomGenerator random) {
			return switch (step) {
				case -3 -> new Hall3(BigGlobeStructures.LARGE_DUNGEON_HALL3_TYPE, x, y, z, palette, direction.getOpposite(), random);
				case -2 -> new Hall2(BigGlobeStructures.LARGE_DUNGEON_HALL2_TYPE, x, y, z, palette, direction.getOpposite(), random);
				case -1 -> new Hall1(BigGlobeStructures.LARGE_DUNGEON_HALL1_TYPE, x, y, z, palette, direction.getOpposite(), random);
				case  0 -> new Hall0(BigGlobeStructures.LARGE_DUNGEON_HALL0_TYPE, x, y, z, palette, direction, random);
				case  1 -> new Hall1(BigGlobeStructures.LARGE_DUNGEON_HALL1_TYPE, x, y, z, palette, direction, random);
				case  2 -> new Hall2(BigGlobeStructures.LARGE_DUNGEON_HALL2_TYPE, x, y, z, palette, direction, random);
				case  3 -> new Hall3(BigGlobeStructures.LARGE_DUNGEON_HALL3_TYPE, x, y, z, palette, direction, random);
				default -> throw new IllegalArgumentException(Integer.toString(step));
			};
		}

		public Coordinator wall(Coordinator base) {
			return base.multiTranslate(
				0, 0, this.getLeft() - 1,
				0, 0, this.getRight() + 1
			);
		}

		public Coordinator center(Coordinator base) {
			return base.stack(0, 0, 1, this.getRight() - this.getLeft() + 1).translate(0, 0, this.getLeft());
		}
	}

	public static class Hall0 extends Hall {

		public Hall0(StructurePieceType type, int x, int y, int z, Palette palette, Direction direction, RandomGenerator random) {
			super(type, x, y, z, palette, direction, random);
		}

		public Hall0(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
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
			Coordinator coordinator = this.coordinator(world, chunkBox);
			Palette palette = this.palette();
			this.wall(coordinator).setBlockStateCuboid(-1, 0, 0, 1, 6, 0, palette.mainSupplier());
			this.center(coordinator).stack(0, 6, 0, 2).setBlockStateLine(-1, 0, 0, 1, 0, 0, 3, palette.mainSupplier());
			this.center(coordinator).setBlockStateCuboid(-2, 1, 0, 2, 5, 0, BlockStates.AIR);
			if (this.hasBars()) this.center(coordinator).setBlockStateLine(0, 1, 0, 0, 1, 0, 5, palette.barsSupplier(true, false, true, false));
		}
	}

	public static class Hall1 extends Hall {

		public Hall1(StructurePieceType type, int x, int y, int z, Palette palette, Direction direction, RandomGenerator random) {
			super(type, x, y, z, palette, direction, random);
		}

		public Hall1(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
			Coordinator coordinator = this.coordinator(world, chunkBox);
			Palette palette = this.palette();
			this.wall(coordinator).stack(2, 7, 0, 2).setBlockState(-1, 0, 0, palette.mainSupplier());
			this.wall(coordinator).setBlockStateCuboid(-1, 1, 0, 1, 6, 0, palette.mainSupplier());
			this.center(coordinator).setBlockState(0, 1, 0, palette.slabSupplier(SlabType.BOTTOM));
			this.center(coordinator).setBlockState(0, 6, 0, palette.slabSupplier(SlabType.TOP));
			this.center(coordinator).stack(2, 1, 0, 2).stack(0, 6, 0, 2).setBlockState(-1, 0, 0, palette.mainSupplier());
			this.center(coordinator).stack(1, 0, 0, 2).stack(3, 1, 0, 2).setBlockStateLine(-2, 1, 0, 0, 1, 0, 5, BlockStates.AIR);
			this.center(coordinator).setBlockStateLine(0, 2, 0, 0, 1, 0, 4, BlockStates.AIR);
		}
	}

	public static class Hall2 extends Hall {

		public Hall2(StructurePieceType type, int x, int y, int z, Palette palette, Direction direction, RandomGenerator random) {
			super(type, x, y, z, palette, direction, random);
		}

		public Hall2(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
			Coordinator coordinator = this.coordinator(world, chunkBox);
			Palette palette = this.palette();
			this.wall(coordinator).stack(1, 6, 0, 2).setBlockStateLine(-1, 1, 0, 1, 0, 0, 2, palette.mainSupplier());
			this.wall(coordinator).setBlockStateCuboid(-1, 2, 0, 1, 6, 0, palette.mainSupplier());
			this.center(coordinator).setBlockStateLine(-1, 1, 0, 2, 1, 0, 2, palette.slabSupplier(SlabType.BOTTOM));
			this.center(coordinator).setBlockStateLine(-1, 6, 0, 2, 1, 0, 2, palette.slabSupplier(SlabType.TOP));
			this.center(coordinator).setBlockStateLine(0, 1, 0, 0, 6, 0, 2, palette.mainSupplier());
			this.center(coordinator).stack(2, 1, 0, 3).setBlockStateLine(-2, 1, 0, 0, 1, 0, 5, BlockStates.AIR);
			this.center(coordinator).stack(2, 1, 0, 2).setBlockStateLine(-1, 2, 0, 0, 1, 0, 4, BlockStates.AIR);
			if (this.hasBars()) this.center(coordinator).setBlockStateLine(0, 2, 0, 0, 1, 0, 5, palette.barsSupplier(true, false, true, false));
		}
	}

	public static class Hall3 extends Hall {

		public Hall3(StructurePieceType type, int x, int y, int z, Palette palette, Direction direction, RandomGenerator random) {
			super(type, x, y, z, palette, direction, random);
		}

		public Hall3(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
			Coordinator coordinator = this.coordinator(world, chunkBox);
			Palette palette = this.palette();
			this.wall(coordinator).setBlockStateLine(-1, 1, 0, 2, 7, 0, 2, palette.mainSupplier());
			this.wall(coordinator).setBlockStateCuboid(-1, 2, 0, 1, 7, 0, palette.mainSupplier());
			this.center(coordinator).setBlockStateLine(-2, 1, 0, 2, 1, 0, 3, palette.slabSupplier(SlabType.BOTTOM));
			this.center(coordinator).setBlockStateLine(-2, 6, 0, 2, 1, 0, 3, palette.slabSupplier(SlabType.TOP));
			this.center(coordinator).stack(2, 1, 0, 2).stack(0, 6, 0, 2).setBlockState(-1, 1, 0, palette.mainSupplier());
			this.center(coordinator).stack(2, 1, 0, 3).setBlockStateLine(-2, 2, 0, 0, 1, 0, 4, BlockStates.AIR);
			this.center(coordinator).stack(2, 1, 0, 2).setBlockStateLine(-1, 2, 0, 0, 1, 0, 5, BlockStates.AIR);
		}
	}
}