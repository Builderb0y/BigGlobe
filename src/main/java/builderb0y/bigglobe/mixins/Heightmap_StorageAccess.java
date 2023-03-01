package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.Heightmap;

@Mixin(Heightmap.class)
public interface Heightmap_StorageAccess {

	@Accessor("storage")
	public abstract PaletteStorage bigglobe_getStorage();
}