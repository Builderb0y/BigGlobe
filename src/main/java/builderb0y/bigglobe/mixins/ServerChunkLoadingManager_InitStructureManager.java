package builderb0y.bigglobe.mixins;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import com.mojang.datafixers.DataFixer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.ChunkStatusChangeListener;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelStorage.Session;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

#if MC_VERSION >= MC_1_21_0
	@Mixin(net.minecraft.server.world.ServerChunkLoadingManager.class)
#else
	@Mixin(net.minecraft.server.world.ThreadedAnvilChunkStorage.class)
#endif
public class ServerChunkLoadingManager_InitStructureManager {

	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/chunk/ChunkGenerator;createStructurePlacementCalculator(Lnet/minecraft/registry/RegistryWrapper;Lnet/minecraft/world/gen/noise/NoiseConfig;J)Lnet/minecraft/world/gen/chunk/placement/StructurePlacementCalculator;", shift = Shift.AFTER))
	private void bigglobe_initStructureManager(
		ServerWorld world,
		Session session,
		DataFixer dataFixer,
		StructureTemplateManager structureTemplateManager,
		Executor executor,
		ThreadExecutor<Runnable> mainThreadExecutor,
		ChunkProvider chunkProvider,
		ChunkGenerator chunkGenerator,
		#if MC_VERSION < MC_1_21_9
			net.minecraft.server.WorldGenerationProgressListener worldGenerationProgressListener,
		#endif
		ChunkStatusChangeListener chunkStatusChangeListener,
		Supplier<PersistentStateManager> persistentStateManagerFactory,
		#if MC_VERSION >= MC_1_21_5
			ChunkTicketManager ticketManager,
		#endif
		int viewDistance,
		boolean dsync,
		CallbackInfo callback
	) {
		if (chunkGenerator instanceof BigGlobeScriptedChunkGenerator generator) {
			generator.initStructureManager(world.getServer().getSaveProperties().getGeneratorOptions().shouldGenerateStructures());
		}
	}
}