package builderb0y.bigglobe.mixins;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import builderb0y.bigglobe.registration.BigGlobeGameRules;

@Mixin(ServerWorld.class)
public abstract class ServerWorld_CustomTimeSpeed extends World {

	@Unique
	private double bigglobe_customTime;

	public ServerWorld_CustomTimeSpeed(
		MutableWorldProperties properties,
		RegistryKey<World> registryRef,
		RegistryEntry<DimensionType> dimension,
		Supplier<Profiler> profiler,
		boolean isClient,
		boolean debugWorld,
		long seed,
		int maxChainedNeighborUpdates
	) {
		super(properties, registryRef, dimension, profiler, isClient, debugWorld, seed, maxChainedNeighborUpdates);
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