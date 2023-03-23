package builderb0y.bigglobe.overriders.overworld;

import org.apache.commons.lang3.math.NumberUtils;

import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.MathHelper;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.columns.OverworldColumn.CaveCell;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.scripting.ColumnYScriptEnvironment;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.settings.OverworldCaveSettings.LocalOverworldCaveSettings;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface OverworldCaveOverrider extends Script {

	public abstract void override(Context context);

	@Wrapper
	public static class Holder extends ScriptHolder<OverworldCaveOverrider> implements OverworldCaveOverrider {

		public Holder(String script) throws ScriptParsingException {
			super(
				new ScriptParser<>(OverworldCaveOverrider.class, script)
				.addEnvironment(OverworldCaveOverrider.Environment.INSTANCE)
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(JavaUtilScriptEnvironment.ALL)
				.addEnvironment(
					new ColumnYScriptEnvironment(
						getField(
							load("context", 1, type(Context.class)),
							FieldInfo.getField(Context.class, "column")
						),
						null,
						true
					)
				)
				.parse()
			);
		}

		@Override
		public void override(Context context) {
			try {
				this.script.override(context);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
		}
	}

	public static class Environment extends OverworldDataOverrider.Environment {

		public static final Environment INSTANCE = new Environment();

		public Environment() {
			super();

			InsnTree loadContext = load("context", 1, type(Context.class));
			InsnTree loadColumn = InsnTrees.getField(loadContext, FieldInfo.getField(Context.class, "column"));
			this.addVariableGetFields(loadContext, Context.class, "structureStarts", "rawGeneration");
			this.addDistanceFunctions(loadColumn);
			this.addFunctionMultiInvokes(loadContext, Context.class, "excludeSurface", "excludeCuboid", "excludeCylinder", "excludeSphere");
		}
	}

	public static class Context {

		public final ScriptStructures structureStarts;
		public final OverworldColumn column;
		public final boolean rawGeneration;
		public final CaveCell caveCell;
		public final LocalOverworldCaveSettings caveSettings;
		public final int topI, bottomI;
		public final double topD, bottomD;
		public final double ledgeMin;

		public Context(ScriptStructures structureStarts, OverworldColumn column, boolean rawGeneration) {
			this.structureStarts = structureStarts;
			this.column = column;
			this.rawGeneration = rawGeneration;
			this.caveCell = column.getCaveCell();
			this.caveSettings = this.caveCell.settings;
			this.topD = column.getFinalTopHeightD();
			this.topI = column.getFinalTopHeightI();
			this.bottomD  = this.topD - this.caveSettings.depth();
			this.bottomI  = this.topI - this.caveSettings.depth();
			this.ledgeMin = this.caveSettings.ledge_noise() != null ? this.caveSettings.ledge_noise().minValue() : 0.0D;
		}

		public double getExclusionMultiplier(int y) {
			double width = this.caveSettings.getWidthSquared(this.topD, y);
			width -= this.ledgeMin * width;
			return width;
		}

		public int yToIndex(int y) {
			return y - this.bottomI;
		}

		public void excludeUnchecked(int y, double exclusion) {
			this.column.caveNoise[this.yToIndex(y)] += exclusion * this.getExclusionMultiplier(y);
		}

		public void exclude(int y, double exclusion) {
			if (y >= this.bottomI && y < this.topI) {
				this.excludeUnchecked(y, exclusion);
			}
		}

		//////////////////////////////// script methods ////////////////////////////////

		public void excludeSurface(double multiplier) {
			if (!(multiplier > 0.0D)) return;
			double baseY = this.topD;
			double width = this.caveSettings.upper_width();
			double intersection = baseY - width * 2.0D;
			multiplier /= width;
			int minY = Math.max(BigGlobeMath.ceilI(intersection), this.bottomI);
			int maxY = this.topI;
			for (int y = minY; y < maxY; y++) {
				this.excludeUnchecked(y, BigGlobeMath.squareD((y - intersection) * multiplier));
			}
		}

		public void excludeCuboid(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double padding) {
			double paddingSquared = padding * padding;
			double clampedRelativeX = MathHelper.clamp(this.column.x, minX, maxX) - this.column.x;
			double clampedRelativeZ = MathHelper.clamp(this.column.z, minZ, maxZ) - this.column.z;
			double horizontalDistanceSquared = BigGlobeMath.squareD(clampedRelativeX, clampedRelativeZ);
			if (horizontalDistanceSquared >= paddingSquared) return;
			int lowerY = Math.max(BigGlobeMath. ceilI(minY - padding), this.bottomI);
			int upperY = Math.min(BigGlobeMath.floorI(maxY + padding), this.topI - 1);
			for (int y = lowerY; y <= upperY; y++) {
				double clampedRelativeY = MathHelper.clamp(y, minY, maxY) - y;
				double distanceSquared = horizontalDistanceSquared + clampedRelativeY * clampedRelativeY;
				if (distanceSquared < paddingSquared) {
					double fraction = 1.0D - Math.sqrt(distanceSquared) / padding;
					this.excludeUnchecked(y, fraction * fraction);
				}
			}
		}

		public void _excludeCuboid(BlockBox box, double padding) {
			this.excludeCuboid(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ(), padding);
		}

		public void excludeCuboid(StructureStartWrapper start, double padding) {
			this._excludeCuboid(start.box(), padding);
		}

		public void excludeCuboid(StructurePiece piece, double padding) {
			this._excludeCuboid(piece.getBoundingBox(), padding);
		}

		public void excludeCylinder(double centerX, double centerZ, double radius, double minY, double maxY, double padding) {
			double horizontalDistanceSquared = BigGlobeMath.squareD(
				this.column.x - centerX,
				this.column.z - centerZ
			);
			if (horizontalDistanceSquared >= BigGlobeMath.squareD(radius + padding)) {
				return;
			}
			else if (horizontalDistanceSquared >= BigGlobeMath.squareD(radius)) {
				horizontalDistanceSquared = BigGlobeMath.squareD(Math.sqrt(horizontalDistanceSquared) - radius);
			}
			else {
				horizontalDistanceSquared = 0.0D;
			}
			int lowerY = Math.max(BigGlobeMath. ceilI(minY - padding), this.bottomI);
			int upperY = Math.min(BigGlobeMath.floorI(maxY + padding), this.topI - 1);
			double paddingSquared = padding * padding;
			for (int y = lowerY; y <= upperY; y++) {
				double verticalDistance = Interpolator.clamp(minY, maxY, y) - y;
				double distanceSquared = horizontalDistanceSquared + verticalDistance * verticalDistance;
				if (distanceSquared < paddingSquared) {
					double fraction = 1.0D - Math.sqrt(distanceSquared) / padding;
					this.excludeUnchecked(y, fraction * fraction);
				}
			}
		}

		public void _excludeCylinder(BlockBox box, double radius, double padding) {
			this.excludeCylinder(
				(box.getMinX() + box.getMaxX()) * 0.5D,
				(box.getMinZ() + box.getMaxZ()) * 0.5D,
				radius,
				box.getMinY(),
				box.getMaxY(),
				padding
			);
		}

		public void _excludeCylinder(BlockBox box, double padding) {
			this._excludeCylinder(
				box,
				Math.min(
					box.getMaxX() - box.getMinX(),
					box.getMaxZ() - box.getMinZ()
				)
				* 0.5D,
				padding
			);
		}

		public void excludeCylinder(StructureStartWrapper start, double radius, double padding) {
			this._excludeCylinder(start.box(), radius, padding);
		}

		public void excludeCylinder(StructurePiece piece, double radius, double padding) {
			this._excludeCylinder(piece.getBoundingBox(), radius, padding);
		}

		public void excludeCylinder(StructureStartWrapper start, double padding) {
			this._excludeCylinder(start.box(), padding);
		}

		public void excludeCylinder(StructurePiece piece, double padding) {
			this._excludeCylinder(piece.getBoundingBox(), padding);
		}

		public void excludeSphere(double centerX, double centerY, double centerZ, double radius, double padding) {
			double outerRadius = radius + padding;
			double distanceXZ2 = BigGlobeMath.squareD(this.column.x - centerX, this.column.z - centerZ);
			if (distanceXZ2 >= BigGlobeMath.squareD(outerRadius)) return;
			int
				minY = Math.max(BigGlobeMath. ceilI(centerY - outerRadius), this.bottomI),
				maxY = Math.min(BigGlobeMath.floorI(centerY + outerRadius), this.topI - 1),
				startY = MathHelper.clamp(BigGlobeMath.floorI(centerY), minY, maxY);
			double innerRadiusSquared = radius * radius;
			double outerRadiusSquared = outerRadius * outerRadius;
			for (int y = startY; ++y <= maxY;) {
				double offsetY = y - centerY;
				double distanceXYZSquared = distanceXZ2 + offsetY * offsetY;
				if (distanceXYZSquared <= innerRadiusSquared) {
					this.excludeUnchecked(y, 1.0D);
				}
				else if (distanceXYZSquared <= outerRadiusSquared) {
					double distanceXYZ = Math.sqrt(distanceXYZSquared);
					double exclusion = Interpolator.unmixLinear(outerRadius, radius, distanceXYZ);
					//float exclusion = (float)((outerRadius - distanceXYZ) / (outerRadius - innerRadius));
					this.excludeUnchecked(y, exclusion * exclusion);
				}
				else {
					break;
				}
			}
			for (int y = startY; y >= minY; y--) {
				double offsetY = y - centerY;
				double distanceXYZSquared = distanceXZ2 + offsetY * offsetY;
				if (distanceXYZSquared <= innerRadiusSquared) {
					this.excludeUnchecked(y, 1.0D);
				}
				else if (distanceXYZSquared <= outerRadiusSquared) {
					double distanceXYZ = Math.sqrt(distanceXYZSquared);
					float exclusion = (float)(Interpolator.unmixLinear(outerRadius, radius, distanceXYZ));
					//float exclusion = (outerRadius - distanceXYZ) / (outerRadius - innerRadius);
					this.excludeUnchecked(y, exclusion * exclusion);
				}
				else {
					break;
				}
			}
		}

		public void _excludeSphere(BlockBox box, double radius, double padding) {
			this.excludeSphere(
				(box.getMinX() + box.getMaxX()) * 0.5D,
				(box.getMinY() + box.getMaxY()) * 0.5D,
				(box.getMinZ() + box.getMaxZ()) * 0.5D,
				radius,
				padding
			);
		}

		public void _excludeSphere(BlockBox box, double padding) {
			this._excludeSphere(
				box,
				NumberUtils.min(
					box.getMaxX() - box.getMinX(),
					box.getMaxY() - box.getMinY(),
					box.getMaxZ() - box.getMinZ()
				)
				* 0.5D,
				padding
			);
		}

		public void excludeSphere(StructureStartWrapper start, double radius, double padding) {
			this._excludeSphere(start.box(), radius, padding);
		}

		public void excludeSphere(StructureStartWrapper start, double padding) {
			this._excludeSphere(start.box(), padding);
		}

		public void excludeSphere(StructurePiece piece, double radius, double padding) {
			this._excludeSphere(piece.getBoundingBox(), radius, padding);
		}

		public void excludeSphere(StructurePiece piece, double padding) {
			this._excludeSphere(piece.getBoundingBox(), padding);
		}
	}
}