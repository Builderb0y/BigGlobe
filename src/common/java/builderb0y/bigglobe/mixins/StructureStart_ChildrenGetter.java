package builderb0y.bigglobe.mixins;

import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructureStart.class)
public interface StructureStart_ChildrenGetter {

	@Accessor("pieceContainer")
	public abstract PiecesContainer bigglobe_getChildren();
}