package net.fabricmc.fabric.api.object.builder.v1.block.type;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class BlockSetTypeBuilder {

	public BlockSetTypeBuilder pressurePlateActivationRule(BlockSetType.PressurePlateSensitivity rule) {
		return this;
	}

	public BlockSetType register(ResourceLocation id) {
		return BlockSetType.register(new BlockSetType(id.toString()));
	}
}
