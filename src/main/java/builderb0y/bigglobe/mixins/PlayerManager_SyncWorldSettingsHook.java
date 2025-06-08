package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.compat.ImmersivePortalsCompat;
import builderb0y.bigglobe.compat.InstalledMods;

@Mixin(PlayerManager.class)
public class PlayerManager_SyncWorldSettingsHook {

	@Shadow @Final private MinecraftServer server;

	@Inject(method = "sendWorldInfo", at = @At("RETURN"))
	private void bigglobe_syncWorldSettings(ServerPlayerEntity player, ServerWorld world, CallbackInfo callback) {
		if (InstalledMods.IMMERSIVE_PORTALS) {
			ClientState.syncWaypoints(world, player);
		}
		else {
			ClientState.sync(world, player);
		}
	}

	@Inject(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;sendWorldInfo(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/world/ServerWorld;)V"))
	private void bigglobe_syncAllDimSettings(
		ClientConnection connection,
		ServerPlayerEntity player,
		#if MC_VERSION >= MC_1_20_2
		net.minecraft.server.network.ConnectedClientData clientData,
		#endif
		CallbackInfo callback
	) {
		if (InstalledMods.IMMERSIVE_PORTALS) {
			ImmersivePortalsCompat.forEachDimension(this.server, player, ClientState::sync);
		}
	}
}