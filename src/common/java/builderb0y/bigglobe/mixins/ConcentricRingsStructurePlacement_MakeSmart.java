package builderb0y.bigglobe.mixins;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.management.SmartStructurePlacement;

@Mixin(ConcentricRingsStructurePlacement.class)
public abstract class ConcentricRingsStructurePlacement_MakeSmart
#if MC_VERSION >= MC_26_3_0
	extends net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement
#else
	extends StructurePlacement
#endif
implements SmartStructurePlacement
{

	public ConcentricRingsStructurePlacement_MakeSmart() {
		super(null, null, 0.0F, 0, null);
	}

	@Override
	public Stream<StructureStartWrapper> bigglobe_generateStructuresInArea(Context context) {
		List<ChunkPos> list = context.structureState().getRingPositionsFor((ConcentricRingsStructurePlacement)(Object)(this));
		if (list == null) return Stream.empty();
		return list.stream().map(context::createRandomFromSetAt).filter(Objects::nonNull);
	}
}