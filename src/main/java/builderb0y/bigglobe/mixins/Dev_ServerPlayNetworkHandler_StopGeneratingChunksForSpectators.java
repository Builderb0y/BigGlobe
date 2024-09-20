package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

/**
turns out, the server will still generate chunks for spectators
when /gamerule spectatorsGenerateChunks is set to false.
this mixin fixes that bug.
*/
@Mixin(ServerPlayNetworkHandler.class)
public class Dev_ServerPlayNetworkHandler_StopGeneratingChunksForSpectators {

	@WrapOperation(method = "onPlayerMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;hasLandedInFluid()Z"))
	private boolean bigglobe_checkSpectatorFirst(ServerPlayerEntity instance, Operation<Boolean> original) {
		return instance.isSpectator() || original.call(instance);
	}
}