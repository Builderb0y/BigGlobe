package builderb0y.bigglobe.mixins;

import com.seibel.distanthorizons.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.distanthorizons.core.dataObjects.transformers.LodDataBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import loaderCommon.fabric.com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.compat.dhChunkGen.DHOverworldChunkGenerator;
import builderb0y.bigglobe.compat.dhChunkGen.FakeChunk;

/** not currently enabled, only for use in testing. to be removed later once DH's API adds a replacement. */
@Mixin(LodDataBuilder.class)
public class DistantHorizons_LodDataBuilder_BuildBigGlobeChunks {

	@Inject(method = "createChunkData(Lcom/seibel/distanthorizons/core/wrapperInterfaces/chunk/IChunkWrapper;)Lcom/seibel/distanthorizons/core/dataObjects/fullData/accessor/ChunkSizedFullDataAccessor;", at = @At("HEAD"), cancellable = true, remap = false)
	private static void bigglobe_generateFromColumns(IChunkWrapper chunkWrapper, CallbackInfoReturnable<ChunkSizedFullDataAccessor> callback) {
		if (chunkWrapper instanceof ChunkWrapper impl && impl.getChunk() instanceof FakeChunk fakeChunk) {
			ChunkSizedFullDataAccessor result = DHOverworldChunkGenerator.generateForReal(fakeChunk);
			if (result != null) callback.setReturnValue(result);
			else BigGlobeMod.LOGGER.warn("Failed to generate DH chunk");
		}
	}
}