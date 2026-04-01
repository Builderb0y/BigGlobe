package builderb0y.bigglobe.structures.dungeons;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import builderb0y.autocodec.annotations.DefaultDouble;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNotEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToIntScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.ConstantWeightRandomList.RandomAccessConstantWeightRandomList;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.IRestrictedListElement;
import builderb0y.bigglobe.randomLists.RestrictedList;
import builderb0y.bigglobe.structures.BigGlobeStructure;
import builderb0y.bigglobe.structures.LabyrinthLayout;
import builderb0y.bigglobe.structures.LabyrinthLayout.DecorationPiece;
import builderb0y.bigglobe.structures.LabyrinthLayout.HallPiece;
import builderb0y.bigglobe.structures.LabyrinthLayout.LabyrinthPiece;
import builderb0y.bigglobe.structures.LabyrinthLayout.RoomPiece;
import builderb0y.bigglobe.structures.RawGenerationStructure;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinateSupplier;
import builderb0y.bigglobe.util.coordinators.Coordinator;
import builderb0y.bigglobe.versions.*;

public abstract class AbstractDungeonStructure extends BigGlobeStructure implements RawGenerationStructure {

	public final @VerifyNullable DelayedEntryList<ConfiguredFeature<?, ?>> room_decorators;
	public final @VerifyNotEmpty IRandomList<@UseName("entity") Holder<EntityType<?>>> spawner_entries;
	public final @VerifyNotEmpty List<Palette> palettes;

	public AbstractDungeonStructure(
		StructureSettings config,
		ColumnToIntScript.@VerifyNullable Catcher min_y,
		ColumnToIntScript.@VerifyNullable Catcher surface_y,
		@VerifyNullable DelayedEntryList<ConfiguredFeature<?, ?>> room_decorators,
		IRandomList<Holder<EntityType<?>>> spawner_entries,
		List<Palette> palettes
	) {
		super(config, min_y, surface_y);
		this.room_decorators = room_decorators;
		this.spawner_entries = spawner_entries;
		this.palettes = palettes;
	}

	@Override
	public int bigglobe_getMaxRadiusInChunks() {
		return 8;
	}

	public abstract DungeonLayout layout(ScriptedColumn column, int y, RandomGenerator random);

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		if (!(context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator)) return Optional.empty();
		BlockPos startPos = this.randomBlockInChunk(context, 64, 64);
		if (startPos == null) return Optional.empty();

