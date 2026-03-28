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
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;
import builderb0y.bigglobe.rendering.lods.LodSystem;

@Environment(EnvType.CLIENT)
public class BigGlobeDebugHudEntries {

	public static final Identifier LOD_SECTION = BigGlobeMod.modID("lods");

	public static void init() {
		register(
			"lod_backend", (DebugScreenDisplayer lines, Level world, LevelChunk clientChunk, LevelChunk chunk) -> {
				LodSystem system = lodSystem();
				if (system != null) {
					String backendName = system.renderer.getClass().getSimpleName();
					if (backendName.endsWith("LodRenderer")) backendName = backendName.substring(0, backendName.length() - "LodRenderer".length());
					lines.addToGroup(LOD_SECTION, "[BG] LOD Backend: " + backendName);
				}
			}
		);
		register(
			"lod_nodes", (DebugScreenDisplayer lines, Level world, LevelChunk clientChunk, LevelChunk chunk) -> {
				LodSystem system = lodSystem();
				if (system != null) {
					int meshedNodes = LodSystem.countMeshes(system.tree);
					int renderingNodes = LodSystem.countRenderingNodes(system.tree);
					int dirtyNodes = LodSystem.countDirty(system.tree);
					int totalNodes = LodSystem.countTotalNodes(system.tree);
					lines.addToGroup(LOD_SECTION, "[BG] LOD Nodes: R: " + renderingNodes + ", M: " + meshedNodes + ", D: " + dirtyNodes + ", T: " + totalNodes);
				}
			}
		);
		register(
			"lod_quality", (DebugScreenDisplayer lines, Level world, LevelChunk clientChunk, LevelChunk chunk) -> {
				LodSystem system = lodSystem();
				if (system != null) {
					double currentQuality = system.currentQuality;
					double qualityLimit = system.qualityLimit;
					double qualityConfig = BigGlobeConfig.INSTANCE.get().lodRendering.quality;
					int levelLimit = system.levelLimit;
					lines.addToGroup(LOD_SECTION, "[BG] LOD Quality: " + currentQuality + "/" + qualityLimit + "/" + qualityConfig + ", L: " + levelLimit);
				}
			}
		);
		register(
			"lod_geometry", (DebugScreenDisplayer lines, Level world, LevelChunk clientChunk, LevelChunk chunk) -> {
				LodSystem system = lodSystem();
				if (system != null) {
					lines.addToGroup(LOD_SECTION, system.renderer.getF3MenuText());
				}
			}
		);
		register(
			"lod_generator", (DebugScreenDisplayer lines, Level world, LevelChunk clientChunk, LevelChunk chunk) -> {
				LodSystem system = lodSystem();
				if (system != null) {
					lines.addToGroup(LOD_SECTION, system.generator.f3Message());
				}
			}
		);
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