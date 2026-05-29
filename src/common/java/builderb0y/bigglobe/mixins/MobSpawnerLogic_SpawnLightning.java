package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

@Mixin(BaseSpawner.class)
public abstract class MobSpawnerLogic_SpawnLightning {

	@Unique
	public boolean bigglobe_spawnLightning;

	@Inject(method = "load", at = @At("HEAD"))
	private void bigglobe_readLightning(Level world, BlockPos pos, ValueInput view, CallbackInfo callback) {
		this.bigglobe_spawnLightning = view.getBooleanOr("bigglobe_SpawnLightning", this.bigglobe_spawnLightning);
	}

	@Inject(method = "save", at = @At("HEAD"))
	private void bigglobe_writeLightning(ValueOutput view, CallbackInfo callback) {
		view.putBoolean("bigglobe_SpawnLightning", this.bigglobe_spawnLightning);
	}

	@Inject(method = "serverTick", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
	private void bigglobe_spawnLightning(ServerLevel world, BlockPos pos, CallbackInfo callback, boolean spawned) {
		if (spawned && this.bigglobe_spawnLightning) {
			LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, world);
			lightning.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
			lightning.setVisualOnly(true);
			world.addFreshEntity(lightning);
		}
	}
}