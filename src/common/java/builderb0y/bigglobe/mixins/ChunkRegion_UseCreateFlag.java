package builderb0y.bigglobe.mixins;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
when calling getChunk(), there is a parameter named "create".
if this flag is set to true, the world is supposed to generate
or load the chunk, or throw an exception if this is not possible.
ChunkRegion however ignores this flag, and throws regardless.
this is a vanilla bug, which I am fixing here.
*/
@Mixin(WorldGenRegion.class)
public class ChunkRegion_UseCreateFlag {

	@Inject(
		method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/CrashReport;forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;"
		),
		cancellable = true
	)
	private void bigglobe_returnNullIfCreateIsFalse(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create, CallbackInfoReturnable<ChunkAccess> callback) {
		if (!create) callback.setReturnValue(null);
	}
}