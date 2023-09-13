package builderb0y.bigglobe.commands;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.command.ServerCommandSource;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.pointSequences.BoundedPointIterator;
import builderb0y.bigglobe.math.pointSequences.BoundedPointIterator3D;
import builderb0y.bigglobe.scripting.interfaces.ColumnYToDoubleScript;

public class LocateNoiseCommand3D extends LocateNoiseCommand {

	public final BoundedPointIterator3D iterator;

	public LocateNoiseCommand3D(
		ServerCommandSource source,
		ColumnYToDoubleScript script,
		CompareMode compareMode,
		int radius,
		BoundedPointIterator3D iterator
	) {
		super(source, script, compareMode, radius);
		this.iterator = iterator;
	}

	@Override
	public @Nullable Result nextResult(boolean bounded) {
		Result bestResult = this.getResultAt(
			this.iterator.floorX(),
			this.iterator.floorY(),
			this.iterator.floorZ()
		);
		if (bestResult == null) return null;

		int stepSize = 64;
		while (true) {
			Result nextResult = bestResult;
			for (int offsetX = -stepSize; offsetX <= stepSize; offsetX += stepSize) {
				for (int offsetZ = -stepSize; offsetZ <= stepSize; offsetZ += stepSize) {
					for (int offsetY = -stepSize; offsetY <= stepSize; offsetY += stepSize) {
						if (offsetX == 0 && offsetY == 0 && offsetZ == 0) continue;
						int testX = bestResult.x + offsetX;
						int testY = bestResult.y + offsetY;
						int testZ = bestResult.z + offsetZ;
						if (bounded && !this.iterator.contains(testX, testY, testZ)) continue;
						Result testResult = this.getResultAt(testX, testY, testZ);
						if (testResult != null) {
							nextResult = this.getBest(nextResult, testResult);
						}
					}
				}
			}
			if (nextResult != bestResult) {
				nextResult.pathLength = (
					bestResult.pathLength
					+ Math.sqrt(
						BigGlobeMath.squareI(
							nextResult.x - bestResult.x,
							nextResult.y - bestResult.y,
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
		return bestResult;
	}

	public @Nullable Result getResultAt(int x, int y, int z) {
		this.column.setPos(x, z);
		double value = this.script.evaluate(this.column, y);
		if (Double.isNaN(value)) return null;
		Result result = new Result();
		result.x = x;
		result.y = y;
		result.z = z;
		result.value = value;
		return result;
	}

	@Override
	public BoundedPointIterator getIterator() {
		return this.iterator;
	}
}