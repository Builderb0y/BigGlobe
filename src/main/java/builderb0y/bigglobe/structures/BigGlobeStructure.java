package builderb0y.bigglobe.structures;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToIntScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;

//config needs to be encoded inline, but I can't annotate the field on the super class.
//and also my ReflectionManager can't see the backing field anyway.
@AddPseudoField(name = "config", getter = "getConfig")
public abstract class BigGlobeStructure extends Structure {

	public final ColumnToIntScript.@VerifyNullable Holder surface_y;

	public BigGlobeStructure(Config config, ColumnToIntScript.@VerifyNullable Holder surface_y) {
		super(config);
		this.surface_y = surface_y;
	}

	public @EncodeInline Config getConfig() {
		return this.config;
	}

	public static long chunkSeed(Context context, long salt) {
		return Permuter.permute((context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator ? generator.columnSeed : context.seed()) ^ salt, context.chunkPos());
	}

	@Override
	public abstract Optional<StructurePosition> getStructurePosition(Context context);

	public @Nullable BlockPos randomBlockInSurface(Context context, int offset) {
		int bits = context.random().nextInt();
		int x = context.chunkPos().getStartX() | (bits & 15);
		int z = context.chunkPos().getStartZ() | ((bits >>> 4) & 15);
		int y = (
			this.surface_y != null && context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator scripted
			? this.surface_y.get(scripted.newColumn(context.world(), x, z, ColumnUsage.GENERIC.maybeDhHints()))
			: context.chunkGenerator().getHeightInGround(x, z, Heightmap.Type.OCEAN_FLOOR_WG, context.world(), context.noiseConfig())
		);
		if (y >= context.world().getBottomY()) {
			return new BlockPos(x, y + offset, z);
		}
		else {
			return null;
		}
	}

	public @Nullable BlockPos randomBlockInChunk(Context context, double horizontalRadius, int verticalRadius) {
		int bits = context.random().nextInt();
		int x = context.chunkPos().getStartX() | (bits & 15);
		int z = context.chunkPos().getStartZ() | ((bits >>> 4) & 15);
		int minY = context.chunkGenerator().getMinimumY() + verticalRadius;
		int maxY = context.chunkGenerator().getHeight(x, z, Heightmap.Type.OCEAN_FLOOR_WG, context.world(), context.noiseConfig());
		ScriptedColumn column = (
			this.surface_y != null && context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator scripted
			? scripted.newColumn(context.world(), x, z, ColumnUsage.GENERIC.maybeDhHints())
			: null
		);
		for (int angleIndex = 0; angleIndex < 8; angleIndex++) {
			double angle = angleIndex * (BigGlobeMath.TAU / 8.0D);
			int x2 = BigGlobeMath.floorI(x + Math.cos(angle) * horizontalRadius);
			int z2 = BigGlobeMath.floorI(z + Math.sin(angle) * horizontalRadius);
			if (column != null) {
				column.setParamsUnchecked(column.params.at(x2, z2));
				maxY = Math.min(maxY, this.surface_y.get(column));
			}
			else {
				maxY = Math.min(maxY, context.chunkGenerator().getHeight(x2, z2, Heightmap.Type.OCEAN_FLOOR_WG, context.world(), context.noiseConfig()));
			}
		}
		maxY -= verticalRadius;
		if (maxY >= minY) {
			int y = context.random().nextBetween(minY, maxY);
			return new BlockPos(x, y, z);
		}
		else {
			return null;
		}
	}
}