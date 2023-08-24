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
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
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
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.autocodec.annotations.DefaultDouble;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNotEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.*;
import builderb0y.bigglobe.structures.BigGlobeStructure;
import builderb0y.bigglobe.structures.LabyrinthLayout;
import builderb0y.bigglobe.structures.LabyrinthLayout.DecorationPiece;
import builderb0y.bigglobe.structures.LabyrinthLayout.HallPiece;
import builderb0y.bigglobe.structures.LabyrinthLayout.LabyrinthPiece;
import builderb0y.bigglobe.structures.LabyrinthLayout.RoomPiece;
import builderb0y.bigglobe.structures.RawGenerationStructure;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinateSupplier;
import builderb0y.bigglobe.util.coordinators.Coordinator;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

public abstract class AbstractDungeonStructure extends BigGlobeStructure implements RawGenerationStructure {

	public final @VerifyNullable TagKey<ConfiguredFeature<?, ?>> room_decorators;
	public final @VerifyNotEmpty IRandomList<@UseName("entity") EntityType<?>> spawner_entries;
	public final @VerifyNotEmpty List<Palette> palettes;

	public AbstractDungeonStructure(
		Config config,
		@VerifyNullable TagKey<ConfiguredFeature<?, ?>> room_decorators,
		RandomList<EntityType<?>> spawner_entries,
		List<Palette> palettes
	) {
		super(config);
		this.room_decorators = room_decorators;
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
			context.seed(),
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
		public @Nullable TagKey<ConfiguredFeature<?, ?>> roomDecorators;
		public IRandomList<EntityType<?>> spawnerEntries;

		public DungeonLayout(
			WorldColumn column,
			int y,
			RandomGenerator random,
			int maxRooms,
			@Nullable TagKey<ConfiguredFeature<?, ?>> roomDecorators,
			IRandomList<EntityType<?>> spawnerEntries,
			List<Palette> palettes
		) {
			super(random, maxRooms);
			this.palette = new RestrictedList<>(palettes, column, y).getRandomElement(random);
			this.centerX = column.x;
			this.centerZ = column.z;
			this.roomDecorators = roomDecorators;
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
			nbt.put("palette", BigGlobeAutoCodec.AUTO_CODEC.encode(Palette.CODER, this.palette, NbtOps.INSTANCE));
		}

		@Override
		public BlockBox boundingBox() {
			return this.boundingBox;
		}

		public Coordinator coordinator(StructureWorldAccess world, BlockBox limit) {
			return Coordinator.forWorld(world, Block.NOTIFY_LISTENERS).inBox(limit, false).translate(this.x(), this.y(), this.z());
		}

		public Coordinator coordinator(RawGenerationStructurePiece.Context context) {
			return Coordinator.forChunk(context.chunk, context.columns::getColumnChecked).inBox(WorldUtil.chunkBox(context.chunk), false).translate(this.x(), this.y(), this.z());
		}

		public Palette palette() {
			return this.palette;
		}
	}

	public static abstract class RoomDungeonPiece extends DungeonPiece implements RoomPiece, RawGenerationStructurePiece {

		public static final int PIT_BIT = 1 << 1;

		public final RoomDungeonPiece[] connections = new RoomDungeonPiece[4];
		public @Nullable TagKey<ConfiguredFeature<?, ?>> decorators;

		public RoomDungeonPiece(StructurePieceType type, int chainLength, BlockBox boundingBox, Palette palette, @Nullable TagKey<ConfiguredFeature<?, ?>> decorators) {
			super(type, chainLength, boundingBox, palette);
			this.decorators = decorators;
		}

		public RoomDungeonPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
			String id = nbt.getString("decorators");
			this.decorators = id.isEmpty() ? null : TagKey.of(RegistryKeyVersions.configuredFeature(), new Identifier(id));
		}

