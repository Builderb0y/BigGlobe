package net.fabricmc.fabric.api.object.builder.v1.block.type;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public class WoodTypeBuilder {

	public WoodType register(ResourceLocation id, BlockSetType setType) {
		return WoodType.register(new WoodType(id.toString(), setType));
	}
}
