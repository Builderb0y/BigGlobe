package builderb0y.bigglobe.commands;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.command.ServerCommandSource;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.pointSequences.BoundedPointIterator;
import builderb0y.bigglobe.math.pointSequences.BoundedPointIterator2D;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;

public class LocateNoiseCommand2D extends LocateNoiseCommand {

	public final BoundedPointIterator2D iterator;

	public LocateNoiseCommand2D(
		ServerCommandSource source,
		ColumnYToDoubleScript script,
		CompareMode compareMode,
		int radius,
		BoundedPointIterator2D iterator
	) {
		super(source, script, compareMode, radius);
		this.iterator = iterator;
	}

	@Override
	public @Nullable Result nextResult(boolean bounded) {
		Result bestResult = this.getResultAt(
			this.iterator.floorX(),
			this.iterator.floorY()
		);
		if (bestResult == null) return null;

		int stepSize = 64;
		while (true) {
			Result nextResult = bestResult;
			for (int offsetX = -stepSize; offsetX <= stepSize; offsetX += stepSize) {
				for (int offsetZ = -stepSize; offsetZ <= stepSize; offsetZ += stepSize) {
					if (offsetX == 0 && offsetZ == 0) continue;
					int testX = bestResult.x + offsetX;
					int testZ = bestResult.z + offsetZ;
					if (bounded && !this.iterator.contains(testX, testZ)) continue;
					Result testResult = this.getResultAt(testX, testZ);
					if (testResult != null) {
						nextResult = this.getBest(nextResult, testResult);
					}
				}
			}
			if (nextResult != bestResult) {
				nextResult.pathLength = (
					bestResult.pathLength
					+ Math.sqrt(
						BigGlobeMath.squareI(
							nextResult.x - bestResult.x,
							nextResult.z - bestResult.z
						)
					)
				);
				bestResult = nextResult;
			}
			else if ((stepSize >>>= 1) == 0) {
				break;
			}
		}
		this.column.setPos(bestResult.x, bestResult.z);
		bestResult.y = this.column.getFinalTopHeightI();
		return bestResult;
	}

	public @Nullable Result getResultAt(int x, int z) {
		this.column.setPos(x, z);
		double value = this.script.evaluate(this.column, 0.0D);
		if (Double.isNaN(value)) return null;
		Result result = new Result();
		result.x = x;
		result.z = z;
		result.value = value;
		return result;
	}

	@Override
	public BoundedPointIterator getIterator() {
		return this.iterator;
	}
}