package builderb0y.bigglobe.mixins;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import builderb0y.bigglobe.spawning.BigGlobeSpawnLocator;

@Mixin(PlayerManager.class)
public abstract class PlayerManager_InitializeSpawnPoint {

	@Inject(method = "loadPlayerData", at = @At("RETURN"))
	private void bigglobe_setPerPlayerSpawnIfEnabled(
		ServerPlayerEntity player,
		#if MC_VERSION >= MC_1_21_6 net.minecraft.util.ErrorReporter errorReporter, #endif
		CallbackInfoReturnable<Optional<?>> callback
	) {
		if (callback.getReturnValue() == null) { //player's first time on the server.
			BigGlobeSpawnLocator.initPlayerSpawn(player);
		}
	}
}