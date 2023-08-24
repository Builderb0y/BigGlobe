package builderb0y.bigglobe.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;

import builderb0y.bigglobe.ClientState;

@Mixin(ClientWorld.class)
@Environment(EnvType.CLIENT)
public abstract class ClientWorld_CustomTimeSpeed extends World {

	@Unique
	private double bigglobe_customTime;

	public ClientWorld_CustomTimeSpeed() {
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
		this.bigglobe_customTime += ClientState.timeSpeed;
		int elapsedTicks = (int)(this.bigglobe_customTime);
		if (elapsedTicks > 0) {
			this.bigglobe_customTime -= elapsedTicks;
			this.setTimeOfDay(this.properties.getTimeOfDay() + elapsedTicks);
		}
		callback.cancel();
	}
}