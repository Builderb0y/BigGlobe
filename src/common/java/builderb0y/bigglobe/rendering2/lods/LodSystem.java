package builderb0y.bigglobe.rendering2.lods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator.GameMechanics.LodOverrides;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.util.SafeCloseable;

public abstract class LodSystem implements SafeCloseable {

	public final ClientGeneratorParams params;
	public final LodOverrides overrides;
	public final LodFrustum frustum;
	public double currentQuality, qualityLimit, loadDistance;
	public int levelLimit;
	public boolean renderingThisFrame;

	public LodSystem(ClientGeneratorParams params) {
		this.params = params;
		this.overrides = params.generatorLodOverrides;
		this.qualityLimit = BigGlobeConfig.INSTANCE.get().lodRendering.quality;
		this.frustum = new LodFrustum(this);
	}

	public void oom() {
		this.currentQuality = Math.min(this.currentQuality, this.qualityLimit -= 0.5D);

		LocalPlayer player = Minecraft.getInstance().player;
		Component text = Component.translatable("bigglobe.lod.oom", this.qualityLimit);
		if (player != null) {
			player.sendSystemMessage(text);
		}
		else {
			BigGlobeMod.LOGGER.warn(text.getString());
		}
	}
}