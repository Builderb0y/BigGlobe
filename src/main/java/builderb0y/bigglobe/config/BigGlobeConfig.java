package builderb0y.bigglobe.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry.BoundedDiscrete;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.CollapsibleObject;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.EnumHandler;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.EnumHandler.EnumDisplayOption;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Excluded;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.opengl.*;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

import builderb0y.autocodec.annotations.*;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.UndergroundMode;
import builderb0y.bigglobe.compat.ClothConfigCompat;
import builderb0y.bigglobe.compat.InstalledMods;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;
import builderb0y.bigglobe.rendering.lods.*;

//reminder: any time I add something new to this file, I need to add a lang entry for it too.
@Config(name = BigGlobeMod.MODID)
@UseFixer(name = "INSTANCE", in = BigGlobeConfigFixer.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public class BigGlobeConfig {

	@Excluded
	public static final Supplier<BigGlobeConfig> INSTANCE = ClothConfigCompat.init();
	public static void init() {}

	public void validatePostLoad() {
		this.threads = Math.max(Math.min(this.threads, Runtime.getRuntime().availableProcessors()), 1);
		this.moltenRockOreificationChance = MathHelper.clamp(this.moltenRockOreificationChance, 0.0F, 1.0F);
		this.lodRendering.validatePostLoad();
		this.distantHorizonsIntegration.validatePostLoad();
		this.voxyIntegration.validatePostLoad();
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
	public int threads = Math.max(Runtime.getRuntime().availableProcessors() - 2, 1);

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
		public boolean structureSpawning = false;

		@Tooltip(count = 3)
		@UseName("Log empty tags")
		@DefaultIgnore
		public boolean emptyTags = false;

		@Tooltip(count = 3)
		@UseName("Reject unused overriders")
		@DefaultIgnore
		public boolean rejectUnusedOverriders = false;

		@Tooltip(count = 3)
		@UseName("Log extra mob spawns")
		@DefaultIgnore
		public boolean logExtraMobSpawns = false;
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
					case ON   -> true;
					case OFF  -> false;
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

		public static enum RendererBackend {
			AUTO,
			SIMPLE_SEPARATE,
			SIDED_SEPARATE,
			SIDED_COMBINED;

			@Environment(EnvType.CLIENT)
			public LodRenderer createRenderer(LodRendering config) {
				int quads = config.maxQuads;
				return switch (this) {
					case            AUTO -> GL.getCapabilities().GL_ARB_shader_draw_parameters ? new SidedCombinedLodRenderer(quads) : new SidedSeparateLodRenderer(quads);
					case SIMPLE_SEPARATE -> new SimpleLodRenderer(quads);
					case  SIDED_SEPARATE -> new SidedSeparateLodRenderer(quads);
					case  SIDED_COMBINED -> new SidedCombinedLodRenderer(quads);
				};
			}
		}

		@Tooltip(count = 7)
		@UseName("Renderer Backend")
		@EnumHandler(option = EnumDisplayOption.BUTTON)
		@DefaultIgnore
		public RendererBackend rendererBackend = RendererBackend.AUTO;
		@Excluded
		public static transient RendererBackend previousRendererBackend = RendererBackend.AUTO;

		@Environment(EnvType.CLIENT)
		public LodRenderer createRendererBackend() {
			return this.rendererBackend.createRenderer(this);
		}

		@Tooltip(count = 3)
		@UseName("Maximum Quad Count")
		@DefaultIgnore
		@BoundedDiscrete(min = 10_000_000L, max = 100_000_000L)
		@VerifyIntRange(min = 10_000_000L, max = 100_000_000L)
		public int maxQuads = 20_000_000;
		@Excluded
		public static transient int previousMaxQuads = 20_000_000;

		@Tooltip(count = 3)
		@UseName("Quality")
		@DefaultIgnore
		public double quality = 2.0D;

		@Tooltip(count = 3)
		@UseName("Min View Distance")
		@DefaultIgnore
		public float minViewDistance = 0.25F;

		@Tooltip(count = 3)
		@UseName("Max View Distance")
		@DefaultIgnore
		public float maxViewDistance = 1024.0F;

		@Tooltip(count = 3)
		@UseName("Generation Buffer Distance")
		@DefaultIgnore
		public float generationBufferDistance = 1536.0F;

		@Tooltip(count = 3)
		@UseName("Fog Density")
		@DefaultIgnore
		public float fogDensity = 64.0F;

		@Tooltip(count = 3)
		@UseName("Fog Height Scale")
		@DefaultIgnore
		public float fogHeightScale = 4.0F;

		@Tooltip(count = 5)
		@UseName("Underground Mode")
		@EnumHandler(option = EnumDisplayOption.BUTTON)
		@DefaultIgnore
		public UndergroundMode undergroundMode = UndergroundMode.FILL;
		@Excluded
		public static transient UndergroundMode previousUndergroundMode = UndergroundMode.FILL;

		public void validatePostLoad() {
			this.maxQuads = MathHelper.clamp(this.maxQuads, 10_000_000, 100_000_000);
			this.quality = MathHelper.clamp(this.quality, 1.0D, 3.0D);
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
			MinecraftClient client = MinecraftClient.getInstance();
			LodSystemHolder holder = client != null ? (LodSystemHolder)(client.worldRenderer) : null;
			if (
				this.renderingEnabled() != previousEnabled         ||
				this.rendererBackend    != previousRendererBackend ||
				this.maxQuads           != previousMaxQuads        ||
				this.undergroundMode    != previousUndergroundMode
			) {
				previousEnabled         = this.renderingEnabled();
				previousRendererBackend = this.rendererBackend;
				previousMaxQuads        = this.maxQuads;
				previousUndergroundMode = this.undergroundMode;
				if (holder != null) LodSystem.reload(holder, client.world);
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
	@UseName("Voxy Integration")
	@CollapsibleObject(startExpanded = true)
	@DefaultIgnore
	public final VoxyIntegration voxyIntegration = new VoxyIntegration();

	public static class VoxyIntegration {

		@Tooltip(count = 3)
		@UseName("Use Worldgen Thread")
		@DefaultIgnore
		public boolean useWorldgenThread = true;

		@Tooltip(count = 5)
		@UseName("Underground Mode")
		@EnumHandler(option = EnumDisplayOption.BUTTON)
		@DefaultIgnore
		public UndergroundMode undergroundMode = UndergroundMode.NONE;

		@Tooltip(count = 3)
		@UseName("Light Air")
		@DefaultIgnore
		public boolean lightAir = false;

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
		public boolean multiThreadedStructures = false;

		public boolean multiThreadedStructures() {
			return InstalledMods.C2ME && this.multiThreadedStructures;
		}
	}

	/**
	tricks AutoCodec into ignoring missing data and leaving
	the java object as it was after initialization,
	while simultaneously tricking intellij into
	*not* complaining that the object can be null.
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
	public static final int CONFIG_VERSION = 1;
}