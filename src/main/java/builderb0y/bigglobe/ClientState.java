package builderb0y.bigglobe;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.color.world.FoliageColors;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.client.world.ClientWorld;
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
import builderb0y.bigglobe.compat.SodiumCompat;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.mixins.ClientWorld_CustomTimeSpeed;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
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
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		ClientWorldEvents.UNLOAD.register((ClientWorld world) -> {
			BigGlobeMod.LOGGER.info("Resetting ClientState on disconnect.");
			generatorParams = null;
			timeSpeed = 1.0D;
		});
	}

	public static void overrideColor(int x, int y, int z, ColorResolver colorResolver, CallbackInfoReturnable<Integer> callback) {
		ClientGeneratorParams params = generatorParams;
		if (params != null) {
			if (colorResolver == BiomeColors.GRASS_COLOR) {
				if (params.grassColor != null) {
					callback.setReturnValue(SodiumCompat.maybeSwapChannels(params.grassColor.getColor(params.getColumn(x, z), y)));
				}
			}
			else if (colorResolver == BiomeColors.FOLIAGE_COLOR) {
				if (params.foliageColor != null) {
					callback.setReturnValue(SodiumCompat.maybeSwapChannels(params.foliageColor.getColor(params.getColumn(x, z), y)));
				}
			}
			else if (colorResolver == BiomeColors.WATER_COLOR) {
				if (params.waterColor != null) {
					callback.setReturnValue(SodiumCompat.maybeSwapChannels(params.waterColor.getColor(params.getColumn(x, z), y)));
				}
			}
		}
	}

	public static <T> SimpleRegistry<T> convertToSimpleRegistry(RegistryKey<Registry<T>> key, Map<Identifier, T> map) {
		SimpleRegistry<T> registry = new SimpleRegistry<>(key, Lifecycle.experimental());
		for (Map.Entry<Identifier, T> entry : map.entrySet()) {
			Registry.register(registry, entry.getKey(), entry.getValue());
		}
		return registry;
	}

	public static <T> BetterRegistry<T> convertToBetterRegistry(RegistryKey<Registry<T>> key, Map<Identifier, T> map) {
		return new BetterHardCodedRegistry<>(convertToSimpleRegistry(key, map));
	}

	public static class TemplateRegistry extends HashMap<Identifier, ScriptTemplate> {

		public static final AutoCoder<TemplateRegistry> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(TemplateRegistry.class);

		public <T> RegistryOps<T> createOps(DynamicOps<T> delegate) {
			SimpleRegistry<ScriptTemplate> registry = convertToSimpleRegistry(BigGlobeDynamicRegistries.SCRIPT_TEMPLATE_REGISTRY_KEY, this);
			return RegistryOps.of(delegate, new RegistryInfoGetter() {

				@Override
				@SuppressWarnings({ "unchecked", "rawtypes" })
				public <T> Optional<RegistryInfo<T>> getRegistryInfo(RegistryKey<? extends Registry<? extends T>> key) {
					return (
						((RegistryKey<?>)(key)) == ((RegistryKey<?>)(BigGlobeDynamicRegistries.SCRIPT_TEMPLATE_REGISTRY_KEY))
						? (Optional)(Optional.of(new RegistryInfo<>(registry.getEntryOwner(), registry.createMutableEntryLookup(), registry.getLifecycle())))
						: Optional.empty()
					);
				}
			});
		}
	}

	public static class ClientGeneratorParams {

		public static final AutoCoder<ClientGeneratorParams> NULLABLE_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(new ReifiedType<@VerifyNullable ClientGeneratorParams>() {});

		public final TemplateRegistry templates;
		public final Map<Identifier, ColumnEntry> columnEntries;
		public final Map<Identifier, VoronoiSettings> voronoiSettings;
		public final Map<Identifier, DecisionTreeSettings> decisionTrees;

		public final int minY, maxY;
		public final @VerifyNullable Integer seaLevel;
		public final long columnSeed;
		public final ColorScript.@VerifyNullable Holder grassColor;
		public final ColorScript.@VerifyNullable Holder foliageColor;
		public final ColorScript.@VerifyNullable Holder waterColor;
		public transient ColumnEntryRegistry columnEntryRegistry;
		public final transient ThreadLocal<ScriptedColumn> columns;

		public ClientGeneratorParams(
			TemplateRegistry templates,
			Map<Identifier, ColumnEntry> columnEntries,
			Map<Identifier, VoronoiSettings> voronoiSettings,
			Map<Identifier, DecisionTreeSettings> decisionTrees,
			int minY,
			int maxY,
			@VerifyNullable Integer seaLevel,
			long columnSeed,
			ColorScript.@VerifyNullable Holder grassColor,
			ColorScript.@VerifyNullable Holder foliageColor,
			ColorScript.@VerifyNullable Holder waterColor
		) {
			this.templates = templates;
			this.columnEntries = columnEntries;
			this.voronoiSettings = voronoiSettings;
			this.decisionTrees = decisionTrees;
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
			this.templates       = new TemplateRegistry();
			this.columnEntries   = new HashMap<>(16);
			this.voronoiSettings = new HashMap<>(16);
			this.decisionTrees   = new HashMap<>(32);
			this.minY = generator.height.min_y();
			this.maxY = generator.height.max_y();
			this.seaLevel = generator.height.sea_level();
			this.columnSeed = generator.columnSeed;
			this.grassColor = generator.colors != null ? generator.colors.grass() : null;
			this.foliageColor = generator.colors != null ? generator.colors.foliage() : null;
			this.waterColor = generator.colors != null ? generator.colors.water() : null;
			this.columns = null;

			IndirectDependencyCollector collector = new IndirectDependencyCollector();
			if (this.grassColor   != null) this.  grassColor.streamDirectDependencies().forEach(collector);
			if (this.foliageColor != null) this.foliageColor.streamDirectDependencies().forEach(collector);
			if (this.waterColor   != null) this.  waterColor.streamDirectDependencies().forEach(collector);
			for (RegistryEntry<? extends DependencyView> entry : collector) {
				if (entry.value() instanceof ColumnEntry column) {
					this.columnEntries.put(UnregisteredObjectException.getID(entry), column);
				}
				else if (entry.value() instanceof VoronoiSettings voronoi) {
					this.voronoiSettings.put(UnregisteredObjectException.getID(entry), voronoi);
				}
				else if (entry.value() instanceof DecisionTreeSettings decision) {
					this.decisionTrees.put(UnregisteredObjectException.getID(entry), decision);
				}
				else if (entry.value() instanceof ScriptTemplate template) {
					this.templates.put(UnregisteredObjectException.getID(entry), template);
				}
				else {
					throw new IllegalStateException("Unhandled dependency view type: " + entry.value());
				}
			}
		}

		public void compile() throws ScriptParsingException {
			if (this.grassColor == null && this.foliageColor == null && this.waterColor == null) return;
			this.columnEntryRegistry = new ColumnEntryRegistry(this.createLookup(), "client");
			if (this.grassColor   != null) this.  grassColor.compile(this.columnEntryRegistry);
			if (this.foliageColor != null) this.foliageColor.compile(this.columnEntryRegistry);
			if (this.waterColor   != null) this.  waterColor.compile(this.columnEntryRegistry);
		}

		public BetterRegistry.Lookup createLookup() {
			BetterRegistry<DecisionTreeSettings> decisionTrees = convertToBetterRegistry(BigGlobeDynamicRegistries.DECISION_TREE_SETTINGS_REGISTRY_KEY, this.decisionTrees);
			BetterRegistry<VoronoiSettings> voronoiSettings = convertToBetterRegistry(BigGlobeDynamicRegistries.VORONOI_SETTINGS_REGISTRY_KEY, this.voronoiSettings);
			BetterRegistry<ColumnEntry> columnEntries = convertToBetterRegistry(BigGlobeDynamicRegistries.COLUMN_ENTRY_REGISTRY_KEY, this.columnEntries);
			BetterRegistry<ScriptTemplate> templates = convertToBetterRegistry(BigGlobeDynamicRegistries.SCRIPT_TEMPLATE_REGISTRY_KEY, this.templates);
			return new BetterRegistry.Lookup() {

				@Override
				@SuppressWarnings("unchecked")
				public <T> BetterRegistry<T> getRegistry(RegistryKey<Registry<T>> key) {
					if (((RegistryKey<?>)(key)) == ((RegistryKey<?>)(BigGlobeDynamicRegistries.COLUMN_ENTRY_REGISTRY_KEY))) {
						return (BetterRegistry<T>)(columnEntries);
					}
					else if (((RegistryKey<?>)(key)) == ((RegistryKey<?>)(BigGlobeDynamicRegistries.VORONOI_SETTINGS_REGISTRY_KEY))) {
						return (BetterRegistry<T>)(voronoiSettings);
					}
					else if (((RegistryKey<?>)(key)) == ((RegistryKey<?>)(BigGlobeDynamicRegistries.DECISION_TREE_SETTINGS_REGISTRY_KEY))) {
						return (BetterRegistry<T>)(decisionTrees);
					}
					else if (((RegistryKey<?>)(key)) == ((RegistryKey<?>)(BigGlobeDynamicRegistries.SCRIPT_TEMPLATE_REGISTRY_KEY))) {
						return (BetterRegistry<T>)(templates);
					}
					else {
						throw new IllegalArgumentException("Something has a dependency on a registry that isn't synced.");
					}
				}
			};
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
			return packed & 255;
		}

		public static int greenI(int packed) {
			return (packed >>> 8) & 255;
		}

		public static int blueI(int packed) {
			return (packed >>> 16) & 255;
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
			return (blue << 16) | (green << 8) | red;
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