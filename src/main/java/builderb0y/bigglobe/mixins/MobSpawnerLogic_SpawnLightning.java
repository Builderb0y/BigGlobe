package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.MobSpawnerLogic;
import net.minecraft.world.World;

@Mixin(MobSpawnerLogic.class)
public abstract class MobSpawnerLogic_SpawnLightning {

	@Unique
	public boolean bigglobe_spawnLightning;

	@Inject(method = "readNbt", at = @At("HEAD"))
	private void bigglobe_readLightning(World world, BlockPos pos, NbtCompound nbt, CallbackInfo callback) {
		if (nbt.contains("bigglobe_SpawnLightning", NbtCompound.NUMBER_TYPE)) {
			this.bigglobe_spawnLightning = nbt.getBoolean("bigglobe_SpawnLightning");
		}
	}

	@Inject(method = "writeNbt", at = @At("HEAD"))
	private void bigglobe_writeLightning(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> callback) {
		nbt.putBoolean("bigglobe_SpawnLightning", this.bigglobe_spawnLightning);
	}

	@Inject(method = "serverTick", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
	private void bigglobe_spawnLightning(ServerWorld world, BlockPos pos, CallbackInfo callback, boolean spawned) {
		if (spawned && this.bigglobe_spawnLightning) {
			LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
			lightning.setPosition(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
			lightning.setCosmetic(true);
			world.spawnEntity(lightning);
		}
	}
}