package builderb0y.bigglobe.rendering2.lods.flat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.rendering2.ResourceTracker;
import builderb0y.bigglobe.rendering2.lods.*;

@Environment(EnvType.CLIENT)
public class FlatLodSystem extends LodSystem {

	public final FlatLodTree tree;
	public final FlatGenerationPipeline generator;
	public final FlatLodRenderer renderer;

	@Override
	public void close() {
		ResourceTracker.closeAll(super::close, this.generator, this.renderer);
	}

	public FlatLodSystem(ClientGeneratorParams params, ClientLevel world) {
		super(params);
		this.levelLimit = LodQuadNode.MAX_LEVEL;
		try {
			this.tree = new FlatLodTree(this);
			MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
			ServerLevel serverWorld = server != null ? server.getLevel(world.dimension()) : null;
			this.generator = new FlatGenerationPipeline(
				this,
				serverWorld != null
				? new FlatLoadingLodGenerator(this, serverWorld)
				: new LodGenerator<>(this, world.dimensionType()),
				new LodMesher()
			);
			this.renderer = new FlatLodRenderer(this);
			this.generator.start();
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}

	@Override
	public void extract(LevelExtractionContext context) {
		super.extract(context);
		if (this.levelLimit > 0 && BigGlobeConfig.INSTANCE.get().lodRendering.showProgress) {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null) {
				int percent = 100 - this.levelLimit * 100 / LodQuadNode.MAX_LEVEL;
				player.sendOverlayMessage(Component.translatable("bigglobe.lod.generating", percent));
			}
		}
	}

	@Override
	public LodTree getTree() {
		return this.tree;
	}

	@Override
	public GenerationPipeline getGenerationPipeline() {
		return this.generator;
	}

	@Override
	public LodRenderer getRenderer() {
		return this.renderer;
	}

	@Override
	public void doClose() {
		ResourceTracker.closeAll(this.tree, this.generator);
	}
}