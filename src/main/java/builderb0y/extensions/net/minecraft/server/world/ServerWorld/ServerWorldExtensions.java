package builderb0y.extensions.net.minecraft.server.world.ServerWorld;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Extension
public class ServerWorldExtensions {

	public static ChunkGenerator getChunkGenerator(@This ServerWorld world) {
		return world.getChunkManager().getChunkGenerator();
	}

	public static @Nullable BigGlobeScriptedChunkGenerator getScriptedChunkGenerator(@This ServerWorld world) {
		return world.getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator ? generator : null;
	}

	public static @NotNull BigGlobeScriptedChunkGenerator requireScriptedChunkGenerator(@This ServerWorld world) {
		return (BigGlobeScriptedChunkGenerator)(world.getChunkGenerator());
	}
}