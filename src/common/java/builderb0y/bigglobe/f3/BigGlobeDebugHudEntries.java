package builderb0y.bigglobe.f3;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;
import builderb0y.bigglobe.rendering2.lods.LodSystem;

@Environment(EnvType.CLIENT)
public class BigGlobeDebugHudEntries {

	public static final Identifier LOD_SECTION = BigGlobeMod.modID("lods");

	public static void init() {
		register("lod_nodes", (DebugScreenDisplayer lines, Level world, LevelChunk clientChunk, LevelChunk chunk) -> {
			LodSystem system = lodSystem();
			if (system != null) lines.addToGroup(LOD_SECTION, system.getTree().f3Message());
		});
		register("lod_quality", (DebugScreenDisplayer lines, Level world, LevelChunk clientChunk, LevelChunk chunk) -> {
			LodSystem system = lodSystem();
			if (system != null) lines.addToGroup(LOD_SECTION, system.f3Message());
		});
		register("lod_generator_pipeline", (DebugScreenDisplayer lines, Level world, LevelChunk clientChunk, LevelChunk chunk) -> {
			LodSystem system = lodSystem();
			if (system != null) lines.addToGroup(LOD_SECTION, system.getGenerationPipeline().f3Message());
		});
		register("lod_generator", (DebugScreenDisplayer lines, Level world, LevelChunk clientChunk, LevelChunk chunk) -> {
			LodSystem system = lodSystem();
			if (system != null) lines.addToGroup(LOD_SECTION, system.getGenerationPipeline().generator.f3Message());
		});
	}

	public static LodSystem lodSystem() {
		return LodSystemHolder.of(Minecraft.getInstance().levelRenderer).bigglobe_getLodSystem();
	}

	public static void register(String id, BigGlobeDebugHudEntry entry) {
		DebugScreenEntries.register(BigGlobeMod.modID(id), entry);
	}

	@Environment(EnvType.CLIENT)
	public static interface BigGlobeDebugHudEntry extends DebugScreenEntry {

		@Override
		public default boolean isAllowed(boolean reducedDebugInfo) {
			return true;
		}
	}
}