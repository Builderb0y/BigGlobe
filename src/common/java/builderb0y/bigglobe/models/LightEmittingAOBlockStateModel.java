package builderb0y.bigglobe.models;

import java.util.List;
import java.util.function.Predicate;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad.MaterialFlags;
import net.minecraft.client.resources.model.sprite.Material.Baked;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class LightEmittingAOBlockStateModel implements BlockStateModel {

	public final BlockStateModel model;

	public LightEmittingAOBlockStateModel(BlockStateModel model) {
		this.model = model;
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest) {
		emitter.pushTransform((MutableQuadView quad) -> {
			quad.ambientOcclusion(TriState.TRUE);
			return true;
		});
		try {
			this.model.emitQuads(emitter, level, pos, state, random, cullTest);
		}
		finally {
			emitter.popTransform();
		}
	}

	@Override
	public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
		this.model.collectParts(random, output);
	}

	@Override
	public Baked particleMaterial() {
		return this.model.particleMaterial();
	}

	@Override
	public @MaterialFlags int materialFlags() {
		return this.model.materialFlags();
	}
}