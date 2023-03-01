package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.structure.StructurePiece;
import net.minecraft.util.BlockRotation;

@Mixin(StructurePiece.class)
public interface StructurePiece_DirectRotationSetter {

	@Accessor("rotation")
	public abstract void bigglobe_setRotationDirect(BlockRotation rotation);
}