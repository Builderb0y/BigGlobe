package builderb0y.bigglobe.mixins;

import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import builderb0y.bigglobe.mixinInterfaces.NbtCompoundExtensions;

/**
{@link CompoundTag#put(String, Tag)} returns the old value,
but {@link CompoundTag#remove(String)} doesn't.
returning the old value is required for some pre-update script operators.
*/
@Mixin(CompoundTag.class)
public class NbtCompound_ImplementExtensions implements NbtCompoundExtensions {

	@Shadow
	@Final
	private Map<String, Tag> tags;

	@Override
	public Tag bigglobe_remove(String key) {
		return this.tags.remove(key);
	}

	@Override
	public Set<Map.Entry<String, Tag>> bigglobe_getEntrySet() {
		return this.tags.entrySet();
	}
}