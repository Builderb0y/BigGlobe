package builderb0y.bigglobe.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.CollapsibleObject;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.EnumHandler;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.EnumHandler.EnumDisplayOption;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Excluded;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import builderb0y.autocodec.annotations.*;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn.UndergroundMode;
import builderb0y.bigglobe.compat.ClothConfigCompat;
import builderb0y.bigglobe.compat.InstalledMods;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;
import builderb0y.bigglobe.rendering.lods.LodSystem;

//reminder: any time I add something new to this file, I need to add a lang entry for it too.
@Config(name = BigGlobeMod.MODID)
@UseFixer(name = "INSTANCE", in = BigGlobeConfigFixer.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public class BigGlobeConfig {

	@Excluded
	public static final Supplier<BigGlobeConfig> INSTANCE = ClothConfigCompat.init();

	public static void init() {}

	public void validatePostLoad() {
		this.threads = Math.max(Math.min(this.threads, Runtime.getRuntime().availableProcessors()), 1);
		this.moltenRockOreificationChance = Mth.clamp(this.moltenRockOreificationChance, 0.0F, 1.0F);
		this.dataPackDebugging.validatePostLoad();
		this.lodRendering.validatePostLoad();
		this.distantHorizonsIntegration.validatePostLoad();
		this.playerSpawning.validatePostLoad();
	}

	@Tooltip(count = 3)
	@UseName("Default World Type")
	@DefaultIgnore
	public String defaultWorldType = "bigglobe:bigglobe";

	@Tooltip(count = 3)
	@UseName("Sanity Check World Height")
	@DefaultIgnore
	public boolean checkWorldHeight = true;

	@Tooltip(count = 3)
	@UseName("Big Globe Trees In Big Globe Worlds")
	@DefaultIgnore
	public boolean bigGlobeTreesInBigGlobeWorlds = true;

	@Tooltip(count = 3)
	@UseName("Hyperspace Enabled")
	@DefaultIgnore
	public boolean hyperspaceEnabled = true;

	@Tooltip(count = 3)
	@UseName("Molten Rock Ore-ification Chance")
	@DefaultIgnore
	public float moltenRockOreificationChance = 1.0F;

	@Tooltip(count = 3)
	@UseName("Threads")
	@DefaultIgnore
	public int threads = Math.max(Runtime.getRuntime().availableProcessors() - 4, 1);

	public int threads() {
		return Math.max(Math.min(this.threads, Runtime.getRuntime().availableProcessors()), 1);
	}

	@Tooltip(count = 2)
	@UseName("Player Spawning")
	@CollapsibleObject(startExpanded = true)
	@DefaultIgnore
	public final PlayerSpawning playerSpawning = new PlayerSpawning();

	public static class PlayerSpawning {

		@Tooltip(count = 2)
		@UseName("Max Spawn Radius")
		@DefaultIgnore
		public double maxSpawnRadius = 10000.0D;

		@Tooltip(count = 3)
		@UseName("Per-Player Spawn Points")
		@DefaultIgnore
		public boolean perPlayerSpawnPoints = false;

		public void validatePostLoad() {
			this.maxSpawnRadius = Math.max(this.maxSpawnRadius, 0.0D);
		}
	}

	@Tooltip(count = 2)
	@UseName("Data Pack Debugging")
	@CollapsibleObject(startExpanded = false)
	@DefaultIgnore
	public final DataPackDebugging dataPackDebugging = new DataPackDebugging();

	public static class DataPackDebugging {

		@Tooltip(count = 3)
		@UseName("Generate dependency graphs")
		@DefaultIgnore
		public boolean dependencyGraphs = false;

		@Tooltip(count = 3)
		@UseName("Print decision trees")
		@DefaultIgnore
		public boolean decisionTrees = false;

		@Tooltip(count = 3)
		@UseName("Log structure spawn attempts")
		@DefaultIgnore
		public boolean logStructureSpawning = false;

		@Tooltip(count = 3)
		@UseName("Structure spawning log filter")
		@DefaultIgnore
		public String structureLogFilter = "";

		@Excluded
		public static transient Pattern structureLogFilterPattern;

		@Tooltip(count = 3)
		@UseName("Log empty tags")
		@DefaultIgnore
		public boolean emptyTags = false;

		@Tooltip(count = 3)
		@UseName("Reject unused overriders")
		@DefaultIgnore
		public boolean rejectUnusedOverriders = false;

		@Tooltip(count = 4)
		@UseName("Invalid tag handling")
		@EnumHandler(option = EnumDisplayOption.BUTTON)
		@DefaultIgnore
		public InvalidTagHandling invalidTagHandling = InvalidTagHandling.VANILLA;

		@Tooltip(count = 3)
		@UseName("Log extra mob spawns")
		@DefaultIgnore
		public boolean logExtraMobSpawns = false;

		public void validatePostLoad() {
			try {
				structureLogFilterPattern = this.structureLogFilter.isEmpty() ? null : Pattern.compile(this.structureLogFilter);
			}
			catch (PatternSyntaxException exception) {
				structureLogFilterPattern = null;
				BigGlobeMod.LOGGER.warn("Config option 'Structure spawning log filter' was set to an invalid regex: " + this.structureLogFilter, exception);
			}
		}
	}

	public static enum InvalidTagHandling {
		VANILLA,
		FORCE_LOAD,
		FORCE_ABORT;
	}

	@Tooltip(count = 2)
	@UseName("LOD Rendering")
	@CollapsibleObject(startExpanded = true)
	@DefaultIgnore
	public final LodRendering lodRendering = new LodRendering();

	public static class LodRendering {

		public static enum EnabledMode {
			AUTO,
			ON,
			OFF;

			public boolean isEnabled() {
				return switch (this) {
					case AUTO -> !InstalledMods.DISTANT_HORIZONS && !InstalledMods.VOXY;
					case ON -> true;
					case OFF -> false;
				};
			}
		}

		@Tooltip(count = 4)
		@UseName("Enabled")
		@EnumHandler(option = EnumDisplayOption.BUTTON)
		@DefaultIgnore
		public EnabledMode enabled = EnabledMode.AUTO;
		@Excluded
		public static transient boolean previousEnabled = EnabledMode.AUTO.isEnabled();

		public boolean renderingEnabled() {
			return this.enabled.isEnabled();
		}

		@Tooltip(count = 3)
		@UseName("Quality")
		@DefaultIgnore
		public double quality = 7.0D;

		@Tooltip(count = 3)
		@UseName("Max LOD For Chunk Loading")
		@DefaultIgnore
		public int maxLodForChunkLoading = 5;
		@Excluded
		public static transient int previousMaxLodForChunkLoading = 5;

		@Tooltip(count = 3)
		@UseName("Vertical Compression")
		@DefaultIgnore
		public int verticalCompression = 16;
		@Excluded
		public static transient int previousVerticalCompression = 16;

		@Tooltip(count = 3)
		@UseName("Cave Culling Depth")
		@DefaultIgnore
		public int caveCullingDepth = 16;
		@Excluded
		public static transient int previousCaveCullingDepth = 16;

		@Tooltip(count = 3)
		@UseName("Min View Distance")
		@DefaultIgnore
		public float minViewDistance = 0.25F;

		@Tooltip(count = 3)
		@UseName("Max View Distance")
		@DefaultIgnore
		public float maxViewDistance = 256.0F;

		@Tooltip(count = 3)
		@UseName("Generation Buffer Distance")
		@DefaultIgnore
		public float generationBufferDistance = 512.0F;

		@Tooltip(count = 3)
		@UseName("Fog Density")
		@DefaultIgnore
		public float fogDensity = 32.0F;

		@Tooltip(count = 3)
		@UseName("Fog Height Scale")
		@DefaultIgnore
		public float fogHeightScale = 6.0F;

		@Tooltip(count = 5)
		@UseName("Underground Mode")
		@EnumHandler(option = EnumDisplayOption.BUTTON)
		@DefaultIgnore
		public UndergroundMode undergroundMode = UndergroundMode.FILL;
		@Excluded
		public static transient UndergroundMode previousUndergroundMode = UndergroundMode.FILL;

		@Tooltip(count = 3)
		@UseName("Show Progress")
		@DefaultIgnore
		public boolean showProgress = true;

		public void validatePostLoad() {
			this.quality = Mth.clamp(this.quality, 4.0D, 8.0D);
			this.maxLodForChunkLoading = Mth.clamp(this.maxLodForChunkLoading, 0, 5);
			this.verticalCompression = Math.max(this.verticalCompression, 0);
			this.caveCullingDepth = Math.max(this.caveCullingDepth, -1);
			this.minViewDistance = Math.max(this.minViewDistance, 1.0F / 256.0F);
			this.maxViewDistance = Math.max(this.maxViewDistance, this.minViewDistance + 1.0F / 256.0F);
			this.generationBufferDistance = Math.max(this.generationBufferDistance, this.maxViewDistance);
			this.fogDensity = Math.max(this.fogDensity, 0.0F);
			this.fogHeightScale = Math.max(this.fogHeightScale, 0.0F);
			if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
				this.maybeReloadLODs();
			}
		}

		@Environment(EnvType.CLIENT)
		public void maybeReloadLODs() {
			Minecraft client = Minecraft.getInstance();
			LodSystemHolder holder = client != null ? LodSystemHolder.of(client.levelRenderer) : null;
			if (
				this.renderingEnabled() != previousEnabled ||
				this.undergroundMode != previousUndergroundMode ||
				this.maxLodForChunkLoading != previousMaxLodForChunkLoading ||
				this.verticalCompression != previousVerticalCompression ||
				this.caveCullingDepth != previousCaveCullingDepth
			) {
				previousEnabled = this.renderingEnabled();
				previousUndergroundMode = this.undergroundMode;
				previousMaxLodForChunkLoading = this.maxLodForChunkLoading;
				previousVerticalCompression = this.verticalCompression;
				previousCaveCullingDepth = this.caveCullingDepth;
				if (holder != null) LodSystem.reload(holder, client.level);
			}
			else if (holder != null) {
				LodSystem system = holder.bigglobe_getLodSystem();
				if (system != null) {
					system.qualityLimit = this.quality;
				}
			}
		}
	}

	@Tooltip(count = 2)
	@UseName("Distant Horizons Integration")
	@CollapsibleObject(startExpanded = true)
	@DefaultIgnore
	public final DistantHorizonsIntegration distantHorizonsIntegration = new DistantHorizonsIntegration();

	public static class DistantHorizonsIntegration {

		@Tooltip(count = 3)
		@UseName("Hyperspeed Generation")
		@DefaultIgnore
		public boolean hyperspeedGeneration = false;

		@Tooltip(count = 5)
		@UseName("Underground Mode")
		@EnumHandler(option = EnumDisplayOption.BUTTON)
		@DefaultIgnore
		public UndergroundMode undergroundMode = UndergroundMode.FILL;

		public void validatePostLoad() {}
	}

	@Tooltip(count = 2)
	@UseName("C2ME Integration")
	@CollapsibleObject(startExpanded = true)
	@DefaultIgnore
	public final C2MEIntegration c2meIntegration = new C2MEIntegration();

	public static class C2MEIntegration {

		@Tooltip(count = 3)
		@UseName("Multi-Threaded Structures")
		@DefaultIgnore
		public boolean multiThreadedStructures = true;

		public boolean multiThreadedStructures() {
			return InstalledMods.C2ME && this.multiThreadedStructures;
		}
	}

	/**
	tricks AutoCodec into ignoring missing data and leaving
	the java object as it was after initialization,
	while simultaneously tricking intellij into
	not* complaining that the object can be null.
	*/
	@VerifyNullable
	@Mirror(VerifyNullable.class)
	@Target(ElementType.TYPE_USE)
	@Retention(RetentionPolicy.RUNTIME)
	@SuppressWarnings("NullableProblems")
	public static @interface DefaultIgnore {}

	@Excluded
	@Tooltip(count = 1)
	@UseName("Config Version")
	public static final int CONFIG_VERSION = 3;
}