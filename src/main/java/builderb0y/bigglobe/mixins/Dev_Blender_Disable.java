package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.ChunkRegion;
import net.minecraft.world.gen.chunk.Blender;

/**
the default {@link Blender#getBlender(ChunkRegion)}
implementation pauses on chunk IO sometimes,
which makes worldgen slower. so, I'm yeeting it in development.
*/
@Mixin(Blender.class)
public class Dev_Blender_Disable {

	@Shadow @Final private static Blender NO_BLENDING;

	@Overwrite
	public static Blender getBlender(ChunkRegion chunkRegion) {
		return NO_BLENDING;
	}
}