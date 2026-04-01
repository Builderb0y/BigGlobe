package builderb0y.bigglobe.structures.dungeons;

import java.util.List;
import java.util.random.RandomGenerator;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.Fluids;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToIntScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.mixins.MobSpawnerLogic_GettersAndSettersForEverything;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.structures.BigGlobeStructures;
import builderb0y.bigglobe.structures.LabyrinthLayout;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinateSupplier;
import builderb0y.bigglobe.util.coordinators.Coordinator;
import builderb0y.bigglobe.versions.BlockStateVersions;

public class SmallDungeonStructure extends AbstractDungeonStructure {

	public static final MapCodec<SmallDungeonStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(SmallDungeonStructure.class);

	public SmallDungeonStructure(
		StructureSettings config,
		ColumnToIntScript.@VerifyNullable Catcher min_y,
		ColumnToIntScript.@VerifyNullable Catcher surface_y,
		DelayedEntryList<ConfiguredFeature<?, ?>> room_decorators,
		IRandomList<Holder<EntityType<?>>> spawner_entries,
		List<Palette> palettes
	) {
		super(config, min_y, surface_y, room_decorators, spawner_entries, palettes);
	}

	@Override
	public DungeonLayout layout(ScriptedColumn column, int y, RandomGenerator random) {
		return new Layout(column, y, random, this.room_decorators, this.spawner_entries, getActualEntry(this));
	}

	@Override
	public StructureType<?> type() {
		return BigGlobeStructures.SMALL_DUNGEON_TYPE;
	}

	public static class Layout extends DungeonLayout {

		public Layout(
			ScriptedColumn column,
			int y,
			RandomGenerator random,
			@Nullable DelayedEntryList<ConfiguredFeature<?, ?>> roomDecorators,
			IRandomList<Holder<EntityType<?>>> spawnerEntries,
			Holder<Structure> owningStructure
		) {
			super(column, y, random, random.nextInt(384) + 192, roomDecorators, spawnerEntries, owningStructure);
		}

		@Override
		public RoomDungeonPiece newRoom() {
			return new Room(BigGlobeStructures.SMALL_DUNGEON_ROOM_TYPE, this.owningStructure, this.paletteIndex, this.random, this.roomDecorators);
		}

		@Override
		public HallDungeonPiece newHall(RoomPiece room1, RoomPiece room2, Direction direction) {
			return Hall.create((Room)(room1), (Room)(room2), direction, this.random);
		}

		@Override
		public int distanceBetweenRooms() {
			return 4;
		}

		@Override
		public boolean isValidPosition(RoomPiece next) {
			return (
				BigGlobeMath.squareI(
					next.x() - this.centerX,
					next.z() - this.centerZ
				)
				< (this.random.nextInt() & 1023)
			);
		}

		@Override
		public int maxHeightDifference() {
			return 1;
		}
	}

	public static class Room extends RoomDungeonPiece {

		public Room(
			StructurePieceType type,
			Holder<Structure> owningStructure,
			int paletteIndex,
			RandomGenerator random,
			@Nullable DelayedEntryList<ConfiguredFeature<?, ?>> decorators
		) {
			super(type, 0, null, owningStructure, paletteIndex, decorators);
			this.setPit((random.nextInt() & 15) == 0);
			this.setPos(0, 0, 0);
		}

		public Room(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
			super(type, context, nbt);
		}

		@Override
		public void postProcess(
			WorldGenLevel world,
			StructureManager structureAccessor,
			ChunkGenerator chunkGenerator,
			RandomSource random,
			BoundingBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			super.postProcess(
				world,
				structureAccessor,
				chunkGenerator,
				random,
				chunkBox,
				chunkPos,
				pivot
			);
			if (!this.hasPit() && this.support) {
				BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
				int x = this.x(), y = this.y() - 1, z = this.z();
				this.generateDown(world, chunkBox, pos.set(x, y, z), this.palette().mainSupplier(), false);
				this.generateDown(world, chunkBox, pos.set(x - 1, y, z), this.palette().mainSupplier(), false);
				this.generateDown(world, chunkBox, pos.set(x + 1, y, z), this.palette().mainSupplier(), false);
				this.generateDown(world, chunkBox, pos.set(x, y, z - 1), this.palette().mainSupplier(), false);
				this.generateDown(world, chunkBox, pos.set(x, y, z + 1), this.palette().mainSupplier(), false);
				this.generateDown(world, chunkBox, pos.set(x - 1, y, z - 1), this.palette().wallSupplier(WallSide.NONE, WallSide.TALL, WallSide.TALL, WallSide.NONE, true), true);
				this.generateDown(world, chunkBox, pos.set(x - 1, y, z + 1), this.palette().wallSupplier(WallSide.TALL, WallSide.TALL, WallSide.NONE, WallSide.NONE, true), true);
				this.generateDown(world, chunkBox, pos.set(x + 1, y, z - 1), this.palette().wallSupplier(WallSide.NONE, WallSide.NONE, WallSide.TALL, WallSide.TALL, true), true);
				this.generateDown(world, chunkBox, pos.set(x + 1, y, z + 1), this.palette().wallSupplier(WallSide.TALL, WallSide.NONE, WallSide.NONE, WallSide.TALL, true), true);
			}
		}

