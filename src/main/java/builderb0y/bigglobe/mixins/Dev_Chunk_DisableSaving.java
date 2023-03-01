package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;

/**
prevents chunks from being saved to disk.
not only does this save disk space,
but it also means I can exit to the main menu faster.
plus I delete existing chunk data on world load anyway,
so it's not doing any harm to just not save it in the first place.
*/
@Mixin(Chunk.class)
public class Dev_Chunk_DisableSaving {

	@Shadow @Final protected HeightLimitView heightLimitView;

	@Inject(method = "needsSaving", at = @At("HEAD"), cancellable = true)
	private void bigglobe_cancelSave(CallbackInfoReturnable<Boolean> callback) {
		if (
			this.heightLimitView instanceof ServerWorld world &&
			world.getChunkManager().getChunkGenerator() instanceof BigGlobeChunkGenerator &&
			switch (world.getServer().getDefaultGameMode()) {
				case CREATIVE, SPECTATOR -> true;
				case SURVIVAL, ADVENTURE -> false;
			}
		) {
			callback.setReturnValue(false);
		}
	}
}