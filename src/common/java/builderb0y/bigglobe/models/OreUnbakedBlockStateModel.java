package builderb0y.bigglobe.models;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class OreUnbakedBlockStateModel implements CustomUnbakedBlockStateModel {

	public static final MapCodec<OreUnbakedBlockStateModel> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(OreUnbakedBlockStateModel.class);

	public final BlockStateModel.Unbaked default_model, overlay;

	public OreUnbakedBlockStateModel(BlockStateModel.Unbaked default_model, BlockStateModel.Unbaked overlay) {
		this.default_model = default_model;
		this.overlay = overlay;
	}

	@Override
	public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
		return CODEC;
	}

	@Override
	public BlockStateModel bake(ModelBaker modelBakery) {
		return new OreBlockStateModel(
			this.default_model.bake(modelBakery),
			this.overlay.bake(modelBakery)
		);
	}

	@Override
	public void resolveDependencies(Resolver resolver) {
		this.default_model.resolveDependencies(resolver);
		this.overlay.resolveDependencies(resolver);
	}
}