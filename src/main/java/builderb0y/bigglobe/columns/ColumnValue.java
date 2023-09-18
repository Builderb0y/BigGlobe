package builderb0y.bigglobe.columns;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.versions.EntityVersions;

import static builderb0y.bigglobe.math.BigGlobeMath.floorI;

@UseCoder(name = "CODER", usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public class ColumnValue<T_Column extends WorldColumn> {

	public static final ObjectArrayFactory<ColumnValue<?>> ARRAY_FACTORY = new ObjectArrayFactory<>(ColumnValue.class).generic();
	public static final RegistryKey<Registry<ColumnValue<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.modID("column_value"));
	public static final Registry<ColumnValue<?>> REGISTRY = BigGlobeMod.newRegistry(REGISTRY_KEY);
	public static final AutoCoder<ColumnValue<?>> CODER = new AutoCoder<>() {

		@Override
		public <T_Encoded> @Nullable ColumnValue<?> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			return get(context.forceAsString());
		}

		@Override
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, ColumnValue<?>> context) throws EncodeException {
			ColumnValue<?> value = context.input;
			if (value == null) return context.empty();
			Identifier id = REGISTRY.getId(value);
			if (id == null) throw new EncodeException(() -> "Unregistered ColumnValue: " + value);
			return context.createString(id.getNamespace().equals(BigGlobeMod.MODID) ? id.getPath() : id.toString());
		}
	};

	public static ColumnValue<?> get(String name) {
		int colon = name.indexOf(':');
		Identifier id;
		if (colon >= 0) {
			id = new Identifier(name.substring(0, colon), name.substring(colon + 1));
		}
		else {
			id = BigGlobeMod.modID(name);
		}
		ColumnValue<?> value = REGISTRY.get(id);
		if (value != null) return value;
		else throw new IllegalArgumentException("Unknown column value: " + id);
	}

	@SuppressWarnings("unused")
	public static final ColumnValue<WorldColumn>
		Y                                                 = registerAnyDim ("y",                                         withY(      (column, y) -> y                                      ), null),
		MIN_Y                                             = registerAnyDim ("min_y",                                  withoutY(       WorldColumn::getFinalBottomHeightD                   ), null),
		MAX_Y                                             = registerAnyDim ("max_y",                                  withoutY(       WorldColumn::getFinalTopHeightD                      ), null),
		DISTANCE_ABOVE_MAX_Y                              = registerAnyDim ("distance_above_max_y",                      withY(       WorldColumn::getDistanceAboveMaxY                    ), null),
		DISTANCE_BELOW_MAX_Y                              = registerAnyDim ("distance_below_max_y",                      withY(       WorldColumn::getDistanceBelowMaxY                    ), null),
		DISTANCE_ABOVE_MIN_Y                              = registerAnyDim ("distance_above_min_y",                      withY(       WorldColumn::getDistanceAboveMinY                    ), null),
		DISTANCE_BELOW_MIN_Y                              = registerAnyDim ("distance_below_min_y",                      withY(       WorldColumn::getDistanceBelowMinY                    ), null);

	@SuppressWarnings("unused")
	public static final ColumnValue<VanillaWorldColumn>
		VANILLA_TEMPERATURE                               = registerVanilla("temperature",                               withY(VanillaWorldColumn::getTemperature                          ), null),
		VANILLA_HUMIDITY                                  = registerVanilla("humidity",                                  withY(VanillaWorldColumn::getHumidity                             ), null),
		VANILLA_CONTINENTALNESS                           = registerVanilla("continentalness",                           withY(VanillaWorldColumn::getContinentalness                      ), null),
		VANILLA_EROSION                                   = registerVanilla("erosion",                                   withY(VanillaWorldColumn::getErosion                              ), null),
		VANILLA_DEPTH                                     = registerVanilla("depth",                                     withY(VanillaWorldColumn::getDepth                                ), null),
		VANILLA_WEIRDNESS                                 = registerVanilla("weirdness",                                 withY(VanillaWorldColumn::getWeirdness                            ), null),
		VANILLA_ROUTER_BARRIER                            = registerVanilla("router/barrier",                            withY(VanillaWorldColumn::getRouterBarrier                        ), null),
		VANILLA_ROUTER_FLUID_LEVEL_FLOODEDNESS            = registerVanilla("router/fluid_level_floodedness",            withY(VanillaWorldColumn::getRouterFluidLevelFloodedness          ), null),
		VANILLA_ROUTER_FLUID_LEVEL_SPREAD                 = registerVanilla("router/fluid_level_spread",                 withY(VanillaWorldColumn::getRouterFluidLevelSpread               ), null),
		VANILLA_ROUTER_LAVA                               = registerVanilla("router/lava",                               withY(VanillaWorldColumn::getRouterLava                           ), null),
		VANILLA_ROUTER_TEMPERATURE                        = registerVanilla("router/temperature",                        withY(VanillaWorldColumn::getRouterTemperature                    ), null),
		VANILLA_ROUTER_VEGETATION                         = registerVanilla("router/vegetation",                         withY(VanillaWorldColumn::getRouterVegetation                     ), null),
		VANILLA_ROUTER_CONTINENTS                         = registerVanilla("router/continents",                         withY(VanillaWorldColumn::getRouterContinents                     ), null),
		VANILLA_ROUTER_EROSION                            = registerVanilla("router/erosion",                            withY(VanillaWorldColumn::getRouterErosion                        ), null),
		VANILLA_ROUTER_DEPTH                              = registerVanilla("router/depth",                              withY(VanillaWorldColumn::getRouterDepth                          ), null),
		VANILLA_ROUTER_RIDGES                             = registerVanilla("router/ridges",                             withY(VanillaWorldColumn::getRouterRidges                         ), null),
		VANILLA_ROUTER_INITIAL_DENSITY_WITHOUT_JAGGEDNESS = registerVanilla("router/initial_density_without_jaggedness", withY(VanillaWorldColumn::getRouterInitialDensityWithoutJaggedness), null),
		VANILLA_ROUTER_FINAL_DENSITY                      = registerVanilla("router/final_density",                      withY(VanillaWorldColumn::getRouterFinalDensity                   ), null),
		VANILLA_ROUTER_VEIN_TOGGLE                        = registerVanilla("router/vein_toggle",                        withY(VanillaWorldColumn::getRouterVeinToggle                     ), null),
		VANILLA_ROUTER_VEIN_RIDGED                        = registerVanilla("router/vein_ridged",                        withY(VanillaWorldColumn::getRouterVeinRidged                     ), null),
		VANILLA_ROUTER_VEIN_GAP                           = registerVanilla("router/vein_gap",                           withY(VanillaWorldColumn::getRouterVeinGap                        ), null);

	@SuppressWarnings("unused")
	public static final ColumnValue<OverworldColumn>
		OVERWORLD_SEA_LEVEL                     = registerOverworld("sea_level",                     withoutY(OverworldColumn::getSeaLevel                    ), null),

		OVERWORLD_HILLINESS                     = registerOverworld("hilliness",                     withoutY(OverworldColumn::getHilliness                   ), null),
		OVERWORLD_CLIFFINESS                    = registerOverworld("cliffiness",                    withoutY(OverworldColumn::getCliffiness                  ), null),
		OVERWORLD_RAW_EROSION                   = registerOverworld("raw_erosion",                   withoutY(OverworldColumn::getRawErosion                  ), null),
		OVERWORLD_PRE_CLIFF_HEIGHT              = registerOverworld("pre_cliff_height",              withoutY(OverworldColumn::getPreCliffHeight              ), null),

		OVERWORLD_TEMPERATURE                   = registerOverworld("temperature",                   withoutY(OverworldColumn::getTemperature                 ), null),
		OVERWORLD_HEIGHT_ADJUSTED_TEMPERATURE   = registerOverworld("height_adjusted_temperature",      withY(OverworldColumn::getHeightAdjustedTemperature   ), null),
		OVERWORLD_SURFACE_TEMPERATURE           = registerOverworld("surface_temperature",           withoutY(OverworldColumn::getSurfaceTemperature          ), null),

		OVERWORLD_FOLIAGE                       = registerOverworld("foliage",                       withoutY(OverworldColumn::getFoliage                     ), null),
		OVERWORLD_HEIGHT_ADJUSTED_FOLIAGE       = registerOverworld("height_adjusted_foliage",          withY(OverworldColumn::getHeightAdjustedFoliage       ), null),
		OVERWORLD_SURFACE_FOLIAGE               = registerOverworld("surface_foliage",               withoutY(OverworldColumn::getSurfaceFoliage              ), null),

		OVERWORLD_MAGICALNESS                   = registerOverworld("magicalness",                   withoutY(OverworldColumn::getMagicalness                 ), null),

		OVERWORLD_RAW_SNOW                      = registerOverworld("raw_snow",                      withoutY(OverworldColumn::getRawSnow                     ), null),
		OVERWORLD_SNOW_HEIGHT                   = registerOverworld("snow_height",                   withoutY(OverworldColumn::getSnowHeight                  ), null),
		OVERWORLD_SNOW_CHANCE                   = registerOverworld("snow_chance",                   withoutY(OverworldColumn::getSnowChance                  ), null),

		OVERWORLD_GLACIER_BOTTOM_HEIGHT         = registerOverworld("glacier_bottom_height",         withoutY(OverworldColumn::getGlacierBottomHeightD        ), null),
		OVERWORLD_GLACIER_TOP_HEIGHT            = registerOverworld("glacier_top_height",            withoutY(OverworldColumn::getGlacierTopHeightD           ), null),
		OVERWORLD_GLACIER_CRACK_FRACTION        = registerOverworld("glacier_crack_fraction",        withoutY(OverworldColumn::getGlacierCrackFraction        ), null),
		OVERWORLD_GLACIER_CRACK_THRESHOLD       = registerOverworld("glacier_crack_threshold",       withoutY(OverworldColumn::getGlacierCrackThreshold       ), null),

		OVERWORLD_CAVE_NOISE                    = registerOverworld("cave_noise",                       withY(OverworldColumn::getCaveNoise                   ), null),
		OVERWORLD_CACHED_CAVE_NOISE             = registerOverworld("cached_cave_noise",                withY(OverworldColumn::getCachedCaveNoise             ), null),
		OVERWORLD_CAVE_DEPTH                    = registerOverworld("cave_depth",                    withoutY(OverworldColumn::getCaveDepthD                  ), null),
		OVERWORLD_CAVE_SURFACE_DEPTH            = registerOverworld("cave_surface_depth",            withoutY(OverworldColumn::getCaveSurfaceDepth            ), null),
		OVERWORLD_NORMALIZED_CAVE_SURFACE_DEPTH = registerOverworld("normalized_cave_surface_depth", withoutY(OverworldColumn::getNormalizedCaveSurfaceDepth  ), null),
		OVERWORLD_CAVE_NOISE_THRESHOLD          = registerOverworld("cave_noise_threshold",             withY(OverworldColumn::getCaveNoiseThreshold          ), null),
		OVERWORLD_CAVE_EFFECTIVE_WIDTH          = registerOverworld("cave_effective_width",             withY(OverworldColumn::getCaveEffectiveWidth          ), null),
		OVERWORLD_CAVE_SYSTEM_CENTER_X          = registerOverworld("cave_system_center_x",          withoutY(OverworldColumn::getCaveSystemCenterX           ), OverworldColumn::debugCaveSystemCenterX),
		OVERWORLD_CAVE_SYSTEM_CENTER_Z          = registerOverworld("cave_system_center_z",          withoutY(OverworldColumn::getCaveSystemCenterZ           ), OverworldColumn::debugCaveSystemCenterZ),
		OVERWORLD_CAVE_SYSTEM_EDGINESS          = registerOverworld("cave_system_edginess",          withoutY(OverworldColumn::getCaveSystemEdginess          ), null),
		OVERWORLD_CAVE_SYSTEM_EDGINESS_SQUARED  = registerOverworld("cave_system_edginess_squared",  withoutY(OverworldColumn::getCaveSystemEdginessSquared   ), null),

		OVERWORLD_CAVERN_AVERAGE_CENTER         = registerOverworld("cavern_average_center",         withoutY(OverworldColumn::getCavernAverageCenter         ), null),
		OVERWORLD_CAVERN_CENTER                 = registerOverworld("cavern_center",                 withoutY(OverworldColumn::getCavernCenter                ), null),
		OVERWORLD_CAVERN_THICKNESS_SQUARED      = registerOverworld("cavern_thickness_squared",      withoutY(OverworldColumn::getCavernThicknessSquared      ), null),
		OVERWORLD_CAVERN_THICKNESS              = registerOverworld("cavern_thickness",              withoutY(OverworldColumn::getCavernThickness             ), null),
		OVERWORLD_CAVERN_CENTER_X               = registerOverworld("cavern_center_x",               withoutY(OverworldColumn::getCavernCenterX               ), OverworldColumn::debugCavernCenterX),
		OVERWORLD_CAVERN_CENTER_Z               = registerOverworld("cavern_center_z",               withoutY(OverworldColumn::getCavernCenterZ               ), OverworldColumn::debugCavernCenterZ),
		OVERWORLD_CAVERN_EDGINESS               = registerOverworld("cavern_edginess",               withoutY(OverworldColumn::getCavernEdginess              ), null),
		OVERWORLD_CAVERN_EDGINESS_SQUARED       = registerOverworld("cavern_edginess_squared",       withoutY(OverworldColumn::getCavernEdginessSquared       ), null),

		OVERWORLD_SKYLAND_AVERAGE_CENTER        = registerOverworld("skyland_average_center",        withoutY(OverworldColumn::getSkylandAverageCenter        ), null),
		OVERWORLD_SKYLAND_CENTER                = registerOverworld("skyland_center",                withoutY(OverworldColumn::getSkylandCenter               ), null),
		OVERWORLD_SKYLAND_THICKNESS             = registerOverworld("skyland_thickness",             withoutY(OverworldColumn::getSkylandThickness            ), null),
		OVERWORLD_SKYLAND_AUXILIARY_NOISE       = registerOverworld("skyland_auxiliary_noise",       withoutY(OverworldColumn::getSkylandAuxiliaryNoise       ), null),
		OVERWORLD_SKYLAND_MIN_Y                 = registerOverworld("skyland_min_y",                 withoutY(OverworldColumn::getSkylandMinY                 ), null),
		OVERWORLD_SKYLAND_MAX_Y                 = registerOverworld("skyland_max_y",                 withoutY(OverworldColumn::getSkylandMaxY                 ), null),
		OVERWORLD_SKYLAND_CENTER_X              = registerOverworld("skyland_center_x",              withoutY(OverworldColumn::getSkylandCenterX              ), OverworldColumn::debugSkylandCenterX),
		OVERWORLD_SKYLAND_CENTER_Z              = registerOverworld("skyland_center_z",              withoutY(OverworldColumn::getSkylandCenterZ              ), OverworldColumn::debugSkylandCenterZ),
		OVERWORLD_SKYLAND_EDGINESS              = registerOverworld("skyland_edginess",              withoutY(OverworldColumn::getSkylandEdginess             ), null),
		OVERWORLD_SKYLAND_EDGINESS_SQUARED      = registerOverworld("skyland_edginess_squared",      withoutY(OverworldColumn::getSkylandEdginessSquared      ), null);

	@SuppressWarnings("unused")
	public static final ColumnValue<NetherColumn>
		NETHER_BIOME_CENTER_X                   = registerNether   ("biome_center_x",                withoutY(   NetherColumn::getBiomeCenterX                ), NetherColumn::debugBiomeCenterX),
		NETHER_BIOME_CENTER_Z                   = registerNether   ("biome_center_z",                withoutY(   NetherColumn::getBiomeCenterZ                ), NetherColumn::debugBiomeCenterZ),
		NETHER_BIOME_EDGINESS                   = registerNether   ("biome_edginess",                withoutY(   NetherColumn::getEdginess                    ), null),
		NETHER_BIOME_EDGINESS_SQUARED           = registerNether   ("biome_edginess_squared",        withoutY(   NetherColumn::getEdginessSquared             ), null),
		NETHER_LAVA_LEVEL                       = registerNether   ("lava_level",                    withoutY(   NetherColumn::getLavaLevel                   ), null),
		NETHER_CAVE_NOISE                       = registerNether   ("cave_noise",                       withY(   NetherColumn::getCaveNoise                   ), null),
		NETHER_CACHED_CAVE_NOISE                = registerNether   ("cached_cave_noise",                withY(   NetherColumn::getCachedCaveNoise             ), null),
		NETHER_CAVE_NOISE_THRESHOLD             = registerNether   ("cave_noise_threshold",             withY(   NetherColumn::getCaveNoiseThreshold          ), null),
		NETHER_CAVE_EFFECTIVE_WIDTH             = registerNether   ("cave_effective_width",             withY(   NetherColumn::getCaveEffectiveWidth          ), null),
		NETHER_CAVERN_NOISE                     = registerNether   ("cavern_noise",                     withY(   NetherColumn::getCavernNoise                 ), null),
		NETHER_CACHED_CAVERN_NOISE              = registerNether   ("cached_cavern_noise",              withY(   NetherColumn::getCachedCavernNoise           ), null);

	@SuppressWarnings("unused")
	public static final ColumnValue<EndColumn>
		END_WARP_X                              = registerEnd      ("warp_x",                        withoutY(      EndColumn::getWarpX                       ), null),
		END_WARP_Z                              = registerEnd      ("warp_z",                        withoutY(      EndColumn::getWarpZ                       ), null),
		END_WARP_RADIUS                         = registerEnd      ("warp_radius",                   withoutY(      EndColumn::getWarpRadius                  ), null),
		END_WARP_ANGLE                          = registerEnd      ("warp_angle",                    withoutY(      EndColumn::getWarpAngle                   ), null),
		END_DISTANCE_TO_ORIGIN                  = registerEnd      ("distance_to_origin",            withoutY(      EndColumn::getDistanceToOrigin            ), EndColumn::debug_distanceToOrigin),
		END_ANGLE_TO_ORIGIN                     = registerEnd      ("angle_to_origin",               withoutY(      EndColumn::getAngleToOrigin               ), null),
		END_MOUNTAIN_CENTER_Y                   = registerEnd      ("mountain_center_y",             withoutY(      EndColumn::getMountainCenterY             ), null),
		END_MOUNTAIN_THICKNESS                  = registerEnd      ("mountain_thickness",            withoutY(      EndColumn::getMountainThickness           ), null),
		END_FOLIAGE                             = registerEnd      ("foliage",                       withoutY(      EndColumn::getFoliage                     ), null),
		NEST_NOISE                              = registerEnd      ("nest_noise",                       withY(      EndColumn::getNestNoise                   ), null),
		END_RING_CLOUD_HORIZONTAL_BIAS          = registerEnd      ("ring_cloud_horizontal_bias",    withoutY(      EndColumn::getRingCloudHorizontalBias     ), null),
		END_LOWER_RING_CLOUD_CENTER_Y           = registerEnd      ("lower_ring_cloud_center_y",     withoutY(      EndColumn::getLowerRingCloudCenterY       ), null),
		END_UPPER_RING_CLOUD_CENTER_Y           = registerEnd      ("upper_ring_cloud_center_y",     withoutY(      EndColumn::getUpperRingCloudCenterY       ), null),
		END_LOWER_RING_CLOUD_VERTICAL_BIAS      = registerEnd      ("lower_ring_cloud_vertical_bias",   withY(      EndColumn::getLowerRingCloudVerticalBias  ), null),
		END_UPPER_RING_CLOUD_VERTICAL_BIAS      = registerEnd      ("upper_ring_cloud_vertical_bias",   withY(      EndColumn::getUpperRingCloudVerticalBias  ), null),
		END_LOWER_RING_CLOUD_BIAS               = registerEnd      ("lower_ring_cloud_bias",            withY(      EndColumn::getLowerRingCloudBias          ), null),
		END_UPPER_RING_CLOUD_BIAS               = registerEnd      ("upper_ring_cloud_bias",            withY(      EndColumn::getUpperRingCloudBias          ), null),
		END_LOWER_RING_CLOUD_RAW_NOISE          = registerEnd      ("lower_ring_cloud_raw_noise",       withY(      EndColumn::getRingCloudRawNoise           ), null),
		END_UPPER_RING_CLOUD_RAW_NOISE          = registerEnd      ("upper_ring_cloud_raw_noise",       withY(      EndColumn::getRingCloudRawNoise           ), null),
		END_LOWER_RING_CLOUD_BIASED_NOISE       = registerEnd      ("lower_ring_cloud_biased_noise",    withY(      EndColumn::getLowerRingCloudBiasedNoise   ), null),
		END_UPPER_RING_CLOUD_BIASED_NOISE       = registerEnd      ("upper_ring_cloud_biased_noise",    withY(      EndColumn::getUpperRingCloudBiasedNoise   ), null),
		END_BRIDGE_CLOUD_RADIAL_BIAS            = registerEnd      ("bridge_cloud_radial_bias",      withoutY(      EndColumn::getBridgeCloudRadialBias       ), null),
		END_BRIDGE_CLOUD_ANGULAR_BIAS           = registerEnd      ("bridge_cloud_angular_bias",     withoutY(      EndColumn::getBridgeCloudAngularBias      ), null),
		END_BRIDGE_CLOUD_HORIZONTAL_BIAS        = registerEnd      ("bridge_cloud_horizontal_bias",  withoutY(      EndColumn::getBridgeCloudHorizontalBias   ), null),
		END_LOWER_BRIDGE_CLOUD_CENTER_Y         = registerEnd      ("lower_bridge_cloud_center_y",   withoutY(      EndColumn::getLowerBridgeCloudCenterY     ), null),
		END_UPPER_BRIDGE_CLOUD_CENTER_Y         = registerEnd      ("upper_bridge_cloud_center_y",   withoutY(      EndColumn::getUpperBridgeCloudCenterY     ), null),
		END_LOWER_BRIDGE_CLOUD_VERTICAL_BIAS    = registerEnd      ("lower_bridge_cloud_vertical_bias", withY(      EndColumn::getLowerBridgeCloudVerticalBias), null),
		END_UPPER_BRIDGE_CLOUD_VERTICAL_BIAS    = registerEnd      ("upper_bridge_cloud_vertical_bias", withY(      EndColumn::getUpperBridgeCloudVerticalBias), null),
		END_LOWER_BRIDGE_CLOUD_BIAS             = registerEnd      ("lower_bridge_cloud_bias",          withY(      EndColumn::getLowerBridgeCloudBias        ), null),
		END_UPPER_BRIDGE_CLOUD_BIAS             = registerEnd      ("upper_bridge_cloud_bias",          withY(      EndColumn::getUpperBridgeCloudBias        ), null),
		END_LOWER_BRIDGE_CLOUD_RAW_NOISE        = registerEnd      ("lower_bridge_cloud_raw_noise",     withY(      EndColumn::getBridgeCloudRawNoise         ), null),
		END_UPPER_BRIDGE_CLOUD_RAW_NOISE        = registerEnd      ("upper_bridge_cloud_raw_noise",     withY(      EndColumn::getBridgeCloudRawNoise         ), null),
		END_LOWER_BRIDGE_CLOUD_BIASED_NOISE     = registerEnd      ("lower_bridge_cloud_biased_noise",  withY(      EndColumn::getLowerBridgeCloudBiasedNoise ), null),
		END_UPPER_BRIDGE_CLOUD_BIASED_NOISE     = registerEnd      ("upper_bridge_cloud_biased_noise",  withY(      EndColumn::getUpperBridgeCloudBiasedNoise ), null);

	public final Class<T_Column> columnClass;
	public final Getter<T_Column> getter;
	public final @Nullable CustomDisplay customDisplay;

	public ColumnValue(
		Class<T_Column> columnClass,
		Getter<T_Column> getter,
		@Nullable CustomDisplay customDisplay
	) {
		this.columnClass   = columnClass;
		this.getter        = getter;
		this.customDisplay = customDisplay;
	}

	/** provided for completeness with {@link #withoutY(GetterWithoutY)}. */
	public static <T_Column extends WorldColumn> Getter<T_Column> withY(Getter<T_Column> getter) {
		return getter;
	}

	/**
	provided for syntax sugar, because withoutY(Column::getValue) is
	so much cleaner than (GetterWithoutY<Column>)(Column::getValue).
	*/
	public static <T_Column extends WorldColumn> GetterWithoutY<T_Column> withoutY(GetterWithoutY<T_Column> getter) {
		return getter;
	}

	public static ColumnValue<WorldColumn> registerAnyDim(String name, Getter<WorldColumn> getter, @Nullable CustomDisplay customDisplay) {
		return register(name, WorldColumn.class, getter, customDisplay);
	}

	public static ColumnValue<VanillaWorldColumn> registerVanilla(String name, Getter<VanillaWorldColumn> getter, @Nullable CustomDisplay customDisplay) {
		return Registry.register(REGISTRY, BigGlobeMod.mcID(name), new ColumnValue<>(VanillaWorldColumn.class, getter, customDisplay));
	}

	public static ColumnValue<OverworldColumn> registerOverworld(String name, Getter<OverworldColumn> getter, @Nullable CustomDisplay customDisplay) {
		return register("overworld/" + name, OverworldColumn.class, getter, customDisplay);
	}

	public static ColumnValue<NetherColumn> registerNether(String name, Getter<NetherColumn> getter, @Nullable CustomDisplay customDisplay) {
		return register("nether/" + name, NetherColumn.class, getter, customDisplay);
	}

	public static ColumnValue<EndColumn> registerEnd(String name, Getter<EndColumn> getter, @Nullable CustomDisplay customDisplay) {
		return register("end/" + name, EndColumn.class, getter, customDisplay);
	}

	public static <T_Column extends WorldColumn> ColumnValue<T_Column> register(String name, Class<T_Column> columnClass, Getter<T_Column> getter, @Nullable CustomDisplay customDisplay) {
		return Registry.register(REGISTRY, BigGlobeMod.modID(name), new ColumnValue<>(columnClass, getter, customDisplay));
	}

	@SuppressWarnings("unchecked")
	public double getValue(WorldColumn column, double y) {
		return this.accepts(column) ? this.getter.getFrom((T_Column)(column), y) : Double.NaN;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public double getValueWithoutY(WorldColumn column) {
		return this.accepts(column) ? ((GetterWithoutY)(this.getter)).getFrom(column) : Double.NaN;
	}

	public boolean accepts(WorldColumn column) {
		return this.columnClass.isInstance(column);
	}

	public boolean dependsOnY() {
		return !(this.getter instanceof GetterWithoutY<?>);
	}

	public String getName() {
		return Objects.toString(REGISTRY.getId(this), "<unregistered>");
	}

	public String getDisplayText(CustomDisplayContext context) {
		if (this.customDisplay != null) {
			return this.customDisplay.getDisplayText(context);
		}
		else {
			return CustomDisplayContext.format(this.getValue(context.column, context.y()));
		}
	}

	@Override
	public String toString() {
		return "ColumnValue: " + this.getName();
	}

	@FunctionalInterface
	public static interface Getter<T_Column extends WorldColumn> {

		public abstract double getFrom(T_Column column, double y);
	}

	@FunctionalInterface
	public static interface GetterWithoutY<T_Column extends WorldColumn> extends Getter<T_Column> {

		public abstract double getFrom(T_Column column);

		@Override
		public default double getFrom(T_Column column, double y) {
			return this.getFrom(column);
		}
	}

	@FunctionalInterface
	public static interface CustomDisplay {

		public abstract String getDisplayText(CustomDisplayContext context);
	}

	public static class CustomDisplayContext {

		public static final String NOT_APPLICABLE = "N/A";
		public static final DecimalFormat DECIMAL_FORMAT;
		static {
			DECIMAL_FORMAT = new DecimalFormat();
			DECIMAL_FORMAT.setDecimalSeparatorAlwaysShown(true);
			DECIMAL_FORMAT.setMinimumFractionDigits(1);
			DECIMAL_FORMAT.setMaximumFractionDigits(3);
			DecimalFormatSymbols symbols = DECIMAL_FORMAT.getDecimalFormatSymbols();
			symbols.setNaN(NOT_APPLICABLE);
			DECIMAL_FORMAT.setDecimalFormatSymbols(symbols);
		}

		public final PlayerEntity player;
		public final WorldColumn column;
		public final int y;

		public CustomDisplayContext(PlayerEntity player, WorldColumn column, int y) {
			this.player = player;
			this.column = column;
			this.y = y;
		}

		public CustomDisplayContext(ServerPlayerEntity player) {
			this.player = player;
			this.y = floorI(player.getY());
			this.column = WorldColumn.forWorld(
				EntityVersions.getServerWorld(player),
				floorI(player.getX()),
				floorI(player.getZ())
			);
		}

		@SuppressWarnings("unchecked")
		public <C extends WorldColumn> C column() {
			return (C)(this.column);
		}

		public PlayerEntity player() {
			return this.player;
		}

		public World world() {
			return EntityVersions.getWorld(this.player);
		}

		public Chunk chunk() {
			return this.world().getChunk(this.x() >> 4, this.z() >> 4);
		}

		public int x() {
			return this.column.x;
		}

		public int y() {
			return this.y;
		}

		public int z() {
			return this.column.z;
		}

		/** returns a unicode arrow which points to the specified coordinates. */
		public char arrow(int x, int z) {
			double angle = Math.toDegrees(Math.atan2(this.z() - z, this.x() - x)) - this.player.getYaw();
			return "→↘↓↙←↖↑↗".charAt(BigGlobeMath.roundI(angle * (8.0D / 360.0D)) & 7);
		}

		public String distance(int x, int z) {
			return format(Math.sqrt(BigGlobeMath.squareD(x - this.x(), z - this.z())));
		}

		public static String format(double number) {
			synchronized (DECIMAL_FORMAT) {
				return DECIMAL_FORMAT.format(number);
			}
		}
	}
}