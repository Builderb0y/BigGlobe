package builderb0y.bigglobe.rendering2.lods.flat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.rendering2.ResourceTracker;
import builderb0y.bigglobe.rendering2.lods.LodGenerator;
import builderb0y.bigglobe.rendering2.lods.LodSystem;

public class FlatLodSystem extends LodSystem {

	public FlatLodTree tree;
	public final LodGenerator<?> generator;

	public FlatLodSystem(ClientGeneratorParams params, ClientLevel world) {
		super(params);
		MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
		ServerLevel serverWorld = server != null ? server.getLevel(world.dimension()) : null;
		this.generator = (
			serverWorld != null
			? new FlatLoadingLodGenerator(params, serverWorld)
			: new LodGenerator<>(params, world.dimensionType())
		);
	}

	@Override
	public void close() {
		ResourceTracker.closeAll(this.tree, this.generator);
	}
}