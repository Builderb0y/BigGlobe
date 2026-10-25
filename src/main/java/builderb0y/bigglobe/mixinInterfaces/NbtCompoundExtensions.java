package builderb0y.bigglobe.mixinInterfaces;

import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public interface NbtCompoundExtensions {

	/**
	{@link CompoundTag#put(String, Tag)} returns the old value,
	but {@link CompoundTag#remove(String)} doesn't.
	returning the old value is required for some pre-update script operators.
	*/
	public abstract Tag bigglobe_remove(String key);

	/**
	only available in some MC versions and not others.
	*/
	public abstract Set<Map.Entry<String, Tag>> bigglobe_getEntrySet();
}