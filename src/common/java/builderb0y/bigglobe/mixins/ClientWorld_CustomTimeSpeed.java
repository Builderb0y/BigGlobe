package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

import builderb0y.bigglobe.ClientState;

@Mixin(ClientLevel.class)
@Environment(EnvType.CLIENT)
public abstract class ClientWorld_CustomTimeSpeed extends Level {

	@Unique
	private double bigglobe_customTime;

	public ClientWorld_CustomTimeSpeed() {
		super(null, null, null, null, false, false, 0L, 0);
	}

	/**
	use same strategy as {@link ServerWorld_CustomTimeSpeed},
	not because it's necessary on clients too, but in case someone
	else mixes into tickTime() and wants to be notified when the time changes.
	*/
	@WrapMethod(method = "tickTime")
	private void tickTime(Operation<Void> original) {
		ClientState state = ClientState.get(this.dimension());
		if (state != null) {
			this.bigglobe_customTime += state.timeSpeed;
			int elapsedTicks = (int)(this.bigglobe_customTime);
			this.bigglobe_customTime -= elapsedTicks;
			while (--elapsedTicks >= 0) {
				original.call();
			}
		}
		else {
			original.call();
		}
	}
}