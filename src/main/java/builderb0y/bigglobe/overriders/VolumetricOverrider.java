package builderb0y.bigglobe.overriders;

import org.apache.commons.lang3.math.NumberUtils;

import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.MathHelper;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.environments.ColumnScriptEnvironmentBuilder;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface VolumetricOverrider extends Overrider {

	public static final ScriptEnvironment STRUCTURE_START_EXCLUDE_ENVIRONMENT = (
		new MutableScriptEnvironment().configure((MutableScriptEnvironment environment) -> {
			InsnTree loadContext = load("context", 1, type(Context.class));
			environment
			.addVariableGetField(loadContext, Context.class, "structureStarts")
			.addFunctionMultiInvokes(loadContext, Context.class, "exclude", "excludeCuboid", "excludeCylinder", "excludeSphere")
			.addVariableRenamedGetField(loadContext, "volumeMinY", Context.class, "minY")
			.addVariableRenamedGetField(loadContext, "volumeMaxY", Context.class, "maxY")
			;
		})
	);

	public abstract void override(Context context);

	public static abstract class Context {

		public final ScriptStructures structureStarts;
		public final WorldColumn column;
		public final int minY, maxY;
		public final NumberArray noise;

		public Context(ScriptStructures structureStarts, WorldColumn column, int minY, NumberArray noise) {
			this.structureStarts = structureStarts;
			this.column          = column;
			this.minY            = minY;
			this.noise           = noise;
			this.maxY            = minY + noise.length();
		}

		public abstract double getExclusionMultiplier(int y);

		public void excludeUnchecked(int y, double amount) {
			this.noise.add(y - this.minY, amount * this.getExclusionMultiplier(y));
		}

		public void exclude(int y, double amount) {
			if (y >= this.minY && y < this.maxY) {
				this.excludeUnchecked(y, amount);
			}
		}

		public void excludeCuboid(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double padding) {
			WorldColumn column = this.column;
			double paddingSquared = padding * padding;
			double clampedRelativeX = MathHelper.clamp(column.x, minX, maxX) - column.x;
			double clampedRelativeZ = MathHelper.clamp(column.z, minZ, maxZ) - column.z;
			double horizontalDistanceSquared = BigGlobeMath.squareD(clampedRelativeX, clampedRelativeZ);
			if (horizontalDistanceSquared >= paddingSquared) return;
			int lowerY = Math.max(BigGlobeMath. ceilI(minY - padding), this.minY);
			int upperY = Math.min(BigGlobeMath.floorI(maxY + padding), this.maxY - 1);
			for (int y = lowerY; y <= upperY; y++) {
				double clampedRelativeY = MathHelper.clamp(y, minY, maxY) - y;
				double distanceSquared = horizontalDistanceSquared + clampedRelativeY * clampedRelativeY;
				if (distanceSquared < paddingSquared) {
					double fraction = 1.0D - Math.sqrt(distanceSquared) / padding;
					this.excludeUnchecked(y, Interpolator.smooth(fraction));
				}
			}
		}

		public void _excludeCuboid(BlockBox box, double padding) {
			this.excludeCuboid(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX() + 1, box.getMaxY() + 1, box.getMaxZ() + 1, padding);
		}

		public void excludeCuboid(StructureStartWrapper start, double padding) {
			this._excludeCuboid(start.box(), padding);
		}

		public void excludeCuboid(StructurePiece piece, double padding) {
			this._excludeCuboid(piece.getBoundingBox(), padding);
		}

		public void excludeCylinder(double centerX, double centerZ, double radius, double minY, double maxY, double padding) {
			WorldColumn column = this.column;
			double horizontalDistanceSquared = BigGlobeMath.squareD(
				column.x - centerX,
				column.z - centerZ
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
			int lowerY = Math.max(BigGlobeMath. ceilI(minY - padding), this.minY);
			int upperY = Math.min(BigGlobeMath.floorI(maxY + padding), this.maxY - 1);
			double paddingSquared = padding * padding;
			for (int y = lowerY; y <= upperY; y++) {
				double verticalDistance = Interpolator.clamp(minY, maxY, y) - y;
				double distanceSquared = horizontalDistanceSquared + verticalDistance * verticalDistance;
				if (distanceSquared < paddingSquared) {
					double fraction = 1.0D - Math.sqrt(distanceSquared) / padding;
					this.excludeUnchecked(y, Interpolator.smooth(fraction));
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
			WorldColumn column = this.column;
			double outerRadius = radius + padding;
			double distanceXZ2 = BigGlobeMath.squareD(column.x - centerX, column.z - centerZ);
			if (distanceXZ2 >= BigGlobeMath.squareD(outerRadius)) return;
			int
				minY = Math.max(BigGlobeMath. ceilI(centerY - outerRadius), this.minY),
				maxY = Math.min(BigGlobeMath.floorI(centerY + outerRadius), this.maxY - 1),
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
					this.excludeUnchecked(y, Interpolator.smooth(exclusion));
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
					double exclusion = Interpolator.unmixLinear(outerRadius, radius, distanceXYZ);
					this.excludeUnchecked(y, Interpolator.smooth(exclusion));
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

	public static abstract class Holder<T_Overrider extends VolumetricOverrider> extends Overrider.Holder<T_Overrider> implements VolumetricOverrider {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, TemplateScriptParser<T_Overrider> parser, Class<? extends Context> contextClass) throws ScriptParsingException {
			super(
				usage,
				parser
				.addEnvironment(STRUCTURE_START_EXCLUDE_ENVIRONMENT)
				.addEnvironment(
					Overrider.createDistanceEnvironment(
						getField(
							load("context", 1, type(contextClass)),
							FieldInfo.getField(Context.class, "column")
						)
					)
				)
				.addEnvironment(
					ColumnScriptEnvironmentBuilder.createFixedXZVariableY(
						ColumnValue.REGISTRY,
						getField(
							load("context", 1, type(contextClass)),
							FieldInfo.getField(Context.class, "column")
						),
						null
					)
					.addXZ("x", "z")
					.build()
				)
			);
		}
	}
}