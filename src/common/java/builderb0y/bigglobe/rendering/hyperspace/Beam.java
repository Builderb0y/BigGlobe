package builderb0y.bigglobe.rendering.hyperspace;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.rendering.CommonState;

@Environment(EnvType.CLIENT)
public abstract class Beam {

	public static final int maxBeamTicks = 10 * 20;

	public float
		originX,
		originY,
		originZ,
		directionSeed;
	public int
		beamTicks = -1;
	public Permuter
		random = new Permuter(Permuter.stafford(System.currentTimeMillis() ^ System.nanoTime()));

	public float getBeamTime() {
		return (this.beamTicks + CommonState.partialTicks) / maxBeamTicks * 2.0F - 1.0F;
	}

	public void tick() {
		if (this.beamTicks >= 0) {
			this.beamTicks++;
			if (this.beamTicks >= maxBeamTicks) {
				this.beamTicks = -1;
				this.directionSeed = -1.0F;
				this.originX = 0.0F;
				this.originY = 0.0F;
				this.originZ = 0.0F;
			}
			else if (this.beamTicks == maxBeamTicks >> 1) {
				this.playSound();
			}
		}
		else if (this.random.nextInt(100) == 0) {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null) {
				PlayerWaypointManager manager = PlayerWaypointManager.get(player);
				if (manager != null && manager.collapseProgress < 0) {
					this.beamTicks = 0;
					this.directionSeed = this.random.nextFloat();
					this.originX = (float)(this.random.nextGaussian() * 16.0D);
					this.originY = (float)(this.random.nextGaussian() * 16.0D);
					this.originZ = (float)(this.random.nextGaussian() * 16.0D);
				}
			}
		}
	}

	public abstract void playSound();
}