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
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

#if MC_VERSION >= MC_1_20_3
	import net.minecraft.block.spawner.MobSpawnerLogic;
#else
	import net.minecraft.world.MobSpawnerLogic;
#endif

@Mixin(MobSpawnerLogic.class)
public abstract class MobSpawnerLogic_SpawnLightning {

	@Unique
	public boolean bigglobe_spawnLightning;

	#if MC_VERSION >= MC_1_21_6

		@Inject(method = "readData", at = @At("HEAD"))
		private void bigglobe_readLightning(World world, BlockPos pos, net.minecraft.storage.ReadView view, CallbackInfo callback) {
			this.bigglobe_spawnLightning = view.getBoolean("bigglobe_SpawnLightning", this.bigglobe_spawnLightning);
		}

		@Inject(method = "writeData", at = @At("HEAD"))
		private void bigglobe_writeLightning(net.minecraft.storage.WriteView view, CallbackInfo callback) {
			view.putBoolean("bigglobe_SpawnLightning", this.bigglobe_spawnLightning);
		}

	#else

		@Inject(method = "readNbt", at = @At("HEAD"))
		private void bigglobe_readLightning(World world, BlockPos pos, NbtCompound nbt, CallbackInfo callback) {
			if (nbt.get("bigglobe_SpawnLightning") instanceof AbstractNbtNumber number) {
				this.bigglobe_spawnLightning = number.byteValue() != 0;
			}
		}

		@Inject(method = "writeNbt", at = @At("HEAD"))
		private void bigglobe_writeLightning(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> callback) {
			nbt.putBoolean("bigglobe_SpawnLightning", this.bigglobe_spawnLightning);
		}

	#endif

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