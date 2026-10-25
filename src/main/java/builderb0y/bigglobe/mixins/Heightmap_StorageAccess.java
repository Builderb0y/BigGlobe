package builderb0y.bigglobe.mixins;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Heightmap.class)
public interface Heightmap_StorageAccess {

	@Accessor("data")
	public abstract BitStorage bigglobe_getStorage();
}