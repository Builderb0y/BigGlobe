package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtEnd;

import builderb0y.bigglobe.BigGlobeMod;

@Mixin(NbtCompound.class)
public class Dev_NbtCompound_SanityCheckValues {

	@Inject(method = "put(Ljava/lang/String;Lnet/minecraft/nbt/NbtElement;)Lnet/minecraft/nbt/NbtElement;", at = @At("HEAD"))
	private void bigglobe_sanityCheckValue(String key, NbtElement element, CallbackInfoReturnable<NbtElement> callback) {
		if (element == null || element instanceof NbtEnd) {
			BigGlobeMod.LOGGER.error("Someone attempted to put " + element + " in an NBT compound. See the stack trace below to find out who.", new IllegalArgumentException());
			callback.cancel();
		}
	}
}