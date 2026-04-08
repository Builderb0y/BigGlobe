package builderb0y.bigglobe.rendering2.lods;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.rendering2.lods.LodGenerator.LoadMode;
import builderb0y.bigglobe.util.BigGlobeThreadPool;

public abstract class GenerationPipeline {

	public final LodSystem system;
	public final LodGenerator<?> generator;
	public final Executor generationExecutor;

	public GenerationPipeline(LodSystem system, LodGenerator<?> generator) {
		this.system = system;
		this.generator = generator;
		this.generationExecutor = Executors.newSingleThreadExecutor(
			Thread
			.ofPlatform()
			.name("Big Globe LOD generation thread")
			.uncaughtExceptionHandler((Thread thread, Throwable exception) -> {
				BigGlobeMod.LOGGER.error("Uncaught exception in " + thread, exception);
			})
			.daemon()
			.factory()
		);
	}

	public CompletableFuture<QuadPacker> request(BoundingBox area, byte lod, LoadMode mode, QuadPacker packer) {
		return (
			CompletableFuture.supplyAsync(
				() -> this.generateWithPadding(area, lod, mode),
				this.generationExecutor
			)
			.thenApplyAsync(
				(ColumnBlockGetter generatedArea) -> {
					try (generatedArea) {
						LodMesher.mesh(generatedArea, packer);
					}
					return packer;
				},
				BigGlobeThreadPool.lodExecutor()
			)
		);
	}


	public abstract ColumnBlockGetter generateWithPadding(BoundingBox area, byte lod, LoadMode mode);
}