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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.*;
import net.minecraft.registry.RegistryOps.RegistryInfo;
import net.minecraft.registry.RegistryOps.RegistryInfoGetter;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.biome.ColorResolver;

import builderb0y.autocodec.annotations.Hidden;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
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
import builderb0y.bigglobe.rendering.lods.LodGenerator;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.mixinInterfaces.DimensionalBlockView;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.packets.DangerousRapidsPacket;
import builderb0y.bigglobe.networking.packets.SettingsSyncS2CPacketHandler;
import builderb0y.bigglobe.networking.packets.TimeSpeedS2CPacketHandler;
import builderb0y.bigglobe.scripting.environments.MinecraftScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.RandomScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.ColorScriptEnvironment;
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

#if MC_VERSION >= MC_1_21_0
	import net.minecraft.world.biome.FoliageColors;
	import net.minecraft.world.biome.GrassColors;
#else
	import net.minecraft.client.color.world.FoliageColors;
	import net.minecraft.client.color.world.GrassColors;
#endif

public class ClientState {

	public static final Map<RegistryKey<World>, ClientState> INSTANCES = new HashMap<>();
	public static final ReentrantReadWriteLock INSTANCE_LOCK = new ReentrantReadWriteLock();

	public ClientGeneratorParams generatorParams;
	public double timeSpeed = 1.0D;
	public boolean dangerousRapids;

