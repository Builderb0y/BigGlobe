package builderb0y.bigglobe.commands;

import java.util.Comparator;
import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.ColumnValue.CustomDisplayContext;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.commands.LocateNoiseCommand.Result;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.pointSequences.AdditiveRecurrenceIterator2D;
import builderb0y.bigglobe.math.pointSequences.AdditiveRecurrenceIterator3D;
import builderb0y.bigglobe.math.pointSequences.BoundedPointIterator;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;
import builderb0y.bigglobe.versions.ServerCommandSourceVersions;

public abstract class LocateNoiseCommand extends AsyncLocateCommand<Result> {

	public static final DynamicCommandExceptionType INVALID_COLUMN_VALUE = new DynamicCommandExceptionType(value -> Text.translatable("commands." + BigGlobeMod.MODID + ".locate.noise.invalidColumnValue", value));

	public final CompareMode compareMode;
	public final WorldColumn column;
	public final ColumnYToDoubleScript script;
	public final int radius;
	public double maxPathLength = 16.0D;

	public LocateNoiseCommand(ServerCommandSource source, ColumnYToDoubleScript script, CompareMode compareMode, int radius) {
		super(source);
		this.column      = WorldColumn.forWorld(source.getWorld(), 0, 0);
		this.script      = script;
		this.compareMode = compareMode;
		this.radius      = radius;
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager
			.literal(BigGlobeMod.MODID + ":locateNoise")
			.requires(source -> source.hasPermissionLevel(2))
			.then(
				CommandManager
				.argument("range", IntegerArgumentType.integer(1, 30_000_000))
				.then(
					CommandManager
					.argument("compareMode", new EnumArgument<>(CompareMode.class))
					.then(
						CommandManager
						.argument("script", new LocateNoiseLazyScript.Argument())
						.executes(LocateNoiseCommand::executeMany)
					)
				)
			)
		);
	}

	public static LocateNoiseLazyScript getScript(CommandContext<ServerCommandSource> context, WorldColumn column) throws CommandSyntaxException {
		LocateNoiseLazyScript script = context.getArgument("script", LocateNoiseLazyScript.class);
		for (ColumnValue<?> value : script.usedValues) {
			if (!value.accepts(column)) {
				throw INVALID_COLUMN_VALUE.create(value.getName());
			}
		}
		return script;
	}

	public static int executeOne(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		CompareMode compareMode = context.getArgument("compareMode", CompareMode.class);
		WorldColumn column = WorldColumn.forWorld(source.getWorld(), 0, 0);
		LocateNoiseLazyScript script = getScript(context, column);
		Vec3d centerPos = source.getPosition();
		LocateNoiseCommand command;
		if (script.usedValues.stream().anyMatch(ColumnValue::dependsOnY)) {
			command = new LocateNoiseCommand3D(
				source,
				script.getScript(),
				compareMode,
				0,
				new AdditiveRecurrenceIterator3D(
					centerPos.x,
					centerPos.y,
					centerPos.z,
					centerPos.x,
					centerPos.y,
					centerPos.z,
					source.getWorld().random.nextDouble(),
					source.getWorld().random.nextDouble(),
					source.getWorld().random.nextDouble()
				)
			);
		}
		else {
			command = new LocateNoiseCommand2D(
				source,
				script.getScript(),
				compareMode,
				0,
				new AdditiveRecurrenceIterator2D(
					centerPos.x,
					centerPos.z,
					centerPos.x,
					centerPos.z,
					source.getWorld().random.nextDouble(),
					source.getWorld().random.nextDouble()
				)
			);
		}
		Result result = command.nextResult(false);
		if (result != null) {
			ServerCommandSourceVersions.sendFeedback(source, () -> Text.translatable("commands." + BigGlobeMod.MODID + ".locate.noise." + compareMode.name().toLowerCase(Locale.ROOT) + ".success.single", script.getSource()), false);
			ServerCommandSourceVersions.sendFeedback(source, () -> result.toText(source), false);
			return 1;
		}
		else {
			source.sendError(Text.translatable("commands." + BigGlobeMod.MODID + ".locate.noise." + compareMode.name().toLowerCase(Locale.ROOT) + ".fail.single", script.getSource()));
			return 0;
		}
	}

	public static int executeMany(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		CompareMode compareMode = context.getArgument("compareMode", CompareMode.class);
		WorldColumn column = WorldColumn.forWorld(source.getWorld(), 0, 0);
		LocateNoiseLazyScript script = getScript(context, column);
		Vec3d centerPos = source.getPosition();
		int
			centerX = BigGlobeMath.floorI(centerPos.x),
			centerZ = BigGlobeMath.floorI(centerPos.z),
			range   = context.getArgument("range", int.class);
		LocateNoiseCommand command;
		if (script.usedValues.stream().anyMatch(ColumnValue::dependsOnY)) {
			DimensionType dimension = source.getWorld().getDimension();
			int minY = dimension.minY();
			int maxY = minY + dimension.height();
			int centerY = MathHelper.clamp(
				BigGlobeMath.floorI(centerPos.y),
				minY,
				maxY
			);
			command = new LocateNoiseCommand3D(
				source,
				script.getScript(),
				compareMode,
				range,
				new AdditiveRecurrenceIterator3D(
					(double)(centerX - range),
					(double)(Math.max(centerY - range, minY)),
					(double)(centerX - range),
					(double)(centerX + range + 1),
					(double)(Math.min(centerY + range, maxY)),
					(double)(centerX + range + 1),
					source.getWorld().random.nextDouble(),
					source.getWorld().random.nextDouble(),
					source.getWorld().random.nextDouble()
				)
			);
		}
		else {
			command = new LocateNoiseCommand2D(
				source,
				script.getScript(),
				compareMode,
				range,
				new AdditiveRecurrenceIterator2D(
					(double)(centerX - range),
					(double)(centerZ - range),
					(double)(centerX + range + 1),
					(double)(centerZ + range + 1),
					source.getWorld().random.nextDouble(),
					source.getWorld().random.nextDouble()
				)
			);
		}
		ServerCommandSourceVersions.sendFeedback(source, () -> Text.translatable("commands." + BigGlobeMod.MODID + ".locate.searching"), false);
		command.start(context.getInput());
		return 1;
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
		long checkTime = System.currentTimeMillis() + 1000L;
		while (true) {
			if (System.currentTimeMillis() > checkTime) {
				if (!this.isValid()) break;
				checkTime += 1000L;
			}
			if (this.getIterator().averageDistanceBetweenPoints() < this.maxPathLength) {
				this.source.getServer().execute(this::sendFeedback);
				break;
			}
			Result result = this.nextResult(true);
			if (result != null) {
				this.addResult(result);
			}
			this.getIterator().next();
		}
	}

	public abstract BoundedPointIterator getIterator();

	public abstract @Nullable Result nextResult(boolean bounded);

	public void sendFeedback() {
		if (this.isValid()) {
			if (this.results.isEmpty()) {
				this.source.sendError(Text.translatable("commands." + BigGlobeMod.MODID + ".locate.noise." + this.compareMode.lowerCaseName + ".fail.multi", this.script.getSource(), this.radius));
			}
			else {
				ServerCommandSourceVersions.sendFeedback(this.source, () -> Text.translatable("commands." + BigGlobeMod.MODID + ".locate.noise." + this.compareMode.lowerCaseName + ".success.multi", this.script.getSource(), this.radius), false);
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
			return CustomDisplayContext.format(this.value);
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