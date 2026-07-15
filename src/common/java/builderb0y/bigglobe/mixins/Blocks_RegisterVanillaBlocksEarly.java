package builderb0y.bigglobe.mixins;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.block.Blocks;

import builderb0y.bigglobe.blockdefs.VanillaBlocks;

@Mixin(Blocks.class)
public class Blocks_RegisterVanillaBlocksEarly {

	@Inject(method = "<clinit>", at = @At(value = "FIELD", target = "Lnet/minecraft/core/registries/BuiltInRegistries;BLOCK:Lnet/minecraft/core/DefaultedRegistry;", opcode = Opcodes.GETSTATIC))
	private static void bigglobe_registerVanillaBlocksEarly(CallbackInfo callback) {
		VanillaBlocks.init();
	}
}