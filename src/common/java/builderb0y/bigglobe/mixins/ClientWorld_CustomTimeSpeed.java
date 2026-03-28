package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientLevel.ClientLevelData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import builderb0y.bigglobe.ClientState;

@Mixin(ClientLevel.class)
@Environment(EnvType.CLIENT)
public abstract class ClientWorld_CustomTimeSpeed extends Level {

	@Unique
	private double bigglobe_customTime;

	public ClientWorld_CustomTimeSpeed() {
		super(null, null, null, null, false, false, 0L, 0);
	}

	@WrapOperation(method = "tickTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel$ClientLevelData;setDayTime(J)V"))
	private void bigglobe_tickTime(ClientLevelData instance, long timeOfDay, Operation<Void> original) {
		ClientState state = ClientState.get(this.dimension());
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