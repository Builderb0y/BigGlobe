package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import builderb0y.bigglobe.versions.GameruleVersions;

@Mixin(ServerLevel.class)
public abstract class ServerWorld_CustomTimeSpeed extends Level {

	@Unique
	private double bigglobe_customTime;

	public ServerWorld_CustomTimeSpeed() {
		super(null, null, null, null, false, false, 0L, 0);
	}

	@WrapOperation(method = "tickTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"))
	private void bigglobe_tickTime(ServerLevel instance, long timeOfDay, Operation<Void> original) {
		this.bigglobe_customTime += GameruleVersions.daylightCycleSpeed(instance);
		int elapsedTicks = (int)(this.bigglobe_customTime);
		if (elapsedTicks > 0) {
			this.bigglobe_customTime -= elapsedTicks;
			original.call(instance, timeOfDay + elapsedTicks - 1L);
		}
	}
}