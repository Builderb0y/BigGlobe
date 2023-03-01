package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.ClientState;

@Mixin(PlayerManager.class)
public class PlayerManager_SyncOverworldSettingsHook {

	@Inject(method = "sendWorldInfo", at = @At("RETURN"))
	private void bigglobe_syncOverworldSettings(ServerPlayerEntity player, ServerWorld world, CallbackInfo callback) {
		ClientState.sync(player);
	}
}