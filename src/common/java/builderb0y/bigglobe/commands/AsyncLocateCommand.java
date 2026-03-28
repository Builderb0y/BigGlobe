package builderb0y.bigglobe.commands;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.commands.AsyncLocateCommand.Result;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.versions.TextVersions;

public abstract class AsyncLocateCommand<T_Result extends Result> extends AsyncCommand implements Comparator<T_Result> {

	public final TreeSet<T_Result> results = new TreeSet<>(this);

	public AsyncLocateCommand(CommandSourceStack source) {
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
			this.source.sendSuccess(() -> result.toText(this.source), false);
			BigGlobeMod.LOGGER.info(result.toString());
		}
		if (iterator.hasNext()) {
			//if there are more than 10 results, send feedback saying how many more results there were.
			this.source.sendSuccess(() -> Component.translatable("commands." + BigGlobeMod.MODID + ".locate.more", this.results.size() - 10), false);
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

		public int x, z;

		public abstract String valueToString();

		public Style toStyle() {
			return (
				Style
					.EMPTY
					.withHoverEvent(
						TextVersions.showText(
							Component.translatable("commands." + BigGlobeMod.MODID + ".locate.clickToTeleport")
						)
					)
					.withClickEvent(
						TextVersions.suggestCommand(
							"/tp @s " + this.x + " ~ " + this.z
						)
					)
			);
		}

		public Component toText(CommandSourceStack source) {
			return (
				Component.literal("(" + this.x + ", " + this.z + ')')
					.setStyle(this.toStyle())
					.withStyle(ChatFormatting.GREEN)
					.append(Component.literal(" -> ").withStyle(ChatFormatting.WHITE))
					.append(Component.literal(this.valueToString()).withStyle(ChatFormatting.AQUA))
					.append(Component.literal(" (").withStyle(ChatFormatting.WHITE))
					.append(
						Component.literal(
								BigGlobeCommands.format(
									Math.sqrt(
										BigGlobeMath.squareD(
											source.getPosition().x - (this.x + 0.5D),
											source.getPosition().z - (this.z + 0.5D)
										)
									)
								)
								+ " block(s) away"
							)
							.withStyle(ChatFormatting.BLUE)
					)
					.append(Component.literal(")").withStyle(ChatFormatting.WHITE))
			);
		}

		@Override
		public String toString() {
			return "(" + this.x + ", " + this.z + ") -> " + this.valueToString();
		}
	}
}