		@Override
		@MustBeInvokedByOverriders
		public void writeNbt(StructureContext context, NbtCompound nbt) {
			super.writeNbt(context, nbt);
			if (this.decorators != null) nbt.putString("decorators", this.decorators.id().toString());
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
		public void generateRaw(Context context) {
			BlockBox chunkBox = WorldUtil.chunkBox(context.chunk);
			BlockBox intersection = WorldUtil.intersection(this.boundingBox, chunkBox);
			if (intersection == null) return;
			BlockPos.Mutable pos = new BlockPos.Mutable();
			CoordinateSupplier<BlockState> mainBlock = this.palette().mainSupplier();
			//this code compiles. intellij lies.
			for (pos.setY(this.y()); pos.getY() <= intersection.getMaxY(); pos.setY(pos.getY() + 1)) {
				for (pos.setZ(intersection.getMinZ()); pos.getZ() <= intersection.getMaxZ(); pos.setZ(pos.getZ() + 1)) {
					for (pos.setX(intersection.getMinX()); pos.getX() <= intersection.getMaxX(); pos.setX(pos.getX() + 1)) {
						context.chunk.setBlockState(
							pos,
							pos.getX() == this.boundingBox.getMinX() ||
							pos.getX() == this.boundingBox.getMaxX() ||
							pos.getY() == this.y() ||
							pos.getY() == this.boundingBox.getMaxY() ||
							pos.getZ() == this.boundingBox.getMinZ() ||
							pos.getZ() == this.boundingBox.getMaxZ()
							? mainBlock.get(pos)
							: BlockStates.AIR,
							false
						);
					}
				}
			}
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
			int x = this.x();
			int y = this.y();
			int z = this.z();
			if (this.decorators != null && contains(chunkBox, x, y, z)) {
				RegistryEntryList<ConfiguredFeature<?, ?>> tag = world.getRegistryManager().get(RegistryKeyVersions.configuredFeature()).getEntryList(this.decorators).orElse(null);
				if (tag != null) {
					RegistryEntry<ConfiguredFeature<?, ?>> entry = tag.getRandom(random).orElse(null);
					if (entry != null) {
						entry.value().generate(world, chunkGenerator, new MojangPermuter(Permuter.permute(world.getSeed() ^ 0x265B4B7BF1BC7786L, x, y, z)), new BlockPos(x, y, z));
					}
				}
			}
		}

		/**
		BlockBox.contains(int, int, int) was added in 1.19.4.
		in 1.19.2, only BlockBox.contains(Vec3i) existed.
		*/
		public static boolean contains(BlockBox box, int x, int y, int z) {
			return x >= box.getMinX() && x <= box.getMaxX() && y >= box.getMinY() && y <= box.getMaxY() && z >= box.getMinZ() && z <= box.getMaxZ();
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

		@Override
		public Coordinator coordinator(RawGenerationStructurePiece.Context context) {
			return super.coordinator(context).rotate1x(Directions.rotationOf(Directions.POSITIVE_X, this.getFacing()));
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
			String id = nbt.getString("entityType");
			if (id.isEmpty()) id = "minecraft:zombie";
			this.spawnerType = RegistryVersions.entityType().get(new Identifier(id));
		}

		@Override
		@MustBeInvokedByOverriders
		public void writeNbt(StructureContext context, NbtCompound nbt) {
			super.writeNbt(context, nbt);
			nbt.putString("entityType", RegistryVersions.entityType().getId(this.spawnerType).toString());
		}

		public void initSpawner(BlockPos pos, MobSpawnerBlockEntity spawner) {
			#if MC_VERSION <= MC_1_19_2
				spawner.getLogic().setEntityId(this.spawnerType);
			#else
				spawner.setEntityType(this.spawnerType, new Permuter(Permuter.permute(0x61DE982B73AD4955L, pos)).mojang());
			#endif
		}
	}

	public static abstract class HallDungeonPiece extends DungeonPiece implements HallPiece, RawGenerationStructurePiece {

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

		@Override
		public Coordinator coordinator(Context context) {
			return super.coordinator(context).rotate1x(Directions.rotationOf(Directions.POSITIVE_X, this.getFacing()));
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
		) {}
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