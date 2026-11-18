package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
turns out, the server will still generate chunks for spectators
when /gamerule spectatorsGenerateChunks is set to false.
this mixin fixes that bug.

in MC 26.3 this should technically target onClimbable() instead,
but short circuit evaluation means that targeting hasLandedInLiquid()
works just as well, and simplifies cross-version support.
*/
@Mixin(ServerGamePacketListenerImpl.class)
public class Dev_ServerPlayNetworkHandler_StopGeneratingChunksForSpectators {

	@WrapOperation(
		#if MC_VERSION >= MC_26_3_0
			method = "handlePlayerPositionChange",
		#else
			method = "handleMovePlayer",
		#endif
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;hasLandedInLiquid()Z")
	)
	private boolean bigglobe_checkSpectatorFirst(ServerPlayer instance, Operation<Boolean> original) {
		return instance.isSpectator() || original.call(instance);
	}
}