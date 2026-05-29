package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.levelgen.Heightmap;

@Mixin(Heightmap.class)
public interface Heightmap_StorageAccess {

	@Accessor("data")
	public abstract BitStorage bigglobe_getStorage();
}