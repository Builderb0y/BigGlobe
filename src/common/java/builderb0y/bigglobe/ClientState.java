package builderb0y.bigglobe;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.RegistryOps.RegistryInfo;
import net.minecraft.resources.RegistryOps.RegistryInfoLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.*;

import builderb0y.autocodec.annotations.Hidden;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator.GameMechanics.ColorOverrides;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator.GameMechanics.LodOverrides;
import builderb0y.bigglobe.chunkgen.ScriptedColumnBiomeSource;
import builderb0y.bigglobe.chunkgen.scripted.Layer;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ColumnScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.columns.scripted.VoronoiSettings;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.IndirectDependencyCollector;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.traits.WorldTrait;
import builderb0y.bigglobe.columns.scripted.traits.WorldTraitProvider;
import builderb0y.bigglobe.columns.scripted.traits.WorldTraits;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;
import builderb0y.bigglobe.hyperspace.ServerPlayerWaypointManager;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.mixinInterfaces.DimensionalBlockView;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.packets.DangerousRapidsPacket;
import builderb0y.bigglobe.networking.packets.SettingsSyncS2CPacketHandler;
import builderb0y.bigglobe.networking.packets.TimeSpeedS2CPacketHandler;
import builderb0y.bigglobe.rendering2.lods.LodMesher;
import builderb0y.bigglobe.scripting.environments.ColorScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.MinecraftScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.RandomScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.bigglobe.util.ClientWorldEvents;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.input.FileScriptUsage;
import builderb0y.scripting.parsing.input.ScriptFileResolver;
import builderb0y.scripting.parsing.input.ScriptFileResolver.ResolvedInclude;
import builderb0y.scripting.parsing.input.ScriptTemplate;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.InfoHolder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ClientState {

	public static final Map<ResourceKey<Level>, ClientState> INSTANCES = new HashMap<>();
	public static final ReentrantReadWriteLock INSTANCE_LOCK = new ReentrantReadWriteLock();

	public ClientGeneratorParams generatorParams;
	public double timeSpeed = 1.0D;
	public boolean dangerousRapids;

	static {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			initClient();
		}
	}

	public static @Nullable ClientState get(ResourceKey<Level> key) {
		INSTANCE_LOCK.readLock().lock();
		try {
			return INSTANCES.get(key);
		}
		finally {
			INSTANCE_LOCK.readLock().unlock();
		}
	}

	public static @Nullable ClientState get(BlockGetter world) {
		return world != null ? get(((DimensionalBlockView)(world)).bigglobe_getDimension()) : null;
	}

	@Environment(EnvType.CLIENT)
	public static @Nullable ClientState get() {
		return get(Minecraft.getInstance().level);
	}

	public static @NotNull ClientState getOrCreate(ResourceKey<Level> key) {
		INSTANCE_LOCK.readLock().lock();
		try {
			return INSTANCES.computeIfAbsent(key, key_ -> new ClientState());
		}
		finally {
			INSTANCE_LOCK.readLock().unlock();
		}
	}

	public static @NotNull ClientState getOrCreate(@NotNull Level world) {
		return getOrCreate(world.dimension());
	}

	public static void reset() {
		INSTANCE_LOCK.writeLock().lock();
		try {
			INSTANCES.clear();
		}
		finally {
			INSTANCE_LOCK.writeLock().unlock();
		}
	}

	public static void retain(Set<ResourceKey<Level>> worlds) {
		INSTANCE_LOCK.writeLock().lock();
		try {
			INSTANCES.keySet().retainAll(worlds);
			for (ResourceKey<Level> world : worlds) {
				INSTANCES.putIfAbsent(world, new ClientState());
			}
		}
		finally {
			INSTANCE_LOCK.writeLock().unlock();
		}
	}

	public static void forEach(Consumer<ClientState> action) {
		INSTANCE_LOCK.readLock().lock();
		try {
			INSTANCES.values().forEach(action);
		}
		finally {
			INSTANCE_LOCK.readLock().unlock();
		}
	}

	/**
	called by the server to sync overworld settings to the client.
	*/
	public static void sync(ServerLevel world, ServerPlayer player) {
		BigGlobeNetwork.LOGGER.debug("Syncing ClientState to " + player);
		SettingsSyncS2CPacketHandler.INSTANCE.send(world, player);
		TimeSpeedS2CPacketHandler.INSTANCE.send(player);
		DangerousRapidsPacket.INSTANCE.send(player);
		syncWaypoints(world, player);
	}

	public static void syncWaypoints(ServerLevel world, ServerPlayer player) {
		if (PlayerWaypointManager.get(player) instanceof ServerPlayerWaypointManager manager) {
			manager.updateOnWorldChange(world);
		}
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		ClientWorldEvents.WORLD_CHANGED.register((ClientLevel oldWorld, ClientLevel newWorld) -> {
			if (newWorld == null) {
				BigGlobeMod.LOGGER.info("Resetting ClientState on disconnect.");
				reset();
			}
		});
	}

	public static void overrideColor(BlockGetter world, int x, int y, int z, ColorResolver colorResolver, CallbackInfoReturnable<Integer> callback) {
		//don't intercept for my own drawing code,
		//since it contains intentionally incorrect coordinates.
		if (LodMesher.isMeshing()) return;
		ResourceKey<Level> dimension = ((DimensionalBlockView)(world)).bigglobe_getDimension();
		if (dimension == null) return;
		ClientState state = get(dimension);
		if (state == null) return;
		ClientGeneratorParams params = state.generatorParams;
		if (params == null || params.colors == null) return;
		ColorScript.Catcher script = params.colors.forColorResolver(colorResolver);
		if (script == null) return;
		callback.setReturnValue(script.getColor(params.getColumn(x, z), y));
	}

	public static class Syncing {

		public static final AutoCoder<Syncing> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Syncing.class);

		public boolean containsLayers;
		public Map<Identifier, String> includes;
		public Map<Identifier, Tag> templates, columnEntries, voronoiSettings, decisionTrees, worldTraits, layers;
		public transient MappedRegistry<ScriptTemplate> templateRegistry = new MappedRegistry<>(BigGlobeDynamicRegistries.SCRIPT_TEMPLATE_REGISTRY_KEY, Lifecycle.experimental());
		public transient MappedRegistry<ColumnEntry> columnEntryRegistry = new MappedRegistry<>(BigGlobeDynamicRegistries.COLUMN_ENTRY_REGISTRY_KEY, Lifecycle.experimental());
		public transient MappedRegistry<VoronoiSettings> voronoiSettingsRegistry = new MappedRegistry<>(BigGlobeDynamicRegistries.VORONOI_SETTINGS_REGISTRY_KEY, Lifecycle.experimental());
		public transient MappedRegistry<DecisionTreeSettings> decisionTreeRegistry = new MappedRegistry<>(BigGlobeDynamicRegistries.DECISION_TREE_SETTINGS_REGISTRY_KEY, Lifecycle.experimental());
		public transient MappedRegistry<WorldTrait> worldTraitRegistry = new MappedRegistry<>(BigGlobeDynamicRegistries.WORLD_TRAIT_REGISTRY_KEY, Lifecycle.experimental());
		public transient MappedRegistry<Layer> layerRegistry = new MappedRegistry<>(BigGlobeDynamicRegistries.LAYER_REGISTRY_KEY, Lifecycle.experimental());

		public Syncing(
			boolean containsLayers,
			Map<Identifier, String> includes,
			Map<Identifier, Tag> templates,
			Map<Identifier, Tag> columnEntries,
			Map<Identifier, Tag> voronoiSettings,
			Map<Identifier, Tag> decisionTrees,
			Map<Identifier, Tag> worldTraits,
			Map<Identifier, Tag> layers
		) {
			this.containsLayers = containsLayers;
			this.includes = includes;
			this.templates = templates;
			this.columnEntries = columnEntries;
			this.voronoiSettings = voronoiSettings;
			this.decisionTrees = decisionTrees;
			this.worldTraits = worldTraits;
			this.layers = layers;
		}

		@Hidden
		public Syncing(BigGlobeScriptedChunkGenerator generator) {
			this(BigGlobeConfig.INSTANCE.get().lodRendering.renderingEnabled(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
			ColorOverrides colors = generator.game_mechanics.colors();
			if (colors != null || this.containsLayers) {
				IndirectDependencyCollector collector = new IndirectDependencyCollector(generator);
				if (colors != null) {
					if (colors.grass  () != null) colors.grass  ().streamDirectDependencies().forEach(collector);
					if (colors.foliage() != null) colors.foliage().streamDirectDependencies().forEach(collector);
					if (colors.water  () != null) colors.water  ().streamDirectDependencies().forEach(collector);
				}
				if (this.containsLayers) collector.accept(generator.layer);
				generator
				.columnEntryRegistry
				.registries
				.getRegistry(BigGlobeDynamicRegistries.VORONOI_SETTINGS_REGISTRY_KEY)
				.streamEntries()
				.filter((Holder<VoronoiSettings> entry) -> collector.contains(entry.value().owner()))
				.forEach(collector);
				for (Holder<? extends DependencyView> entry : collector) {
					if (entry.value() instanceof ResolvedInclude include) {
						this.includes.put(include.id(), include.source());
					}
					else if (entry.value() instanceof FileScriptUsage file) {
						this.includes.put(file.file, file.getSource());
					}
					else if (entry.value() instanceof ScriptTemplate template) {
						Registry.register(this.templateRegistry, UnregisteredObjectException.getID(entry), template);
					}
					else if (entry.value() instanceof ColumnEntry columnEntry) {
						Registry.register(this.columnEntryRegistry, UnregisteredObjectException.getID(entry), columnEntry);
					}
					else if (entry.value() instanceof VoronoiSettings voronoiSettings) {
						Registry.register(this.voronoiSettingsRegistry, UnregisteredObjectException.getID(entry), voronoiSettings);
					}
					else if (entry.value() instanceof DecisionTreeSettings decisionTree) {
						Registry.register(this.decisionTreeRegistry, UnregisteredObjectException.getID(entry), decisionTree);
					}
					else if (entry.value() instanceof WorldTrait trait) {
						Registry.register(this.worldTraitRegistry, UnregisteredObjectException.getID(entry), trait);
					}
					else if (entry.value() instanceof Layer layer) {
						Registry.register(this.layerRegistry, UnregisteredObjectException.getID(entry), layer);
					}
					else {
						throw new IllegalStateException("Unhandled dependency view type: " + entry.value());
					}
				}
				RegistryOps<Tag> ops = this.createOps(NbtOps.INSTANCE, false);
				for (Map.Entry<ResourceKey<ScriptTemplate>, ScriptTemplate> entry : this.templateRegistry.entrySet()) {
					this.templates.put(entry.getKey().identifier(), BigGlobeAutoCodec.AUTO_CODEC.encode(ScriptTemplate.CODER, entry.getValue(), ops));
				}
				for (Map.Entry<ResourceKey<ColumnEntry>, ColumnEntry> entry : this.columnEntryRegistry.entrySet()) {
					this.columnEntries.put(entry.getKey().identifier(), BigGlobeAutoCodec.AUTO_CODEC.encode(ColumnEntry.REGISTRY, entry.getValue(), ops));
				}
				for (Map.Entry<ResourceKey<VoronoiSettings>, VoronoiSettings> entry : this.voronoiSettingsRegistry.entrySet()) {
					this.voronoiSettings.put(entry.getKey().identifier(), BigGlobeAutoCodec.AUTO_CODEC.encode(VoronoiSettings.CODER, entry.getValue(), ops));
				}
				for (Map.Entry<ResourceKey<DecisionTreeSettings>, DecisionTreeSettings> entry : this.decisionTreeRegistry.entrySet()) {
					this.decisionTrees.put(entry.getKey().identifier(), BigGlobeAutoCodec.AUTO_CODEC.encode(DecisionTreeSettings.CODER, entry.getValue(), ops));
				}
				for (Map.Entry<ResourceKey<WorldTrait>, WorldTrait> entry : this.worldTraitRegistry.entrySet()) {
					this.worldTraits.put(entry.getKey().identifier(), BigGlobeAutoCodec.AUTO_CODEC.encode(WorldTrait.CODER, entry.getValue(), ops));
				}
				for (Map.Entry<ResourceKey<Layer>, Layer> entry : this.layerRegistry.entrySet()) {
					this.layers.put(entry.getKey().identifier(), BigGlobeAutoCodec.AUTO_CODEC.encode(Layer.REGISTRY, entry.getValue(), ops));
				}
			}
		}

		public void parse() throws DecodeException {
			try {
				ScriptFileResolver.OVERRIDES.set(this.includes);
				RegistryOps<Tag> ops = this.createOps(NbtOps.INSTANCE, true);
				for (Map.Entry<Identifier, Tag> entry : this.templates.entrySet()) {
					Registry.register(this.templateRegistry, entry.getKey(), BigGlobeAutoCodec.AUTO_CODEC.decode(ScriptTemplate.CODER, entry.getValue(), ops));
				}
				for (Map.Entry<Identifier, Tag> entry : this.columnEntries.entrySet()) {
					Registry.register(this.columnEntryRegistry, entry.getKey(), BigGlobeAutoCodec.AUTO_CODEC.decode(ColumnEntry.REGISTRY, entry.getValue(), ops));
				}
				for (Map.Entry<Identifier, Tag> entry : this.voronoiSettings.entrySet()) {
					Registry.register(this.voronoiSettingsRegistry, entry.getKey(), BigGlobeAutoCodec.AUTO_CODEC.decode(VoronoiSettings.CODER, entry.getValue(), ops));
				}
				for (Map.Entry<Identifier, Tag> entry : this.decisionTrees.entrySet()) {
					Registry.register(this.decisionTreeRegistry, entry.getKey(), BigGlobeAutoCodec.AUTO_CODEC.decode(DecisionTreeSettings.CODER, entry.getValue(), ops));
				}
				for (Map.Entry<Identifier, Tag> entry : this.worldTraits.entrySet()) {
					Registry.register(this.worldTraitRegistry, entry.getKey(), BigGlobeAutoCodec.AUTO_CODEC.decode(WorldTrait.CODER, entry.getValue(), ops));
				}
				for (Map.Entry<Identifier, Tag> entry : this.layers.entrySet()) {
					Registry.register(this.layerRegistry, entry.getKey(), BigGlobeAutoCodec.AUTO_CODEC.decode(Layer.REGISTRY, entry.getValue(), ops));
				}
				this.templateRegistry.freeze();
				this.columnEntryRegistry.freeze();
				this.voronoiSettingsRegistry.freeze();
				this.decisionTreeRegistry.freeze();
				this.worldTraitRegistry.freeze();
				this.layerRegistry.freeze();
			}
			finally {
				ScriptFileResolver.OVERRIDES.set(null);
			}
		}

		@SuppressWarnings("unchecked")
		public <T_Element> @Nullable MappedRegistry<T_Element> getRegistry(ResourceKey<? extends Registry<? extends T_Element>> key) {
			ResourceKey<?> wildcard = key;
			MappedRegistry<?> registry;
			if (wildcard == BigGlobeDynamicRegistries.SCRIPT_TEMPLATE_REGISTRY_KEY) registry = this.templateRegistry;
			else if (wildcard == BigGlobeDynamicRegistries.COLUMN_ENTRY_REGISTRY_KEY) registry = this.columnEntryRegistry;
			else if (wildcard == BigGlobeDynamicRegistries.VORONOI_SETTINGS_REGISTRY_KEY) registry = this.voronoiSettingsRegistry;
			else if (wildcard == BigGlobeDynamicRegistries.DECISION_TREE_SETTINGS_REGISTRY_KEY) registry = this.decisionTreeRegistry;
			else if (wildcard == BigGlobeDynamicRegistries.WORLD_TRAIT_REGISTRY_KEY) registry = this.worldTraitRegistry;
			else if (wildcard == BigGlobeDynamicRegistries.LAYER_REGISTRY_KEY) registry = this.layerRegistry;
			else registry = null;
			return (MappedRegistry<T_Element>)(registry);
		}

		public <T_Encoded> RegistryOps<T_Encoded> createOps(DynamicOps<T_Encoded> delegate, boolean mutable) {
			return RegistryOps.create(
				delegate,
				new RegistryInfoLookup() {

					@Override
					public <T_Registry> Optional<RegistryInfo<T_Registry>> lookup(ResourceKey<? extends Registry<? extends T_Registry>> key) {
						MappedRegistry<T_Registry> registry = Syncing.this.getRegistry(key);
						if (registry == null) {
							if (BigGlobeMod.getClientRegistry(key) instanceof BetterHardCodedRegistry<T_Registry> better && better.registry instanceof MappedRegistry<T_Registry> simple) {
								registry = simple;
							}
							else {
								return Optional.empty();
							}
						}
						return Optional.of(
							new RegistryInfo<>(

								registry,
								mutable
									? registry.createRegistrationLookup()
									: registry,

								Lifecycle.experimental()
							)
						);
					}
				}
			);
		}

		public BetterRegistry.Lookup lookup() {
			return new BetterRegistry.Lookup() {

				@Override
				public <T> BetterRegistry<T> getRegistry(ResourceKey<Registry<T>> key) {
					Registry<T> registry = Syncing.this.getRegistry(key);
					if (registry != null) return new BetterHardCodedRegistry<>(registry);
					else return BigGlobeMod.getClientRegistry(key);
				}
			};
		}
	}

	public static class ClientGeneratorParams {

		public static final AutoCoder<@Nullable ClientGeneratorParams> NULLABLE_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(new ReifiedType<@VerifyNullable ClientGeneratorParams>() {});

		public final int minY, maxY;
		public final @VerifyNullable Integer seaLevel;
		public final long columnSeed;
		public final @VerifyNullable ColorOverrides colors;
		public final LodOverrides generatorLodOverrides;
		public final @VerifyNullable ScriptedColumnBiomeSource biomeSource;
		public final Map<Holder<WorldTrait>, WorldTraitProvider> worldTraits;
		public final @VerifyNullable Holder<Layer> layer;
		public transient ColumnEntryRegistry columnEntryRegistry;
		public transient WorldTraits compiledWorldTraits;
		public final transient ThreadLocal<ScriptedColumn> column;

		public ClientGeneratorParams(
			int minY,
			int maxY,
			@VerifyNullable Integer seaLevel,
			long columnSeed,
			@VerifyNullable ColorOverrides colors,
			LodOverrides generatorLodOverrides,
			@VerifyNullable ScriptedColumnBiomeSource biomeSource,
			Map<Holder<WorldTrait>, WorldTraitProvider> worldTraits,
			@VerifyNullable Holder<Layer> layer
		) {
			this.minY = minY;
			this.maxY = maxY;
			this.seaLevel = seaLevel;
			this.columnSeed = columnSeed;
			this.colors = colors;
			this.generatorLodOverrides = generatorLodOverrides;
			this.biomeSource = biomeSource;
			this.worldTraits = worldTraits;
			this.layer = layer;
			this.column = ThreadLocal.withInitial(this::createColumn);
		}

		@Hidden //we want AutoCodec to target the other constructor.
		public ClientGeneratorParams(BigGlobeScriptedChunkGenerator generator, Syncing syncing) {
			this.minY = generator.height.min_y();
			this.maxY = generator.height.max_y();
			this.seaLevel = generator.height.sea_level();
			this.columnSeed = generator.columnSeed;
			this.colors = generator.game_mechanics.colors();
			this.generatorLodOverrides = generator.game_mechanics.lods();
			this.biomeSource = generator.biome_source() instanceof ScriptedColumnBiomeSource source ? source : null;
			this.worldTraits = new HashMap<>(generator.world_traits != null ? generator.loadedWorldTraits.size() : 0);
			if (generator.world_traits != null) {
				for (Map.Entry<Holder<WorldTrait>, WorldTraitProvider> entry : generator.loadedWorldTraits.entrySet()) {
					if (syncing.worldTraits.containsKey(UnregisteredObjectException.getID(entry.getKey()))) {
						this.worldTraits.put(entry.getKey(), entry.getValue());
					}
				}
			}
			this.layer = syncing.containsLayers ? generator.layer : null;
			this.column = null;
		}

		public void compile(ColumnEntryRegistry.Loading loading) throws Exception {
			if (
				(
					this.colors == null || (
						this.colors.grass() == null &&
						this.colors.foliage() == null &&
						this.colors.water() == null
					)
				)
				&& this.layer == null
			) {
				return;
			}
			loading.compile();
			this.columnEntryRegistry = loading.getRegistry();
			this.compiledWorldTraits = this.columnEntryRegistry.traitManager.createTraits(this.worldTraits);
		}

		public ScriptedColumn createColumn() {
			if (this.columnEntryRegistry == null) {
				throw new IllegalStateException("Not compiled");
			}
			return this.columnEntryRegistry.columnFactory.create(new Params(this.columnSeed, 0, 0, this.minY, this.maxY, ColumnUsage.GENERIC.normalHints(), this.compiledWorldTraits));
		}

		public ScriptedColumn getColumn(int x, int z) {
			ScriptedColumn column = this.column.get();
			column.setParams(column.params.at(x, z));
			return column;
		}
	}

	public static interface ColorScript extends ColumnScript {

		public static final Info INFO = new Info();

		public static class Info extends InfoHolder {

			public MethodInfo
				getDefaultGrassColor,
				getDefaultFoliageColor;

			public void addAllTo(MutableScriptEnvironment environment) {
				environment
					.addFunctionInvokeStatic(this.getDefaultGrassColor)
					.addFunctionInvokeStatic(this.getDefaultFoliageColor)
				;
			}
		}

		public abstract int getColor(ScriptedColumn column, int y);

		public static int getDefaultGrassColor(double temperature, double foliage) {
			return GrassColor.get(
				Interpolator.clamp(0.0D, 1.0D, temperature),
				Interpolator.clamp(0.0D, 1.0D, foliage)
			);
		}

		public static int getDefaultFoliageColor(double temperature, double foliage) {
			return FoliageColor.get(
				Interpolator.clamp(0.0D, 1.0D, temperature),
				Interpolator.clamp(0.0D, 1.0D, foliage)
			);
		}

		@Wrapper
		public static class Catcher extends BaseCatcher<ColorScript> implements ColorScript {

			public Catcher(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public int getColor(ScriptedColumn column, int y) {
				try {
					return this.script.getColor(column, y) | 0xFF000000;
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0xFF000000;
				}
			}

			@Override
			public Class<ColorScript> getScriptClass() {
				return ColorScript.class;
			}

			@Override
			public void addExtraFunctionsToEnvironment(ImplParameters parameters, MutableScriptEnvironment environment) {
				//don't call super, because I don't want to deal with syncing grids.
				environment
					.addAll(MathScriptEnvironment.INSTANCE)
					.addAll(StatelessRandomScriptEnvironment.INSTANCE)
					//.addAll(GridScriptEnvironment.createWithSeed(ScriptedColumn.INFO.baseSeed(load(parameters.actualColumn))))
					.configure(
						parameters.random != null
							? MinecraftScriptEnvironment.createWithRandom(load(parameters.random))
							: MinecraftScriptEnvironment.create()
					)
					.configure(ScriptedColumn.baseEnvironment(load(parameters.actualColumn)))
					.addAll(ColorScriptEnvironment.ENVIRONMENT);
				if (parameters.y != null) environment.addVariableLoad(parameters.y);
				if (parameters.random != null) environment.configure(RandomScriptEnvironment.create(load(parameters.random)));
				INFO.addAllTo(environment);
			}
		}
	}
}