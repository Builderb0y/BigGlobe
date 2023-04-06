package builderb0y.bigglobe.structures;

import java.util.Optional;

import com.mojang.serialization.Codec;

import net.minecraft.util.math.Vec3i;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.chunk.placement.StructurePlacementType;

import builderb0y.autocodec.annotations.DefaultInt;
import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;

public class RestrictedStructurePlacement extends StructurePlacement {

	public static final Codec<RestrictedStructurePlacement> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(RestrictedStructurePlacement.class);

	public final @EncodeInline SpacedPlacement placement;
	public final ColumnRestriction restrictions;
	public final @VerifyIntRange(min = 0) @DefaultInt(0) int radius;

	public RestrictedStructurePlacement(SpacedPlacement placement, ColumnRestriction restrictions, int radius) {
		super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0F, 0, Optional.empty());
		this.placement    = placement;
		this.restrictions = restrictions;
		this.radius       = radius;
	}

	@Override
	public boolean shouldGenerate(StructurePlacementCalculator calculator, int chunkX, int chunkZ) {
		if (!this.isStartChunk(calculator, chunkX, chunkZ)) return false;
		int startX = (chunkX << 4) | 8;
		int startZ = (chunkZ << 4) | 8;
		WorldColumn column = WorldColumn.forGenerator(calculator.getStructureSeed(), null, calculator.getNoiseConfig(), startX, startZ);
		double y = column.getFinalTopHeightD();
		ColumnRestriction restriction = this.restrictions;
		if (!restriction.test(column, y, calculator.getStructureSeed())) return false;
		int radius = this.radius;
		if (radius > 0) {
			int step = (int)(Math.sqrt(radius));
			for (int offsetX = -radius; offsetX <= radius; offsetX += step) {
				for (int offsetZ = -radius; offsetZ <= radius; offsetZ += step) {
					column.setPosUnchecked(startX + offsetX, startZ + offsetZ);
					if (!(restriction.getRestriction(column, y) > 0.0D)) return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean isStartChunk(StructurePlacementCalculator calculator, int chunkX, int chunkZ) {
		return this.placement.isStartChunk(chunkX, chunkZ, calculator.getStructureSeed());
	}

	@Override
	public StructurePlacementType<?> getType() {
		return BigGlobeStructures.RESTRICTED_PLACEMENT_TYPE;
	}
}