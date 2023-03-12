package builderb0y.bigglobe.columns;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
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

import static builderb0y.bigglobe.math.BigGlobeMath.floorI;

@UseCoder(name = "CODER", usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public class ColumnValue<T_Column extends WorldColumn> {

	public static final ObjectArrayFactory<ColumnValue<?>> ARRAY_FACTORY = new ObjectArrayFactory<>(ColumnValue.class).generic();
	public static final RegistryKey<Registry<ColumnValue<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.modID("column_value"));
	public static final Registry<ColumnValue<?>> REGISTRY = BigGlobeMod.newRegistry(REGISTRY_KEY);
	public static final AutoCoder<ColumnValue<?>> CODER = new AutoCoder<>() {

		@Override
		public @Nullable <T_Encoded> ColumnValue<?> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			return get(context.forceAsString());
		}

		@Override
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, ColumnValue<?>> context) throws EncodeException {
			if (context.input == null) return context.empty();
			Identifier id = REGISTRY.getId(context.input);
			if (id == null) throw new EncodeException("Unregistered ColumnValue: " + context.input);
			return context.createString(id.getNamespace().equals(BigGlobeMod.MODID) ? id.getPath() : id.toString());
		}
	};

	public static @Nullable ColumnValue<?> get(String name) {
		try {
			int colon = name.indexOf(':');
			Identifier id;
			if (colon >= 0) {
				id = new Identifier(name.substring(0, colon), name.substring(colon + 1));
			}
			else {
				id = BigGlobeMod.modID(name);
			}
			return REGISTRY.get(id);
		}
		catch (InvalidIdentifierException ignored) {
			return null;
		}
	}

	public static final ColumnValue<WorldColumn>
		Y                                       = registerAnyDim   ("y",                                withY(   (column, y) -> y                           ), null),
		MIN_Y                                   = registerAnyDim   ("min_y",                         withoutY(    WorldColumn::getFinalBottomHeightD        ), null),
		MAX_Y                                   = registerAnyDim   ("max_y",                         withoutY(    WorldColumn::getFinalTopHeightD           ), null);

	public static final ColumnValue<OverworldColumn>
		OVERWORLD_SEA_LEVEL                     = registerOverworld("sea_level",                     withoutY(OverworldColumn::getSeaLevel                  ), null),

		OVERWORLD_HILLINESS                     = registerOverworld("hilliness",                     withoutY(OverworldColumn::getHilliness                 ), null),
		OVERWORLD_CLIFFINESS                    = registerOverworld("cliffiness",                    withoutY(OverworldColumn::getCliffiness                ), null),
		OVERWORLD_RAW_EROSION                   = registerOverworld("raw_erosion",                   withoutY(OverworldColumn::getRawErosion                ), null),
		OVERWORLD_PRE_CLIFF_HEIGHT              = registerOverworld("pre_cliff_height",              withoutY(OverworldColumn::getPreCliffHeight            ), null),
		OVERWORLD_POST_CLIFF_HEIGHT             = registerOverworld("post_cliff_height",             withoutY(OverworldColumn::getPostCliffHeight           ), null),

		OVERWORLD_TEMPERATURE                   = registerOverworld("temperature",                   withoutY(OverworldColumn::getTemperature               ), null),
		OVERWORLD_HEIGHT_ADJUSTED_TEMPERATURE   = registerOverworld("height_adjusted_temperature",      withY(OverworldColumn::getHeightAdjustedTemperature ), null),
		OVERWORLD_SURFACE_TEMPERATURE           = registerOverworld("surface_temperature",           withoutY(OverworldColumn::getSurfaceTemperature        ), null),

		OVERWORLD_FOLIAGE                       = registerOverworld("foliage",                       withoutY(OverworldColumn::getFoliage                   ), null),
		OVERWORLD_HEIGHT_ADJUSTED_FOLIAGE       = registerOverworld("height_adjusted_foliage",          withY(OverworldColumn::getHeightAdjustedFoliage     ), null),
		OVERWORLD_SURFACE_FOLIAGE               = registerOverworld("surface_foliage",               withoutY(OverworldColumn::getSurfaceFoliage            ), null),

		OVERWORLD_RAW_SNOW                      = registerOverworld("raw_snow",                      withoutY(OverworldColumn::getRawSnow                   ), null),
		OVERWORLD_SNOW_HEIGHT                   = registerOverworld("snow_height",                   withoutY(OverworldColumn::getSnowHeight                ), null),
		OVERWORLD_SNOW_CHANCE                   = registerOverworld("snow_chance",                   withoutY(OverworldColumn::getSnowChance                ), null),

		OVERWORLD_CAVE_NOISE                    = registerOverworld("cave_noise",                       withY((column, y) -> column.getCaveNoise(floorI(y), false) ), null),
		OVERWORLD_CAVE_SURFACE_DEPTH            = registerOverworld("cave_surface_depth",            withoutY(OverworldColumn::getCaveSurfaceDepth          ), null),
		OVERWORLD_NORMALIZED_CAVE_SURFACE_DEPTH = registerOverworld("normalized_cave_surface_depth", withoutY(OverworldColumn::getNormalizedCaveSurfaceDepth), null),
		OVERWORLD_CAVE_WIDTH                    = registerOverworld("cave_width",                       withY(OverworldColumn::getCaveWidth                 ), null),
		OVERWORLD_CAVE_WIDTH_SQUARED            = registerOverworld("cave_width_squared",               withY(OverworldColumn::getCaveWidthSquared          ), null),
		OVERWORLD_CAVE_SYSTEM_CENTER_X          = registerOverworld("cave_system_center_x",          withoutY(OverworldColumn::getCaveSystemCenterX         ), OverworldColumn::debugCaveSystemCenterX),
		OVERWORLD_CAVE_SYSTEM_CENTER_Z          = registerOverworld("cave_system_center_z",          withoutY(OverworldColumn::getCaveSystemCenterZ         ), OverworldColumn::debugCaveSystemCenterZ),
		OVERWORLD_CAVE_SYSTEM_EDGINESS          = registerOverworld("cave_system_edginess",          withoutY(OverworldColumn::getCaveSystemEdginess        ), null),
		OVERWORLD_CAVE_SYSTEM_EDGINESS_SQUARED  = registerOverworld("cave_system_edginess_squared",  withoutY(OverworldColumn::getCaveSystemEdginessSquared ), null),

		OVERWORLD_CAVERN_AVERAGE_CENTER         = registerOverworld("cavern_average_center",         withoutY(OverworldColumn::getCavernAverageCenter       ), null),
		OVERWORLD_CAVERN_CENTER                 = registerOverworld("cavern_center",                 withoutY(OverworldColumn::getCavernCenter              ), null),
		OVERWORLD_CAVERN_THICKNESS_SQUARED      = registerOverworld("cavern_thickness_squared",      withoutY(OverworldColumn::getCavernThicknessSquared    ), null),
		OVERWORLD_CAVERN_THICKNESS              = registerOverworld("cavern_thickness",              withoutY(OverworldColumn::getCavernThickness           ), null),
		OVERWORLD_CAVERN_CENTER_X               = registerOverworld("cavern_center_x",               withoutY(OverworldColumn::getCavernCenterX             ), OverworldColumn::debugCavernCenterX),
		OVERWORLD_CAVERN_CENTER_Z               = registerOverworld("cavern_center_z",               withoutY(OverworldColumn::getCavernCenterZ             ), OverworldColumn::debugCavernCenterZ),
		OVERWORLD_CAVERN_EDGINESS               = registerOverworld("cavern_edginess",               withoutY(OverworldColumn::getCavernEdginess            ), null),
		OVERWORLD_CAVERN_EDGINESS_SQUARED       = registerOverworld("cavern_edginess_squared",       withoutY(OverworldColumn::getCavernEdginessSquared     ), null),

		OVERWORLD_SKYLAND_AVERAGE_CENTER        = registerOverworld("skyland_average_center",        withoutY(OverworldColumn::getSkylandAverageCenter      ), null),
		OVERWORLD_SKYLAND_CENTER                = registerOverworld("skyland_center",                withoutY(OverworldColumn::getSkylandCenter             ), null),
		OVERWORLD_SKYLAND_THICKNESS             = registerOverworld("skyland_thickness",             withoutY(OverworldColumn::getSkylandThickness          ), null),
		OVERWORLD_SKYLAND_AUXILIARY_NOISE       = registerOverworld("skyland_auxiliary_noise",       withoutY(OverworldColumn::getSkylandAuxiliaryNoise     ), null),
		OVERWORLD_SKYLAND_MIN_Y                 = registerOverworld("skyland_min_y",                 withoutY(OverworldColumn::getSkylandMinY               ), null),
		OVERWORLD_SKYLAND_MAX_Y                 = registerOverworld("skyland_max_y",                 withoutY(OverworldColumn::getSkylandMaxY               ), null),
		OVERWORLD_SKYLAND_CENTER_X              = registerOverworld("skyland_center_x",              withoutY(OverworldColumn::getSkylandCenterX            ), OverworldColumn::debugSkylandCenterX),
		OVERWORLD_SKYLAND_CENTER_Z              = registerOverworld("skyland_center_z",              withoutY(OverworldColumn::getSkylandCenterZ            ), OverworldColumn::debugSkylandCenterZ),
		OVERWORLD_SKYLAND_EDGINESS              = registerOverworld("skyland_edginess",              withoutY(OverworldColumn::getSkylandEdginess           ), null),
		OVERWORLD_SKYLAND_EDGINESS_SQUARED      = registerOverworld("skyland_edginess_squared",      withoutY(OverworldColumn::getSkylandEdginessSquared    ), null);

	public static final ColumnValue<NetherColumn>
		BIOME_CENTER_X                          = registerNether   ("biome_center_x",                withoutY(   NetherColumn::getBiomeCenterX              ), NetherColumn::debugBiomeCenterX),
		BIOME_CENTER_Z                          = registerNether   ("biome_center_z",                withoutY(   NetherColumn::getBiomeCenterZ              ), NetherColumn::debugBiomeCenterZ),
		BIOME_EDGINESS                          = registerNether   ("biome_edginess",                withoutY(   NetherColumn::getEdginess                  ), null),
		BIOME_EDGINESS_SQUARED                  = registerNether   ("biome_edginess_squared",        withoutY(   NetherColumn::getEdginessSquared           ), null);

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

	public static ColumnValue<OverworldColumn> registerOverworld(String name, Getter<OverworldColumn> getter, @Nullable CustomDisplay customDisplay) {
		return register("overworld/" + name, OverworldColumn.class, getter, customDisplay);
	}

	public static ColumnValue<NetherColumn> registerNether(String name, Getter<NetherColumn> getter, @Nullable CustomDisplay customDisplay) {
		return register("nether/" + name, NetherColumn.class, getter, customDisplay);
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
				player.getWorld(),
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
			return this.player.getWorld();
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