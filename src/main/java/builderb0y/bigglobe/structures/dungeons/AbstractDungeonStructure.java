package builderb0y.bigglobe.structures.dungeons;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.*;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.block.enums.StairShape;
import net.minecraft.block.enums.WallShape;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.state.property.Property;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import builderb0y.autocodec.annotations.DefaultDouble;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifySizeRange;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.*;
import builderb0y.bigglobe.structures.BigGlobeStructure;
import builderb0y.bigglobe.structures.LabyrinthLayout;
import builderb0y.bigglobe.structures.LabyrinthLayout.DecorationPiece;
import builderb0y.bigglobe.structures.LabyrinthLayout.HallPiece;
import builderb0y.bigglobe.structures.LabyrinthLayout.LabyrinthPiece;
import builderb0y.bigglobe.structures.LabyrinthLayout.RoomPiece;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinateSupplier;
import builderb0y.bigglobe.util.coordinators.Coordinator;

public abstract class AbstractDungeonStructure extends BigGlobeStructure {

	public final @VerifySizeRange(min = 0, minInclusive = false) IRandomList<@UseName("entity") EntityType<?>> spawner_entries;
	public final @VerifySizeRange(min = 0, minInclusive = false) List<Palette> palettes;

	public AbstractDungeonStructure(Config config, RandomList<EntityType<?>> spawner_entries, List<Palette> palettes) {
		super(config);
		this.spawner_entries = spawner_entries;
		this.palettes = palettes;
	}

	public abstract DungeonLayout layout(WorldColumn column, int y, RandomGenerator random);

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		BlockPos startPos = randomBlockInChunk(context, 64, 64);
		if (startPos == null) return Optional.empty();

