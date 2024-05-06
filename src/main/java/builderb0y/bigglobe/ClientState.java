package builderb0y.bigglobe;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.color.world.FoliageColors;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryOps.RegistryInfo;
import net.minecraft.registry.RegistryOps.RegistryInfoGetter;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.ColorResolver;

import builderb0y.autocodec.annotations.Hidden;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ColumnScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Purpose;
import builderb0y.bigglobe.columns.scripted.VoronoiSettings;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.IndirectDependencyCollector;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.hyperspace.ServerPlayerWaypointManager;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;
import builderb0y.bigglobe.mixins.ClientWorld_CustomTimeSpeed;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.networking.packets.DangerousRapidsPacket;
import builderb0y.bigglobe.networking.packets.SettingsSyncS2CPacketHandler;
import builderb0y.bigglobe.networking.packets.TimeSpeedS2CPacketHandler;
import builderb0y.bigglobe.util.ClientWorldEvents;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.ScriptUsage.ScriptTemplate;
import builderb0y.scripting.util.InfoHolder;

public class ClientState {

	public static ClientGeneratorParams generatorParams;
	/** used by {@link ClientWorld_CustomTimeSpeed}. */
	public static double timeSpeed = 1.0D;
	public static boolean dangerousRapids;

	static {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			initClient();
		}
	}

	/** called by the server to sync overworld settings to the client. */
	public static void sync(ServerPlayerEntity player) {
		BigGlobeNetwork.LOGGER.debug("Syncing ClientState to " + player);
		SettingsSyncS2CPacketHandler.INSTANCE.send(player);
		TimeSpeedS2CPacketHandler.INSTANCE.send(player);
		DangerousRapidsPacket.INSTANCE.send(player);
		//player
		//.as(WaypointTracker)
		//.bigglobe_getWaypointManager()
		//.as(ServerPlayerWaypointManager)
		//.updateOnWorldChange()
		(
			(ServerPlayerWaypointManager)(
				(
					(WaypointTracker)(player)
				)
				.bigglobe_getWaypointManager()
			)
		)
		.updateOnWorldChange();
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		ClientWorldEvents.WORLD_CHANGED.register((ClientWorld oldWorld, ClientWorld newWorld) -> {
			if (newWorld == null) {
				BigGlobeMod.LOGGER.info("Resetting ClientState on disconnect.");
				generatorParams = null;
				timeSpeed = 1.0D;
			}
		});
	}

	public static void overrideColor(int x, int y, int z, ColorResolver colorResolver, CallbackInfoReturnable<Integer> callback) {
		ClientGeneratorParams params = generatorParams;
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

		public Map<Identifier, NbtElement> templates, columnEntries, voronoiSettings, decisionTrees;
		public transient SimpleRegistry<ScriptTemplate> templateRegistry = new SimpleRegistry<>(BigGlobeDynamicRegistries.SCRIPT_TEMPLATE_REGISTRY_KEY, Lifecycle.experimental());
		public transient SimpleRegistry<ColumnEntry> columnEntryRegistry = new SimpleRegistry<>(BigGlobeDynamicRegistries.COLUMN_ENTRY_REGISTRY_KEY, Lifecycle.experimental());
		public transient SimpleRegistry<VoronoiSettings> voronoiSettingsRegistry = new SimpleRegistry<>(BigGlobeDynamicRegistries.VORONOI_SETTINGS_REGISTRY_KEY, Lifecycle.experimental());
		public transient SimpleRegistry<DecisionTreeSettings> decisionTreeRegistry = new SimpleRegistry<>(BigGlobeDynamicRegistries.DECISION_TREE_SETTINGS_REGISTRY_KEY, Lifecycle.experimental());

		public Syncing(
			Map<Identifier, NbtElement> templates,
			Map<Identifier, NbtElement> columnEntries,
			Map<Identifier, NbtElement> voronoiSettings,
			Map<Identifier, NbtElement> decisionTrees
		) {
			this.templates = templates;
			this.columnEntries = columnEntries;
			this.voronoiSettings = voronoiSettings;
			this.decisionTrees = decisionTrees;
		}

		@Hidden
		public Syncing(BigGlobeScriptedChunkGenerator generator) {
			this(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
			if (generator.colors != null) {
				IndirectDependencyCollector collector = new IndirectDependencyCollector();
				if (generator.colors.grass  () != null) generator.colors.grass  ().streamDirectDependencies().forEach(collector);
				if (generator.colors.foliage() != null) generator.colors.foliage().streamDirectDependencies().forEach(collector);
				if (generator.colors.water  () != null) generator.colors.water  ().streamDirectDependencies().forEach(collector);
				for (RegistryEntry<? extends DependencyView> entry : collector) {
					if (entry.value() instanceof ScriptTemplate template) {
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
			}
		}

		public void parse() throws DecodeException {
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
			this.templateRegistry.freeze();
			this.columnEntryRegistry.freeze();
			this.voronoiSettingsRegistry.freeze();
			this.decisionTreeRegistry.freeze();
		}

		@SuppressWarnings("unchecked")
		public <T_Element> @Nullable SimpleRegistry<T_Element> getRegistry(RegistryKey<? extends Registry<? extends T_Element>> key) {
			RegistryKey<?> wildcard = key;
			SimpleRegistry<?> registry;
			if      (wildcard == BigGlobeDynamicRegistries.       SCRIPT_TEMPLATE_REGISTRY_KEY) registry = this.            templateRegistry;
			else if (wildcard == BigGlobeDynamicRegistries.          COLUMN_ENTRY_REGISTRY_KEY) registry = this.         columnEntryRegistry;
			else if (wildcard == BigGlobeDynamicRegistries.      VORONOI_SETTINGS_REGISTRY_KEY) registry = this.     voronoiSettingsRegistry;
			else if (wildcard == BigGlobeDynamicRegistries.DECISION_TREE_SETTINGS_REGISTRY_KEY) registry = this.decisionTreeRegistry;
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
						if (registry == null) return Optional.empty();
						return Optional.of(
							new RegistryInfo<>(
								registry.getEntryOwner(),
								mutable
								? registry.createMutableEntryLookup()
								: registry.getReadOnlyWrapper(),
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
					SimpleRegistry<T> registry = Syncing.this.getRegistry(key);
					if (registry != null) return new BetterHardCodedRegistry<>(registry);
					else throw new IllegalStateException("Missing registry: " + key);
				}
			};
		}
	}

	public static class ClientGeneratorParams {

		public static final AutoCoder<ClientGeneratorParams> NULLABLE_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(new ReifiedType<@VerifyNullable ClientGeneratorParams>() {});

		public final int minY, maxY;
		public final @VerifyNullable Integer seaLevel;
		public final long columnSeed;
		public final ColorScript.@VerifyNullable Holder grassColor;
		public final ColorScript.@VerifyNullable Holder foliageColor;
		public final ColorScript.@VerifyNullable Holder waterColor;
		public transient ColumnEntryRegistry columnEntryRegistry;
		public final transient ThreadLocal<ScriptedColumn> columns;

		public ClientGeneratorParams(
			int minY,
			int maxY,
			@VerifyNullable Integer seaLevel,
			long columnSeed,
			ColorScript.@VerifyNullable Holder grassColor,
			ColorScript.@VerifyNullable Holder foliageColor,
			ColorScript.@VerifyNullable Holder waterColor
		) {
			this.minY = minY;
			this.maxY = maxY;
			this.seaLevel = seaLevel;
			this.columnSeed = columnSeed;
			this.grassColor = grassColor;
			this.foliageColor = foliageColor;
			this.waterColor = waterColor;
			this.columns = ThreadLocal.withInitial(this::createColumn);
		}

		@Hidden //we want AutoCodec to target the other constructor.
		public ClientGeneratorParams(BigGlobeScriptedChunkGenerator generator) {
			this.minY = generator.height.min_y();
			this.maxY = generator.height.max_y();
			this.seaLevel = generator.height.sea_level();
			this.columnSeed = generator.columnSeed;
			this.grassColor = generator.colors != null ? generator.colors.grass() : null;
			this.foliageColor = generator.colors != null ? generator.colors.foliage() : null;
			this.waterColor = generator.colors != null ? generator.colors.water() : null;
			this.columns = null;
		}

		public void compile(Syncing syncing) throws Exception {
			if (this.grassColor == null && this.foliageColor == null && this.waterColor == null) return;
			ColumnEntryRegistry.Loading.OVERRIDE.accept(new ColumnEntryRegistry.Loading(syncing.lookup()), (ColumnEntryRegistry.Loading loading) -> {
				this.columnEntryRegistry = loading.getRegistry();
				if (this.grassColor   != null) this.  grassColor.compile(this.columnEntryRegistry);
				if (this.foliageColor != null) this.foliageColor.compile(this.columnEntryRegistry);
				if (this.waterColor   != null) this.  waterColor.compile(this.columnEntryRegistry);
			});
		}

		public ScriptedColumn createColumn() {
			if (this.columnEntryRegistry == null) {
				throw new IllegalStateException("Not compiled");
			}
			return this.columnEntryRegistry.columnFactory.create(new Params(this.columnSeed, 0, 0, this.minY, this.maxY, Purpose.GENERIC));
		}

		public ScriptedColumn getColumn(int x, int z) {
			ScriptedColumn column = this.columns.get();
			column.setParams(column.params.at(x, z));
			return column;
		}
	}

	public static interface ColorScript extends ColumnScript {

		public static final Info INFO = new Info();
		public static class Info extends InfoHolder {

			public MethodInfo
				getDefaultGrassColor,
				getDefaultFoliageColor,
				redI, greenI, blueI,
				redF, greenF, blueF,
				redD, greenD, blueD,
				packI, packF, packD;

			public void addAllTo(MutableScriptEnvironment environment) {
				environment
				.addFunctionInvokeStatic(this.getDefaultGrassColor)
				.addFunctionInvokeStatic(this.getDefaultFoliageColor)
				.addFieldInvokeStatic(this.redI)
				.addFieldInvokeStatic(this.greenI)
				.addFieldInvokeStatic(this.blueI)
				.addFieldInvokeStatic(this.redF)
				.addFieldInvokeStatic(this.greenF)
				.addFieldInvokeStatic(this.blueF)
				.addFieldInvokeStatic(this.redD)
				.addFieldInvokeStatic(this.greenD)
				.addFieldInvokeStatic(this.blueD)
				.addFunctionInvokeStatic(this.packI)
				.addFunctionInvokeStatic(this.packF)
				.addFunctionInvokeStatic(this.packD)
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

		public static int redI(int packed) {
			return (packed >>> 16) & 255;
		}

		public static int greenI(int packed) {
			return (packed >>> 8) & 255;
		}

		public static int blueI(int packed) {
			return packed & 255;
		}

		public static float redF(int packed) {
			return redI(packed) / 255.0F;
		}

		public static float greenF(int packed) {
			return greenI(packed) / 255.0F;
		}

		public static float blueF(int packed) {
			return blueI(packed) / 255.0F;
		}

		public static double redD(int packed) {
			return redI(packed) / 255.0D;
		}

		public static double greenD(int packed) {
			return greenI(packed) / 255.0D;
		}

		public static double blueD(int packed) {
			return blueI(packed) / 255.0D;
		}

		public static int packI(int red, int green, int blue) {
			red   = Interpolator.clamp(0, 255, red);
			green = Interpolator.clamp(0, 255, green);
			blue  = Interpolator.clamp(0, 255, blue);
			return (red << 16) | (green << 8) | blue;
		}

		public static int packF(float red, float green, float blue) {
			return packI((int)(red * 255.0F + 0.5F), (int)(green * 255.0F + 0.5F), (int)(blue * 255.0F + 0.5F));
		}

		public static int packD(double red, double green, double blue) {
			return packI((int)(red * 255.0D + 0.5D), (int)(green * 255.0D + 0.5D), (int)(blue * 255.0D + 0.5D));
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
			public void addExtraFunctionsToEnvironment(ColumnEntryRegistry registry, MutableScriptEnvironment environment) {
				super.addExtraFunctionsToEnvironment(registry, environment);
				INFO.addAllTo(environment);
			}
		}
	}
}