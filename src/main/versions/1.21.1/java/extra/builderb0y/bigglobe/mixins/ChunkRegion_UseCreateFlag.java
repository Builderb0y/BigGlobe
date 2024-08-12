package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

/**
when calling getChunk(), there is a parameter named "create".
if this flag is set to true, the world is supposed to generate
or load the chunk, or throw an exception if this is not possible.
ChunkRegion however ignores this flag, and throws regardless.
this is a vanilla bug, which I am fixing here.
*/
@Mixin(ChunkRegion.class)
public class ChunkRegion_UseCreateFlag {

	@Inject(
		method = "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/crash/CrashReport;create(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/util/crash/CrashReport;"
		),
		cancellable = true
	)
	private void bigglobe_returnNullIfCreateIsFalse(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create, CallbackInfoReturnable<Chunk> callback) {
		if (!create) callback.setReturnValue(null);
	}
}