		public void generateDown(
			WorldGenLevel world,
			BoundingBox chunkBox,
			BlockPos.MutableBlockPos pos,
			CoordinateSupplier<BlockState> stateSupplier,
			boolean wall
		) {
			if (chunkBox.isInside(pos) && BlockStateVersions.isReplaceable(world.getBlockState(pos))) {
				world.setBlock(pos, this.palette().mainSupplier().get(pos), Block.UPDATE_ALL);
				while (true) {
					if (world.isOutsideBuildHeight(pos.setY(pos.getY() - 1))) break;
					BlockState toReplace = world.getBlockState(pos);
					if (!BlockStateVersions.isReplaceable(toReplace)) break;
					BlockState toPlace = stateSupplier.get(pos);
					if (wall) toPlace = toPlace.setValue(BlockStateProperties.WATERLOGGED, toReplace.getFluidState().isSourceOfType(Fluids.WATER));
					world.setBlock(pos, toPlace, Block.UPDATE_ALL);
					if (wall) world.getChunk(pos).markPosForPostprocessing(pos);
				}
				pos.setY(pos.getY() + 1);
				world.setBlock(pos, this.palette().mainSupplier().get(pos), Block.UPDATE_ALL);
			}
		}

		@Override
		public void addDecorations(LabyrinthLayout layout) {
			super.addDecorations(layout);
			Direction deadEndDirection;
			if (this.hasPit()) {
				layout.decorations.add(new PitDungeonPiece(BigGlobeStructures.DUNGEON_PIT_TYPE, this.x(), this.y(), this.z(), this.owningStructure, this.paletteIndex, layout.random.nextInt(2), layout.random));
				this.decorators = null;
			}
			else if ((deadEndDirection = this.getDeadEndDirection()) != null) {
				if (layout.random.nextBoolean()) {
					layout.decorations.add(new ChestPiece(BigGlobeStructures.SMALL_DUNGEON_CHEST_TYPE, this.x(), this.y() + 1, this.z(), this.owningStructure, this.paletteIndex, deadEndDirection, layout.random.nextLong()));
					this.decorators = null;
				}
			}
			else {
				if ((layout.random.nextInt() & 15) == 0) {
					layout.decorations.add(new SpawnerPiece(BigGlobeStructures.SMALL_DUNGEON_SPAWNER_TYPE, this.x(), this.y() + 1, this.z(), this.owningStructure, this.paletteIndex, ((Layout)(layout)).spawnerEntries.getRandomElement(layout.random)));
					this.decorators = null;
				}
			}
		}

		@Override
		public void setPos(int x, int y, int z) {
			this.boundingBox = new BoundingBox(x - 2, this.hasPit() ? y - 2 : y, z - 2, x + 2, y + 4, z + 2);
		}
	}

	public static class ChestPiece extends ChestDungeonPiece {

		public ChestPiece(
			StructurePieceType type,
			int x,
			int y,
			int z,
			Holder<Structure> owningStructure,
			int paletteIndex,
			Direction direction,
			long seed
		) {
			super(type, 0, new BoundingBox(x, y, z, x, y, z), owningStructure, paletteIndex, direction, seed);
			this.setOrientation(direction);
		}

		public ChestPiece(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
			super(type, context, nbt);
		}

		@Override
		public void postProcess(
			WorldGenLevel world,
			StructureManager structureAccessor,
			ChunkGenerator chunkGenerator,
			RandomSource random,
			BoundingBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			Coordinator root = this.coordinator(world, chunkBox);
			root.setBlockStateAndBlockEntity(0, 0, 0, Blocks.CHEST.defaultBlockState(), ChestBlockEntity.class, this::initChest);
		}
	}

	public static class SpawnerPiece extends SpawnerDungeonPiece {

		public SpawnerPiece(
			StructurePieceType type,
			int x,
			int y,
			int z,
			Holder<Structure> owningStructure,
			int paletteIndex,
			Holder<EntityType<?>> spawnerType
		) {
			super(type, 0, new BoundingBox(x, y, z, x, y, z), owningStructure, paletteIndex, spawnerType);
		}

