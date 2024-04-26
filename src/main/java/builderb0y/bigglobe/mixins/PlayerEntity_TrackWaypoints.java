package builderb0y.bigglobe.mixins;

import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import builderb0y.bigglobe.hyperspace.ClientWaypointManager;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;

@Mixin(PlayerEntity.class)
public class PlayerEntity_TrackWaypoints implements WaypointTracker {

	public ClientWaypointManager bigglobe_waypoints;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void bigglobe_initClientWaypointManager(World world, BlockPos pos, float yaw, GameProfile gameProfile, CallbackInfo callback) {
		this.bigglobe_waypoints = new ClientWaypointManager(null);
	}

	@Override
	public ClientWaypointManager bigglobe_getWaypointManager() {
		return this.bigglobe_waypoints;
	}

	@Override
	public void bigglobe_setWaypointManager(ClientWaypointManager entrance) {
		this.bigglobe_waypoints = entrance;
	}

	//todo: save entrance at the absolute least to NBT.
}