		long seed = chunkSeed(context, 0x9DFB0A6E61391175L);
		ScriptedColumn column = generator.newColumn(
			context.heightAccessor(),
			startPos.getX(),
			startPos.getZ(),
			ColumnUsage.GENERIC.maybeDhHints()
		);
		int y = startPos.getY();
		return Optional.of(
			new GenerationStub(
				startPos,
				(StructurePiecesBuilder pieces) -> {
					DungeonLayout layout = this.layout(column, y, new Permuter(seed));
					layout.generate();
					layout.addTo(pieces);
				}
			)
		);
	}

	public static abstract class DungeonLayout extends LabyrinthLayout {

		public int centerX, centerZ;
		public Holder<Structure> owningStructure;
		public int paletteIndex;
		public @Nullable DelayedEntryList<ConfiguredFeature<?, ?>> roomDecorators;
		public IRandomList<Holder<EntityType<?>>> spawnerEntries;

		public DungeonLayout(
			ScriptedColumn column,
			int y,
			RandomGenerator random,
			int maxRooms,
			@Nullable DelayedEntryList<ConfiguredFeature<?, ?>> roomDecorators,
			IRandomList<Holder<EntityType<?>>> spawnerEntries,
			Holder<Structure> owningStructure
		) {
			super(random, maxRooms);
			this.owningStructure = owningStructure;
			this.centerX = column.x();
			this.centerZ = column.z();
			this.paletteIndex = (
				owningStructure.value() instanceof AbstractDungeonStructure dungeon
					? new RestrictedList<>(dungeon.palettes, column, y).getRandomIndex(random)
					: -1
			);
			this.roomDecorators = roomDecorators;
			this.spawnerEntries = spawnerEntries;
			RoomDungeonPiece room = this.newRoom();
			room.setPos(column.x(), y, column.z());
			this.rooms.add(room);
			this.activeRooms.add(room);
		}

		@Override
		public abstract RoomDungeonPiece newRoom();

		@Override
		public abstract HallDungeonPiece newHall(RoomPiece room1, RoomPiece room2, Direction direction);

		public void addTo(StructurePiecesBuilder collector) {
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

		public static final AutoCoder<Holder<Structure>> STRUCTURE_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(new ReifiedType<>() {

		});

		public byte variant;
		public @Nullable Holder<Structure> owningStructure;
		public int paletteIndex;
		public Palette cachedPalette;

		public DungeonPiece(
			StructurePieceType type,
			int length,
			BoundingBox boundingBox,
			@Nullable Holder<Structure> owningStructure,
			int paletteIndex
		) {
			super(type, length, boundingBox);
			this.owningStructure = owningStructure;
			this.paletteIndex = paletteIndex;
			this.cachedPalette = (
				owningStructure != null &&
				owningStructure.value() instanceof AbstractDungeonStructure dungeon
				&& paletteIndex >= 0
				&& paletteIndex < dungeon.palettes.size()
					? dungeon.palettes.get(paletteIndex)
					: Palette.COBBLE
			);
		}

		public DungeonPiece(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
			super(type, nbt);
			if (nbt.get("var") instanceof NumericTag number) {
				this.variant = number.byteValue();
			}
			Tag owningStructure = nbt.get("struct");
			if (owningStructure != null) try {
				this.owningStructure = BigGlobeAutoCodec.AUTO_CODEC.decode(
					STRUCTURE_CODER,
					owningStructure,
					RegistryOps.create(
						NbtOps.INSTANCE,
						context.registryAccess()
					)
				);
			}
			catch (DecodeException exception) {
				throw new RuntimeException(exception);
			}
			this.paletteIndex = nbt.get("paletteIndex") instanceof NumericTag number ? number.intValue() : -1;
			if (
				this.owningStructure != null &&
				this.owningStructure.value() instanceof AbstractDungeonStructure dungeon &&
				this.paletteIndex >= 0 &&
				this.paletteIndex < dungeon.palettes.size()
			) {
				this.cachedPalette = dungeon.palettes.get(this.paletteIndex);
			}
			else {
				this.cachedPalette = Palette.COBBLE;
			}
		}

		@Override
		@MustBeInvokedByOverriders
		public void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
			nbt.putByte("var", this.variant);
			if (this.owningStructure != null) nbt.put("struct", BigGlobeAutoCodec.AUTO_CODEC.encode(STRUCTURE_CODER, this.owningStructure, RegistryOps.create(NbtOps.INSTANCE, context.registryAccess())));
			nbt.putInt("paletteIndex", this.paletteIndex);
		}

		@Override
		public BoundingBox boundingBox() {
			return this.boundingBox;
		}

		public Coordinator coordinator(WorldGenLevel world, BoundingBox limit) {
			return Coordinator.forWorld(world, Block.UPDATE_CLIENTS).inBox(limit, false).translate(this.x(), this.y(), this.z());
		}

		public Coordinator coordinator(RawGenerationStructurePiece.Context context) {
			return Coordinator.forChunk(context.chunk).inBox(WorldUtil.chunkBox(context.chunk), false).translate(this.x(), this.y(), this.z());
		}

		public Palette palette() {
			return this.cachedPalette;
		}
	}

	public static abstract class RoomDungeonPiece extends DungeonPiece implements RoomPiece, RawGenerationStructurePiece {

		public static final AutoCoder<DelayedEntryList<ConfiguredFeature<?, ?>>> DECORATORS_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(new ReifiedType<>() {

		});
		public static final int PIT_BIT = 1 << 1;

		public final RoomDungeonPiece[] connections = new RoomDungeonPiece[4];
		public @Nullable DelayedEntryList<ConfiguredFeature<?, ?>> decorators;
		public boolean support;

		public RoomDungeonPiece(
			StructurePieceType type,
			int chainLength,
			BoundingBox boundingBox,
			Holder<Structure> owningStructure,
			int paletteIndex,
			@Nullable DelayedEntryList<ConfiguredFeature<?, ?>> decorators
		) {
			super(type, chainLength, boundingBox, owningStructure, paletteIndex);
			this.decorators = decorators;
		}

		public RoomDungeonPiece(
			StructurePieceType type,
			StructurePieceSerializationContext context,
			CompoundTag nbt
		) {
			super(type, context, nbt);
			Tag nbtDecorators = nbt.get("decorators_v2");
			if (nbtDecorators != null) try {
				this.decorators = BigGlobeAutoCodec.AUTO_CODEC.decode(
					DECORATORS_CODER,
					nbtDecorators,
					RegistryOps.create(NbtOps.INSTANCE, BigGlobeMod.getCurrentServer().registryAccess())
				);
			}
			catch (DecodeException exception) {
				BigGlobeMod.LOGGER.error("Exception reading dungeon room decorator from NBT: ", exception);
			}
			if (nbt.get("support") instanceof NumericTag number) {
				this.support = number.byteValue() != 0;
			}
		}

		@Override
		@MustBeInvokedByOverriders
		public void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
			super.addAdditionalSaveData(context, nbt);
			if (this.decorators != null) {
				nbt.put(
					"decorators_v2",
					BigGlobeAutoCodec.AUTO_CODEC.encode(
						DECORATORS_CODER,
						this.decorators,
						RegistryOps.create(NbtOps.INSTANCE, BigGlobeMod.getCurrentServer().registryAccess())
					)
				);
			}
			nbt.putBoolean("support", this.support);
		}

		public boolean hasPit() {
			return (this.variant & PIT_BIT) != 0;
		}

		public void setPit(boolean pit) {
			this.variant = (byte)(pit ? (this.variant | PIT_BIT) : (this.variant & ~PIT_BIT));
		}

		@Override
		public RoomDungeonPiece getConnectedRoom(Direction direction) {
			return this.connections[DirectionVersions.horizontal(direction)];
		}

		@Override
		public void setConnectedRoom(Direction direction, RoomPiece connection) {
			this.connections[DirectionVersions.horizontal(direction)] = (RoomDungeonPiece)(connection);
		}

		@Override
		public void generateRaw(Context context) {
			BoundingBox chunkBox = WorldUtil.chunkBox(context.chunk);
			BoundingBox intersection = WorldUtil.intersection(this.boundingBox, chunkBox);
			if (intersection == null) return;
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			CoordinateSupplier<BlockState> mainBlock = this.palette().mainSupplier();
			for (pos.setY(this.y()); pos.getY() <= intersection.maxY(); pos.setY(pos.getY() + 1)) {
				for (pos.setZ(intersection.minZ()); pos.getZ() <= intersection.maxZ(); pos.setZ(pos.getZ() + 1)) {
					for (pos.setX(intersection.minX()); pos.getX() <= intersection.maxX(); pos.setX(pos.getX() + 1)) {
						ChunkVersions.setBlockState(
							context.chunk,
							pos,
							pos.getX() == this.boundingBox.minX() ||
							pos.getX() == this.boundingBox.maxX() ||
							pos.getY() == this.y() ||
							pos.getY() == this.boundingBox.maxY() ||
							pos.getZ() == this.boundingBox.minZ() ||
							pos.getZ() == this.boundingBox.maxZ()
								? mainBlock.get(pos)
								: BlockStates.AIR,
							Block.UPDATE_CLIENTS
						);
					}
				}
			}
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
			int x = this.x();
			int y = this.y();
			int z = this.z();
			if (this.decorators != null && contains(chunkBox, x, y, z)) {
				ConfiguredFeature<?, ?> feature = this.decorators.randomObject(random.nextLong());
				if (feature != null) {
					feature.place(world, chunkGenerator, new MojangPermuter(Permuter.permute(world.getSeed() ^ 0x265B4B7BF1BC7786L, x, y, z)), new BlockPos(x, y, z));
				}
			}
		}

		/**
		BlockBox.contains(int, int, int) was added in 1.19.4.
		in 1.19.2, only BlockBox.contains(Vec3i) existed.
		*/
		public static boolean contains(BoundingBox box, int x, int y, int z) {
			return x >= box.minX() && x <= box.maxX() && y >= box.minY() && y <= box.maxY() && z >= box.minZ() && z <= box.maxZ();
		}

		@Override
		@MustBeInvokedByOverriders
		public void addDecorations(LabyrinthLayout layout) {
			this.support = layout.random.nextBoolean() && !layout.isSharingFloor(this);
		}

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
			LAVA_BIT = 1 << 1,
			WATER_BIT = 1 << 2,
			RADIUS_SHIFT = 3,
			RADIUS_MASK = 0b11000;

		public PitDungeonPiece(
			StructurePieceType type,
			int x,
			int y,
			int z,
			Holder<Structure> owningStructure,
			int paletteIndex,
			int innerRadius,
			RandomGenerator random
		) {
			super(type, 0, new BoundingBox(x - innerRadius - 1, y - 2, z - innerRadius - 1, x + innerRadius + 1, y, z + innerRadius + 1), owningStructure, paletteIndex);
			this.setToRandomType(random);
			this.setInnerRadius(innerRadius);
		}

		public PitDungeonPiece(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
			super(type, context, nbt);
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
		public void postProcess(
			WorldGenLevel world,
			StructureManager structureAccessor,
			ChunkGenerator chunkGenerator,
			RandomSource random,
			BoundingBox chunkBox,
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
			return this.boundingBox().maxY();
		}
	}

	public static abstract class ChestDungeonPiece extends DecorationDungeonPiece {

		public long seed;

		public ChestDungeonPiece(
			StructurePieceType type,
			int length,
			BoundingBox boundingBox,
			Holder<Structure> owningStructure,
			int paletteIndex,
			Direction facing,
			long seed
		) {
			super(type, length, boundingBox, owningStructure, paletteIndex);
			this.setOrientation(facing);
			this.seed = seed;
		}

		public ChestDungeonPiece(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
			super(type, context, nbt);
			if (nbt.get("seed") instanceof NumericTag number) {
				this.seed = number.longValue();
			}
		}

		@Override
		@MustBeInvokedByOverriders
		public void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
			super.addAdditionalSaveData(context, nbt);
			nbt.putLong("seed", this.seed);
		}

		public void initChest(BlockPos pos, ChestBlockEntity chest) {
			Identifier identifier = BigGlobeMod.modID("chests/advanced_dungeon");
			chest.setLootTable(

				ResourceKey.create(Registries.LOOT_TABLE, identifier),

				this.seed
			);
		}

		@Override
		public Coordinator coordinator(WorldGenLevel world, BoundingBox limit) {
			return super.coordinator(world, limit).rotate1x(Directions.rotationOf(Direction.NORTH, this.getOrientation()));
		}

		@Override
		public Coordinator coordinator(RawGenerationStructurePiece.Context context) {
			return super.coordinator(context).rotate1x(Directions.rotationOf(Directions.POSITIVE_X, this.getOrientation()));
		}
	}

	public static abstract class SpawnerDungeonPiece extends DecorationDungeonPiece {

		public final Holder<EntityType<?>> spawnerType;

		public SpawnerDungeonPiece(
			StructurePieceType type,
			int length,
			BoundingBox boundingBox,
			Holder<Structure> owningStructure,
			int paletteIndex,
			Holder<EntityType<?>> spawnerType
		) {
			super(type, length, boundingBox, owningStructure, paletteIndex);
			this.spawnerType = spawnerType;
		}

		public SpawnerDungeonPiece(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
			super(type, context, nbt);
			String id = nbt.get("entityType") instanceof StringTag string ? NbtVersions.stringValue(string) : "minecraft:zombie";
			this.spawnerType = RegistryVersions.getEntry(BuiltInRegistries.ENTITY_TYPE, ResourceKey.create(Registries.ENTITY_TYPE, IdentifierVersions.create(id)));
		}

		@Override
		@MustBeInvokedByOverriders
		public void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
			super.addAdditionalSaveData(context, nbt);
			nbt.putString("entityType", UnregisteredObjectException.getID(this.spawnerType).toString());
		}

		public void initSpawner(BlockPos pos, SpawnerBlockEntity spawner) {
			spawner.setEntityId(this.spawnerType.value(), new Permuter(Permuter.permute(0x61DE982B73AD4955L, pos)).mojang());
		}
	}

	public static abstract class HallDungeonPiece extends DungeonPiece implements HallPiece, RawGenerationStructurePiece {

		public static final int BARS_BIT = 1 << 1;

		public byte sidewaysness;

		public HallDungeonPiece(
			StructurePieceType type,
			int chainLength,
			BoundingBox boundingBox,
			Holder<Structure> owningStructure,
			int paletteIndex
		) {
			super(type, chainLength, boundingBox, owningStructure, paletteIndex);
		}

		public HallDungeonPiece(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
			super(type, context, nbt);
			if (nbt.get("side") instanceof NumericTag number) {
				this.sidewaysness = number.byteValue();
			}
		}

		@Override
		@MustBeInvokedByOverriders
		public void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
			super.addAdditionalSaveData(context, nbt);
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
		public Coordinator coordinator(WorldGenLevel world, BoundingBox limit) {
			return super.coordinator(world, limit).rotate1x(Directions.rotationOf(Directions.POSITIVE_X, this.getOrientation()));
		}

		@Override
		public Coordinator coordinator(Context context) {
			return super.coordinator(context).rotate1x(Directions.rotationOf(Directions.POSITIVE_X, this.getOrientation()));
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
		}
	}

	public static abstract class DecorationDungeonPiece extends DungeonPiece implements DecorationPiece {

		public DecorationDungeonPiece(
			StructurePieceType type,
			int length,
			BoundingBox boundingBox,
			Holder<Structure> owningStructure,
			int paletteIndex
		) {
			super(type, length, boundingBox, owningStructure, paletteIndex);
		}

		public DecorationDungeonPiece(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
			super(type, context, nbt);
		}
	}

	public static record Palette(
		@DefaultDouble(IRandomList.DEFAULT_WEIGHT) double weight,
		ColumnRestriction restrictions,
		IRandomList<@UseName("block") Holder<Block>> main,
		IRandomList<@UseName("block") Holder<Block>> slab,
		IRandomList<@UseName("block") Holder<Block>> stairs,
		IRandomList<@UseName("block") Holder<Block>> wall
	)
		implements IRestrictedListElement {

		public static final AutoCoder<Palette> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Palette.class);

		@SuppressWarnings("deprecation")
		public static Palette
			COBBLE = new Palette(
			IRandomList.DEFAULT_WEIGHT,
			ColumnRestriction.EMPTY,
			new RandomAccessConstantWeightRandomList<>(List.of(Blocks.COBBLESTONE.builtInRegistryHolder(), Blocks.MOSSY_COBBLESTONE.builtInRegistryHolder()), 1.0D),
			new RandomAccessConstantWeightRandomList<>(List.of(Blocks.COBBLESTONE_SLAB.builtInRegistryHolder(), Blocks.MOSSY_COBBLESTONE_SLAB.builtInRegistryHolder()), 1.0D),
			new RandomAccessConstantWeightRandomList<>(List.of(Blocks.COBBLESTONE_STAIRS.builtInRegistryHolder(), Blocks.MOSSY_COBBLESTONE_STAIRS.builtInRegistryHolder()), 1.0D),
			new RandomAccessConstantWeightRandomList<>(List.of(Blocks.COBBLESTONE_WALL.builtInRegistryHolder(), Blocks.MOSSY_COBBLESTONE_WALL.builtInRegistryHolder()), 1.0D)
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

		public CoordinateSupplier<BlockState> stairsSupplier(Half half, Direction facing, StairsShape shape) {
			return (
				BlockStateSupplier.forBlocks(this.stairs)
					.with(StairBlock.HALF, half)
					.with(StairBlock.FACING, facing)
					.with(StairBlock.SHAPE, shape)
			);
		}

		public CoordinateSupplier<BlockState> wallSupplier(WallSide north, WallSide east, WallSide south, WallSide west, boolean up) {
			return (
				BlockStateSupplier.forBlocks(this.wall)
					.with(WallBlockVersions.NORTH_SHAPE, north)
					.with(WallBlockVersions.EAST_SHAPE, east)
					.with(WallBlockVersions.SOUTH_SHAPE, south)
					.with(WallBlockVersions.WEST_SHAPE, west)
			);
		}

		public CoordinateSupplier<BlockState> barsSupplier(boolean north, boolean east, boolean south, boolean west) {
			return (
				new SingleStateSupplier(Blocks.IRON_BARS.defaultBlockState())
					.with(CrossCollisionBlock.NORTH, north)
					.with(CrossCollisionBlock.EAST, east)
					.with(CrossCollisionBlock.SOUTH, south)
					.with(CrossCollisionBlock.WEST, west)
			);
		}

		public CoordinateSupplier<BlockState> air() {
			return new SingleStateSupplier(BlockStates.AIR);
		}
	}

	public static interface BlockStateSupplier extends CoordinateSupplier<BlockState> {

		public static BlockStateSupplier forBlocks(IRandomList<Holder<Block>> blocks) {
			return (
				blocks.size() == 1
					? new SingleStateSupplier(blocks.get(0).value().defaultBlockState())
					: new RandomListBlockStateSupplier(blocks)
			);
		}

		public abstract <C extends Comparable<C>> BlockStateSupplier with(Property<C> property, C value);
	}

	public static class RandomListBlockStateSupplier implements BlockStateSupplier {

		public IRandomList<Holder<Block>> blocks;
		public List<Object> properties;

		public RandomListBlockStateSupplier(IRandomList<Holder<Block>> blocks) {
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
		public BlockState get(BlockPos.MutableBlockPos pos) {
			BlockState state = this.blocks.getRandomElement(Permuter.permute(0xFE1FCB62BC2A3608L, pos)).value().defaultBlockState();
			List<Object> properties = this.properties;
			for (int index = 0, size = properties.size(); index < size; index += 2) {
				Property property = (Property<?>)(properties.get(index));
				if (state.hasProperty(property)) {
					Comparable value = (Comparable<?>)(properties.get(index + 1));
					state = state.setValue(property, value);
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
			if (this.state.hasProperty(property)) {
				this.state = this.state.setValue(property, value);
			}
			return this;
		}

		@Override
		public BlockState get(BlockPos.MutableBlockPos pos) {
			return this.state;
		}
	}
}