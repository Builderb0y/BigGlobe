package builderb0y.bigglobe.commands;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.ColumnValue.CustomDisplayContext;
import builderb0y.bigglobe.commands.AsyncLocateCommand.Result;
import builderb0y.bigglobe.versions.ServerCommandSourceVersions;

public abstract class AsyncLocateCommand<T_Result extends Result> extends AsyncCommand implements Comparator<T_Result> {

	public final TreeSet<T_Result> results = new TreeSet<>(this);

	public AsyncLocateCommand(ServerCommandSource source) {
		super(source);
	}

	public void addResult(T_Result result) {
		this.results.add(result);
	}

	public void sendResults() {
		Iterator<T_Result> iterator = this.results.iterator();
		//first 10 results get sent as feedback and logged.
		for (int index = 0; index < 10 && iterator.hasNext(); index++) {
			T_Result result = iterator.next();
			ServerCommandSourceVersions.sendFeedback(this.source, () -> result.toText(this.source), false);
			BigGlobeMod.LOGGER.info(result.toString());
		}
		if (iterator.hasNext()) {
			//if there are more than 10 results, send feedback saying how many more results there were.
			ServerCommandSourceVersions.sendFeedback(this.source, () -> Text.translatable("commands." + BigGlobeMod.MODID + ".locate.more", this.results.size() - 10), false);
			//first 100 results only get logged.
			for (int index = 10; index < 100 && iterator.hasNext(); index++) {
				T_Result result = iterator.next();
				BigGlobeMod.LOGGER.info(result.toString());
			}
			if (iterator.hasNext()) {
				//if there are more than 100 results, log how many more results there were.
				BigGlobeMod.LOGGER.info("..." + (this.results.size() - 100) + " more");
			}
		}
	}

	public static abstract class Result {

		public int x, y, z;

		public abstract String valueToString();

		public Style toStyle() {
			return (
				Style
				.EMPTY
				.withHoverEvent(
					new HoverEvent(
						HoverEvent.Action.SHOW_TEXT,
						Text.translatable("commands." + BigGlobeMod.MODID + ".locate.clickToTeleport")
					)
				)
				.withClickEvent(
					new ClickEvent(
						ClickEvent.Action.SUGGEST_COMMAND,
						"/tp @s " + this.x + ' ' + this.y + ' ' + this.z
					)
				)
			);
		}

		public Text toText(ServerCommandSource source) {
			return (
				Text.literal("(" + this.x + ", " + this.y + ", " + this.z + ')')
				.setStyle(this.toStyle())
				.formatted(Formatting.GREEN)
				.append(Text.literal(" -> ").formatted(Formatting.WHITE))
				.append(Text.literal(this.valueToString()).formatted(Formatting.AQUA))
				.append(Text.literal(" (").formatted(Formatting.WHITE))
				.append(
					Text.literal(
						CustomDisplayContext.format(
							Math.sqrt(
								source.getPosition()
								.squaredDistanceTo(this.x + 0.5D, this.y, this.z + 0.5D)
							)
						)
						+ " block(s) away"
					)
					.formatted(Formatting.BLUE)
				)
				.append(Text.literal(")").formatted(Formatting.WHITE))
			);
		}

		@Override
		public String toString() {
			return "(" + this.x + ", " + this.y + ", " + this.z + ") -> " + this.valueToString();
		}
	}
}