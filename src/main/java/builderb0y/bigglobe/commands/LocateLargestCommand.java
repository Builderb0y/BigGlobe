package builderb0y.bigglobe.commands;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToBooleanScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.pointSequences.BoundedPointIterator2D;
import builderb0y.bigglobe.math.pointSequences.GoldenSpiralIterator;

public class LocateLargestCommand extends AsyncLocateCommand<LocateLargestCommand.Result> {

	public final BoundedPointIterator2D iterator;
	public final ColumnToBooleanScript.Holder predicate;
	public final int radius;
	public int largestArea = 256;

	public LocateLargestCommand(
		ServerCommandSource source,
		BoundedPointIterator2D iterator,
		ColumnToBooleanScript.Holder predicate,
		int radius
	) {
		super(source);
		this.iterator = iterator;
		this.predicate = predicate;
		this.radius = radius;
	}

	@Override
	public void addResult(Result result) {
		super.addResult(result);
		if (result.diameter > this.largestArea) {
			this.largestArea = result.diameter;
		}
	}

	@Override
	public void run() {
		long checkTime = System.currentTimeMillis() + 1000L;
		while (true) {
			if (System.currentTimeMillis() > checkTime) {
				if (!this.isValid()) break;
				checkTime += 1000L;
			}
			if (this.iterator.averageDistanceBetweenPoints() < this.largestArea) {
				this.source.getServer().execute(this::sendFeedback);
				break;
			}
			Result result = this.nextResult();
			if (result != null) {
				this.addResult(result);
			}
			this.iterator.next();
		}
	}

	public @Nullable Result nextResult() {
		ScriptedColumn column = this.newScriptedColumn();
		column.setParamsUnchecked(column.params.at(this.iterator.floorX(), this.iterator.floorY()));
		ColumnToBooleanScript.Holder predicate = this.predicate;
		if (!predicate.get(column)) return null;
		GoldenSpiralIterator spiral = new GoldenSpiralIterator(this.iterator.x(), this.iterator.y(), 4.0D, 0.0D);
		while (true) {
			spiral.next();
			column.setParamsUnchecked(column.params.at(spiral.floorX(), spiral.floorY()));
			if (this.iterator.contains(column.x(), column.z()) && predicate.get(column)) {
				continue;
			}
			int reverseX = BigGlobeMath.floorI(spiral.originX - spiral.normX * spiral.radius);
			int reverseZ = BigGlobeMath.floorI(spiral.originY - spiral.normY * spiral.radius);
			column.setParamsUnchecked(column.params.at(reverseX, reverseZ));
			if (this.iterator.contains(column.x(), column.z()) && predicate.get(column)) {
				spiral.originX -= spiral.normX * spiral.radiusStepSize;
				spiral.originY -= spiral.normY * spiral.radiusStepSize;
				continue;
			}
			break;
		}
		column.setParamsUnchecked(column.params.at(BigGlobeMath.floorI(spiral.originX), BigGlobeMath.floorI(spiral.originY)));
		Result result = new Result();
		result.x = column.x();
		result.z = column.z();
		result.diameter = ((int)(spiral.radius)) << 1;
		return result;
	}

	public void sendFeedback() {
		if (this.isValid()) {
			if (this.results.isEmpty()) {
				this.source.sendError(Text.translatable("commands.bigglobe.locate.largest.fail", this.predicate.getSource(), this.radius));
			}
			else {
				this.source.sendFeedback(() -> Text.translatable("commands.bigglobe.locate.largest.success", this.predicate.getSource(), this.radius), false);
				this.sendResults();
			}
		}
	}

	@Override
	public int compare(Result r1, Result r2) {
		//intentionally reversed so that large radii are selected first.
		return Integer.compare(r2.diameter, r1.diameter);
	}

	public static class Result extends AsyncLocateCommand.Result {

		public int diameter;

		@Override
		public String valueToString() {
			return this.diameter + " blocks wide";
		}
	}
}