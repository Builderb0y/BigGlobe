package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.Tag;

import builderb0y.bigglobe.BigGlobeMod;

@Mixin(CompoundTag.class)
public class Dev_NbtCompound_SanityCheckValues {

	@Inject(method = "put(Ljava/lang/String;Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;", at = @At("HEAD"), cancellable = true)
	private void bigglobe_sanityCheckValue(String key, Tag element, CallbackInfoReturnable<Tag> callback) {
		if (element == null || element instanceof EndTag) {
			BigGlobeMod.LOGGER.error("Someone attempted to put " + element + " in an NBT compound. See the stack trace below to find out who.", new IllegalArgumentException());
			callback.cancel();
		}
	}
}