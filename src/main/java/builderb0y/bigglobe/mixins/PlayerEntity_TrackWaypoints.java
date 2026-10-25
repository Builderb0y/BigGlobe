package builderb0y.bigglobe.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.hyperspace.PackedWorldPos;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;
import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;

@Mixin(Player.class)
public abstract class PlayerEntity_TrackWaypoints implements WaypointTracker {

	@Unique
	public PlayerWaypointManager bigglobe_waypoints;

	@Override
	public @Nullable PlayerWaypointManager bigglobe_getWaypointManager() {
		return this.bigglobe_waypoints;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void bigglobe_initPlayerWaypointManager(
		Level world,
		BlockPos pos,
		float yaw,
		GameProfile profile,
		CallbackInfo callback
	) {
		this.bigglobe_waypoints = PlayerWaypointManager.create((Player)(Object)(this));
	}

	@Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
	private void bigglobe_loadHyperspaceEntrance(CompoundTag nbt, CallbackInfo callback) {
		Tag entranceNBT = nbt.get("bigglobe_hyperspace_entrance");
		if (this.bigglobe_waypoints != null && entranceNBT != null) try {
			this.bigglobe_waypoints.entrance = BigGlobeAutoCodec.AUTO_CODEC.decode(
				PackedWorldPos.CoderHolder.CODER,
				entranceNBT,
				NbtOps.INSTANCE
			);
		}
		catch (Exception exception) {
			BigGlobeMod.LOGGER.error("Could not load hyperspace entrance for " + ((Player)(Object)this).getName().getString(), exception);
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
	private void bigglobe_saveHyperspaceEntrance(CompoundTag nbt, CallbackInfo callback) {
		if (this.bigglobe_waypoints != null && this.bigglobe_waypoints.entrance != null) try {
			Tag entranceNBT = BigGlobeAutoCodec.AUTO_CODEC.encode(
				PackedWorldPos.CoderHolder.CODER,
				this.bigglobe_waypoints.entrance,
				NbtOps.INSTANCE
			);
			nbt.put("bigglobe_hyperspace_entrance", entranceNBT);
		}
		catch (Exception exception) {
			BigGlobeMod.LOGGER.error("Could not save hyperspace entrance for " + ((Player)(Object)this).getName().getString(), exception);
		}
	}
}
