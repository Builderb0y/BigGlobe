package builderb0y.bigglobe.scripting;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.random.RandomGenerator;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.BlockArgumentParser.BlockResult;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.state.property.Property;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.StructurePool.Projection;
import net.minecraft.tag.FluidTags;
import net.minecraft.tag.TagKey;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryList;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@SuppressWarnings({ "unused", "StaticMethodOnlyUsedInOneClass" })
public class Wrappers {

	public static interface TagWrapper<T> extends Iterable<T> {

		public abstract T random(RandomGenerator random);
	}

	public static record BlockTagKey(TagKey<Block> key) implements TagWrapper<Block> {

		public static final TypeInfo TYPE = type(BlockTagKey.class);
		public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(BlockTagKey.class, "of", String.class, BlockTagKey.class);

		public static BlockTagKey of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
			return of(id);
		}

		public static BlockTagKey of(String id) {
			return new BlockTagKey(TagKey.of(Registry.BLOCK_KEY, new Identifier(id)));
		}

		@Override
		public Block random(RandomGenerator random) {
			Optional<RegistryEntryList.Named<Block>> list = Registry.BLOCK.getEntryList(this.key);
			if (list.isEmpty()) throw new RuntimeException("Block tag does not exist: " + this.key.id());
			Optional<RegistryEntry<Block>> block = list.get().getRandom(new MojangPermuter(random.nextLong()));
			if (block.isEmpty()) throw new RuntimeException("Block tag is empty: " + this.key.id());
			return block.get().value();
		}

