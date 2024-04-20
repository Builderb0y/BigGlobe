package builderb0y.bigglobe.commands;

import java.util.Comparator;
import java.util.Locale;

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToDoubleScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.commands.LocateMinMaxCommand.Result;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.pointSequences.BoundedPointIterator2D;
import builderb0y.bigglobe.versions.ServerCommandSourceVersions;

public class LocateMinMaxCommand extends AsyncLocateCommand<Result> {

	public static final DynamicCommandExceptionType INVALID_COLUMN_VALUE = new DynamicCommandExceptionType(value -> Text.translatable("commands." + BigGlobeMod.MODID + ".locate.noise.invalidColumnValue", value));

	public final CompareMode compareMode;
	public final BoundedPointIterator2D iterator;
	public final ColumnToDoubleScript.Holder script;
	public final int radius;
	public double maxPathLength = 16.0D;

	public LocateMinMaxCommand(
		ServerCommandSource source,
		BoundedPointIterator2D iterator,
		ColumnToDoubleScript.Holder script,
		CompareMode compareMode,
		int radius
	) {
		super(source);
		this.iterator    = iterator;
		this.script      = script;
		this.compareMode = compareMode;
		this.radius      = radius;
	}
	@Override
	public void addResult(Result result) {
		super.addResult(result);
		if (result.pathLength > this.maxPathLength) {
			this.maxPathLength = result.pathLength;
		}
	}

	@Override
	public void run() {
		ScriptedColumn column = this.newScriptedColumn();
		long checkTime = System.currentTimeMillis() + 1000L;
		while (true) {
			if (System.currentTimeMillis() > checkTime) {
				if (!this.isValid()) break;
				checkTime += 1000L;
			}
			if (this.iterator.averageDistanceBetweenPoints() < this.maxPathLength) {
				this.source.getServer().execute(this::sendFeedback);
				break;
			}
			Result result = this.nextResult(column, true);
			if (result != null) {
				this.addResult(result);
			}
			this.iterator.next();
		}
	}

	public @Nullable Result nextResult(ScriptedColumn column, boolean bounded) {
		Result bestResult = this.getResultAt(
			column,
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
					Result testResult = this.getResultAt(column, testX, testZ);
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
		return bestResult;
	}

	public @Nullable Result getResultAt(ScriptedColumn column, int x, int z) {
		column.setParamsUnchecked(column.params.at(x, z));
		double value = this.script.get(column);
		if (Double.isNaN(value)) return null;
		Result result = new Result();
		result.x = x;
		result.z = z;
		result.value = value;
		return result;
	}

	public void sendFeedback() {
		if (this.isValid()) {
			if (this.results.isEmpty()) {
				this.source.sendError(Text.translatable("commands." + BigGlobeMod.MODID + ".locate." + this.compareMode.lowerCaseName + ".fail", this.script.getSource(), this.radius));
			}
			else {
				ServerCommandSourceVersions.sendFeedback(this.source, () -> Text.translatable("commands." + BigGlobeMod.MODID + ".locate." + this.compareMode.lowerCaseName + ".success", this.script.getSource(), this.radius), false);
				this.sendResults();
			}
		}
	}

	@Override
	public int compare(Result r1, Result r2) {
		return this.compareMode.compare(r1, r2);
	}

	public Result getBest(Result r1, Result r2) {
		return this.compareMode.getBest(r1, r2);
	}

	public static class Result extends AsyncLocateCommand.Result {

		public double value;
		public double pathLength;

		@Override
		public String valueToString() {
			return BigGlobeCommands.format(this.value);
		}
	}

	public static enum CompareMode implements Comparator<Result>, StringIdentifiable {

		MIN {

			@Override
			public int compare(Result r1, Result r2) {
				return Double.compare(r1.value, r2.value);
			}

			@Override
			public Result getBest(Result r1, Result r2) {
				return r2.value < r1.value ? r2 : r1;
			}
		},

		MAX {

			@Override
			public int compare(Result r1, Result r2) {
				return Double.compare(r2.value, r1.value);
			}

			@Override
			public Result getBest(Result r1, Result r2) {
				return r2.value > r1.value ? r2 : r1;
			}
		};

		public final String lowerCaseName = this.name().toLowerCase(Locale.ROOT);

		public abstract Result getBest(Result r1, Result r2);

		@Override
		public String asString() {
			return this.lowerCaseName;
		}
	}
}