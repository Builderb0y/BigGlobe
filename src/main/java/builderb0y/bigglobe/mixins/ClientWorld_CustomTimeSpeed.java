package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.world.ClientWorld.Properties;
import net.minecraft.world.World;

import builderb0y.bigglobe.ClientState;

@Mixin(ClientWorld.class)
@Environment(EnvType.CLIENT)
public abstract class ClientWorld_CustomTimeSpeed extends World {

	@Unique
	private double bigglobe_customTime;

	public ClientWorld_CustomTimeSpeed() {
		#if MC_VERSION >= MC_1_21_2
			super(null, null, null, null, false, false, 0L, 0);
		#else
			super(null, null, null, null, null, false, false, 0L, 0);
		#endif
	}

	#if MC_VERSION >= MC_1_21_2
		@WrapOperation(method = "tickTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld$Properties;setTimeOfDay(J)V"))
		private void bigglobe_tickTime(Properties instance, long timeOfDay, Operation<Void> original)
	#else
		@WrapOperation(method = "tickTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;setTimeOfDay(J)V"))
		private void bigglobe_tickTime(ClientWorld instance, long timeOfDay, Operation<Void> original)
	#endif
	{
		ClientState state = ClientState.get(this.getRegistryKey());
		if (state != null) {
			this.bigglobe_customTime += state.timeSpeed;
			int elapsedTicks = (int)(this.bigglobe_customTime);
			if (elapsedTicks > 0) {
				this.bigglobe_customTime -= elapsedTicks;
				original.call(instance, timeOfDay + elapsedTicks - 1L);
			}
		}
		else {
			original.call(instance, timeOfDay);
		}
	}
}