		@Override
		public Iterator<Block> iterator() {
			Optional<RegistryEntryList.Named<Block>> list = Registry.BLOCK.getEntryList(this.key);
			if (list.isEmpty()) throw new RuntimeException("Block tag does not exist: " + this.key.id());
			return list.get().stream().map(RegistryEntry::value).iterator();
		}
	}

	public static class BlockWrapper {

		public static final TypeInfo TYPE = type(Block.class);
		public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(BlockWrapper.class, "getBlock", String.class, Block.class);

		public static Block getBlock(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
			return getBlock(id);
		}

		public static Block getBlock(String id) {
			return Registry.BLOCK.get(new Identifier(id));
		}

		@SuppressWarnings("deprecation")
		public static boolean isIn(Block block, BlockTagKey key) {
			return block.getRegistryEntry().isIn(key.key);
		}

		public static BlockState getDefaultState(Block block) {
			return block.getDefaultState();
		}
	}

	public static class BlockStateWrapper {

		public static final TypeInfo TYPE = type(BlockState.class);
		public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(BlockStateWrapper.class, "getState", String.class, BlockState.class);

		public static BlockState getState(MethodHandles.Lookup caller, String name, Class<?> type, String id) throws CommandSyntaxException {
			BlockResult result = BlockArgumentParser.block(Registry.BLOCK, id, false);
			Set<Property<?>> remaining = new HashSet<>(result.blockState().getProperties());
			remaining.removeAll(result.properties().keySet());
			if (!remaining.isEmpty()) {
				ScriptLogger.LOGGER.warn("Missing properties for state " + id + ": " + remaining);
			}
			return result.blockState();
		}

		public static BlockState getState(String id) throws CommandSyntaxException {
			//this method will be called only if the string is non-constant.
			//for performance reasons, we will skip properties checking here.
			return BlockArgumentParser.block(Registry.BLOCK, id, false).blockState();
		}

		public static boolean isIn(BlockState state, BlockTagKey key) {
			return state.isIn(key.key);
		}

		public static Block getBlock(BlockState state) {
			return state.getBlock();
		}

		public static boolean isAir(BlockState state) {
			return state.isAir();
		}

		public static boolean isReplaceable(BlockState state) {
			return state.getMaterial().isReplaceable();
		}

		public static boolean blocksLight(BlockState state) {
			return state.isOpaque();
		}

		public static boolean hasCollision(BlockState state) {
			return !state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).isEmpty();
		}

		public static boolean hasFullCubeCollision(BlockState state) {
			return Block.isShapeFullCube(state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN));
		}

		public static BlockState rotate(BlockState state, int rotation) {
			return switch (rotation) {
				case  90 -> state.rotate(BlockRotation.CLOCKWISE_90);
				case 180 -> state.rotate(BlockRotation.CLOCKWISE_180);
				case 270 -> state.rotate(BlockRotation.COUNTERCLOCKWISE_90);
				default  -> state;
			};
		}

		public static BlockState mirror(BlockState state, String axis) {
			if (axis.length() == 1) {
				char c = axis.charAt(0);
				if (c == 'x') return state.mirror(BlockMirror.FRONT_BACK);
				if (c == 'z') return state.mirror(BlockMirror.LEFT_RIGHT);
			}
			return state;
		}

		public static @Nullable Comparable<?> getProperty(BlockState state, String name) {
			Property<?> property = state.getBlock().getStateManager().getProperty(name);
			if (property == null) return null;
			Comparable<?> value = state.get(property);
			if (value instanceof StringIdentifiable e) {
				value = e.asString();
			}
			return value;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public static BlockState with(BlockState state, String name, Comparable<?> value) {
			Property<?> property = state.getBlock().getStateManager().getProperty(name);
			if (property == null) return state;
			if (value instanceof String string) {
				value = property.parse(string).orElse(null);
				if (value == null) return state;
			}
			if (!property.getType().isInstance(value)) return state;
			return state.with((Property)(property), (Comparable)(value));
		}

		public static boolean canPlaceAt(WorldWrapper world, BlockState state, int x, int y, int z) {
			return world.getBlockState(x, y, z).getMaterial().isReplaceable() && state.canPlaceAt(world.world, world.pos.set(x, y, z));
		}

		public static boolean hasWater(BlockState state) {
			return state.getFluidState().isIn(FluidTags.WATER);
		}

		public static boolean hasLava(BlockState state) {
			return state.getFluidState().isIn(FluidTags.LAVA);
		}
	}

	public static record BiomeEntry(RegistryEntry<Biome> biome) {

		public static final TypeInfo TYPE = type(BiomeEntry.class);
		public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(BiomeEntry.class, "of", String.class, BiomeEntry.class);

		public static BiomeEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
			return of(id);
		}

		public static BiomeEntry of(String id) {
			return new BiomeEntry(
				BigGlobeMod
				.getCurrentServer()
				.getRegistryManager()
				.get(Registry.BIOME_KEY)
				.entryOf(RegistryKey.of(Registry.BIOME_KEY, new Identifier(id)))
			);
		}

		public boolean isIn(BiomeTagKey key) {
			return this.biome.isIn(key.key);
		}

		public float temperature() {
			return this.biome.value().getTemperature();
		}

		public float downfall() {
			return this.biome.value().getDownfall();
		}

		public String precipitation() {
			return this.biome.value().getPrecipitation().asString();
		}

		@Override
		public int hashCode() {
			return this.biome.getKey().orElseThrow().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || (
				obj instanceof BiomeEntry that &&
				this.biome.getKey().orElseThrow().equals(that.biome.getKey().orElseThrow())
			);
		}

		@Override
		public String toString() {
			return "Biome: { " + this.biome.getKey().orElseThrow().getValue() + " }";
		}
	}

	public static record BiomeTagKey(TagKey<Biome> key) implements TagWrapper<BiomeEntry> {

		public static final TypeInfo TYPE = type(BiomeTagKey.class);
		public static final MethodInfo
			RANDOM   = method(ACC_PUBLIC, BiomeTagKey.class, "random", BiomeEntry.class, RandomGenerator.class),
			ITERATOR = method(ACC_PUBLIC, BiomeTagKey.class, "iterator", Iterator.class);
		public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(BiomeTagKey.class, "of", String.class, BiomeTagKey.class);

		public static BiomeTagKey of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
			return of(id);
		}

		public static BiomeTagKey of(String name) {
			return new BiomeTagKey(TagKey.of(Registry.BIOME_KEY, new Identifier(name)));
		}

		@Override
		public BiomeEntry random(RandomGenerator random) {
			Optional<RegistryEntryList.Named<Biome>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(Registry.BIOME_KEY).getEntryList(this.key);
			if (list.isEmpty()) throw new RuntimeException("Biome tag does not exist: " + this.key.id());
			Optional<RegistryEntry<Biome>> biome = list.get().getRandom(new MojangPermuter(random.nextLong()));
			if (biome.isEmpty()) throw new RuntimeException("Biome tag is empty: " + this.key.id());
			return new BiomeEntry(biome.get());
		}

		@Override
		public Iterator<BiomeEntry> iterator() {
			Optional<RegistryEntryList.Named<Biome>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(Registry.BIOME_KEY).getEntryList(this.key);
			if (list.isEmpty()) throw new RuntimeException("Biome tag does not exist: " + this.key.id());
			return list.get().stream().map(BiomeEntry::new).iterator();
		}
	}

	public static record ConfiguredFeatureEntry(RegistryEntry<ConfiguredFeature<?, ?>> entry) {

		public static final TypeInfo TYPE = type(ConfiguredFeatureEntry.class);
		public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(ConfiguredFeatureEntry.class, "of", String.class, ConfiguredFeatureEntry.class);

		public static ConfiguredFeatureEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
			return of(id);
		}

		public static ConfiguredFeatureEntry of(String id) {
			return new ConfiguredFeatureEntry(
				BigGlobeMod
				.getCurrentServer()
				.getRegistryManager()
				.get(Registry.CONFIGURED_FEATURE_KEY)
				.entryOf(RegistryKey.of(Registry.CONFIGURED_FEATURE_KEY, new Identifier(id)))
			);
		}

		public boolean isIn(ConfiguredFeatureTagKey tag) {
			return this.entry.isIn(tag.key);
		}

		@Override
		public int hashCode() {
			return this.entry.getKey().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || (
				obj instanceof ConfiguredFeatureEntry that &&
				this.entry.getKey().equals(that.entry.getKey())
			);
		}
	}

	public static record ConfiguredFeatureTagKey(TagKey<ConfiguredFeature<?, ?>> key) implements TagWrapper<ConfiguredFeatureEntry> {

		public static final TypeInfo TYPE = type(ConfiguredFeatureTagKey.class);
		public static final MethodInfo
			RANDOM = method(ACC_PUBLIC, ConfiguredFeatureTagKey.class, "random", ConfiguredFeatureEntry.class, RandomGenerator.class),
			ITERATOR = method(ACC_PUBLIC, ConfiguredFeatureTagKey.class, "iterator", Iterator.class);
		public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(ConfiguredFeatureTagKey.class, "of", String.class, ConfiguredFeatureTagKey.class);

		public static ConfiguredFeatureTagKey of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
			return of(id);
		}

		public static ConfiguredFeatureTagKey of(String id) {
			return new ConfiguredFeatureTagKey(TagKey.of(Registry.CONFIGURED_FEATURE_KEY, new Identifier(id)));
		}

		@Override
		public ConfiguredFeatureEntry random(RandomGenerator random) {
			Optional<RegistryEntryList.Named<ConfiguredFeature<?, ?>>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(Registry.CONFIGURED_FEATURE_KEY).getEntryList(this.key);
			if (list.isEmpty()) throw new RuntimeException("Biome tag does not exist: " + this.key.id());
			Optional<RegistryEntry<ConfiguredFeature<?, ?>>> feature = list.get().getRandom(new MojangPermuter(random.nextLong()));
			if (feature.isEmpty()) throw new RuntimeException("Biome tag is empty: " + this.key.id());
			return new ConfiguredFeatureEntry(feature.get());
		}

		@Override
		public Iterator<ConfiguredFeatureEntry> iterator() {
			Optional<RegistryEntryList.Named<ConfiguredFeature<?, ?>>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(Registry.CONFIGURED_FEATURE_KEY).getEntryList(this.key);
			if (list.isEmpty()) throw new RuntimeException("Biome tag does not exist: " + this.key.id());
			return list.get().stream().map(ConfiguredFeatureEntry::new).iterator();
		}
	}

	public static class WorldWrapper {

		public static final TypeInfo TYPE = type(WorldWrapper.class);

		public final StructureWorldAccess world;
		public final BlockPos.Mutable pos;
		public final Permuter permuter;
		public final WorldColumn biomeColumn;

		public WorldWrapper(StructureWorldAccess world, Permuter permuter) {
			this.world       = world;
			this.pos         = new BlockPos.Mutable();
			this.permuter    = permuter;
			this.biomeColumn = WorldColumn.forWorld(world, 0, 0);
		}

		public long getSeed() {
			return this.world.getSeed();
		}

		public BlockState getBlockState(int x, int y, int z) {
			return this.world.getBlockState(this.pos.set(x, y, z));
		}

		public void setBlockState(int x, int y, int z, BlockState state) {
			this.world.setBlockState(this.pos.set(x, y, z), state, Block.NOTIFY_ALL);
			if (!state.getFluidState().isEmpty()) {
				this.world.createAndScheduleFluidTick(
					this.pos,
					state.getFluidState().getFluid(),
					state.getFluidState().getFluid().getTickRate(this.world)
				);
			}
		}

		public boolean placeBlockState(int x, int y, int z, BlockState state) {
			return SingleBlockFeature.place(this.world, this.pos.set(x, y, z), state, SingleBlockFeature.IS_REPLACEABLE);
		}

		public void fillBlockState(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {
			int tmp;
			if (maxX < minX) { tmp = minX; minX = maxX; maxX = tmp; }
			if (maxY < minY) { tmp = minY; minY = maxY; maxY = tmp; }
			if (maxZ < minZ) { tmp = minZ; minZ = maxZ; maxZ = tmp; }
			for (int z = minZ; z <= maxZ; z++) {
				for (int x = minX; x <= maxX; x++) {
					for (int y = minY; y <= maxY; y++) {
						this.setBlockState(x, y, z, state);
					}
				}
			}
		}

		public boolean placeFeature(int x, int y, int z, ConfiguredFeatureEntry feature) {
			return feature.entry.value().generate(
				this.world,
				((ServerChunkManager)(this.world.getChunkManager())).getChunkGenerator(),
				this.permuter.mojang(),
				this.pos.set(x, y, z)
			);
		}

		public BiomeEntry getBiome(int x, int y, int z) {
			this.biomeColumn.setPos(x, z);
			return new BiomeEntry(this.biomeColumn.getBiome(y));
		}

		public boolean isYLevelValid(int y) {
			return !this.world.isOutOfHeightLimit(y);
		}

		public @Nullable NbtCompound getBlockData(int x, int y, int z) {
			BlockEntity blockEntity = this.world.getBlockEntity(this.pos.set(x, y, z));
			return blockEntity == null ? null : blockEntity.createNbtWithIdentifyingData();
		}

		public void setBlockData(int x, int y, int z, NbtCompound nbt) {
			BlockEntity blockEntity = this.world.getBlockEntity(this.pos.set(x, y, z));
			if (blockEntity != null) {
				this.doSetBlockData(blockEntity, nbt);
			}
		}

		public void mergeBlockData(int x, int y, int z, NbtCompound nbt) {
			BlockEntity blockEntity = this.world.getBlockEntity(this.pos.set(x, y, z));
			if (blockEntity != null) {
				NbtCompound oldData = blockEntity.createNbtWithIdentifyingData();
				NbtCompound newData = oldData.copy().copyFrom(nbt);
				if (!oldData.equals(newData)) {
					this.doSetBlockData(blockEntity, newData);
				}
			}
		}

		public void doSetBlockData(BlockEntity blockEntity, NbtCompound nbt) {
			blockEntity.readNbt(nbt);
			blockEntity.markDirty();
			if (this.world instanceof World world) {
				BlockState state = this.world.getBlockState(this.pos);
				world.updateListeners(this.pos, state, state, Block.NOTIFY_ALL);
			}
		}

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + ": { " + this.world + " }";
		}
	}

	public static record StructureStartWrapper(RegistryEntry<Structure> entry, StructureStart start, BlockBox box) {

		public static final TypeInfo TYPE = TypeInfo.of(StructureStartWrapper.class);

		public static StructureStartWrapper of(RegistryEntry<Structure> entry, StructureStart start) {
			int
				minX = Integer.MAX_VALUE,
				minY = Integer.MAX_VALUE,
				minZ = Integer.MAX_VALUE,
				maxX = Integer.MIN_VALUE,
				maxY = Integer.MIN_VALUE,
				maxZ = Integer.MIN_VALUE;
			for (StructurePiece child : start.getChildren()) {
				BlockBox box = child.getBoundingBox();
				minX = Math.min(minX, box.getMinX());
				minY = Math.min(minY, box.getMinY());
				minZ = Math.min(minZ, box.getMinZ());
				maxX = Math.max(maxX, box.getMaxX());
				maxY = Math.max(maxY, box.getMaxY());
				maxZ = Math.max(maxZ, box.getMaxZ());
			}
			return new StructureStartWrapper(entry, start, new BlockBox(minX, minY, minZ, maxX, maxY, maxZ));
		}

		public int minX() { return this.box.getMinX(); }
		public int minY() { return this.box.getMinY(); }
		public int minZ() { return this.box.getMinZ(); }
		public int maxX() { return this.box.getMaxX(); }
		public int maxY() { return this.box.getMaxY(); }
		public int maxZ() { return this.box.getMaxZ(); }

		public StructureEntry structure() {
			return new StructureEntry(this.entry);
		}

		public List<StructurePiece> pieces() {
			return this.start.getChildren();
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || (
				obj instanceof StructureStartWrapper that &&
				this.start.equals(that.start)
			);
		}

		@Override
		public int hashCode() {
			return this.start.hashCode();
		}

		@Override
		public String toString() {
			return "StructureStart" + this.pieces();
		}
	}

	public static class StructurePieceWrapper {

		public static final TypeInfo TYPE = TypeInfo.of(StructurePiece.class);

		public static int minX(StructurePiece piece) { return piece.getBoundingBox().getMinX(); }
		public static int minY(StructurePiece piece) { return piece.getBoundingBox().getMinY(); }
		public static int minZ(StructurePiece piece) { return piece.getBoundingBox().getMinZ(); }
		public static int maxX(StructurePiece piece) { return piece.getBoundingBox().getMaxX(); }
		public static int maxY(StructurePiece piece) { return piece.getBoundingBox().getMaxY(); }
		public static int maxZ(StructurePiece piece) { return piece.getBoundingBox().getMaxZ(); }

		public static StructurePieceType type(StructurePiece piece) {
			return piece.getType();
		}

		public static boolean hasPreferredTerrainHeight(StructurePiece piece) {
			return piece instanceof PoolStructurePiece pool && pool.getPoolElement().getProjection() == Projection.RIGID;
		}

		public static int preferredTerrainHeight(StructurePiece piece) {
			int y = piece.getBoundingBox().getMinY();
			return piece instanceof PoolStructurePiece pool ? pool.getGroundLevelDelta() + y : y;
		}
	}

	public static class StructurePieceTypeWrapper {

		public static final TypeInfo TYPE = type(StructurePieceType.class);
		public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(StructurePieceTypeWrapper.class, "of", String.class, StructurePieceType.class);

		public static StructurePieceType of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
			return of(id);
		}

		public static StructurePieceType of(String id) {
			return Registry.STRUCTURE_PIECE.get(new Identifier(id));
		}
	}

	public static record StructureEntry(RegistryEntry<Structure> entry) {

		public static final TypeInfo TYPE = TypeInfo.of(StructureEntry.class);
		public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(StructureEntry.class, "of", String.class, StructureEntry.class);

		public static StructureEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
			return of(id);
		}

		public static StructureEntry of(String id) {
			return new StructureEntry(
				BigGlobeMod
				.getCurrentServer()
				.getRegistryManager()
				.get(Registry.STRUCTURE_KEY)
				.entryOf(RegistryKey.of(Registry.STRUCTURE_KEY, new Identifier(id)))
			);
		}

		public boolean isIn(StructureTagKey tag) {
			return this.entry.isIn(tag.key);
		}

		public StructureType<?> type() {
			return this.entry.value().getType();
		}

		public String generationStep() {
			return this.entry.value().getFeatureGenerationStep().asString();
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || (
				obj instanceof StructureEntry that &&
				this.entry.getKey().orElseThrow().equals(that.entry.getKey().orElseThrow())
			);
		}

		@Override
		public int hashCode() {
			return this.entry.getKey().orElseThrow().hashCode();
		}

		@Override
		public String toString() {
			return "Structure: { " + this.entry.getKey().orElseThrow().getValue() + " }";
		}
	}

	public static class StructureTypeWrapper {

		public static final TypeInfo TYPE = type(StructureType.class);
		public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(StructureTypeWrapper.class, "of", String.class, StructureType.class);

		public static StructureType<?> of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
			return of(id);
		}

		public static StructureType<?> of(String id) {
			return Registry.STRUCTURE_TYPE.get(new Identifier(id));
		}
	}

	public static record StructureTagKey(TagKey<Structure> key) implements TagWrapper<StructureEntry> {

		public static final TypeInfo TYPE = TypeInfo.of(StructureTagKey.class);
		public static final MethodInfo
			RANDOM = method(ACC_PUBLIC, StructureTagKey.class, "random", StructureTagKey.class, RandomGenerator.class),
			ITERATOR = method(ACC_PUBLIC, StructureTagKey.class, "iterator", Iterator.class);
		public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(StructureTagKey.class, "of", String.class, StructureTagKey.class);

		public static StructureTagKey of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
			return of(id);
		}

		public static StructureTagKey of(String id) {
			return new StructureTagKey(TagKey.of(Registry.STRUCTURE_KEY, new Identifier(id)));
		}

		@Override
		public StructureEntry random(RandomGenerator random) {
			Optional<RegistryEntryList.Named<Structure>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(Registry.STRUCTURE_KEY).getEntryList(this.key);
			if (list.isEmpty()) throw new RuntimeException("Structure tag does not exist: " + this.key.id());
			Optional<RegistryEntry<Structure>> feature = list.get().getRandom(new MojangPermuter(random.nextLong()));
			if (feature.isEmpty()) throw new RuntimeException("Structure tag is empty: " + this.key.id());
			return new StructureEntry(feature.get());
		}

		@Override
		public Iterator<StructureEntry> iterator() {
			Optional<RegistryEntryList.Named<Structure>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(Registry.STRUCTURE_KEY).getEntryList(this.key);
			if (list.isEmpty()) throw new RuntimeException("Structure tag does not exist: " + this.key.id());
			return list.get().stream().map(StructureEntry::new).iterator();
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || (
				obj instanceof StructureTagKey that &&
				this.key.id().equals(that.key.id())
			);
		}

		@Override
		public int hashCode() {
			return this.key.id().hashCode();
		}

		@Override
		public String toString() {
			return "StructureTag: { " + this.key.id() + " }";
		}
	}
}