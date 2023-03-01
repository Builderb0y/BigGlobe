package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;

@Mixin(StructureStart.class)
public interface StructureStart_BoundingBoxSetter {

	@Accessor("boundingBox")
	public abstract void bigglobe_setBoundingBox(BlockBox box);
}