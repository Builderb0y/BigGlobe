package builderb0y.bigglobe.mixinInterfaces;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

/**
{@link NbtCompound#put(String, NbtElement)} returns the old value,
but {@link NbtCompound#remove(String)} doesn't.
returning the old value is required for some pre-update script operators.
*/
public interface NbtCompoundRemoveAndReturnAccess {

	public abstract NbtElement bigglobe_remove(String key);
}