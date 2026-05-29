package builderb0y.bigglobe.mixins;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import com.mojang.datafixers.DataFixer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import net.minecraft.world.level.storage.SavedDataStorage;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(ChunkMap.class)
public class ServerChunkLoadingManager_InitStructureManager {

	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;createState(Lnet/minecraft/core/HolderLookup;Lnet/minecraft/world/level/levelgen/RandomState;J)Lnet/minecraft/world/level/chunk/ChunkGeneratorStructureState;", shift = Shift.AFTER))
	private void bigglobe_initStructureManager(
		ServerLevel world,
		LevelStorageAccess session,
		DataFixer dataFixer,
		StructureTemplateManager structureTemplateManager,
		Executor executor,
		BlockableEventLoop<Runnable> mainThreadExecutor,
		LightChunkGetter chunkProvider,
		ChunkGenerator chunkGenerator,
		ChunkStatusUpdateListener chunkStatusChangeListener,
		Supplier<SavedDataStorage> persistentStateManagerFactory,
		TicketStorage ticketManager,
		int viewDistance,
		boolean dsync,
		CallbackInfo callback
	) {
		if (chunkGenerator instanceof BigGlobeScriptedChunkGenerator generator) {
			generator.setStructuresEnabled(world.getServer().getWorldGenSettings().options().generateStructures());
		}
	}
}