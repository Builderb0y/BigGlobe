package builderb0y.bigglobe.structures.dungeons;

import java.util.List;
import java.util.random.RandomGenerator;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.mixins.MobSpawnerLogic_GettersAndSettersForEverything;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.structures.BigGlobeStructures;
import builderb0y.bigglobe.structures.LabyrinthLayout;
import builderb0y.bigglobe.util.coordinators.Coordinator;

public class SmallDungeonStructure extends AbstractDungeonStructure {

	public static final Codec<SmallDungeonStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(SmallDungeonStructure.class);

	public SmallDungeonStructure(Config config, TagKey<ConfiguredFeature<?, ?>> room_decorators, IRandomList<EntityType<?>> spawner_entries, List<Palette> palettes) {
		super(config, room_decorators, spawner_entries, palettes);
	}

	@Override
	public DungeonLayout layout(WorldColumn column, int y, RandomGenerator random) {
		return new Layout(column, y, random, this.room_decorators, this.spawner_entries, this.palettes);
	}

	@Override
	public StructureType<?> getType() {
		return BigGlobeStructures.SMALL_DUNGEON_TYPE;
	}

	public static class Layout extends DungeonLayout {

		public Layout(WorldColumn column, int y, RandomGenerator random, @Nullable TagKey<ConfiguredFeature<?, ?>> roomDecorators, IRandomList<EntityType<?>> spawnerEntries, List<Palette> palettes) {
			super(column, y, random, random.nextInt(384) + 192, roomDecorators, spawnerEntries, palettes);
		}

		@Override
		public RoomDungeonPiece newRoom() {
			return new Room(BigGlobeStructures.SMALL_DUNGEON_ROOM_TYPE, this.palette, this.random, this.roomDecorators);
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

		public Room(StructurePieceType type, Palette palette, RandomGenerator random, @Nullable TagKey<ConfiguredFeature<?, ?>> decorators) {
			super(type, 0, null, palette, decorators);
			this.setPit((random.nextInt() & 15) == 0);
			this.setPos(0, 0, 0);
		}

		public Room(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public void addDecorations(LabyrinthLayout layout) {
			Direction deadEndDirection;
			if (this.hasPit()) {
				layout.decorations.add(new PitDungeonPiece(BigGlobeStructures.DUNGEON_PIT_TYPE, this.x(), this.y(), this.z(), this.palette, layout.random.nextInt(2), layout.random));
				this.decorators = null;
			}
			else if ((deadEndDirection = this.getDeadEndDirection()) != null) {
				if (layout.random.nextBoolean()) {
					layout.decorations.add(new ChestPiece(BigGlobeStructures.SMALL_DUNGEON_CHEST_TYPE, this.x(), this.y() + 1, this.z(), this.palette, deadEndDirection, layout.random.nextLong()));
					this.decorators = null;
				}
			}
			else {
				if ((layout.random.nextInt() & 15) == 0) {
					layout.decorations.add(new SpawnerPiece(BigGlobeStructures.SMALL_DUNGEON_SPAWNER_TYPE, this.x(), this.y() + 1, this.z(), this.palette, ((Layout)(layout)).spawnerEntries.getRandomElement(layout.random)));
					this.decorators = null;
				}
			}
		}

		@Override
		public void setPos(int x, int y, int z) {
			this.boundingBox = new BlockBox(x - 2, this.hasPit() ? y - 2 : y, z - 2, x + 2, y + 4, z + 2);
		}
	}

	public static class ChestPiece extends ChestDungeonPiece {

		public ChestPiece(StructurePieceType type, int x, int y, int z, Palette palette, Direction direction, long seed) {
			super(type, 0, new BlockBox(x, y, z, x, y, z), palette, direction, seed);
			this.setOrientation(direction);
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
			Coordinator root = this.coordinator(world, chunkBox);
			root.setBlockStateAndBlockEntity(0, 0, 0, Blocks.CHEST.getDefaultState(), ChestBlockEntity.class, this::initChest);
		}
	}

	public static class SpawnerPiece extends SpawnerDungeonPiece {

		public SpawnerPiece(StructurePieceType type, int x, int y, int z, Palette palette, EntityType<?> spawnerType) {
			super(type, 0, new BlockBox(x, y, z, x, y, z), palette, spawnerType);
		}

		public SpawnerPiece(StructurePieceType type, NbtCompound nbt) {
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
			this.coordinator(world, chunkBox)
			.setBlockStateAndBlockEntity(
				0, 0, 0,
				Blocks.SPAWNER.getDefaultState(),
				MobSpawnerBlockEntity.class,
				this::initSpawner
			);
		}

		@Override
		public void initSpawner(BlockPos pos, MobSpawnerBlockEntity spawner) {
			super.initSpawner(pos, spawner);
			MobSpawnerLogic_GettersAndSettersForEverything logic = (MobSpawnerLogic_GettersAndSettersForEverything)(spawner.getLogic());
			logic.bigglobe_setRequiredPlayerRange(16);
			logic.bigglobe_setMaxNearbyEntities(2);
			logic.bigglobe_setSpawnCount(2);
		}
	}

	public static abstract class Hall extends HallDungeonPiece {

		public Hall(StructurePieceType type, int x, int y, int z, Palette palette, Direction direction, RandomGenerator random) {
			super(type, 0, new BlockBox(x - 2, y, z - 2, x + 2, y + 4, z + 2), palette);
			this.setOrientation(direction);
			this.setBars((random.nextInt() & 7) == 0);
			int width = random.nextInt(3) + 1;
			int position = random.nextInt(4 - width) - 1;
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
				random,
				to.y() - from.y()
			);
		}

		public static Hall create(int x, int y, int z, Palette palette, Direction direction, RandomGenerator random, int step) {
			if (step != 0) y++;
			return switch (step) {
				case -1 -> new Hall1(BigGlobeStructures.SMALL_DUNGEON_HALL1_TYPE, x, y, z, palette, direction.getOpposite(), random);
				case  0 -> new Hall0(BigGlobeStructures.SMALL_DUNGEON_HALL0_TYPE, x, y, z, palette, direction, random);
				case  1 -> new Hall1(BigGlobeStructures.SMALL_DUNGEON_HALL1_TYPE, x, y, z, palette, direction, random);
				default -> throw new IllegalArgumentException(Integer.toString(step));
			};
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
		public void generateRaw(Context context) {
			Coordinator root = this.coordinator(context);
			int left = this.getLeft(), right = this.getRight();
			root.setBlockStateCuboid(0, 1, left, 0, 3, right, this.hasBars() ? this.palette().barsSupplier(true, false, true, false) : this.palette().air());
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