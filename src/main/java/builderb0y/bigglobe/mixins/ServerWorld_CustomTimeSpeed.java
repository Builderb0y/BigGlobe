package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import builderb0y.bigglobe.gamerules.BigGlobeGameRules;

@Mixin(ServerWorld.class)
public abstract class ServerWorld_CustomTimeSpeed extends World {

	@Unique
	private double bigglobe_customTime;

	public ServerWorld_CustomTimeSpeed() {
		#if MC_VERSION <= MC_1_19_2
			super(null, null, null, null, false, false, 0L, 0);
		#else
			super(null, null, null, null, null, false, false, 0L, 0);
		#endif
	}

	@Shadow
	public abstract void setTimeOfDay(long timeOfDay);

	@Inject(method = "tickTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/MutableWorldProperties;getTimeOfDay()J"), cancellable = true)
	private void bigglobe_tickTime(CallbackInfo callback) {
		this.bigglobe_customTime += this.getGameRules().get(BigGlobeGameRules.DAYLIGHT_CYCLE_SPEED).get();
		int elapsedTicks = (int)(this.bigglobe_customTime);
		if (elapsedTicks > 0) {
			this.bigglobe_customTime -= elapsedTicks;
			this.setTimeOfDay(this.properties.getTimeOfDay() + elapsedTicks);
		}
		callback.cancel();
	}
}