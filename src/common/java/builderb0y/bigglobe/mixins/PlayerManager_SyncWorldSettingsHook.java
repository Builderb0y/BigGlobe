package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.compat.ImmersivePortalsCompat;
import builderb0y.bigglobe.compat.InstalledMods;

@Mixin(PlayerList.class)
public class PlayerManager_SyncWorldSettingsHook {

	@Shadow
	@Final
	private MinecraftServer server;

	@Inject(method = "sendLevelInfo", at = @At("RETURN"))
	private void bigglobe_syncWorldSettings(ServerPlayer player, ServerLevel world, CallbackInfo callback) {
		if (InstalledMods.IMMERSIVE_PORTALS) {
			ClientState.syncWaypoints(world, player);
		}
		else {
			ClientState.sync(world, player);
		}
	}

	@Inject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;sendLevelInfo(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/level/ServerLevel;)V"))
	private void bigglobe_syncAllDimSettings(
		Connection connection,
		ServerPlayer player,
		CommonListenerCookie clientData,
		CallbackInfo callback
	) {
		if (InstalledMods.IMMERSIVE_PORTALS) {
			ImmersivePortalsCompat.forEachDimension(this.server, player, ClientState::sync);
		}
	}
}