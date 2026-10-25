package net.fabricmc.fabric.api.client.render.fluid.v1;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public class SimpleFluidRenderHandler implements FluidRenderHandler {

	public final ResourceLocation stillTexture;
	public final ResourceLocation flowingTexture;

	public SimpleFluidRenderHandler(ResourceLocation still, ResourceLocation flowing) {
		this.stillTexture = still;
		this.flowingTexture = flowing;
	}

	@Override
	public TextureAtlasSprite[] getFluidSprites(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
		return new TextureAtlasSprite[2];
	}
}
