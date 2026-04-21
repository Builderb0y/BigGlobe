package builderb0y.bigglobe.models;

import java.util.List;
import java.util.function.Predicate;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad.MaterialFlags;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.bigglobe.blocks.BigGlobeBlockTags;
import builderb0y.bigglobe.util.BetterScopedValue;
import builderb0y.bigglobe.util.Directions;

public class OreBlockStateModel implements BlockStateModel {

	public static final BetterScopedValue<Boolean> RECURSION_BLOCKER = new BetterScopedValue<>();

	public final BlockStateModel default_model, overlay;

	public OreBlockStateModel(BlockStateModel default_model, BlockStateModel overlay) {
		this.default_model = default_model;
		this.overlay = overlay;
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest) {
		StateModel adjacent = this.getMimicModel(level, pos, state);
		List<BlockTintSource> tints = Minecraft.getInstance().getBlockColors().getTintSources(adjacent.state);
		if (!tints.isEmpty()) emitter.pushTransform((MutableQuadView quad) -> {
			int index = quad.tintIndex();
			if (index >= 0 && index < tints.size()) {
				int multiplier = tints.get(index).colorInWorld(adjacent.state, level, pos);
				quad.multiplyColor(multiplier);
			}
			return true;
		});
		try {
			adjacent.model.emitQuads(emitter, level, pos, adjacent.state, random, cullTest);
		}
		finally {
			if (!tints.isEmpty()) emitter.popTransform();
		}
		this.overlay.emitQuads(emitter, level, pos, state, random, cullTest);
	}

	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		StateModel adjacent = this.getMimicModel(level, pos, state);
		return adjacent.model.particleMaterial(level, pos, adjacent.state);
	}

	@Override
	public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		return this.getMimicModel(level, pos, state);
	}

	public static record StateModel(BlockState state, BlockStateModel model) {}

	public StateModel getMimicModel(BlockGetter world, BlockPos pos, BlockState defaultState) {
		MutableBlockPos mutablePos = new MutableBlockPos();
		BlockState[] states = new BlockState[6];
		BlockStateModel[] models = new BlockStateModel[6];
		int counts = 0;
		int total = 0;
		outer:
		for (Direction direction : Directions.ALL) {
			BlockState adjacentState = world.getBlockState(mutablePos.setWithOffset(pos, direction));
			if (adjacentState.isSolidRender() && !adjacentState.is(BigGlobeBlockTags.MIMIC_ORES_IGNORE)) {
				BlockStateModel adjacentModel = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(adjacentState);
				if (adjacentModel instanceof OreBlockStateModel) {
					continue outer;
				}
				for (int check = 0; check < total; check++) {
					if (adjacentState == models[check]) {
						counts += 1 << (check << 3);
						continue outer;
					}
				}
				states[total] = adjacentState;
				models[total] = adjacentModel;
				counts |= 1 << (total << 3);
				total++;
			}
		}
		int resultIndex = 0;
		int resultCount = 0;
		for (int check = 0; check < total; check++) {
			int count = (counts >> (check << 3)) & 7;
			if (count > resultCount && states[check].is(BigGlobeBlockTags.MIMIC_ORES_PREFER)) {
				resultIndex = check;
				resultCount = count;
			}
		}
		if (resultCount > 0) {
			return new StateModel(states[resultIndex], models[resultIndex]);
		}
		for (int check = 0; check < total; check++) {
			int count = (counts >> (check << 3)) & 7;
			if (count > resultCount) {
				resultIndex = check;
				resultCount = count;
			}
		}
		if (resultCount > 0) {
			return new StateModel(states[resultIndex], models[resultIndex]);
		}
		return new StateModel(defaultState, this.default_model);
	}

	@Override
	public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
		this.default_model.collectParts(random, output);
	}

	@Override
	public Material.Baked particleMaterial() {
		return this.default_model.particleMaterial();
	}

	@Override
	public @MaterialFlags int materialFlags() {
		return this.default_model.materialFlags();
	}
}