		public SpawnerPiece(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
			super(type, context, nbt);
		}

		@Override
		public void postProcess(
			WorldGenLevel world,
			StructureManager structureAccessor,
			ChunkGenerator chunkGenerator,
			RandomSource random,
			BoundingBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			this.coordinator(world, chunkBox)
				.setBlockStateAndBlockEntity(
					0, 0, 0,
					Blocks.SPAWNER.defaultBlockState(),
					SpawnerBlockEntity.class,
					this::initSpawner
				);
		}

		@Override
		public void initSpawner(BlockPos pos, SpawnerBlockEntity spawner) {
			super.initSpawner(pos, spawner);
			MobSpawnerLogic_GettersAndSettersForEverything logic = (MobSpawnerLogic_GettersAndSettersForEverything)(spawner.getSpawner());
			logic.bigglobe_setRequiredPlayerRange(16);
			logic.bigglobe_setMaxNearbyEntities(2);
			logic.bigglobe_setSpawnCount(2);
		}
	}

	public static abstract class Hall extends HallDungeonPiece {

		public Hall(
			StructurePieceType type,
			int x,
			int y,
			int z,
			Holder<Structure> owningStructure,
			int paletteIndex,
			Direction direction,
			RandomGenerator random
		) {
			super(type, 0, new BoundingBox(x - 2, y, z - 2, x + 2, y + 4, z + 2), owningStructure, paletteIndex);
			this.setOrientation(direction);
			this.setBars((random.nextInt() & 7) == 0);
			int width = random.nextInt(3) + 1;
			int position = random.nextInt(4 - width) - 1;
			this.setLeft(position);
			this.setRight(position + width - 1);
		}

		public Hall(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
			super(type, context, nbt);
		}

		public static Hall create(Room from, Room to, Direction direction, RandomGenerator random) {
			return create(
				(from.x() + to.x()) >> 1,
				Math.min(from.y(), to.y()),
				(from.z() + to.z()) >> 1,
				from.owningStructure,
				from.paletteIndex,
				direction,
				random,
				to.y() - from.y()
			);
		}

		public static Hall create(
			int x,
			int y,
			int z,
			Holder<Structure> owningStructure,
			int paletteIndex,
			Direction direction,
			RandomGenerator random,
			int step
		) {
			if (step != 0) y++;
			return switch (step) {
				case -1 -> new Hall1(BigGlobeStructures.SMALL_DUNGEON_HALL1_TYPE, x, y, z, owningStructure, paletteIndex, direction.getOpposite(), random);
				case 0 -> new Hall0(BigGlobeStructures.SMALL_DUNGEON_HALL0_TYPE, x, y, z, owningStructure, paletteIndex, direction, random);
				case 1 -> new Hall1(BigGlobeStructures.SMALL_DUNGEON_HALL1_TYPE, x, y, z, owningStructure, paletteIndex, direction, random);
				default -> throw new IllegalArgumentException(Integer.toString(step));
			};
		}
	}

	public static class Hall0 extends Hall {

		public Hall0(
			StructurePieceType type,
			int x,
			int y,
			int z,
			Holder<Structure> owningStructure,
			int paletteIndex,
			Direction direction,
			RandomGenerator random
		) {
			super(type, x, y, z, owningStructure, paletteIndex, direction, random);
		}

		public Hall0(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
			super(type, context, nbt);
		}

		@Override
		public void generateRaw(Context context) {
			Coordinator root = this.coordinator(context);
			int left = this.getLeft(), right = this.getRight();
			root.setBlockStateCuboid(0, 1, left, 0, 3, right, this.hasBars() ? this.palette().barsSupplier(true, false, true, false) : this.palette().air());
		}
	}

	public static class Hall1 extends Hall {

		public Hall1(
			StructurePieceType type,
			int x,
			int y,
			int z,
			Holder<Structure> owningStructure,
			int paletteIndex,
			Direction direction,
			RandomGenerator random
		) {
			super(type, x, y, z, owningStructure, paletteIndex, direction, random);
		}

		public Hall1(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
			super(type, context, nbt);
		}

		@Override
		public void generateRaw(Context context) {
			Coordinator root = this.coordinator(context);
			int left = this.getLeft(), right = this.getRight();
			Palette palette = this.palette();
			root.setBlockStateLine(0, 0, left, 0, 0, 1, right - left + 1, palette.slabSupplier(SlabType.BOTTOM));
			root.setBlockStateLine(0, 3, left, 0, 0, 1, right - left + 1, palette.slabSupplier(SlabType.TOP));
			root.setBlockStateCuboid(0, 1, left, 0, 2, right, BlockStates.AIR);
		}
	}
}