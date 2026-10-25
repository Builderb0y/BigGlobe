package builderb0y.bigglobe.platform.fabric.fabric.api.blockrenderlayer.v1;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public interface BlockRenderLayerMap {

	BlockRenderLayerMap INSTANCE = new BlockRenderLayerMap() {
		@Override
		public void putBlock(Block block, RenderType renderLayer) {
			ItemBlockRenderTypes.setRenderLayer(block, renderLayer);
		}

		@Override
		public void putBlocks(RenderType renderLayer, Block... blocks) {
			for (Block block : blocks) {
				ItemBlockRenderTypes.setRenderLayer(block, renderLayer);
			}
		}

		@Override
		public void putFluid(Fluid fluid, RenderType renderLayer) {
			ItemBlockRenderTypes.setRenderLayer(fluid, renderLayer);
		}

		@Override
		public void putFluids(RenderType renderLayer, Fluid... fluids) {
			for (Fluid fluid : fluids) {
				ItemBlockRenderTypes.setRenderLayer(fluid, renderLayer);
			}
		}
	};

	void putBlock(Block block, RenderType renderLayer);

	void putBlocks(RenderType renderLayer, Block... blocks);

	void putFluid(Fluid fluid, RenderType renderLayer);

	void putFluids(RenderType renderLayer, Fluid... fluids);
}
