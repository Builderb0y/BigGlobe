package builderb0y.bigglobe.chunkgen;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.hash.Hashing;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.annotations.DefaultObject.DefaultObjectMode;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.coders.RecordCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.StringData;
import builderb0y.autocodec.data.UnknownData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeContext.RootDecodePath;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState.ColorScript;
import builderb0y.bigglobe.blockdefs.BlockStates;
import builderb0y.bigglobe.chunkgen.QuadHolder.QuadColumn;
import builderb0y.bigglobe.chunkgen.QuadHolder.QuadList;
import builderb0y.bigglobe.chunkgen.perSection.SectionUtil;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.LitSegment;
import builderb0y.bigglobe.chunkgen.scripted.Layer;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.VerifyDivisibleBy16;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry.DelayedCompileable;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnRandomToBooleanScript;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToBooleanScript.Catcher;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.*;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.columns.scripted.dependencies.CyclicDependencyAnalyzer;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyDepthSorter;
import builderb0y.bigglobe.columns.scripted.traits.TraitLoader;
import builderb0y.bigglobe.columns.scripted.traits.WorldTrait;
import builderb0y.bigglobe.columns.scripted.traits.WorldTraitProvider;
import builderb0y.bigglobe.columns.scripted.traits.WorldTraits;
import builderb0y.bigglobe.compat.ValkyrienSkiesCompat;
import builderb0y.bigglobe.compat.distanthorizons.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.features.RockReplacerFeature.ConfiguredRockReplacerFeature;
import builderb0y.bigglobe.features.dispatch.FeatureDispatchers;
import builderb0y.bigglobe.mixins.Heightmap_StorageAccess;
import builderb0y.bigglobe.mixins.StructureAccessor_WorldAccess;
import builderb0y.bigglobe.mixins.StructureStart_ChildrenGetter;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.overriders.ColumnValueOverrider;
import builderb0y.bigglobe.overriders.Overrider;
import builderb0y.bigglobe.overriders.Overrider.SortedOverriders;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper.AutoOverride;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper.Coordination;
import builderb0y.bigglobe.spawning.SpawnTweakers;
import builderb0y.bigglobe.structures.RawGenerationStructure;
import builderb0y.bigglobe.structures.RawGenerationStructure.RawGenerationStructurePiece;
import builderb0y.bigglobe.structures.ScriptStructures;
import builderb0y.bigglobe.structures.management.EmptyStructureLocator;
import builderb0y.bigglobe.structures.management.FlatStructureLocator;
import builderb0y.bigglobe.structures.management.StructureLocator;
import builderb0y.bigglobe.structures.management.StructureLocator.WhatToSearchFor.ManyStructuresOneBox;
import builderb0y.bigglobe.util.*;
import builderb0y.bigglobe.util.Tripwire;
import builderb0y.bigglobe.util.WorldOrChunk.ChunkDelegator;
import builderb0y.bigglobe.util.WorldOrChunk.WorldDelegator;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.bigglobe.versions.SpawnEntryVersions.*;

@AddPseudoField("biome_source")
@AddPseudoField("decodeContext")
@UseCoder(name = "createCoder", usage = MemberUsage.METHOD_IS_FACTORY)
public class BigGlobeScriptedChunkGenerator extends ChunkGenerator implements DelayedCompileable {

	public static final boolean WORLD_SLICES = false;

	public static final MapCodec<BigGlobeScriptedChunkGenerator> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(BigGlobeScriptedChunkGenerator.class);
	public static final VarHandle STRUCTURE_LOCATOR;

