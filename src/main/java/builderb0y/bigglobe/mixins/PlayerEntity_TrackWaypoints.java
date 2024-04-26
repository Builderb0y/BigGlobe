package builderb0y.bigglobe.mixins;

import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;

@Mixin(PlayerEntity.class)
public class PlayerEntity_TrackWaypoints implements WaypointTracker {

	public PlayerWaypointManager bigglobe_waypoints;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void bigglobe_initPlayerWaypointManager(World world, BlockPos pos, float yaw, GameProfile gameProfile, CallbackInfo callback) {
		this.bigglobe_waypoints = PlayerWaypointManager.forPlayer((PlayerEntity)(Object)(this));
	}

	@Override
	public PlayerWaypointManager bigglobe_getWaypointManager() {
		return this.bigglobe_waypoints;
	}

	@Override
	public void bigglobe_setWaypointManager(PlayerWaypointManager entrance) {
		this.bigglobe_waypoints = entrance;
	}

	//todo: save entrance at the absolute least to NBT.
}