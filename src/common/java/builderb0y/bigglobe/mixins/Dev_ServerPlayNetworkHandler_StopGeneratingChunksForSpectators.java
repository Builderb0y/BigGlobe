package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
turns out, the server will still generate chunks for spectators
when /gamerule spectatorsGenerateChunks is set to false.
this mixin fixes that bug.
*/
@Mixin(ServerGamePacketListenerImpl.class)
public class Dev_ServerPlayNetworkHandler_StopGeneratingChunksForSpectators {

	@WrapOperation(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;hasLandedInLiquid()Z"))
	private boolean bigglobe_checkSpectatorFirst(ServerPlayer instance, Operation<Boolean> original) {
		return instance.isSpectator() || original.call(instance);
	}
}