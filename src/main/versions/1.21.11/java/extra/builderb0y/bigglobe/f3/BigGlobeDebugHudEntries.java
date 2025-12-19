package builderb0y.bigglobe.f3;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.gui.hud.debug.DebugHudEntry;
import net.minecraft.client.gui.hud.debug.DebugHudLines;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;
import builderb0y.bigglobe.rendering.lods.LodSystem;

@Environment(EnvType.CLIENT)
public class BigGlobeDebugHudEntries {

	public static final Identifier LOD_SECTION = BigGlobeMod.modID("lods");

	public static void init() {
		register("lod_backend", (DebugHudLines lines, World world, WorldChunk clientChunk, WorldChunk chunk) -> {
			LodSystem system = lodSystem();
			if (system != null) {
				String backendName = system.renderer.getClass().getSimpleName();
				if (backendName.endsWith("LodRenderer")) backendName = backendName.substring(0, backendName.length() - "LodRenderer".length());
				lines.addLineToSection(LOD_SECTION, "[BG] LOD Backend: " + backendName);
			}
		});
		register("lod_nodes", (DebugHudLines lines, World world, WorldChunk clientChunk, WorldChunk chunk) -> {
			LodSystem system = lodSystem();
			if (system != null) {
				int meshedNodes = LodSystem.countMeshes(system.tree);
				int renderingNodes = LodSystem.countRenderingNodes(system.tree);
				int dirtyNodes = LodSystem.countDirty(system.tree);
				int totalNodes = LodSystem.countTotalNodes(system.tree);
				lines.addLineToSection(LOD_SECTION, "[BG] LOD Nodes: R: " + renderingNodes + ", M: " + meshedNodes + ", D: " + dirtyNodes + ", T: " + totalNodes);
			}
		});
		register("lod_quality", (DebugHudLines lines, World world, WorldChunk clientChunk, WorldChunk chunk) -> {
			LodSystem system = lodSystem();
			if (system != null) {
				double currentQuality = system.currentQuality;
				double qualityLimit = system.qualityLimit;
				double qualityConfig = BigGlobeConfig.INSTANCE.get().lodRendering.quality;
				int levelLimit = system.levelLimit;
				lines.addLineToSection(LOD_SECTION, "[BG] LOD Quality: "  + currentQuality + "/" + qualityLimit + "/" + qualityConfig + ", L: " + levelLimit);
			}
		});
		register("lod_geometry", (DebugHudLines lines, World world, WorldChunk clientChunk, WorldChunk chunk) -> {
			LodSystem system = lodSystem();
			if (system != null) {
				lines.addLinesToSection(LOD_SECTION, system.renderer.getF3MenuText());
			}
		});
		register("lod_generator", (DebugHudLines lines, World world, WorldChunk clientChunk, WorldChunk chunk) -> {
			LodSystem system = lodSystem();
			if (system != null) {
				lines.addLineToSection(LOD_SECTION, system.generator.f3Message());
			}
		});
	}

	public static LodSystem lodSystem() {
		return LodSystemHolder.of(MinecraftClient.getInstance().worldRenderer).bigglobe_getLodSystem();
	}

	public static void register(String id, BigGlobeDebugHudEntry entry) {
		DebugHudEntries.register(BigGlobeMod.modID(id), entry);
	}

	@Environment(EnvType.CLIENT)
	public static interface BigGlobeDebugHudEntry extends DebugHudEntry {

		@Override
		public default boolean canShow(boolean reducedDebugInfo) {
			return true;
		}
	}
}