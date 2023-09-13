package builderb0y.bigglobe.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.pointSequences.AdditiveRecurrenceIterator2D;
import builderb0y.bigglobe.math.pointSequences.BoundedPointIterator2D;
import builderb0y.bigglobe.math.pointSequences.GoldenSpiralIterator;
import builderb0y.bigglobe.scripting.interfaces.ColumnPredicate;
import builderb0y.bigglobe.versions.ServerCommandSourceVersions;

public class LocateAreaCommand extends AsyncLocateCommand<LocateAreaCommand.Result> {

	public final BoundedPointIterator2D iterator;
	public final WorldColumn column;
	public final ColumnPredicate predicate;
	public final int radius;
	public int largestArea = 256;

	public LocateAreaCommand(ServerCommandSource source, BoundedPointIterator2D iterator, ColumnPredicate predicate, int radius) {
		super(source);
		this.column = WorldColumn.forWorld(source.getWorld(), 0, 0);
		this.iterator = iterator;
		this.predicate = predicate;
		this.radius = radius;
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager
			.literal(BigGlobeMod.MODID + ":locateArea")
			.requires(source -> source.hasPermissionLevel(4))
			.then(
				CommandManager
				.argument("range", IntegerArgumentType.integer(1, 30_000_000))
				.then(
					CommandManager
					.argument("script", new LocateAreaLazyScript.Argument())
					.executes(LocateAreaCommand::execute)
				)
			)
		);
	}

	public static LocateAreaLazyScript getScript(CommandContext<ServerCommandSource> context, WorldColumn column) throws CommandSyntaxException {
		LocateAreaLazyScript script = context.getArgument("script", LocateAreaLazyScript.class);
		for (ColumnValue<?> value : script.usedValues) {
			if (!value.accepts(column)) {
				throw LocateNoiseCommand.INVALID_COLUMN_VALUE.create(value.getName());
			}
		}
		return script;
	}

	public static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		WorldColumn column = WorldColumn.forWorld(source.getWorld(), 0, 0);
		LocateAreaLazyScript script = getScript(context, column);
		Vec3d centerPos = source.getPosition();
		int
			centerX = BigGlobeMath.floorI(centerPos.x),
			centerZ = BigGlobeMath.floorI(centerPos.z),
			range   = context.getArgument("range", int.class);
		LocateAreaCommand command = new LocateAreaCommand(
			source,
			new AdditiveRecurrenceIterator2D(
				centerX - range,
				centerZ - range,
				centerX + range,
				centerZ + range,
				source.getWorld().random.nextDouble(),
				source.getWorld().random.nextDouble()
			),
			script.getScript(),
			range
		);
		ServerCommandSourceVersions.sendFeedback(source, () -> Text.translatable("commands." + BigGlobeMod.MODID + ".locate.searching"), false);
		command.start(context.getInput());
		return 1;
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
		WorldColumn column = this.column;
		column.setPosUnchecked(this.iterator.floorX(), this.iterator.floorY());
		ColumnPredicate predicate = this.predicate;
		if (!predicate.test(column)) return null;
		GoldenSpiralIterator spiral = new GoldenSpiralIterator(this.iterator.x(), this.iterator.y(), 4.0D, 0.0D);
		while (true) {
			spiral.next();
			column.setPosUnchecked(spiral.floorX(), spiral.floorY());
			if (this.iterator.contains(column.x, column.z) && predicate.test(column)) {
				continue;
			}
			int reverseX = BigGlobeMath.floorI(spiral.originX - spiral.normX * spiral.radius);
			int reverseZ = BigGlobeMath.floorI(spiral.originY - spiral.normY * spiral.radius);
			column.setPosUnchecked(reverseX, reverseZ);
			if (this.iterator.contains(column.x, column.z) && predicate.test(column)) {
				spiral.originX -= spiral.normX * spiral.radiusStepSize;
				spiral.originY -= spiral.normY * spiral.radiusStepSize;
				continue;
			}
			break;
		}
		column.setPosUnchecked(BigGlobeMath.floorI(spiral.originX), BigGlobeMath.floorI(spiral.originY));
		int y = column.getFinalTopHeightI();
		Result result = new Result();
		result.x = column.x;
		result.y = y;
		result.z = column.z;
		result.diameter = ((int)(spiral.radius)) << 1;
		return result;
	}

	public void sendFeedback() {
		if (this.isValid()) {
			if (this.results.isEmpty()) {
				this.source.sendError(Text.translatable("commands." + BigGlobeMod.MODID + ".locate.area.fail", this.predicate.getSource(), this.radius));
			}
			else {
				ServerCommandSourceVersions.sendFeedback(this.source, () -> Text.translatable("commands." + BigGlobeMod.MODID + ".locate.area.success", this.predicate.getSource(), this.radius), false);
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