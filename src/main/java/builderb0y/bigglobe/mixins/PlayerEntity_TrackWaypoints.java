package builderb0y.bigglobe.mixins;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
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

	@Unique
	public @Nullable PlayerWaypointManager bigglobe_waypoints;

	@Override
	public @Nullable PlayerWaypointManager bigglobe_getWaypointManager() {
		return this.bigglobe_waypoints;
	}

	#if MC_VERSION >= MC_1_21_6

		@Inject(method = "<init>", at = @At("RETURN"))
		private void bigglobe_initPlayerWaypointManager(
			World world,
			GameProfile profile,
			CallbackInfo callback
		) {
			this.bigglobe_waypoints = PlayerWaypointManager.create((PlayerEntity)(Object)(this));
		}

		@Inject(method = "readCustomData", at = @At("RETURN"))
		private void bigglobe_loadHyperspaceEntrance(net.minecraft.storage.ReadView view, CallbackInfo callback) {
			if (this.bigglobe_waypoints != null) {
				this.bigglobe_waypoints.entrance = view.read("bigglobe_hyperspace_entrance", BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(PackedWorldPos.CoderHolder.CODER)).orElse(null);
			}
		}

		@Inject(method = "writeCustomData", at = @At("RETURN"))
		private void bigglobe_saveHyperspaceEntrance(net.minecraft.storage.WriteView view, CallbackInfo callback) {
			if (this.bigglobe_waypoints != null && this.bigglobe_waypoints.entrance != null) {
				view.put(
					"bigglobe_hyperspace_entrance",
					BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(
						PackedWorldPos.CoderHolder.CODER
					),
					this.bigglobe_waypoints.entrance
				);
			}
		}

	#else

		@Inject(method = "<init>", at = @At("RETURN"))
		private void bigglobe_initPlayerWaypointManager(
			World world,
			BlockPos pos,
			float yaw,
			GameProfile gameProfile,
			CallbackInfo callback
		) {
			this.bigglobe_waypoints = PlayerWaypointManager.create((PlayerEntity)(Object)(this));
		}

		@Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
		private void bigglobe_loadHyperspaceEntrance(NbtCompound nbt, CallbackInfo callback) {
			NbtElement entranceNBT = nbt.get("bigglobe_hyperspace_entrance");
			if (this.bigglobe_waypoints != null && entranceNBT != null) try {
				this.bigglobe_waypoints.entrance = BigGlobeAutoCodec.AUTO_CODEC.decode(
					PackedWorldPos.CoderHolder.CODER,
					entranceNBT,
					NbtOps.INSTANCE
				);
			}
			catch (DecodeException exception) {
				BigGlobeMod.LOGGER.warn("Failed to decode hyperspace entrance point for player " + this + ':', exception);
			}
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

	#endif
}