		long seed = chunkSeed(context, 0x9DFB0A6E61391175L);
		WorldColumn column = WorldColumn.forGenerator(
			context.chunkGenerator(),
			context.noiseConfig(),
			startPos.getX(),
			startPos.getZ()
		);
		int y = startPos.getY();
		return Optional.of(
			new StructurePosition(
				startPos,
				(StructurePiecesCollector pieces) -> {
					DungeonLayout layout = this.layout(column, y, new Permuter(seed));
					layout.generate();
					layout.addTo(pieces);
				}
			)
		);
	}

	public static abstract class DungeonLayout extends LabyrinthLayout {

		public int centerX, centerZ;
		public Palette palette;
		public IRandomList<EntityType<?>> spawnerEntries;

		public DungeonLayout(
			WorldColumn column,
			int y,
			RandomGenerator random,
			int maxRooms,
			IRandomList<EntityType<?>> spawnerEntries,
			List<Palette> palettes
		) {
			super(random, maxRooms);
			this.palette = new RestrictedList<>(palettes, column, y).getRandomElement(random);
			this.centerX = column.x;
			this.centerZ = column.z;
			this.spawnerEntries = spawnerEntries;
			RoomDungeonPiece room = this.newRoom();
			room.setPos(column.x, y, column.z);
			this.rooms.add(room);
			this.activeRooms.add(room);
		}

		@Override
		public abstract RoomDungeonPiece newRoom();

		@Override
		public abstract HallDungeonPiece newHall(RoomPiece room1, RoomPiece room2, Direction direction);

		public void addTo(StructurePiecesCollector collector) {
			for (Object room : this.rooms) {
				collector.addPiece((StructurePiece)(room));
			}
			for (Object hall : this.halls) {
				collector.addPiece((StructurePiece)(hall));
			}
			for (Object decoration : this.decorations) {
				collector.addPiece((StructurePiece)(decoration));
			}
		}
	}

	public static abstract class DungeonPiece extends StructurePiece implements LabyrinthPiece {

		public byte variant;
		public Palette palette;

		public DungeonPiece(StructurePieceType type, int length, BlockBox boundingBox, Palette palette) {
			super(type, length, boundingBox);
			this.palette = palette;
		}

		public DungeonPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
			this.variant = nbt.getByte("var");
			NbtElement paletteNBT = nbt.get("palette");
			if (paletteNBT != null) try {
				this.palette = BigGlobeAutoCodec.AUTO_CODEC.decode(Palette.CODER, paletteNBT, NbtOps.INSTANCE);
			}
			catch (DecodeException exception) {
				throw new RuntimeException(exception);
			}
			else {
				this.palette = Palette.COBBLE;
			}
		}

		@Override
		@MustBeInvokedByOverriders
		public void writeNbt(StructureContext context, NbtCompound nbt) {
			nbt.putByte("var", this.variant);
		}

		@Override
		public BlockBox boundingBox() {
			return this.boundingBox;
		}

		public Coordinator coordinator(StructureWorldAccess world, BlockBox limit) {
			return Coordinator.forWorld(world, Block.NOTIFY_LISTENERS).inBox(limit, false).translate(this.x(), this.y(), this.z());
		}

		public Palette palette() {
			return this.palette;
		}
	}

	public static abstract class RoomDungeonPiece extends DungeonPiece implements RoomPiece {

		public static final int PIT_BIT = 1 << 1;

		public RoomDungeonPiece[] connections = new RoomDungeonPiece[4];

		public RoomDungeonPiece(StructurePieceType type, int chainLength, BlockBox boundingBox, Palette palette) {
			super(type, chainLength, boundingBox, palette);
		}

		public RoomDungeonPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		public boolean hasPit() {
			return (this.variant & PIT_BIT) != 0;
		}

		public void setPit(boolean pit) {
			this.variant = (byte)(pit ? (this.variant | PIT_BIT) : (this.variant & ~PIT_BIT));
		}

		@Override
		public RoomDungeonPiece getConnectedRoom(Direction direction) {
			return this.connections[direction.getHorizontal()];
		}

		@Override
		public void setConnectedRoom(Direction direction, RoomPiece connection) {
			this.connections[direction.getHorizontal()] = (RoomDungeonPiece)(connection);
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
			Palette palette = this.palette();
			int minX = this.boundingBox.getMinX() - this.x();
			int minY = 0;
			int minZ = this.boundingBox.getMinZ() - this.z();
			int maxX = this.boundingBox.getMaxX() - this.x();
			int maxY = this.boundingBox.getMaxY() - this.y();
			int maxZ = this.boundingBox.getMaxZ() - this.z();
			root.setBlockStateCuboid(minX, minY, minZ, maxX, minY, maxZ, palette.mainSupplier());
			root.setBlockStateCuboid(minX, maxY, minZ, maxX, maxY, maxZ, palette.mainSupplier());
			root.rotate4x90().setBlockStateCuboid(minX, minY + 1, minZ, maxX - 1, maxY - 1, minZ, palette.mainSupplier());
			root.setBlockStateCuboid(minX + 1, minY + 1, minZ + 1, maxX - 1, maxY - 1, maxZ - 1, BlockStates.AIR);
		}

		@Override
		public abstract void addDecorations(LabyrinthLayout layout);

		public @Nullable Direction getDeadEndDirection() {
			Direction result = null;
			for (Direction test : Directions.HORIZONTAL) {
				if (this.getConnectedRoom(test) != null) {
					if (result == null) result = test;
					else return null;
				}
			}
			return result;
		}

		@Override
		public int y() {
			return this.hasPit() ? super.y() + 2 : super.y();
		}
	}

	public static class PitDungeonPiece extends DecorationDungeonPiece {

		public static final int
			LAVA_BIT     = 1 << 1,
			WATER_BIT    = 1 << 2,
			RADIUS_SHIFT = 3,
			RADIUS_MASK  = 0b11000;


		public PitDungeonPiece(StructurePieceType type, int x, int y, int z, Palette palette, int innerRadius, RandomGenerator random) {
			super(type, 0, new BlockBox(x - innerRadius - 1, y - 2, z - innerRadius - 1, x + innerRadius + 1, y, z + innerRadius + 1), palette);
			this.setToRandomType(random);
			this.setInnerRadius(innerRadius);
		}

		public PitDungeonPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		public void setToRandomType(RandomGenerator random) {
			this.variant |= random.nextInt(3) << 1; //0, 2, or 4 corresponds to air, lava, or water.
		}

		public int getInnerRadius() {
			return (this.variant & RADIUS_MASK) >>> RADIUS_SHIFT;
		}

		public void setInnerRadius(int innerRadius) {
			this.variant = (byte)((this.variant & ~RADIUS_MASK) | (innerRadius << RADIUS_SHIFT));
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
			int innerRadius = this.getInnerRadius();
			int outerRadius = innerRadius + 1;
			Coordinator root = this.coordinator(world, chunkBox);
			Palette palette = this.palette();
			root.setBlockStateCuboid(-outerRadius, -2, -outerRadius, outerRadius, -1, outerRadius, palette.mainSupplier());
			root.setBlockStateCuboid(-innerRadius, -1, -innerRadius, innerRadius, -1, innerRadius, this.getFiller());
			root.setBlockState(0, 0, 0, palette.barsSupplier(true, true, true, true));
			root.rotate4x90().setBlockStateLine(1, 0, 0, 1, 0, 0, innerRadius, palette.barsSupplier(false, true, false, true));
			root.rotate4x90().setBlockStateCuboid(1, 0, 1, innerRadius, 0, innerRadius, BlockStates.AIR);
		}

		public BlockState getFiller() {
			return switch (this.variant & (LAVA_BIT | WATER_BIT)) {
				case LAVA_BIT -> BlockStates.LAVA;
				case WATER_BIT -> BlockStates.WATER;
				default -> BlockStates.AIR;
			};
		}

		@Override
		public int y() {
			return this.boundingBox().getMaxY();
		}
	}

	public static abstract class ChestDungeonPiece extends DecorationDungeonPiece {

		public final long seed;

		public ChestDungeonPiece(StructurePieceType type, int length, BlockBox boundingBox, Palette palette, Direction facing, long seed) {
			super(type, length, boundingBox, palette);
			this.setOrientation(facing);
			this.seed = seed;
		}

		public ChestDungeonPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
			this.seed = nbt.getLong("seed");
		}

		@Override
		@MustBeInvokedByOverriders
		public void writeNbt(StructureContext context, NbtCompound nbt) {
			super.writeNbt(context, nbt);
			nbt.putLong("seed", this.seed);
		}

		public void initChest(BlockPos pos, ChestBlockEntity chest) {
			chest.setLootTable(BigGlobeMod.modID("chests/advanced_dungeon"), this.seed);
		}

		@Override
		public Coordinator coordinator(StructureWorldAccess world, BlockBox limit) {
			return super.coordinator(world, limit).rotate1x(Directions.rotationOf(Direction.NORTH, this.getFacing()));
		}
	}

	public static abstract class SpawnerDungeonPiece extends DecorationDungeonPiece {

		public final EntityType<?> spawnerType;

		public SpawnerDungeonPiece(StructurePieceType type, int length, BlockBox boundingBox, Palette palette, EntityType<?> spawnerType) {
			super(type, length, boundingBox, palette);
			this.spawnerType = spawnerType;
		}

		public SpawnerDungeonPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
			this.spawnerType = Registry.ENTITY_TYPE.get(new Identifier(nbt.getString("entityType")));
		}

		@Override
		@MustBeInvokedByOverriders
		public void writeNbt(StructureContext context, NbtCompound nbt) {
			super.writeNbt(context, nbt);
			nbt.putString("entityType", Registry.ENTITY_TYPE.getId(this.spawnerType).toString());
		}

		public void initSpawner(BlockPos pos, MobSpawnerBlockEntity spawner) {
			spawner.getLogic().setEntityId(this.spawnerType);
		}
	}

	public static abstract class HallDungeonPiece extends DungeonPiece implements HallPiece {

		public static final int BARS_BIT = 1 << 1;

		public byte sidewaysness;

		public HallDungeonPiece(StructurePieceType type, int chainLength, BlockBox boundingBox, Palette palette) {
			super(type, chainLength, boundingBox, palette);
		}

		public HallDungeonPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
			this.sidewaysness = nbt.getByte("side");
		}

		@Override
		@MustBeInvokedByOverriders
		public void writeNbt(StructureContext context, NbtCompound nbt) {
			super.writeNbt(context, nbt);
			nbt.putByte("side", this.sidewaysness);
		}

		public boolean hasBars() {
			return (this.variant & BARS_BIT) != 0;
		}

		public void setBars(boolean bars) {
			this.variant = (byte)(bars ? this.variant | BARS_BIT : this.variant & ~BARS_BIT);
		}

		public int getLeft() {
			return ((int)(this.sidewaysness)) << 24 >> 28;
		}

		public void setLeft(int left) {
			this.sidewaysness = (byte)((this.sidewaysness & 0b00001111) | ((left & 0b1111) << 4));
		}

		public int getRight() {
			return ((int)(this.sidewaysness)) << 28 >> 28;
		}

		public void setRight(int right) {
			this.sidewaysness = (byte)((this.sidewaysness & 0b11110000) | (right & 0b1111));
		}

		@Override
		public Coordinator coordinator(StructureWorldAccess world, BlockBox limit) {
			return super.coordinator(world, limit).rotate1x(Directions.rotationOf(Directions.POSITIVE_X, this.getFacing()));
		}
	}

	public static abstract class DecorationDungeonPiece extends DungeonPiece implements DecorationPiece {

		public DecorationDungeonPiece(StructurePieceType type, int length, BlockBox boundingBox, Palette palette) {
			super(type, length, boundingBox, palette);
		}

		public DecorationDungeonPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}
	}

	public static record Palette(
		@DefaultDouble(IRandomList.DEFAULT_WEIGHT) double weight,
		ColumnRestriction restrictions,
		IRandomList<@UseName("block") Block> main,
		IRandomList<@UseName("block") Block> slab,
		IRandomList<@UseName("block") Block> stairs,
		IRandomList<@UseName("block") Block> wall
	)
	implements IRestrictedListElement {

		public static final AutoCoder<Palette> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Palette.class);

		public static Palette
			COBBLE = new Palette(
				IRandomList.DEFAULT_WEIGHT,
				ColumnRestriction.EMPTY,
				new ConstantWeightRandomList<>(List.of(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE)),
				new ConstantWeightRandomList<>(List.of(Blocks.COBBLESTONE_SLAB, Blocks.MOSSY_COBBLESTONE_SLAB)),
				new ConstantWeightRandomList<>(List.of(Blocks.COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE_STAIRS)),
				new ConstantWeightRandomList<>(List.of(Blocks.COBBLESTONE_WALL, Blocks.MOSSY_COBBLESTONE_WALL))
			),
			BRICKS = new Palette(
				IRandomList.DEFAULT_WEIGHT,
				ColumnRestriction.EMPTY,
				new RandomList<Block>(3).addSelf(Blocks.MOSSY_STONE_BRICKS, 1.5D).addSelf(Blocks.STONE_BRICKS, 1.0D).addSelf(Blocks.CRACKED_STONE_BRICKS, 0.5D),
				new ConstantWeightRandomList<>(List.of(Blocks.STONE_BRICK_SLAB, Blocks.MOSSY_STONE_BRICK_SLAB)),
				new ConstantWeightRandomList<>(List.of(Blocks.STONE_BRICK_STAIRS, Blocks.MOSSY_STONE_BRICK_STAIRS)),
				new ConstantWeightRandomList<>(List.of(Blocks.STONE_BRICK_WALL, Blocks.MOSSY_STONE_BRICK_WALL))
			),
			DEEPSLATE_COBBLE = new Palette(
				IRandomList.DEFAULT_WEIGHT,
				ColumnRestriction.EMPTY,
				new SingletonRandomList<>(Blocks.COBBLED_DEEPSLATE, 1.0D),
				new SingletonRandomList<>(Blocks.COBBLED_DEEPSLATE_SLAB, 1.0D),
				new SingletonRandomList<>(Blocks.COBBLED_DEEPSLATE_STAIRS, 1.0D),
				new SingletonRandomList<>(Blocks.COBBLED_DEEPSLATE_WALL, 1.0D)
			),
			DEEPSLATE_BRICKS = new Palette(
				IRandomList.DEFAULT_WEIGHT,
				ColumnRestriction.EMPTY,
				new RandomList<Block>().addSelf(Blocks.DEEPSLATE_BRICKS, 1.5D).addSelf(Blocks.CRACKED_DEEPSLATE_BRICKS, 0.5D),
				new SingletonRandomList<>(Blocks.DEEPSLATE_BRICK_SLAB, 1.0D),
				new SingletonRandomList<>(Blocks.DEEPSLATE_BRICK_STAIRS, 1.0D),
				new SingletonRandomList<>(Blocks.DEEPSLATE_BRICK_WALL, 1.0D)
			);

		@Override
		public double getWeight() {
			return this.weight;
		}

		@Override
		public ColumnRestriction getRestrictions() {
			return this.restrictions;
		}

		public CoordinateSupplier<BlockState> mainSupplier() {
			return BlockStateSupplier.forBlocks(this.main);
		}

		public CoordinateSupplier<BlockState> slabSupplier(SlabType slabType) {
			return (
				BlockStateSupplier.forBlocks(this.slab)
				.with(SlabBlock.TYPE, slabType)
			);
		}

		public CoordinateSupplier<BlockState> stairsSupplier(BlockHalf half, Direction facing, StairShape shape) {
			return (
				BlockStateSupplier.forBlocks(this.stairs)
				.with(StairsBlock.HALF,   half  )
				.with(StairsBlock.FACING, facing)
				.with(StairsBlock.SHAPE,  shape )
			);
		}

		public CoordinateSupplier<BlockState> wallSupplier(WallShape north, WallShape east, WallShape south, WallShape west, boolean up) {
			return (
				BlockStateSupplier.forBlocks(this.wall)
				.with(WallBlock.NORTH_SHAPE, north)
				.with(WallBlock.EAST_SHAPE,  east )
				.with(WallBlock.SOUTH_SHAPE, south)
				.with(WallBlock.WEST_SHAPE,  west )
			);
		}

		public CoordinateSupplier<BlockState> barsSupplier(boolean north, boolean east, boolean south, boolean west) {
			return (
				new SingleStateSupplier(Blocks.IRON_BARS.getDefaultState())
				.with(HorizontalConnectingBlock.NORTH, north)
				.with(HorizontalConnectingBlock.EAST,  east )
				.with(HorizontalConnectingBlock.SOUTH, south)
				.with(HorizontalConnectingBlock.WEST,  west )
			);
		}

		public CoordinateSupplier<BlockState> air() {
			return new SingleStateSupplier(BlockStates.AIR);
		}
	}

	public static interface BlockStateSupplier extends CoordinateSupplier<BlockState> {

		public static BlockStateSupplier forBlocks(IRandomList<Block> blocks) {
			return (
				blocks.size() == 1
				? new SingleStateSupplier(blocks.get(0).getDefaultState())
				: new RandomListBlockStateSupplier(blocks)
			);
		}

		public abstract <C extends Comparable<C>> BlockStateSupplier with(Property<C> property, C value);
	}

	public static class RandomListBlockStateSupplier implements BlockStateSupplier {

		public IRandomList<Block> blocks;
		public List<Object> properties;

		public RandomListBlockStateSupplier(IRandomList<Block> blocks) {
			this.blocks = blocks;
			this.properties = new ArrayList<>(4);
		}

		@Override
		public <C extends Comparable<C>> BlockStateSupplier with(Property<C> property, C value) {
			this.properties.add(property);
			this.properties.add(value);
			return this;
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public BlockState get(BlockPos.Mutable pos) {
			BlockState state = this.blocks.getRandomElement(Permuter.permute(0xFE1FCB62BC2A3608L, pos)).getDefaultState();
			List<Object> properties = this.properties;
			for (int index = 0, size = properties.size(); index < size; index += 2) {
				Property property = (Property<?>)(properties.get(index));
				if (state.contains(property)) {
					Comparable value = (Comparable<?>)(properties.get(index + 1));
					state = state.with(property, value);
				}
			}
			return state;
		}
	}

	public static class SingleStateSupplier implements BlockStateSupplier {

		public BlockState state;

		public SingleStateSupplier(BlockState state) {
			this.state = state;
		}

		@Override
		public <C extends Comparable<C>> BlockStateSupplier with(Property<C> property, C value) {
			if (this.state.contains(property)) {
				this.state = this.state.with(property, value);
			}
			return this;
		}

		@Override
		public BlockState get(BlockPos.Mutable pos) {
			return this.state;
		}
	}
}