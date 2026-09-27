package builderb0y.bigglobe.models;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel.Unbaked;
import net.minecraft.client.resources.model.ModelBaker;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class LightEmittingAOUnbakedModel implements CustomUnbakedBlockStateModel {

	public static final MapCodec<LightEmittingAOUnbakedModel> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(LightEmittingAOUnbakedModel.class);

	@Override
	public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
		return CODEC;
	}

	public final BlockStateModel.Unbaked model;

	public LightEmittingAOUnbakedModel(Unbaked model) {
		this.model = model;
	}

	@Override
	public BlockStateModel bake(ModelBaker modelBakery) {
		return new LightEmittingAOBlockStateModel(this.model.bake(modelBakery));
	}

	@Override
	public void resolveDependencies(Resolver resolver) {
		this.model.resolveDependencies(resolver);
	}
}