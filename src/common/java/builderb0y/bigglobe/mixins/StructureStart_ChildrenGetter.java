package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

@Mixin(StructureStart.class)
public interface StructureStart_ChildrenGetter {

	@Accessor("pieceContainer")
	public abstract PiecesContainer bigglobe_getChildren();
}