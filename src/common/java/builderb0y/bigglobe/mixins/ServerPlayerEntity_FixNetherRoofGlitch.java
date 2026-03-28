package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.versions.EntityVersions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
if only it was this easy to fix the bug that causes people
to enjoy exploiting glitches without consequences...
*/
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntity_FixNetherRoofGlitch extends Player {

	@Shadow
	public ServerGamePacketListenerImpl connection;

	public ServerPlayerEntity_FixNetherRoofGlitch() {
		super(null, null);
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void bigglobe_fixNetherRoofGlitch(CallbackInfo callback) {
		double moveDown;
		if (
			EntityVersions.getGameMode((ServerPlayer)(Object)(this)).isSurvival() &&
			EntityVersions.getServerWorld((ServerPlayer)(Object)(this)).getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator &&
			generator.nether_overrides != null && generator.nether_overrides.prevent_roof_exploration() &&
			(moveDown = this.getBoundingBox().maxY - generator.height.max_y()) > 0.0D
		) {
			this.setDeltaMovement(Vec3.ZERO);
			this.needsSync = true;
			this.connection.teleport(this.getX(), this.getY() - moveDown, this.getZ(), this.getYRot(), this.getXRot());
			BigGlobeMod.LOGGER.warn(this.getName().getString() + " ended up on the nether roof somehow. Teleporting them back down so that they're not stranded there.");
		}
	}
}