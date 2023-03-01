package builderb0y.bigglobe.trees;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.block.enums.StairShape;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.decoders.RecordDecoder;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.registration.UnregisteredObjectException;

public class TreeRegistry {

	public static final Logger LOGGER = LogManager.getLogger(BigGlobeMod.MODNAME + "/TreeRegistry");

	public static final RegistryKey<Registry<Entry>> REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.modID("trees"));
	public static final Registry<Entry> REGISTRY = BigGlobeMod.newRegistry(REGISTRY_KEY);

	public static final RegistryKey<Entry>
		OAK              = RegistryKey.of(REGISTRY_KEY, BigGlobeMod.mcID("oak"             )),
		SPRUCE           = RegistryKey.of(REGISTRY_KEY, BigGlobeMod.mcID("spruce"          )),
		BIRCH            = RegistryKey.of(REGISTRY_KEY, BigGlobeMod.mcID("birch"           )),
		JUNGLE           = RegistryKey.of(REGISTRY_KEY, BigGlobeMod.mcID("jungle"          )),
		ACACIA           = RegistryKey.of(REGISTRY_KEY, BigGlobeMod.mcID("acacia"          )),
		DARK_OAK         = RegistryKey.of(REGISTRY_KEY, BigGlobeMod.mcID("dark_oak"        )),
		MANGROVE         = RegistryKey.of(REGISTRY_KEY, BigGlobeMod.mcID("mangrove"        )),
		AZALEA           = RegistryKey.of(REGISTRY_KEY, BigGlobeMod.mcID("azalea"          )),
		FLOWERING_AZALEA = RegistryKey.of(REGISTRY_KEY, BigGlobeMod.mcID("flowering_azalea")),
		CRIMSON          = RegistryKey.of(REGISTRY_KEY, BigGlobeMod.mcID("crimson"         )),
		WARPED           = RegistryKey.of(REGISTRY_KEY, BigGlobeMod.mcID("warped"          ));

	public static void init() {
		Path treeDirectory = FabricLoader.getInstance().getConfigDir().resolve("bigglobe").resolve("trees");
		if (!Files.exists(treeDirectory)) {
			LOGGER.info("config/bigglobe/trees does not exist. Loading defaults...");
			populateDefaults(treeDirectory);
		}
		load(treeDirectory);
		LOGGER.info("Loaded " + REGISTRY.size() + " trees from config folder.");
		TreeSpecialCases.init();
		SaplingGrowHandler.init();
	}

	public static void populateDefaults(Path treeDirectory) {
		for (Identifier identifier : new Identifier[] {
			BigGlobeMod.mcID("oak"),
			BigGlobeMod.mcID("spruce"),
			BigGlobeMod.mcID("birch"),
			BigGlobeMod.mcID("jungle"),
			BigGlobeMod.mcID("acacia"),
			BigGlobeMod.mcID("dark_oak"),
			BigGlobeMod.mcID("mangrove"),
			BigGlobeMod.mcID("azalea"),
			BigGlobeMod.mcID("flowering_azalea"),
		}) {
			String inPath = "/builderb0y/bigglobe/trees/default_configs/" + identifier.getNamespace() + '/' + identifier.getPath() + ".json";
			Path namespacePath = treeDirectory.resolve(identifier.getNamespace());
			try { Files.createDirectories(namespacePath); }
			catch (IOException ioException) { throw new UncheckedIOException(ioException); }
			Path pathPath = namespacePath.resolve(identifier.getPath() + ".json");
			try (
				InputStream in = BigGlobeMod.class.getResourceAsStream(inPath);
				OutputStream out = Files.newOutputStream(pathPath);
			) {
				in.transferTo(out);
			}
			catch (IOException ioException) {
				throw new UncheckedIOException(ioException);
			}
		}
	}

	public static void load(Path treeDirectory) {
		try {
			Files.list(treeDirectory).forEach(namespacePath -> {
				if (Files.isDirectory(namespacePath)) try {
					String namespaceName = namespacePath.getFileName().toString();
					Files.list(namespacePath).forEach(pathPath -> {
						String pathName = pathPath.getFileName().toString();
						if (pathName.endsWith(".json")) {
							pathName = pathName.substring(0, pathName.length() - ".json".length());
							Identifier identifier = new Identifier(namespaceName, pathName);
							try (Reader reader = Files.newBufferedReader(pathPath)) {
								JsonElement element = JsonParser.parseReader(reader);
								Entry entry = BigGlobeAutoCodec.AUTO_CODEC.decode(Entry.LOADER_CODER, element, JsonOps.INSTANCE);
								populateMissingStates(identifier, entry);
								Registry.register(REGISTRY, identifier, entry);
							}
							catch (IOException ioException) {
								throw AutoCodecUtil.rethrow(ioException);
							}
							catch (DecodeException decodeException) {
								LOGGER.error("Error while parsing " + identifier + ':', decodeException);
							}
						}
					});
				}
				catch (IOException ioException) {
					throw AutoCodecUtil.rethrow(ioException);
				}
			});
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	public static void populateMissingStates(Identifier identifier, Entry entry) {
		//quick check to see if we need to do anything or not.
		if (entry.states.size() != Type.VALUES.length) {
			for (Type type : Type.VALUES) {
				if (!entry.states.containsKey(type)) {
					LOGGER.warn(identifier + " is missing " + type.lowerCaseName);
					entry.states.put(type, BlockStates.AIR);
				}
			}
		}
	}

	@AddPseudoField(name = "feature", getter = "getFeatureID")
	@UseCoder(name = "CODER", usage = MemberUsage.FIELD_CONTAINS_HANDLER)
	public static class Entry {

		public static final AutoCoder<Entry> LOADER_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(
			new ReifiedType<@UseCoder(name = "INSTANCE", in = RecordDecoder.Factory.class, usage = MemberUsage.FIELD_CONTAINS_FACTORY) Entry>() {}
		);
		public static final Codec<Entry> LOADER_CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(LOADER_CODER);
		public static final AutoCoder<Entry> CODER = BigGlobeAutoCodec.AUTO_CODEC.wrapDFUCodec(REGISTRY.getCodec(), false);

		public final EnumMap<Type, BlockState> states;
		public final transient @Nullable RegistryKey<ConfiguredFeature<?, ?>> feature;

		public Entry(EnumMap<Type, BlockState> states, @Nullable Identifier feature) {
			this.states = states;
			this.feature = feature == null ? null : RegistryKey.of(Registry.CONFIGURED_FEATURE_KEY, feature);
		}

		public @VerifyNullable Identifier getFeatureID() {
			return this.feature == null ? null : this.feature.getValue();
		}

		public RegistryKey<Entry> getRegistryKey() {
			return REGISTRY.getKey(this).orElseThrow(() -> new UnregisteredObjectException("Unregistered TreeRegistry Entry"));
		}

		public Identifier getID() {
			Identifier id = REGISTRY.getId(this);
			if (id != null) return id;
			else throw new UnregisteredObjectException("Unregistered TreeRegistry Entry");
		}

		public static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, T value) {
			return state.contains(property) ? state.with(property, value) : state;
		}

		public BlockState getDefaultState(Type type) {
			BlockState state = this.states.get(type);
			if (state != null) return state;
			else throw new NoSuchElementException(Objects.toString(REGISTRY.getId(this), "<unregistered TreeRegistry Entry>") + " is missing " + type.lowerCaseName);
		}

		public Block getBlock(Type type) {
			return this.getDefaultState(type).getBlock();
		}

		public BlockState getSapling(int stage) {
			BlockState state = this.getDefaultState(Type.SAPLING);
			state = with(state, SaplingBlock.STAGE, stage);
			return state;
		}

		public BlockState getPottedSapling() {
			return this.getDefaultState(Type.POTTED_SAPLING);
		}

		public BlockState getLog(Direction.Axis axis) {
			BlockState state = this.getDefaultState(Type.LOG);
			state = with(state, PillarBlock.AXIS, axis);
			return state;
		}

		public BlockState getStrippedLog(Direction.Axis axis) {
			BlockState state = this.getDefaultState(Type.STRIPPED_LOG);
			state = with(state, PillarBlock.AXIS, axis);
			return state;
		}

		public BlockState getWood(Direction.Axis axis) {
			BlockState state = this.getDefaultState(Type.WOOD);
			state = with(state, PillarBlock.AXIS, axis);
			return state;
		}

		public BlockState getStrippedWood(Direction.Axis axis) {
			BlockState state = this.getDefaultState(Type.STRIPPED_WOOD);
			state = with(state, PillarBlock.AXIS, axis);
			return state;
		}

		public BlockState getPlanks() {
			return this.getDefaultState(Type.PLANKS);
		}

		public BlockState getLeaves(int distance, boolean persistent) {
			BlockState state = this.getDefaultState(Type.LEAVES);
			state = with(state, LeavesBlock.DISTANCE, distance);
			state = with(state, LeavesBlock.PERSISTENT, persistent);
			return state;
		}

		public BlockState getSlab(SlabType type) {
			BlockState state = this.getDefaultState(Type.SLAB);
			state = with(state, SlabBlock.TYPE, type);
			return state;
		}

		public BlockState getStair(Direction facing, BlockHalf half, StairShape shape) {
			BlockState state = this.getDefaultState(Type.STAIRS);
			state = with(state, StairsBlock.FACING, facing);
			state = with(state, StairsBlock.HALF, half);
			state = with(state, StairsBlock.SHAPE, shape);
			return state;
		}

		public BlockState getFence(boolean north, boolean east, boolean south, boolean west) {
			BlockState state = this.getDefaultState(Type.FENCE);
			state = with(state, HorizontalConnectingBlock.NORTH, north);
			state = with(state, HorizontalConnectingBlock.EAST, east);
			state = with(state, HorizontalConnectingBlock.SOUTH, south);
			state = with(state, HorizontalConnectingBlock.WEST, west);
			return state;
		}

		public BlockState getFenceGate(Direction facing, boolean open, boolean powered, boolean inWall) {
			BlockState state = this.getDefaultState(Type.FENCE_GATE);
			state = with(state, HorizontalFacingBlock.FACING, facing);
			state = with(state, FenceGateBlock.OPEN, open);
			state = with(state, FenceGateBlock.POWERED, powered);
			state = with(state, FenceGateBlock.IN_WALL, inWall);
			return state;
		}
	}

	public static enum Type {
		SAPLING,
		POTTED_SAPLING,
		LOG,
		STRIPPED_LOG,
		WOOD,
		STRIPPED_WOOD,
		PLANKS,
		LEAVES,
		SLAB,
		STAIRS,
		FENCE,
		FENCE_GATE;

		public static final Type[] VALUES = values();

		public final String lowerCaseName = this.name().toLowerCase(Locale.ROOT);
	}
}