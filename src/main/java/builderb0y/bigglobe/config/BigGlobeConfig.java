package builderb0y.bigglobe.config;

import java.util.function.Supplier;

import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.CollapsibleObject;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Excluded;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip;

import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.compat.ClothConfigCompat;

//reminder: any time I add something new to this file, I need to add a lang entry for it too.
@SuppressWarnings("NullableProblems")
@Config(name = BigGlobeMod.MODID)
public class BigGlobeConfig {

	@Excluded
	public static final Supplier<BigGlobeConfig> INSTANCE = ClothConfigCompat.init();
	public static void init() {}

	public void validatePostLoad() {
		this.distantHorizonsIntegration.validatePostLoad();
		this.playerSpawning.validatePostLoad();
	}

	@Tooltip(count = 3)
	@UseName("Big Globe Trees In Big Globe Worlds")
	@VerifyNullable
	public boolean bigGlobeTreesInBigGlobeWorlds = true;

	@Tooltip(count = 3)
	@UseName("Big Globe Trees In Other Worlds")
	@VerifyNullable
	public boolean bigGlobeTreesInOtherWorlds = false;

	@Tooltip(count = 2)
	@UseName("Distant Horizons Integration")
	@CollapsibleObject(startExpanded = true)
	@VerifyNullable
	public final DistantHorizonsIntegration distantHorizonsIntegration = new DistantHorizonsIntegration();
	public static class DistantHorizonsIntegration {

		@Tooltip(count = 3)
		@UseName("Skip Structures")
		@VerifyNullable
		public boolean skipStructures = false;

		@Tooltip(count = 3)
		@UseName("Skip Underground")
		@VerifyNullable
		public boolean skipUnderground = true;

		@Tooltip(count = 3)
		@UseName("Skip Caves")
		@VerifyNullable
		public boolean skipCaves = true;

		public boolean areCavesSkipped() {
			return this.skipUnderground || this.skipCaves;
		}

		public void validatePostLoad() {}
	}

	@Tooltip(count = 2)
	@UseName("Player Spawning")
	@CollapsibleObject(startExpanded = true)
	@VerifyNullable
	public final PlayerSpawning playerSpawning = new PlayerSpawning();
	public static class PlayerSpawning {

		@Tooltip(count = 2)
		@UseName("Max Spawn Radius")
		@VerifyNullable
		public double maxSpawnRadius = 10000.0D;

		@Tooltip(count = 3)
		@UseName("Per-Player Spawn Points")
		@VerifyNullable
		public boolean perPlayerSpawnPoints = false;

		public void validatePostLoad() {
			this.maxSpawnRadius = Math.max(this.maxSpawnRadius, 0.0D);
		}
	}
}