	static {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			initClient();
		}
	}

	public static @Nullable ClientState get(RegistryKey<World> key) {
		INSTANCE_LOCK.readLock().lock();
		try {
			return INSTANCES.get(key);
		}
		finally {
			INSTANCE_LOCK.readLock().unlock();
		}
	}

	public static @Nullable ClientState get(BlockView world) {
		return world != null ? get(((DimensionalBlockView)(world)).bigglobe_getDimension()) : null;
	}

	@Environment(EnvType.CLIENT)
	public static @Nullable ClientState get() {
		return get(MinecraftClient.getInstance().world);
	}

	public static @NotNull ClientState getOrCreate(RegistryKey<World> key) {
		INSTANCE_LOCK.readLock().lock();
		try {
			return INSTANCES.computeIfAbsent(key, key_ -> new ClientState());
		}
		finally {
			INSTANCE_LOCK.readLock().unlock();
		}
	}

	public static @NotNull ClientState getOrCreate(@NotNull World world) {
		return getOrCreate(world.getRegistryKey());
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

	public static void retain(Set<RegistryKey<World>> worlds) {
		INSTANCE_LOCK.writeLock().lock();
		try {
			INSTANCES.keySet().retainAll(worlds);
			for (RegistryKey<World> world : worlds) {
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

	/** called by the server to sync overworld settings to the client. */
	public static void sync(ServerWorld world, ServerPlayerEntity player) {
		BigGlobeNetwork.LOGGER.debug("Syncing ClientState to " + player);
		SettingsSyncS2CPacketHandler.INSTANCE.send(world, player);
		TimeSpeedS2CPacketHandler.INSTANCE.send(player);
		DangerousRapidsPacket.INSTANCE.send(player);
		syncWaypoints(world, player);
	}

	public static void syncWaypoints(ServerWorld world, ServerPlayerEntity player) {
		if (PlayerWaypointManager.get(player) instanceof ServerPlayerWaypointManager manager) {
			manager.updateOnWorldChange(world);
		}
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		ClientWorldEvents.WORLD_CHANGED.register((ClientWorld oldWorld, ClientWorld newWorld) -> {
			if (newWorld == null) {
				BigGlobeMod.LOGGER.info("Resetting ClientState on disconnect.");
				reset();
			}
		});
	}

	public static void overrideColor(BlockView world, int x, int y, int z, ColorResolver colorResolver, CallbackInfoReturnable<Integer> callback) {
		//don't intercept for my own drawing code,
		//since it contains intentionally incorrect coordinates.
		if (LodGenerator.RENDERING_LODS.get()) return;
		RegistryKey<World> dimension = ((DimensionalBlockView)(world)).bigglobe_getDimension();
		if (dimension == null) return;
		ClientState state = get(dimension);
		if (state == null) return;
		ClientGeneratorParams params = state.generatorParams;
		if (params != null) {
			if (colorResolver == BiomeColors.GRASS_COLOR) {
				if (params.grassColor != null) {
					callback.setReturnValue(params.grassColor.getColor(params.getColumn(x, z), y));
				}
			}
			else if (colorResolver == BiomeColors.FOLIAGE_COLOR) {
				if (params.foliageColor != null) {
					callback.setReturnValue(params.foliageColor.getColor(params.getColumn(x, z), y));
				}
			}
			else if (colorResolver == BiomeColors.WATER_COLOR) {
				if (params.waterColor != null) {
					callback.setReturnValue(params.waterColor.getColor(params.getColumn(x, z), y));
				}
			}
		}
	}

	public static class Syncing {

		public static final AutoCoder<Syncing> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Syncing.class);

		public boolean containsLayers;
		public Map<Identifier, String> includes;
		public Map<Identifier, NbtElement> templates, columnEntries, voronoiSettings, decisionTrees, worldTraits, layers;
		public transient SimpleRegistry<ScriptTemplate> templateRegistry = new SimpleRegistry<>(BigGlobeDynamicRegistries.SCRIPT_TEMPLATE_REGISTRY_KEY, Lifecycle.experimental());
		public transient SimpleRegistry<ColumnEntry> columnEntryRegistry = new SimpleRegistry<>(BigGlobeDynamicRegistries.COLUMN_ENTRY_REGISTRY_KEY, Lifecycle.experimental());
		public transient SimpleRegistry<VoronoiSettings> voronoiSettingsRegistry = new SimpleRegistry<>(BigGlobeDynamicRegistries.VORONOI_SETTINGS_REGISTRY_KEY, Lifecycle.experimental());
		public transient SimpleRegistry<DecisionTreeSettings> decisionTreeRegistry = new SimpleRegistry<>(BigGlobeDynamicRegistries.DECISION_TREE_SETTINGS_REGISTRY_KEY, Lifecycle.experimental());
		public transient SimpleRegistry<WorldTrait> worldTraitRegistry = new SimpleRegistry<>(BigGlobeDynamicRegistries.WORLD_TRAIT_REGISTRY_KEY, Lifecycle.experimental());
		public transient SimpleRegistry<Layer> layerRegistry = new SimpleRegistry<>(BigGlobeDynamicRegistries.LAYER_REGISTRY_KEY, Lifecycle.experimental());

		public Syncing(
			boolean                     containsLayers,
			Map<Identifier, String>     includes,
			Map<Identifier, NbtElement> templates,
			Map<Identifier, NbtElement> columnEntries,
			Map<Identifier, NbtElement> voronoiSettings,
			Map<Identifier, NbtElement> decisionTrees,
			Map<Identifier, NbtElement> worldTraits,
			Map<Identifier, NbtElement> layers
		) {
			this.containsLayers  = containsLayers;
			this.includes        = includes;
			this.templates       = templates;
			this.columnEntries   = columnEntries;
			this.voronoiSettings = voronoiSettings;
			this.decisionTrees   = decisionTrees;
			this.worldTraits     = worldTraits;
			this.layers          = layers;
		}

		@Hidden
		public Syncing(BigGlobeScriptedChunkGenerator generator) {
			this(BigGlobeConfig.INSTANCE.get().lodRendering.renderingEnabled(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
			if (generator.colors != null || this.containsLayers) {
				IndirectDependencyCollector collector = new IndirectDependencyCollector(generator);
				if (generator.colors != null) {
					if (generator.colors.grass()   != null) generator.colors.grass  ().streamDirectDependencies().forEach(collector);
					if (generator.colors.foliage() != null) generator.colors.foliage().streamDirectDependencies().forEach(collector);
					if (generator.colors.water()   != null) generator.colors.water  ().streamDirectDependencies().forEach(collector);
				}
				if (this.containsLayers) collector.accept(generator.layer);
				generator
				.columnEntryRegistry
				.registries
				.getRegistry(BigGlobeDynamicRegistries.VORONOI_SETTINGS_REGISTRY_KEY)
				.streamEntries()
				.filter((RegistryEntry<VoronoiSettings> entry) -> collector.contains(entry.value().owner()))
				.forEach(collector);
				for (RegistryEntry<? extends DependencyView> entry : collector) {
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
				RegistryOps<NbtElement> ops = this.createOps(NbtOps.INSTANCE, false);
				for (Map.Entry<RegistryKey<ScriptTemplate>, ScriptTemplate> entry : this.templateRegistry.getEntrySet()) {
					this.templates.put(entry.getKey().getValue(), BigGlobeAutoCodec.AUTO_CODEC.encode(ScriptTemplate.CODER, entry.getValue(), ops));
				}
				for (Map.Entry<RegistryKey<ColumnEntry>, ColumnEntry> entry : this.columnEntryRegistry.getEntrySet()) {
					this.columnEntries.put(entry.getKey().getValue(), BigGlobeAutoCodec.AUTO_CODEC.encode(ColumnEntry.REGISTRY, entry.getValue(), ops));
				}
				for (Map.Entry<RegistryKey<VoronoiSettings>, VoronoiSettings> entry : this.voronoiSettingsRegistry.getEntrySet()) {
					this.voronoiSettings.put(entry.getKey().getValue(), BigGlobeAutoCodec.AUTO_CODEC.encode(VoronoiSettings.CODER, entry.getValue(), ops));
				}
				for (Map.Entry<RegistryKey<DecisionTreeSettings>, DecisionTreeSettings> entry : this.decisionTreeRegistry.getEntrySet()) {
					this.decisionTrees.put(entry.getKey().getValue(), BigGlobeAutoCodec.AUTO_CODEC.encode(DecisionTreeSettings.CODER, entry.getValue(), ops));
				}
				for (Map.Entry<RegistryKey<WorldTrait>, WorldTrait> entry : this.worldTraitRegistry.getEntrySet()) {
					this.worldTraits.put(entry.getKey().getValue(), BigGlobeAutoCodec.AUTO_CODEC.encode(WorldTrait.CODER, entry.getValue(), ops));
				}
				for (Map.Entry<RegistryKey<Layer>, Layer> entry : this.layerRegistry.getEntrySet()) {
					this.layers.put(entry.getKey().getValue(), BigGlobeAutoCodec.AUTO_CODEC.encode(Layer.REGISTRY, entry.getValue(), ops));
				}
			}
		}

		public void parse() throws DecodeException {
			try {
				ScriptFileResolver.OVERRIDES.set(this.includes);
				RegistryOps<NbtElement> ops = this.createOps(NbtOps.INSTANCE, true);
				for (Map.Entry<Identifier, NbtElement> entry : this.templates.entrySet()) {
					Registry.register(this.templateRegistry, entry.getKey(), BigGlobeAutoCodec.AUTO_CODEC.decode(ScriptTemplate.CODER, entry.getValue(), ops));
				}
				for (Map.Entry<Identifier, NbtElement> entry : this.columnEntries.entrySet()) {
					Registry.register(this.columnEntryRegistry, entry.getKey(), BigGlobeAutoCodec.AUTO_CODEC.decode(ColumnEntry.REGISTRY, entry.getValue(), ops));
				}
				for (Map.Entry<Identifier, NbtElement> entry : this.voronoiSettings.entrySet()) {
					Registry.register(this.voronoiSettingsRegistry, entry.getKey(), BigGlobeAutoCodec.AUTO_CODEC.decode(VoronoiSettings.CODER, entry.getValue(), ops));
				}
				for (Map.Entry<Identifier, NbtElement> entry : this.decisionTrees.entrySet()) {
					Registry.register(this.decisionTreeRegistry, entry.getKey(), BigGlobeAutoCodec.AUTO_CODEC.decode(DecisionTreeSettings.CODER, entry.getValue(), ops));
				}
				for (Map.Entry<Identifier, NbtElement> entry : this.worldTraits.entrySet()) {
					Registry.register(this.worldTraitRegistry, entry.getKey(), BigGlobeAutoCodec.AUTO_CODEC.decode(WorldTrait.CODER, entry.getValue(), ops));
				}
				for (Map.Entry<Identifier, NbtElement> entry : this.layers.entrySet()) {
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
		public <T_Element> @Nullable SimpleRegistry<T_Element> getRegistry(RegistryKey<? extends Registry<? extends T_Element>> key) {
			RegistryKey<?> wildcard = key;
			SimpleRegistry<?> registry;
			if      (wildcard == BigGlobeDynamicRegistries.       SCRIPT_TEMPLATE_REGISTRY_KEY) registry = this.       templateRegistry;
			else if (wildcard == BigGlobeDynamicRegistries.          COLUMN_ENTRY_REGISTRY_KEY) registry = this.    columnEntryRegistry;
			else if (wildcard == BigGlobeDynamicRegistries.      VORONOI_SETTINGS_REGISTRY_KEY) registry = this.voronoiSettingsRegistry;
			else if (wildcard == BigGlobeDynamicRegistries.DECISION_TREE_SETTINGS_REGISTRY_KEY) registry = this.   decisionTreeRegistry;
			else if (wildcard == BigGlobeDynamicRegistries.           WORLD_TRAIT_REGISTRY_KEY) registry = this.     worldTraitRegistry;
			else if (wildcard == BigGlobeDynamicRegistries.                 LAYER_REGISTRY_KEY) registry = this.          layerRegistry;
			else registry = null;
			return (SimpleRegistry<T_Element>)(registry);
		}

		public <T_Encoded> RegistryOps<T_Encoded> createOps(DynamicOps<T_Encoded> delegate, boolean mutable) {
			return RegistryOps.of(
				delegate,
				new RegistryInfoGetter() {

					@Override
					public <T_Registry> Optional<RegistryInfo<T_Registry>> getRegistryInfo(RegistryKey<? extends Registry<? extends T_Registry>> key) {
						SimpleRegistry<T_Registry> registry = Syncing.this.getRegistry(key);
						if (registry == null) {
							if (BigGlobeMod.getClientRegistry(key) instanceof BetterHardCodedRegistry<T_Registry> better && better.registry instanceof SimpleRegistry<T_Registry> simple) {
								registry = simple;
							}
							else {
								return Optional.empty();
							}
						}
						return Optional.of(
							new RegistryInfo<>(
								#if MC_VERSION >= MC_1_21_2
									registry,
									mutable
									? registry.createMutableRegistryLookup()
									: registry,
								#else
									registry.getEntryOwner(),
									mutable
									? registry.createMutableEntryLookup()
									: registry.getReadOnlyWrapper(),
								#endif
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
				public <T> BetterRegistry<T> getRegistry(RegistryKey<Registry<T>> key) {
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
		public final ColorScript.@VerifyNullable Holder grassColor;
		public final ColorScript.@VerifyNullable Holder foliageColor;
		public final ColorScript.@VerifyNullable Holder waterColor;
		public final Map<RegistryEntry<WorldTrait>, WorldTraitProvider> worldTraits;
		public final @VerifyNullable RegistryEntry<Layer> layer;
		public transient ColumnEntryRegistry columnEntryRegistry;
		public transient WorldTraits compiledWorldTraits;
		public final transient ThreadLocal<ScriptedColumn> column;

		public ClientGeneratorParams(
			int minY,
			int maxY,
			@VerifyNullable Integer seaLevel,
			long columnSeed,
			ColorScript.@VerifyNullable Holder grassColor,
			ColorScript.@VerifyNullable Holder foliageColor,
			ColorScript.@VerifyNullable Holder waterColor,
			Map<RegistryEntry<WorldTrait>, WorldTraitProvider> worldTraits,
			@VerifyNullable RegistryEntry<Layer> layer
		) {
			this.minY         = minY;
			this.maxY         = maxY;
			this.seaLevel     = seaLevel;
			this.columnSeed   = columnSeed;
			this.grassColor   = grassColor;
			this.foliageColor = foliageColor;
			this.waterColor   = waterColor;
			this.worldTraits  = worldTraits;
			this.layer        = layer;
			this.column       = ThreadLocal.withInitial(this::createColumn);
		}

		@Hidden //we want AutoCodec to target the other constructor.
		public ClientGeneratorParams(BigGlobeScriptedChunkGenerator generator, Syncing syncing) {
			this.minY         = generator.height.min_y();
			this.maxY         = generator.height.max_y();
			this.seaLevel     = generator.height.sea_level();
			this.columnSeed   = generator.columnSeed;
			this.grassColor   = generator.colors != null ? generator.colors.grass() : null;
			this.foliageColor = generator.colors != null ? generator.colors.foliage() : null;
			this.waterColor   = generator.colors != null ? generator.colors.water() : null;
			this.worldTraits  = new HashMap<>(generator.world_traits != null ? generator.loadedWorldTraits.size() : 0);
			if (generator.world_traits != null) {
				for (Map.Entry<RegistryEntry<WorldTrait>, WorldTraitProvider> entry : generator.loadedWorldTraits.entrySet()) {
					if (syncing.worldTraits.containsKey(UnregisteredObjectException.getID(entry.getKey()))) {
						this.worldTraits.put(entry.getKey(), entry.getValue());
					}
				}
			}
			this.layer = syncing.containsLayers ? generator.layer : null;
			this.column = null;
		}

		public void compile(ColumnEntryRegistry.Loading loading) throws Exception {
			if (this.grassColor == null && this.foliageColor == null && this.waterColor == null && this.layer == null) return;
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
			return GrassColors.getColor(
				Interpolator.clamp(0.0D, 1.0D, temperature),
				Interpolator.clamp(0.0D, 1.0D, foliage)
			);
		}

		public static int getDefaultFoliageColor(double temperature, double foliage) {
			return FoliageColors.getColor(
				Interpolator.clamp(0.0D, 1.0D, temperature),
				Interpolator.clamp(0.0D, 1.0D, foliage)
			);
		}

		@Wrapper
		public static class Holder extends ColumnScript.BaseHolder<ColorScript> implements ColorScript {

			public Holder(ScriptUsage usage) {
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