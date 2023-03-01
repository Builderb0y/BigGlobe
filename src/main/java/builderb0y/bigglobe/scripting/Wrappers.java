package builderb0y.bigglobe.scripting;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
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
import net.minecraft.tag.FluidTags;
import net.minecraft.tag.TagKey;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
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

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.scripting.bytecode.FieldInfo;
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

		public static final MethodInfo
			RANDOM   = MethodInfo.findFirstMethod(BlockTagKey.class, "random"),
			ITERATOR = MethodInfo.findFirstMethod(BlockTagKey.class, "iterator");

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
		public static final MethodInfo
			IS_IN             = MethodInfo.findFirstMethod(BlockWrapper.class, "isIn"),
			GET_DEFAULT_STATE = MethodInfo.findFirstMethod(BlockWrapper.class, "getDefaultState");

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
		public static final MethodInfo
			IS_IN                   = MethodInfo.findFirstMethod(BlockStateWrapper.class, "isIn"),
			GET_BLOCK               = MethodInfo.findFirstMethod(BlockStateWrapper.class, "getBlock"),
			IS_AIR                  = MethodInfo.findFirstMethod(BlockStateWrapper.class, "isAir"),
			IS_REPLACEABLE          = MethodInfo.findFirstMethod(BlockStateWrapper.class, "isReplaceable"),
			BLOCKS_LIGHT            = MethodInfo.findFirstMethod(BlockStateWrapper.class, "blocksLight"),
			HAS_COLLISION           = MethodInfo.findFirstMethod(BlockStateWrapper.class, "hasCollision"),
			HAS_FULL_CUBE_COLLISION = MethodInfo.findFirstMethod(BlockStateWrapper.class, "hasFullCubeCollision"),
			ROTATE                  = MethodInfo.findFirstMethod(BlockStateWrapper.class, "rotate"),
			MIRROR                  = MethodInfo.findFirstMethod(BlockStateWrapper.class, "mirror"),
			GET_PROPERTY            = MethodInfo.findFirstMethod(BlockStateWrapper.class, "getProperty"),
			WITH                    = MethodInfo.findFirstMethod(BlockStateWrapper.class, "with"),
			CAN_PLACE_AT            = MethodInfo.findFirstMethod(BlockStateWrapper.class, "canPlaceAt"),
			HAS_WATER               = MethodInfo.findFirstMethod(BlockStateWrapper.class, "hasWater"),
			HAS_LAVA                = MethodInfo.findFirstMethod(BlockStateWrapper.class, "hasLava");

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
		public static final MethodInfo
			IS_IN             = MethodInfo.findFirstMethod(BiomeEntry.class, "isIn"),
			GET_TEMPERATURE   = MethodInfo.findFirstMethod(BiomeEntry.class, "getTemperature"),
			GET_DOWNFALL      = MethodInfo.findFirstMethod(BiomeEntry.class, "getDownfall"),
			GET_PRECIPITATION = MethodInfo.findFirstMethod(BiomeEntry.class, "getPrecipitation");

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

		public float getTemperature() {
			return this.biome.value().getTemperature();
		}

		public float getDownfall() {
			return this.biome.value().getDownfall();
		}

		public String getPrecipitation() {
			return this.biome.value().getPrecipitation().asString();
		}

		@Override
		public int hashCode() {
			return this.biome.getKey().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || (
				obj instanceof BiomeEntry that &&
				this.biome.getKey().equals(that.biome.getKey())
			);
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

	public static record ConfiguredFeatureTag(TagKey<ConfiguredFeature<?, ?>> key) implements TagWrapper<ConfiguredFeatureEntry> {

		public static final TypeInfo TYPE = type(ConfiguredFeatureTag.class);
		public static final MethodInfo
			RANDOM = method(ACC_PUBLIC, ConfiguredFeatureTag.class, "random", ConfiguredFeatureEntry.class, RandomGenerator.class),
			ITERATOR = method(ACC_PUBLIC, ConfiguredFeatureTag.class, "iterator", Iterator.class);
		public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(ConfiguredFeatureTag.class, "of", String.class, ConfiguredFeatureTag.class);

		public static ConfiguredFeatureTag of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
			return of(id);
		}

		public static ConfiguredFeatureTag of(String id) {
			return new ConfiguredFeatureTag(TagKey.of(Registry.CONFIGURED_FEATURE_KEY, new Identifier(id)));
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
		public static final FieldInfo
			PERMUTER          = field(ACC_PUBLIC | ACC_FINAL, WorldWrapper.class, "permuter", Permuter.class),
			COLUMN            = field(ACC_PUBLIC | ACC_FINAL, WorldWrapper.class, "column", WorldColumn.class);
		public static final MethodInfo
			GET_SEED          = MethodInfo.findFirstMethod(WorldWrapper.class, "getSeed"),
			GET_BLOCK_STATE   = MethodInfo.findFirstMethod(WorldWrapper.class, "getBlockState"),
			SET_BLOCK_STATE   = MethodInfo.findFirstMethod(WorldWrapper.class, "setBlockState"),
			PLACE_BLOCK_STATE = MethodInfo.findFirstMethod(WorldWrapper.class, "placeBlockState"),
			FILL_BLOCK_STATE  = MethodInfo.findFirstMethod(WorldWrapper.class, "fillBlockState"),
			PLACE_FEATURE     = MethodInfo.findFirstMethod(WorldWrapper.class, "placeFeature"),
			GET_BIOME         = MethodInfo.findFirstMethod(WorldWrapper.class, "getBiome"),
			IS_Y_LEVEL_VALID  = MethodInfo.findFirstMethod(WorldWrapper.class, "isYLevelValid"),
			GET_BLOCK_DATA    = MethodInfo.findFirstMethod(WorldWrapper.class, "getBlockData"),
			SET_BLOCK_DATA    = MethodInfo.findFirstMethod(WorldWrapper.class, "setBlockData"),
			MERGE_BLOCK_DATA  = MethodInfo.findFirstMethod(WorldWrapper.class, "mergeBlockData");

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
}