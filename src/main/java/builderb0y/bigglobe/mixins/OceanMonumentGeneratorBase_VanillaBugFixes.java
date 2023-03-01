package builderb0y.bigglobe.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.structure.OceanMonumentGenerator;
import net.minecraft.structure.OceanMonumentGenerator.Piece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;

@Mixin(OceanMonumentGenerator.Base.class)
public abstract class OceanMonumentGeneratorBase_VanillaBugFixes extends OceanMonumentGenerator.Piece {

	@Shadow
	private @Final List<Piece> field_14465;

	public OceanMonumentGeneratorBase_VanillaBugFixes(StructurePieceType type, Direction orientation, int length, BlockBox box) {
		super(type, orientation, length, box);
	}

	/**
	bug #1: ocean monuments hard-code the sea level at Y64,
	and fill a massive area up to that level with water.
	this modification ensures that monuments only
	fill the area the monument actually occupies.
	honestly I'm not sure why they fill this area with water
	considering they spawn completely underwater to begin with,
	but nevertheless, I don't want to mess with vanilla
	behavior in edge cases that I can't foresee.
	*/
	@ModifyVariable(method = "generate", at = @At(value = "STORE"), ordinal = 0)
	private int bigglobe_dontFillStupidAreasWithWater(int old) {
		return 23;
	}

	/**
	bug #2: ocean monuments contain only a single StructurePiece,
	but that piece has its own children. no other structures work this way.
	this is a problem because that piece does not override translate(),
	and so nested children do not get moved.

	for the sake of avoiding an XY problem, I will also disclose bug #3:
	ocean monuments spawn at a hard-coded Y level,
	regardless of the Y level of the ocean floor.
	Big Globe uses translate() to move them to a
	more desirable Y level in Big Globe worlds.
	*/
	@Unique
	@Override
	public void translate(int x, int y, int z) {
		super.translate(x, y, z);
		for (Piece child : this.field_14465) {
			child.translate(x, y, z);
		}
	}
}