	static {
		try {
			STRUCTURE_LOCATOR = MethodHandles.lookup().findVarHandle(BigGlobeScriptedChunkGenerator.class, "structureLocator", StructureLocator.class).withInvokeExactBehavior();
		}
		catch (Exception exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
		ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
			if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.dependencyGraphs) {
				for (ServerLevel world : server.getAllLevels()) {
					if (world.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
						Identifier worldID = world.dimension().identifier();
						DependencyDepthSorter.start(generator.compiledWorldTraits, generator.columnEntryRegistry.registries.getRegistry(BigGlobeDynamicRegistries.COLUMN_VALUE_REGISTRY_KEY), "server " + worldID.getNamespace() + ' ' + worldID.getPath());
					}
				}
			}
		});
	}

	public final @VerifyNullable String reload_preset;
	public final @VerifyNullable String reload_dimension;
	public static record Height(
		@VerifyDivisibleBy16 int min_y,
		@VerifyDivisibleBy16 @VerifySorted(greaterThan = "min_y") int max_y,
		@VerifyNullable Integer sea_level
	) {}
	public final Height height;
	public final Holder<Layer> layer;
	public final FeatureDispatchers feature_dispatcher;
	public final DelayedEntryList<Overrider> overriders;
	public final @DefaultObject(name = "DEFAULT", in = GameMechanics.class, mode = DefaultObjectMode.FIELD) GameMechanics game_mechanics;

	public static record GameMechanics(
		ColumnRandomToBooleanScript.@VerifyNullable Catcher spawn_point,
		@VerifyNullable ColorOverrides colors,
		@VerifyNullable Holder<ConfiguredFeature<?, ?>> grass_bonemeal_feature,
		@VerifyNullable NetherOverrides nether,
		@VerifyNullable ResourceKey<Level> creaking_time_reference,
		@VerifyNullable EndOverrides end,
		@DefaultObject(name = "DEFAULT", in = LodOverrides.class, mode = DefaultObjectMode.FIELD) LodOverrides lods
	) {

		public static final GameMechanics DEFAULT = new GameMechanics(
			null,
			null,
			null,
			null,
			null,
			null,
			LodOverrides.DEFAULT
		);

		public static record ColorOverrides(
			ColorScript.@VerifyNullable Catcher grass,
			ColorScript.@VerifyNullable Catcher foliage,
			ColorScript.@VerifyNullable Catcher water
		) {

			@Environment(EnvType.CLIENT)
			public ColorScript.@Nullable Catcher forColorResolver(ColorResolver resolver) {
				if (resolver == BiomeColors.  GRASS_COLOR_RESOLVER) return this.grass;
				if (resolver == BiomeColors.FOLIAGE_COLOR_RESOLVER) return this.foliage;
				if (resolver == BiomeColors.  WATER_COLOR_RESOLVER) return this.water;
				return null;
			}
		}

		public static record NetherOverrides(
			boolean place_portal_at_high_y_level,
			boolean prevent_roof_exploration
		) {}

		public static record EndOverrides(
			Spawning spawning,
			InnerGateways inner_gateways,
			OuterGateways outer_gateways
		) {

			public static record Spawning(
				int @VerifySizeRange(min = 3, max = 3) [] location,
				boolean obsidian_platform
			) {}

			public static record InnerGateways(
				double radius,
				int height
			) {}

			public static record OuterGateways(
				double min_radius,
				double max_radius,
				double step,
				Catcher condition
			) {}
		}

		public static record LodOverrides(
			@DefaultBoolean(true) boolean lod_rendering_enabled,
			@DefaultBoolean(true) boolean can_chunkload,
			@DefaultFloat(1.0F) float view_distance_multiplier,
			@DefaultFloat(1.0F) float fog_density_multiplier,
			@VerifyNullable Float fog_height_scale,
			@VerifyNullable Double fog_base_height
		) {

			public static final LodOverrides DEFAULT = new LodOverrides(
				true,
				true,
				1.0F,
				1.0F,
				null,
				null
			);
		}
	}

	public final @VerifyNullable Identifier world_traits;
	public transient Map<Holder<WorldTrait>, WorldTraitProvider> loadedWorldTraits;
	public transient WorldTraits compiledWorldTraits;
	public transient ColumnEntryRegistry columnEntryRegistry;

	public final SpawnTweakers spawnTweakers;

	public transient SortedOverriders actualOverriders;
	public transient long columnSeed;
	public transient boolean seedSet;
	public transient Pattern displayPattern;
	public transient List<ColumnValueInfo> rootDebugDisplay;

	public transient ChunkGeneratorStructureState structureState;
	public transient boolean structuresEnabled;
	@Deprecated //don't reference directly. use getter instead.
	public transient StructureLocator structureLocator;

	public BigGlobeScriptedChunkGenerator(
		DecodeContext<?>            decodeContext,
		@VerifyNullable String      reload_preset,
		@VerifyNullable String      reload_dimension,
		Height                      height,
		Holder<Layer>               layer,
		FeatureDispatchers          feature_dispatcher,
		BiomeSource                 biome_source,
		DelayedEntryList<Overrider> overriders,
		GameMechanics               game_mechanics,
		@VerifyNullable Identifier  world_traits,
		SpawnTweakers spawnTweakers
	)
	throws VerifyException {
		super(biome_source);
		if (biome_source instanceof ScriptedColumnBiomeSource source) {
			source.generator = this;
		}
		this.reload_preset      = reload_preset;
		this.reload_dimension   = reload_dimension;
		this.height             = height;
		this.layer              = layer;
		this.feature_dispatcher = feature_dispatcher;
		this.overriders         = overriders;
		this.game_mechanics     = game_mechanics;
		this.world_traits       = world_traits;
		this.spawnTweakers      = spawnTweakers;
		this.loadedWorldTraits  = TraitLoader.load(world_traits, decodeContext);
		this.rootDebugDisplay   = Collections.emptyList();
	}

	@Hidden //copy constructor.
	public BigGlobeScriptedChunkGenerator(BigGlobeScriptedChunkGenerator from) {
		super(copyBiomeSource(from.biomeSource));
		this.reload_preset       = from.reload_preset;
		this.reload_dimension    = from.reload_dimension;
		this.columnEntryRegistry = from.columnEntryRegistry;
		this.height              = from.height;
		this.layer               = from.layer;
		this.feature_dispatcher  = from.feature_dispatcher;
		this.overriders          = from.overriders;
		this.game_mechanics      = from.game_mechanics;
		this.world_traits        = from.world_traits;
		this.loadedWorldTraits   = from.loadedWorldTraits;
		this.compiledWorldTraits = from.compiledWorldTraits;
		this.setCompiledWorldTraits(from.compiledWorldTraits);
		this.rootDebugDisplay    = Collections.emptyList();
		this.structuresEnabled   = from.structuresEnabled;
		this.spawnTweakers       = from.spawnTweakers;
	}

	@Override
	public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
		this.columnEntryRegistry = registry;
		this.setCompiledWorldTraits(registry.traitManager.createTraits(this.loadedWorldTraits));
		this.checkCyclicDependencies();
	}

	public void checkCyclicDependencies() {
		this
		.columnEntryRegistry
		.registries
		.getRegistry(BigGlobeDynamicRegistries.COLUMN_VALUE_REGISTRY_KEY)
		.streamEntries()
		.forEach(new CyclicDependencyAnalyzer(this.compiledWorldTraits));
	}

	public static BiomeSource copyBiomeSource(BiomeSource source) {
		return source instanceof ScriptedColumnBiomeSource scripted ? new ScriptedColumnBiomeSource(scripted.script, scripted.all_possible_biomes, scripted.biomeRegistry) : source;
	}

	public void setCompiledWorldTraits(WorldTraits traits) {
		this.compiledWorldTraits = traits;
	}

	/**
	public API.
	*/
	public BigGlobeScriptedChunkGenerator copy() {
		return new BigGlobeScriptedChunkGenerator(this);
	}

	/**
	public API.

	important note: because this seed is sent to clients,
	it is highly recommended to ensure that it cannot be
	used to derive the seed used to create the world.
	one way to do this is to put the seed through a
	secure hash function like SHA-256 before calling
	this method. alternatively, the seed could just be
	chosen completely randomly, and independently of the
	original world seed.
	*/
	public BigGlobeScriptedChunkGenerator copyWithSeed(long seed) {
		BigGlobeScriptedChunkGenerator copy = this.copy();
		copy.columnSeed = seed;
		copy.seedSet = true;
		return copy;
	}

	/**
	public API.

	please only call this on a newly-copied chunk generator.
	setting the world traits of a generator that is
	already in-use is considered undefined behavior.
	*/
	public void setWorldTraits(JsonObject jsonTraits) {
		this.setCompiledWorldTraits(
			this.columnEntryRegistry.traitManager.createTraits(
				this.loadedWorldTraits = TraitLoader.loadFromCode(jsonTraits)
			)
		);
		this.checkCyclicDependencies();
	}

	/**
	public API.
	*/
	public void setStructuresEnabled(boolean structuresEnabled) {
		this.structuresEnabled = structuresEnabled;
		this.structureLocator = null;
	}

	@Override
	public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSetRegistry, RandomState noiseConfig, long seed) {
		if (!this.seedSet) {
			//make it impossible to reverse-engineer the seed from information sent to the client.
			this.columnSeed = Hashing.sha256().hashLong(seed).asLong();
			this.seedSet = true;
		}

		return this.structureState = super.createState(structureSetRegistry, noiseConfig, seed);
	}

	public StructureLocator structureLocator() {
		//fast path: plain access memory semantics are fine.
		StructureLocator locator = this.structureLocator;
		if (locator == null) {
			//slow path: need to ensure that we never have more than one locator,
			//and that any call to this method, no matter which thread,
			//always sees the same locator.
			StructureLocator newLocator = this.structuresEnabled ? new FlatStructureLocator(this.structureState) : EmptyStructureLocator.INSTANCE;
			StructureLocator existing = (StructureLocator)(STRUCTURE_LOCATOR.compareAndExchange(this, (StructureLocator)(null), newLocator));
			locator = existing == null ? newLocator : existing;
		}
		return locator;
	}

	/**
	used by pseudo-field.
	*/
	public DecodeContext<?> decodeContext() {
		return null;
	}

	public BiomeSource biome_source() {
		return this.biomeSource;
	}

	@Override
	public void validate() {
		//no-op.
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, BigGlobeMod.modID("scripted"), CODEC);
		Registry.register(BuiltInRegistries.BIOME_SOURCE, BigGlobeMod.modID("scripted"), ScriptedColumnBiomeSource.CODEC);
	}

	public static AutoCoder<BigGlobeScriptedChunkGenerator> createCoder(FactoryContext<BigGlobeScriptedChunkGenerator> context) {
		AutoCoder<BigGlobeScriptedChunkGenerator> coder = context.forceCreateCoder(RecordCoder.Factory.INSTANCE);
		return new NamedCoder<BigGlobeScriptedChunkGenerator>("jar-reloading AutoCoder for BigGlobeScriptedChunkGenerator") {

			@Override
			public <T_Encoded> @Nullable BigGlobeScriptedChunkGenerator decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
				StringData dimension = context.forceGetMember("reload_dimension").tryAsString();
				if (dimension != null) {
					StringData preset = context.forceGetMember("reload_preset").tryAsString();
					String presetName = preset != null ? preset.value : "bigglobe";
					JsonElement json = this.getDimension(presetName, dimension.value);
					return new DecodeContext<>(context.autoCodec, null, RootDecodePath.INSTANCE, new UnknownData<>(JsonOps.INSTANCE, json), context.ops).decodeWith(coder);
				}
				return context.decodeWith(coder);
			}

			@Override
			public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, BigGlobeScriptedChunkGenerator> context) throws EncodeException {
				return context.encodeWith(coder);
			}

			public JsonElement getDimension(String preset, String dimension) throws DecodeException {
				BigGlobeMod.LOGGER.info("Reading " + dimension + " chunk generator from mod jar.");
				JsonElement element = this.getJson("/data/bigglobe/worldgen/world_preset/" + preset + ".json");
				for (String key : new String[] { "dimensions", dimension, "generator" }) {
					if (element instanceof JsonObject object) element = object.get(key);
					else throw new DecodeException(() -> "Could not find dimension " + dimension + " in mod jar!");
				}
				return element;
			}

			public JsonElement getJson(String path) throws DecodeException {
				try (
					Reader reader = new InputStreamReader(
						Objects.requireNonNull(
							BigGlobeMod.class.getResourceAsStream(path),
							path
						),
						StandardCharsets.UTF_8
					)
				) {
					return JsonParser.parseReader(reader);
				}
				catch (Exception exception) {
					throw new DecodeException(exception);
				}
			}

			@Override
			public @Nullable Stream<String> getKeys() {
				return coder.getKeys();
			}
		};
	}

	public ScriptedColumn newColumn(LevelHeightAccessor world, int x, int z, Hints hints) {
		return this.columnEntryRegistry.columnFactory.create(
			new ScriptedColumn.Params(
				this.columnSeed,
				x,
				z,
				world,
				hints,
				this.compiledWorldTraits
			)
		);
	}

	public ScriptedColumn newColumn(int x, int z, Hints hints) {
		return this.columnEntryRegistry.columnFactory.create(
			new ScriptedColumn.Params(
				this.columnSeed,
				x,
				z,
				this.height.min_y,
				this.height.max_y,
				hints,
				this.compiledWorldTraits
			)
		);
	}

	public ConfiguredColumnFactory configuredColumnFactory(Hints hints) {
		return new ConfiguredColumnFactory(
			this.columnEntryRegistry.columnFactory,
			new WorldInfo(this),
			hints
		);
	}

	public ConfiguredColumnFactory configuredColumnFactory(LevelHeightAccessor world, Hints hints) {
		return new ConfiguredColumnFactory(
			this.columnEntryRegistry.columnFactory,
			new WorldInfo(
				this.columnSeed,
				HeightLimitViewVersions.getMinY(world),
				HeightLimitViewVersions.getMaxY(world),
				this.compiledWorldTraits
			),
			hints
		);
	}

	public ScriptedColumnLookup.Impl newColumnLookup(LevelHeightAccessor world, Hints hints) {
		return new ScriptedColumnLookup.Impl(this.configuredColumnFactory(world, hints));
	}

	@Override
	public MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	public SortedOverriders getOverriders() {
		if (this.actualOverriders == null) {
			this.actualOverriders = new SortedOverriders(this);
		}
		return this.actualOverriders;
	}

	@Override
	public void applyCarvers(
		WorldGenRegion chunkRegion,
		long seed,
		RandomState noiseConfig,
		BiomeManager biomeAccess,
		StructureManager structureAccessor,
		ChunkAccess chunk

	) {
		//no-op.
	}

	@Override
	public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState noiseConfig, ChunkAccess chunk) {
		//no-op.
	}

	@Override
	public WeightedList<SpawnerData> getMobsAt(Holder<Biome> biome, StructureManager accessor, MobCategory group, BlockPos pos) {
		if (((StructureAccessor_WorldAccess)(accessor)).bigglobe_getWorld() instanceof ServerLevel serverLevel) {
			WeightedList<SpawnerData> spawns = this.structureLocator().getMobSpawns(
				new StructureLocator.Context(
					this,
					this.configuredColumnFactory(ColumnUsage.GENERIC.normalHints()),
					serverLevel.getLevel()
				),
				pos,
				group
			);
			return spawns != null ? spawns : this.spawnTweakers.getSpawnEntries(this, pos, group, biome, Permuter.from(serverLevel.getRandom()));
		}
		else {
			if (Tripwire.isEnabled()) {
				Tripwire.logWithStackTrace("getMobsAt() called with a StructureManager whose world is not a ServerLevel.");
			}
			return super.getMobsAt(biome, accessor, group, pos);
		}
	}

	@Override
	@SuppressWarnings("RedundantCast")
	public void spawnOriginalMobs(WorldGenRegion region) {
		//copy-pasted from NoiseChunkGenerator.
		ChunkPos chunkPos = region.getCenter();
		BlockPos chunkCenter = new BlockPos(
			chunkPos.getMinBlockX(),
			this.getHeight(
				this.newColumn(region, chunkPos.getMinBlockX(), chunkPos.getMinBlockZ(), ColumnUsage.HEIGHTMAP.maybeDhHints()),
				Types.OCEAN_FLOOR_WG,
				region
			),
			chunkPos.getMinBlockZ()
		);
		Holder<Biome> registryEntry = region.getBiome(chunkCenter);
		WorldgenRandom chunkRandom = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
		chunkRandom.setDecorationSeed(region.getSeed(), chunkPos.getMinBlockX(), chunkPos.getMinBlockZ());

		//inlined from SpawnHelper.populateEntities(region, registryEntry, chunkPos, chunkRandom);
		//reason: default method only considers biome and does not query chunk generator for mob list.
		MobSpawnSettings spawnSettings = registryEntry.value().getMobSettings();
		WeightedList<SpawnerData> pool = this.spawnTweakers.getSpawnEntries(this, chunkCenter, MobCategory.CREATURE, registryEntry, Permuter.from(chunkRandom));
		if (!pool.isEmpty()) {
			int i = chunkPos.getMinBlockX();
			int j = chunkPos.getMinBlockZ();

			while (chunkRandom.nextFloat() < spawnSettings.getCreatureProbability()) {
				Optional<SpawnerData> optional = pool.getRandom(chunkRandom);
				if (!optional.isEmpty()) {
					SpawnerData spawnEntry = (SpawnerData)optional.get();
					int k = minCount(spawnEntry) + chunkRandom.nextInt(1 + maxCount(spawnEntry) - minCount(spawnEntry));
					SpawnGroupData entityData = null;
					int l = i + chunkRandom.nextInt(16);
					int m = j + chunkRandom.nextInt(16);
					int n = l;
					int o = m;

					for (int p = 0; p < k; p++) {
						boolean bl = false;

						for (int q = 0; !bl && q < 4; q++) {
							BlockPos blockPos = NaturalSpawner.getTopNonCollidingPos((ServerLevelAccessor)region, type(spawnEntry), l, m);
							if (type(spawnEntry).canSummon() && SpawnPlacements.isSpawnPositionOk(type(spawnEntry), region, blockPos)) {
								float f = type(spawnEntry).getWidth();
								double d = Mth.clamp((double)l, (double)i + (double)f, (double)i + 16.0 - (double)f);
								double e = Mth.clamp((double)m, (double)j + (double)f, (double)j + 16.0 - (double)f);
								if (
									!region.noCollision(type(spawnEntry).getSpawnAABB(d, (double)blockPos.getY(), e))
									|| !SpawnPlacements.checkSpawnRules(
										type(spawnEntry),
										region,
										EntitySpawnReason.CHUNK_GENERATION,
										BlockPos.containing(d, (double)blockPos.getY(), e),
										region.getRandom()
									)
								) {
									continue;
								}

								Entity entity;
								try {
									entity = type(spawnEntry).create(((ServerLevelAccessor)region).getLevel(), EntitySpawnReason.NATURAL);
								}
								catch (Exception var27) {
									BigGlobeMod.LOGGER.warn("Failed to create mob", (Throwable)var27);
									continue;
								}

								if (entity == null) {
									continue;
								}

								entity.snapTo(d, (double)blockPos.getY(), e, chunkRandom.nextFloat() * 360.0F, 0.0F);
								if (entity instanceof Mob mobEntity && mobEntity.checkSpawnRules(region, EntitySpawnReason.CHUNK_GENERATION) && mobEntity.checkSpawnObstruction(region)) {
									entityData = mobEntity.finalizeSpawn(region, region.getCurrentDifficultyAt(mobEntity.blockPosition()), EntitySpawnReason.CHUNK_GENERATION, entityData);
									region.addFreshEntityWithPassengers(mobEntity);
									bl = true;
								}
							}

							l += chunkRandom.nextInt(5) - chunkRandom.nextInt(5);

							for (m += chunkRandom.nextInt(5) - chunkRandom.nextInt(5); l < i || l >= i + 16 || m < j || m >= j + 16; m = o + chunkRandom.nextInt(5) - chunkRandom.nextInt(5)) {
								l = n + chunkRandom.nextInt(5) - chunkRandom.nextInt(5);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public int getGenDepth() {
		return this.height.max_y - this.height.min_y;
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(
		Blender blender,
		RandomState noiseConfig,
		StructureManager structureAccessor,
		ChunkAccess chunk
	) {
		if (ValkyrienSkiesCompat.isInShipyard(chunk.getPos())) {
			return CompletableFuture.completedFuture(chunk);
		}
		if (WORLD_SLICES && (chunk.getPos().x() & 3) != 0) {
			return CompletableFuture.completedFuture(chunk);
		}
		boolean distantHorizons = DistantHorizonsCompat.isOnDistantHorizonThread();
		Hints hints = ColumnUsage.RAW_GENERATION.maybeDhHints(distantHorizons);
		Holder<ColumnValueOverrider.Entry>[] overriders = this.getOverriders().rawColumnValues.overriders();
		ScriptStructures[] structures = ScriptStructures.getStructures(
			this,
			this.configuredColumnFactory(chunk, hints),
			structureAccessor,
			chunk.getPos(),
			this.getOverriders().rawColumnValues
		);
		ScriptedColumn.Params params = new ScriptedColumn.Params(
			this.columnSeed,
			0,
			0,
			HeightLimitViewVersions.getMinY(chunk),
			HeightLimitViewVersions.getMaxY(chunk),
			hints,
			this.compiledWorldTraits
		);
		return CompletableFuture.runAsync(
			() -> {
				int startX = chunk.getPos().getMinBlockX();
				int startZ = chunk.getPos().getMinBlockZ();
				int chunkMinY = HeightLimitViewVersions.getMinY(chunk);
				int chunkMaxY = HeightLimitViewVersions.getMaxY(chunk);
				ScriptedColumn[] columns;
				try {
					columns = this.columnEntryRegistry.chunkGeneratorColumns.take();
				}
				catch (InterruptedException exception) {
					BigGlobeMod.LOGGER.warn("Unexpected interrupt", exception);
					return;
				}
				try {

					//////////////////////////////// layers ////////////////////////////////

					BlockSegmentList[] lists = new BlockSegmentList[256];
					try (AsyncRunner async = BigGlobeThreadPool.runner(distantHorizons)) {
						for (int offsetZ = 0; offsetZ < 16; offsetZ += 2) {
							final int offsetZ_ = offsetZ;
							for (int offsetX = 0; offsetX < 16; offsetX += 2) {
								final int offsetX_ = offsetX;
								async.submit(() -> {
									int baseIndex = (offsetZ_ << 4) | offsetX_;
									int quadX = startX | offsetX_;
									int quadZ = startZ | offsetZ_;
									QuadColumn quadColumn = new QuadColumn();
									quadColumn.loadFromArray(columns, baseIndex, 16);
									quadColumn.at(params, quadX, quadZ, 1);
									for (ColumnValueInfo info : this.getOverriders().rawColumnValueDependencies) try {
										quadColumn.preComputeColumnValue(info);
									}
									catch (Throwable throwable) {
										BigGlobeMod.LOGGER.error("Exception pre-computing overrider column value: ", throwable);
									}
									for (int index = 0; index < structures.length; index++) {
										quadColumn.override(overriders[index].value().script, structures[index]);
									}
									QuadList quadList = new QuadList();
									quadList.createNew(chunkMinY, chunkMaxY);
									Layer layer = this.layer.value();
									QuadHolder.generate(quadColumn, quadList, layer);
									quadList.storeInArray(lists, baseIndex, 16);
								});
							}
						}
					}

					//////////////////////////////// compute sections to populate ////////////////////////////////

					int minFilledSectionY = Integer.MAX_VALUE;
					int maxFilledSectionY = Integer.MIN_VALUE;
					for (BlockSegmentList list : lists) {
						int size = list.size();
						for (int index = 0; index < size; index++) {
							LitSegment segment = list.get(index);
							if (!segment.value.isAir()) {
								minFilledSectionY = Math.min(minFilledSectionY, segment.minY);
								break;
							}
						}
						for (int index = size; --index >= 0; ) {
							LitSegment segment = list.get(index);
							if (!segment.value.isAir()) {
								maxFilledSectionY = Math.max(maxFilledSectionY, segment.maxY);
								break;
							}
						}
					}
					minFilledSectionY >>= 4;
					maxFilledSectionY = (maxFilledSectionY >> 4) + 1;

					//////////////////////////////// populate sections ////////////////////////////////

					Async.loop(
						BigGlobeThreadPool.executor(distantHorizons), minFilledSectionY, maxFilledSectionY, 1, (int coord) -> {
							LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(coord));
							int baseY = coord << 4;
							SectionGenerationContext context = SectionGenerationContext.forBlockCoord(chunk, section, baseY);
							BlockState centerState = lists[0x88].getOverlappingObject(baseY | 8);
							if (centerState != null) context.setAllStates(centerState, distantHorizons);
							for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
								BlockSegmentList list = lists[horizontalIndex];
								int size = list.size();
								int yIndex = list.getSegmentIndex(baseY, false);
								while (yIndex < size) {
									LitSegment segment = list.get(yIndex);
									int segmentMinY = Math.max(segment.minY - baseY, 0);
									int segmentMaxY = Math.min(segment.maxY - baseY, 15);
									if (segmentMaxY >= segmentMinY) {
										int id = context.id(segment.value);
										BitStorage storage = context.storage();
										for (int blockY = segmentMinY; blockY <= segmentMaxY; blockY++) {
											storage.set((blockY << 8) | horizontalIndex, id);
										}
									}
									yIndex++;
								}
							}
						}
					);

					//////////////////////////////// heightmaps ////////////////////////////////

					for (Types type : chunk.getPersistedStatus().heightmapsAfter()) {
						Heightmap heightmap = chunk.getOrCreateHeightmapUnprimed(type);
						@SuppressWarnings("CastToIncompatibleInterface")
						BitStorage heightmapStorage = ((Heightmap_StorageAccess)(heightmap)).bigglobe_getStorage();
						for (int horizontalIndex = 0; horizontalIndex < 256; horizontalIndex++) {
							BlockSegmentList list = lists[horizontalIndex];
							if (!list.isEmpty()) {
								int height = getHeight(list, type);
								height = Mth.clamp(height - HeightLimitViewVersions.getMinY(chunk), 0, HeightLimitViewVersions.getHeight(chunk));
								heightmapStorage.set(horizontalIndex, height);
							}
						}
					}

					//////////////////////////////// raw feature dispatchers ////////////////////////////////

					WorldWrapper worldWrapper = new WorldWrapper(
						new ChunkDelegator(chunk, this.columnSeed),
						this,
						new Permuter(Permuter.permute(this.columnSeed, chunk.getPos())),
						new Coordination(
							SymmetricOffset.IDENTITY,
							WorldUtil.chunkBox(chunk),
							WorldUtil.chunkBox(chunk)
						),
						hints
					);
					worldWrapper.overriders = new AutoOverride(
						structures,
						this.getOverriders().rawColumnValues.overriders(),
						this.getOverriders().rawColumnValueDependencies
					);
					for (ScriptedColumn column : columns) {
						worldWrapper.columns.put(ColumnPos.asLong(column.x(), column.z()), column);
					}
					int minFilledSectionY_ = minFilledSectionY;
					int maxFilledSectionY_ = maxFilledSectionY;
					ScriptedColumnLookup.GLOBAL.run(
						worldWrapper, () -> {
							if (!distantHorizons) {
								for (ConfiguredRockReplacerFeature<?> replacer : this.feature_dispatcher.getFlattenedRockReplacers()) {
									replacer.replaceRocks(this, worldWrapper, chunk, minFilledSectionY_, maxFilledSectionY_);
								}
							}
							Async.loop(
								BigGlobeThreadPool.executor(distantHorizons), HeightLimitViewVersions.getSectionMinY(chunk), HeightLimitViewVersions.getSectionMaxY(chunk), 1, (int coord) -> {
									chunk.getSection(chunk.getSectionIndexFromSectionY(coord)).recalcBlockCounts();
								}
							);
							this.generateRawStructures(chunk, structureAccessor, worldWrapper);
							this.feature_dispatcher.generateRaw(worldWrapper);
						}
					);
				}
				finally {
					this.columnEntryRegistry.chunkGeneratorColumns.add(columns);
				}
			},
			Util.backgroundExecutor()
		)
		.handle((Void result, Throwable throwable) -> {
			if (throwable != null) {
				BigGlobeMod.LOGGER.error("Exception populating noise", throwable);
			}
			return chunk;
		});
	}

	@Override
	public void applyBiomeDecoration(WorldGenLevel world, ChunkAccess chunk, StructureManager structureAccessor) {
		if (ValkyrienSkiesCompat.isInShipyard(chunk.getPos())) {
			return;
		}
		if (WORLD_SLICES && (chunk.getPos().x() & 3) != 0) {
			return;
		}
		this.generateStructures(world, chunk, structureAccessor);
		WorldWrapper worldWrapper = new WorldWrapper(
			new WorldDelegator(world),
			this,
			new Permuter(Permuter.permute(this.columnSeed, chunk.getPos())),
			new Coordination(
				SymmetricOffset.IDENTITY,
				WorldUtil.chunkBox(chunk),
				WorldUtil.surroundingChunkBox(chunk)
			),
			ColumnUsage.FEATURES.maybeDhHints()
		);
		ScriptStructures[] structures = ScriptStructures.getStructures(
			this,
			worldWrapper.getSource(),
			structureAccessor,
			chunk.getPos(),
			this.getOverriders().featureColumnValues
		);
		ScriptedColumn[] columns;
		try {
			columns = this.columnEntryRegistry.chunkGeneratorColumns.take();
		}
		catch (InterruptedException exception) {
			BigGlobeMod.LOGGER.warn("Unexpected interrupt", exception);
			return;
		}
		try {
			worldWrapper.overriders = new AutoOverride(
				structures,
				this.getOverriders().featureColumnValues.overriders(),
				this.getOverriders().featureColumnValueDependencies
			);
			try (
				AsyncConsumer<ScriptedColumn> async = new AsyncConsumer<>(
					BigGlobeThreadPool.autoExecutor(),
					(ScriptedColumn column) -> {
						worldWrapper.columns.put(ColumnPos.asLong(column.x(), column.z()), column);
					}
				)
			) {
				for (int index = 0; index < 256; index++) {
					final int index_ = index;
					async.submit(() -> {
						int x = chunk.getPos().getMinBlockX() | (index_ & 15);
						int z = chunk.getPos().getMinBlockZ() | (index_ >>> 4);
						columns[index_].setParamsUnchecked(worldWrapper.getSource().params(x, z));
						worldWrapper.overriders.override(columns[index_]);
						return columns[index_];
					});
				}
			}
			ScriptedColumnLookup.GLOBAL.run(worldWrapper, () -> this.feature_dispatcher.generateNormal(worldWrapper));
		}
		finally {
			this.columnEntryRegistry.chunkGeneratorColumns.add(columns);
		}
	}

	public void generateRawStructures(ChunkAccess chunk, StructureManager structureAccessor, ScriptedColumnLookup columns) {
		if (((StructureAccessor_WorldAccess)(structureAccessor)).bigglobe_getWorld() instanceof ServerLevelAccessor serverWorldAccess) {
			Hints hints = ColumnUsage.GENERIC.maybeDhHints();
			BoundingBox chunkBox = WorldUtil.chunkBox(chunk);
			RawGenerationStructurePiece.Context context = new RawGenerationStructurePiece.Context(chunk, this, columns, DistantHorizonsCompat.isOnDistantHorizonThread());
			Registry<Structure> structureRegistry = StructureLocator.structureRegistry(serverWorldAccess);
			this.structureLocator().getStructuresIntersecting(
				new StructureLocator.Params(
					this,
					this.configuredColumnFactory(serverWorldAccess, hints),
					serverWorldAccess.getLevel(),
					new ManyStructuresOneBox(
						this.structureLocator().allStructures(),
						chunkBox
					)
				)
			)
			.filter((StructureStartWrapper start) -> start.start().getStructure() instanceof RawGenerationStructure)
			.forEachOrdered((StructureStartWrapper start) -> {
				try {
					long structureSeed = getStructureSeed(this.columnSeed, structureRegistry.getKey(start.start().getStructure()), start);
					List<StructurePiece> children = start.start().getPieces();
					for (int pieceIndex = 0, pieceCount = children.size(); pieceIndex < pieceCount; pieceIndex++) {
						StructurePiece piece = children.get(pieceIndex);
						if (piece instanceof RawGenerationStructurePiece rawPiece && piece.getBoundingBox().intersects(chunkBox)) {
							context.pieceSeed = Permuter.permute(structureSeed, pieceIndex);
							rawPiece.generateRaw(context);
						}
					}
				}
				catch (Exception exception) {
					BigGlobeMod.LOGGER.error("Exception placing raw structure: " + start, exception);
				}
			});
		}
	}

	public void generateStructures(WorldGenLevel world, ChunkAccess chunk, StructureManager structureAccessor) {
		BoundingBox chunkBox = WorldUtil.chunkBox(chunk);
		Hints hints = ColumnUsage.GENERIC.maybeDhHints();
		this.structureLocator().getStructuresIntersecting(
			new StructureLocator.Params(
				this,
				this.configuredColumnFactory(world, hints),
				world.getLevel(),
				new ManyStructuresOneBox(
					this.structureLocator().allStructures(),
					chunkBox
				)
			)
		)
		.forEachOrdered((StructureStartWrapper start) -> {
			long structureSeed = getStructureSeed(this.columnSeed, start.originalID(), start);
			List<StructurePiece> children = start.start().getPieces();
			BoundingBox firstPieceBB = children.get(0).getBoundingBox();
			BlockPos pivot = new BlockPos(
				(firstPieceBB.minX() + firstPieceBB.maxX() + 1) >> 1,
				firstPieceBB.minY(),
				(firstPieceBB.minZ() + firstPieceBB.maxZ() + 1) >> 1
			);
			for (int pieceIndex = 0, pieceCount = children.size(); pieceIndex < pieceCount; pieceIndex++) {
				StructurePiece piece = children.get(pieceIndex);
				if (piece.getBoundingBox().intersects(chunkBox)) {
					long pieceSeed = Permuter.permute(structureSeed, pieceIndex);
					try {
						piece.postProcess(
							world,
							structureAccessor,
							this,
							new MojangPermuter(pieceSeed),
							chunkBox,
							chunk.getPos(),
							pivot
						);
					}
					catch (Exception exception) {
						BigGlobeMod.LOGGER.error("Exception placing structure piece " + piece + ':', exception);
					}
				}
			}
			try {
				start.start().getStructure().afterPlace(
					world,
					structureAccessor,
					this,
					new MojangPermuter(structureSeed),
					chunkBox,
					chunk.getPos(),
					((StructureStart_ChildrenGetter)(Object)(start.start())).bigglobe_getChildren()
				);
			}
			catch (Exception exception) {
				BigGlobeMod.LOGGER.error("Exception post-placing structure start " + start + ':', exception);
			}
		});
	}

	public static long getStructureSeed(long worldSeed, Identifier structureID, StructureStartWrapper start) {
		return Permuter.permute(
			worldSeed ^ 0x74ED298CF4DD2677L,
			structureID.hashCode(),
			start.pos().getX(),
			start.pos().getY(),
			start.pos().getZ()
		);
	}

	@Override
	public void createStructures(
		RegistryAccess registryManager,
		ChunkGeneratorStructureState placementCalculator,
		StructureManager structureAccessor,
		ChunkAccess chunk,
		StructureTemplateManager structureTemplateManager,
		ResourceKey<Level> dimension
	) {
		chunk.setAllStarts(
			this.structureLocator().getStructuresInside(
				new StructureLocator.Params(
					new StructureLocator.Context(
						this,
						this.configuredColumnFactory(chunk, ColumnUsage.GENERIC.maybeDhHints()),
						placementCalculator,
						registryManager,
						structureTemplateManager,
						chunk
					),
					new ManyStructuresOneBox(
						this.structureLocator().allStructures(),
						WorldUtil.chunkBox(chunk)
					)
				)
			)
			.map(StructureStartWrapper::start)
			.collect(
				Collectors.toMap(
					StructureStart::getStructure,
					Function.identity(),
					(StructureStart start1, StructureStart start2) -> {
						return new StructureStart(
							start1.getStructure(),
							start1.getChunkPos(),
							0,
							new PiecesContainer(
								Stream
								.concat(
									start1.getPieces().stream(),
									start2.getPieces().stream()
								)
								.toList()
							)
						);
					}
				)
			)
		);
	}

	@Override
	public void createReferences(
		WorldGenLevel world,
		StructureManager structureAccessor,
		ChunkAccess chunk
	) {
		this.structureLocator().getStructuresIntersecting(
			new StructureLocator.Params(
				this,
				this.configuredColumnFactory(chunk, ColumnUsage.GENERIC.maybeDhHints()),
				world.getLevel(),
				new ManyStructuresOneBox(
					this.structureLocator().allStructures(),
					WorldUtil.chunkBox(chunk)
				)
			)
		)
		.map(StructureStartWrapper::start)
		.filter((StructureStart start) -> {
			return start.getChunkPos().getChessboardDistance(chunk.getPos()) <= 8;
		})
		.forEachOrdered((StructureStart start) -> {
			chunk.addReferenceForStructure(start.getStructure(), start.getChunkPos().pack());
		});
	}

	public Stream<StructureStartWrapper> findNearbyStructures(
		ServerLevel world,
		HolderSet<Structure> toFind,
		BoundingBox area,
		BlockPos center
	) {
		if (!this.structuresEnabled) return Stream.empty();
		return (
			this
			.structureLocator()
			.getStructuresNearby(
				new StructureLocator.Params(
					this,
					this.configuredColumnFactory(world, ColumnUsage.GENERIC.normalHints()),
					world,
					new ManyStructuresOneBox(toFind::stream, area)
				),
				center
			)
		);
	}

	public static BoundingBox clampedExpandedBoundingBox(BlockPos center, int radius, LevelHeightAccessor height) {
		return new BoundingBox(
			center.getX() - radius,
			Math.max(center.getY() - radius, HeightLimitViewVersions.getMinY(height)),
			center.getZ() - radius,
			center.getX() + radius,
			Math.min(center.getY() + radius, HeightLimitViewVersions.getMaxY(height) - 1),
			center.getZ() + radius
		);
	}

	@Override
	public @Nullable Pair<BlockPos, Holder<Structure>> findNearestMapStructure(
		ServerLevel world,
		HolderSet<Structure> toFind,
		BlockPos center,
		int chunkRadius,
		boolean skipReferencedStructures
	) {
		return (
			this
			.findNearbyStructures(
				world,
				toFind,
				clampedExpandedBoundingBox(center, chunkRadius << 4, world),
				center
			)
			.findFirst()
			.map((StructureStartWrapper start) -> Pair.of(
				start.box().getCenter(),
				start.originalStructure()
			))
			.orElse(null)
		);
	}

	@Override
	public CompletableFuture<ChunkAccess> createBiomes(
		RandomState noiseConfig,
		Blender blender,
		StructureManager structureAccessor,
		ChunkAccess chunk
	) {
		if (!(this.biomeSource instanceof ScriptedColumnBiomeSource source)) {
			return super.createBiomes(
				noiseConfig,
				blender,
				structureAccessor,
				chunk
			);
		}
		boolean distantHorizons = DistantHorizonsCompat.isOnDistantHorizonThread();
		return CompletableFuture.runAsync(
			() -> {
				int bottomY = HeightLimitViewVersions.getMinY(chunk);
				int topY = HeightLimitViewVersions.getMaxY(chunk);
				ScriptedColumn column = this.newColumn(chunk, 0, 0, ColumnUsage.GENERIC.maybeDhHints(distantHorizons));
				for (int z = 0; z < 16; z += 4) {
					for (int x = 0; x < 16; x += 4) {
						column.setParamsUnchecked(column.params.at(chunk.getPos().getMinBlockX() | x, chunk.getPos().getMinBlockZ() | z));
						for (int y = bottomY; y < topY; y += 4) {
							LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
							PalettedContainer<Holder<Biome>> container = (PalettedContainer<Holder<Biome>>)(section.getBiomes());
							int newID = SectionUtil.id(container, source.script.get(column, y).entry);
							SectionUtil.storage(container).set(((y & 0b1100) << 2) | z | (x >>> 2), newID);
						}
					}
				}
			},
			Util.backgroundExecutor()
		)
		.handle((Void result, Throwable throwable) -> {
			if (throwable != null) {
				BigGlobeMod.LOGGER.error("Exception populating chunk biomes", throwable);
			}
			return chunk;
		});
	}

	@Override
	public int getSeaLevel() {
		Integer seaLevel = this.height.sea_level;
		return seaLevel != null ? seaLevel.intValue() : this.height.min_y;
	}

	@Override
	public int getMinY() {
		return this.height.min_y;
	}

	@Override
	public int getBaseHeight(int x, int z, Types heightmap, LevelHeightAccessor world, RandomState noiseConfig) {
		return this.getHeight(
			this.newColumn(world, x, z, ColumnUsage.HEIGHTMAP.maybeDhHints()),
			heightmap,
			world
		);
	}

	public int getHeight(ScriptedColumn column, Types heightmap, LevelHeightAccessor world) {
		BlockSegmentList list = new BlockSegmentList(HeightLimitViewVersions.getMinY(world), HeightLimitViewVersions.getMaxY(world));
		this.layer.value().emitSegments(column, list);
		return getHeight(list, heightmap);
	}

	public static int getHeight(BlockSegmentList list, Types type) {
		for (int index = list.size(); --index >= 0; ) {
			LitSegment segment = list.get(index);
			if (type.isOpaque().test(segment.value)) {
				return segment.maxY + 1;
			}
		}
		return list.minY();
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world, RandomState noiseConfig) {
		ScriptedColumn column = this.newColumn(world, x, z, ColumnUsage.GENERIC.maybeDhHints());
		BlockSegmentList list = new BlockSegmentList(HeightLimitViewVersions.getMinY(world), HeightLimitViewVersions.getMaxY(world));
		this.layer.value().emitSegments(column, list);
		BlockState[] states = list.flatten(BlockState[]::new);
		for (int index = 0, length = states.length; index < length; index++) {
			if (states[index] == null) states[index] = BlockStates.AIR;
		}
		return new NoiseColumn(HeightLimitViewVersions.getMinY(world), states);
	}

	@Override
	public void	addDebugScreenInfo(List<String> text, RandomState noiseConfig, BlockPos pos) {
		if (!this.rootDebugDisplay.isEmpty()) {
			ScriptedColumn column = this.columnEntryRegistry.columnFactory.create(new ScriptedColumn.Params(this, pos.getX(), pos.getZ(), ColumnUsage.GENERIC.normalHints()));
			for (Iterator<ColumnValueInfo> iterator = this.rootDebugDisplay.iterator(); iterator.hasNext();) {
				ColumnValueInfo entry = iterator.next();
				try {
					text.add(entry.toString() + ": " + entry.getter().invokeExact(column, pos.getY()));
				}
				catch (Throwable throwable) {
					BigGlobeMod.LOGGER.warn("Exception querying column value:", throwable);
					iterator.remove();
				}
			}
		}
	}

	public void setDisplay(String regex) {
		if (regex != null) {
			this.displayPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			this.rootDebugDisplay = (
				ScriptedColumn.getColumnValues(this.columnEntryRegistry)
				.values()
				.stream()
				.filter((ColumnValueInfo info) -> this.displayPattern.matcher(info.toString()).find())
				.sorted(Comparator.comparing(ColumnValueInfo::toString))
				.collect(Collectors.toCollection(ArrayList<ColumnValueInfo>::new)) //ensure mutable.
			);
		}
		else {
			this.displayPattern = null;
			this.rootDebugDisplay = Collections.emptyList();
		}
	}
}