package builderb0y.bigglobe.mixins;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.hyperspace.PackedWorldPos;
import builderb0y.bigglobe.hyperspace.PackedWorldPos.CoderHolder;
import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;

@Mixin(PlayerEntity.class)
public class PlayerEntity_TrackWaypoints implements WaypointTracker {

	public @Nullable PlayerWaypointManager bigglobe_waypoints;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void bigglobe_initPlayerWaypointManager(World world, BlockPos pos, float yaw, GameProfile gameProfile, CallbackInfo callback) {
		this.bigglobe_waypoints = PlayerWaypointManager.create((PlayerEntity)(Object)(this));
	}

	@Override
	public @Nullable PlayerWaypointManager bigglobe_getWaypointManager() {
		return this.bigglobe_waypoints;
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
	private void bigglobe_saveHyperspaceEntrance(NbtCompound nbt, CallbackInfo callback) {
		if (this.bigglobe_waypoints != null && this.bigglobe_waypoints.entrance != null) {
			nbt.put(
				"bigglobe_hyperspace_entrance",
				BigGlobeAutoCodec.AUTO_CODEC.encode(
					PackedWorldPos.CoderHolder.CODER,
					this.bigglobe_waypoints.entrance,
					NbtOps.INSTANCE
				)
			);
		}
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
	private void bigglobe_loadHyperspaceEntrance(NbtCompound nbt, CallbackInfo callback) {
		if (this.bigglobe_waypoints != null && nbt.get("bigglobe_hyperspace_entrance") instanceof NbtCompound compound) try {
			this.bigglobe_waypoints.entrance = BigGlobeAutoCodec.AUTO_CODEC.decode(
				PackedWorldPos.CoderHolder.CODER,
				compound,
				NbtOps.INSTANCE
			);
		}
		catch (DecodeException exception) {
			BigGlobeMod.LOGGER.warn("Failed to decode hyperspace entrance point for player " + this + ':', exception);
		}
	}
}