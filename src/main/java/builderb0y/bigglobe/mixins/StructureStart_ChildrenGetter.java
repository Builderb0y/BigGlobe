package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.structure.StructurePiecesList;
import net.minecraft.structure.StructureStart;

@Mixin(StructureStart.class)
public interface StructureStart_ChildrenGetter {

	@Accessor("children")
	public abstract StructurePiecesList bigglobe_getChildren();
}