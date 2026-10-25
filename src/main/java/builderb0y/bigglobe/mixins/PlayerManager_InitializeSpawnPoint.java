package builderb0y.bigglobe.mixins;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import builderb0y.bigglobe.spawning.BigGlobeSpawnLocator;

@Mixin(PlayerList.class)
public abstract class PlayerManager_InitializeSpawnPoint {

	@Inject(method = "load", at = @At("RETURN"))
	private void bigglobe_setPerPlayerSpawnIfEnabled(
		ServerPlayer player,
		CallbackInfoReturnable<Optional<CompoundTag>> callback
	) {
		if (callback.getReturnValue().isEmpty()) { //player's first time on the server.
			BigGlobeSpawnLocator.initPlayerSpawn(player);
		}
	}
}
