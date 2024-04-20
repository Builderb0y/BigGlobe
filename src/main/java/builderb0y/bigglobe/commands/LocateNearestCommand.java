package builderb0y.bigglobe.commands;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToBooleanScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.pointSequences.GoldenSpiralIterator;
import builderb0y.bigglobe.versions.ServerCommandSourceVersions;

public class LocateNearestCommand extends AsyncCommand {

	public final ColumnToBooleanScript.Holder script;

	public LocateNearestCommand(ServerCommandSource source, ColumnToBooleanScript.Holder script) {
		super(source);
		this.script = script;
	}

	@Override
	public void run() {
		ScriptedColumn column = this.newScriptedColumn();
		for (
			GoldenSpiralIterator iterator = new GoldenSpiralIterator(
				this.source.getPosition().x,
				this.source.getPosition().z,
				4.0D,
				this.source.getWorld().random.nextDouble() * (Math.PI * 2.0D)
			);
			iterator.radius < 1_000_000;
			iterator.next()
		) {
			if (!this.isValid()) return;
			column.setParamsUnchecked(column.params.at(iterator.floorX(), iterator.floorY()));
			if (this.script.get(column)) {
				ServerCommandSourceVersions.sendFeedback(
					this.source,
					() -> (
						Text
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
						.styled((Style style) ->
							style
							.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("commands.bigglobe.locate.clickToTeleport")))
							.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + iterator.floorX() + " ~ " + iterator.floorY()))
						)
					),
					false
				);
				return;
			}
		}
		ServerCommandSourceVersions.sendFeedback(
			this.source,
			() -> Text.translatable("commands.bigglobe.locate.nearest.fail", this.script.getSource()),
			false
		);
	}
}