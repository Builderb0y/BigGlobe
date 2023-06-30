package builderb0y.bigglobe.versions;

import java.util.function.Supplier;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ServerCommandSourceVersions {

	public static void sendFeedback(ServerCommandSource source, Supplier<Text> feedbackSupplier, boolean broadcastToOps) {
		source.sendFeedback(feedbackSupplier.get(), broadcastToOps);
	}
}