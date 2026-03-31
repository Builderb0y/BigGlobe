package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import builderb0y.bigglobe.versions.GameruleVersions;

@Mixin(ServerLevel.class)
public abstract class ServerWorld_CustomTimeSpeed extends Level {

	@Shadow @Final private boolean tickTime;
	@Unique private double bigglobe_customTime;

	public ServerWorld_CustomTimeSpeed() {
		super(null, null, null, null, false, false, 0L, 0);
	}

	/**
	for this to work I'd basically need to wrap part of the method in a while loop.
	since mixin can't do that, the next best solution is wrapping the whole method
	and calling it multiple times. tickTime will still be checked multiple times,
	but this should have a very low performance impact.
	*/
	@WrapMethod(method = "tickTime")
	protected void tickTime(Operation<Void> original) {
		if (this.tickTime) {
			this.bigglobe_customTime += GameruleVersions.daylightCycleSpeed((ServerLevel)(Object)(this));
			int elapsedTicks = (int)(this.bigglobe_customTime);
			this.bigglobe_customTime -= elapsedTicks;
			while (--elapsedTicks >= 0) {
				original.call();
			}
		}
	}
}