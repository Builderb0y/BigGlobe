package builderb0y.bigglobe.mixinInterfaces;

import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

public interface NbtCompoundExtensions {

	/**
	{@link NbtCompound#put(String, NbtElement)} returns the old value,
	but {@link NbtCompound#remove(String)} doesn't.
	returning the old value is required for some pre-update script operators.
	*/
	public abstract NbtElement bigglobe_remove(String key);

	/**
	only available in some MC versions and not others.
	*/
	public abstract Set<Map.Entry<String, NbtElement>> bigglobe_getEntrySet();
}