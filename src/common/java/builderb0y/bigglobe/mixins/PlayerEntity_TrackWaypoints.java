package builderb0y.bigglobe.mixins;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.hyperspace.PackedWorldPos;
import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;

@Mixin(Player.class)
public class PlayerEntity_TrackWaypoints implements WaypointTracker {

	@Unique
	public @Nullable PlayerWaypointManager bigglobe_waypoints;

	@Override
	public @Nullable PlayerWaypointManager bigglobe_getWaypointManager() {
		return this.bigglobe_waypoints;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void bigglobe_initPlayerWaypointManager(
		Level world,
		GameProfile profile,
		CallbackInfo callback
	) {
		this.bigglobe_waypoints = PlayerWaypointManager.create((Player)(Object)(this));
	}

	@Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
	private void bigglobe_loadHyperspaceEntrance(ValueInput view, CallbackInfo callback) {
		if (this.bigglobe_waypoints != null) {
			this.bigglobe_waypoints.entrance = view.read("bigglobe_hyperspace_entrance", BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(PackedWorldPos.CoderHolder.CODER)).orElse(null);
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
	private void bigglobe_saveHyperspaceEntrance(ValueOutput view, CallbackInfo callback) {
		if (this.bigglobe_waypoints != null && this.bigglobe_waypoints.entrance != null) {
			view.store(
				"bigglobe_hyperspace_entrance",
				BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(
					PackedWorldPos.CoderHolder.CODER
				),
				this.bigglobe_waypoints.entrance
			);
		}
	}
}