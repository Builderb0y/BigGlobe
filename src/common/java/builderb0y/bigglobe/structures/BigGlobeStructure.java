package builderb0y.bigglobe.structures;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted2.ColumnScript.ColumnToIntScript;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;

//config needs to be encoded inline, but I can't annotate the field on the super class.
//and also my ReflectionManager can't see the backing field anyway.
@AddPseudoField(name = "config", getter = "getConfig")
public abstract class BigGlobeStructure extends Structure implements SizedStructure {

	public final ColumnToIntScript.@VerifyNullable Catcher min_y, surface_y;

	public BigGlobeStructure(StructureSettings config, ColumnToIntScript.@VerifyNullable Catcher minY, ColumnToIntScript.@VerifyNullable Catcher surface_y) {
		super(config);
		this.surface_y = surface_y;
		this.min_y = minY;
	}

	public @EncodeInline StructureSettings getConfig() {
		return this.settings;
	}

	public static long chunkSeed(GenerationContext context, long salt) {
		return Permuter.permute((context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator ? generator.columnSeed : context.seed()) ^ salt, context.chunkPos());
	}

	@Override
	public abstract Optional<GenerationStub> findGenerationPoint(GenerationContext context);

	public @Nullable BlockPos randomBlockInSurface(GenerationContext context, int offset) {
		int bits = context.random().nextInt();
		int x = context.chunkPos().getMinBlockX() | (bits & 15);
		int z = context.chunkPos().getMinBlockZ() | ((bits >>> 4) & 15);
		int y = (
			this.surface_y != null && context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator scripted
				? this.surface_y.get(scripted.newColumn(context.heightAccessor(), x, z, ColumnUsage.GENERIC.maybeDhHints()))
				: context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState())
		);
		if (y >= HeightLimitViewVersions.getMinY(context.heightAccessor())) {
			return new BlockPos(x, y + offset, z);
		}
		else {
			return null;
		}
	}

	public @Nullable BlockPos randomBlockInChunk(GenerationContext context, double horizontalRadius, int verticalRadius) {
		int bits = context.random().nextInt();
		int x = context.chunkPos().getMinBlockX() | (bits & 15);
		int z = context.chunkPos().getMinBlockZ() | ((bits >>> 4) & 15);
		int minY = context.chunkGenerator().getMinY() + verticalRadius;
		int maxY = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
		ScriptedColumn column = (
			(this.surface_y != null || this.min_y != null)
			&& context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator scripted
				? scripted.newColumn(context.heightAccessor(), x, z, ColumnUsage.GENERIC.maybeDhHints())
				: null
		);
		for (int angleIndex = 0; angleIndex < 8; angleIndex++) {
			double angle = angleIndex * (BigGlobeMath.TAU / 8.0D);
			int x2 = BigGlobeMath.floorI(x + Math.cos(angle) * horizontalRadius);
			int z2 = BigGlobeMath.floorI(z + Math.sin(angle) * horizontalRadius);
			if (column != null) {
				column.setParamsUnchecked(column.params.at(x2, z2));
				if (this.min_y != null) minY = Math.max(minY, this.min_y.get(column));
				if (this.surface_y != null) maxY = Math.min(maxY, this.surface_y.get(column));
			}
			else {
				maxY = Math.min(maxY, context.chunkGenerator().getBaseHeight(x2, z2, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState()));
			}
		}
		maxY -= verticalRadius;
		if (maxY >= minY) {
			int y = context.random().nextIntBetweenInclusive(minY, maxY);
			return new BlockPos(x, y, z);
		}
		else {
			return null;
		}
	}
}