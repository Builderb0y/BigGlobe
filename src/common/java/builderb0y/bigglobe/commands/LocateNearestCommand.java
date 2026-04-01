package builderb0y.bigglobe.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToBooleanScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.pointSequences.GoldenSpiralIterator;
import builderb0y.bigglobe.versions.TextVersions;

public class LocateNearestCommand extends AsyncCommand {

	public final ColumnToBooleanScript.Catcher script;
	public final GoldenSpiralIterator iterator;

	public LocateNearestCommand(CommandSourceStack source, ColumnToBooleanScript.Catcher script) {
		super(source);
		this.script = script;
		this.iterator = new GoldenSpiralIterator(
			source.getPosition().x,
			source.getPosition().z,
			4.0D,
			source.getLevel().getRandom().nextDouble() * (Math.PI * 2.0D)
		);
	}

	@Override
	public void run() {
		ScriptedColumn column = this.newScriptedColumn();
		for (
			GoldenSpiralIterator iterator = this.iterator;
			iterator.radius < 1_000_000;
			iterator.next()
		) {
			if (!this.isValid()) return;
			column.setParamsUnchecked(column.params.at(iterator.floorX(), iterator.floorY()));
			if (this.script.get(column)) {
				this.source.sendSuccess(
					() -> (
						Component
							.translatable(
								"commands.bigglobe.locate.nearest.success",
								this.script.getSource(),
								iterator.floorX(),
								iterator.floorY(),
								BigGlobeCommands.format(
									Math.sqrt(
										BigGlobeMath.squareD(
											this.source.getPosition().x - (iterator.floorX() + 0.5D),
											this.source.getPosition().z - (iterator.floorY() + 0.5D)
										)
									)
								)
							)
							.withStyle((Style style) ->
										style
											.withHoverEvent(TextVersions.showText(Component.translatable("commands.bigglobe.locate.clickToTeleport")))
											.withClickEvent(TextVersions.suggestCommand("/tp @s " + iterator.floorX() + " ~ " + iterator.floorY()))
							)
					),
					false
				);
				return;
			}
		}
		this.source.sendSuccess(() -> Component.translatable("commands.bigglobe.locate.nearest.fail", this.script.getSource()), false);
	}
}