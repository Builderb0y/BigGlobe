package builderb0y.bigglobe.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.CollapsibleObject;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Excluded;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip;

import builderb0y.autocodec.annotations.Mirror;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.compat.ClothConfigCompat;

//reminder: any time I add something new to this file, I need to add a lang entry for it too.
@Config(name = BigGlobeMod.MODID)
public class BigGlobeConfig {

	@Excluded
	public static final Supplier<BigGlobeConfig> INSTANCE = ClothConfigCompat.init();
	public static void init() {}

	public void validatePostLoad() {
		this.distantHorizonsIntegration.validatePostLoad();
		this.voxyIntegration.validatePostLoad();
		this.playerSpawning.validatePostLoad();
	}

	@Tooltip(count = 3)
	@UseName("Make Big Globe the default world type")
	@DefaultIgnore
	public boolean makeBigGlobeDefaultWorldType = true;

	@Tooltip(count = 3)
	@UseName("Big Globe Trees In Big Globe Worlds")
	@DefaultIgnore
	public boolean bigGlobeTreesInBigGlobeWorlds = true;

	@Tooltip(count = 3)
	@UseName("Big Globe Trees In Other Worlds")
	@DefaultIgnore
	public boolean bigGlobeTreesInOtherWorlds = false;

	@Tooltip(count = 3)
	@UseName("Hyperspace Enabled")
	@DefaultIgnore
	public boolean hyperspaceEnabled = true;

	@Tooltip(count = 3)
	@UseName("Output Data Pack Debugging Information")
	@DefaultIgnore
	public boolean dataPackDebugging = false;

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

		@Tooltip(count = 3)
		@UseName("Skip Structures")
		@DefaultIgnore
		public boolean skipStructures = false;

		@Tooltip(count = 3)
		@UseName("Skip Underground")
		@DefaultIgnore
		public boolean skipUnderground = true;

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

		@Tooltip(count = 3)
		@UseName("Skip Underground")
		@DefaultIgnore
		public boolean skipUnderground = true;

		public void validatePostLoad() {}
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
	public static @interface DefaultIgnore {}
}