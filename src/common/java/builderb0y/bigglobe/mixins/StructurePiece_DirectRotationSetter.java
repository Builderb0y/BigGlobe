package builderb0y.bigglobe.mixins;

import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructurePiece.class)
public interface StructurePiece_DirectRotationSetter {

	@Accessor("rotation")
	public abstract void bigglobe_setRotationDirect(Rotation rotation);
}