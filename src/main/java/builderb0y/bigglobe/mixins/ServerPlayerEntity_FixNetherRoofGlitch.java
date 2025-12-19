package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.versions.EntityVersions;

/**
if only it was this easy to fix the bug that causes people
to enjoy exploiting glitches without consequences...
*/
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntity_FixNetherRoofGlitch extends PlayerEntity {

	@Shadow public ServerPlayNetworkHandler networkHandler;

	public ServerPlayerEntity_FixNetherRoofGlitch() {
		super(null, null #if MC_VERSION < MC_1_21_6 , 0.0F, null #endif);
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void bigglobe_fixNetherRoofGlitch(CallbackInfo callback) {
		double moveDown;
		if (
			EntityVersions.getGameMode((ServerPlayerEntity)(Object)(this)).isSurvivalLike() &&
			EntityVersions.getServerWorld((ServerPlayerEntity)(Object)(this)).getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator &&
			generator.nether_overrides != null && generator.nether_overrides.prevent_roof_exploration() &&
			(moveDown = this.getBoundingBox().maxY - generator.height.max_y()) > 0.0D
		) {
			this.setVelocity(Vec3d.ZERO);
			this.velocityDirty = true;
			this.networkHandler.requestTeleport(this.getX(), this.getY() - moveDown, this.getZ(), this.getYaw(), this.getPitch());
			BigGlobeMod.LOGGER.warn(this.getName().getString() + " ended up on the nether roof somehow. Teleporting them back down so that they're not stranded there.");
		}
	}
}