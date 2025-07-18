package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import builderb0y.bigglobe.gamerules.BigGlobeGameRules;

@Mixin(ServerWorld.class)
public abstract class ServerWorld_CustomTimeSpeed extends World {

	@Unique
	private double bigglobe_customTime;

	public ServerWorld_CustomTimeSpeed() {
		#if MC_VERSION >= MC_1_21_2
			super(null, null, null, null, false, false, 0L, 0);
		#else
			super(null, null, null, null, null, false, false, 0L, 0);
		#endif
	}

	#if MC_VERSION >= MC_1_21_2
		@Shadow public abstract GameRules getGameRules();
	#endif

	@WrapOperation(method = "tickTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setTimeOfDay(J)V"))
	private void bigglobe_tickTime(ServerWorld instance, long timeOfDay, Operation<Void> original) {
		this.bigglobe_customTime += this.getGameRules().get(BigGlobeGameRules.DAYLIGHT_CYCLE_SPEED).get();
		int elapsedTicks = (int)(this.bigglobe_customTime);
		if (elapsedTicks > 0) {
			this.bigglobe_customTime -= elapsedTicks;
			original.call(instance, timeOfDay + elapsedTicks - 1L);
		}
	}
}