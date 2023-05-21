package builderb0y.bigglobe.mixins;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import builderb0y.bigglobe.mixinInterfaces.NbtCompoundRemoveAndReturnAccess;

/**
{@link NbtCompound#put(String, NbtElement)} returns the old value,
but {@link NbtCompound#remove(String)} doesn't.
returning the old value is required for some pre-update script operators.
*/
@Mixin(NbtCompound.class)
public class NbtCompound_RemoveAndReturn implements NbtCompoundRemoveAndReturnAccess {

	@Shadow @Final private Map<String, NbtElement> entries;

	@Override
	public NbtElement bigglobe_remove(String key) {
		return this.entries.remove(key);
	}
}