package builderb0y.bigglobe.platform.fabric.fabric.api.client.command.v2;

import java.util.ArrayList;
import java.util.List;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;

public interface ClientCommandRegistrationCallback {

	void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess);

	class EventHolder {
		public final List<ClientCommandRegistrationCallback> listeners = new ArrayList<>();

		public void register(ClientCommandRegistrationCallback callback) {
			this.listeners.add(callback);
		}
	}

	EventHolder EVENT = new EventHolder();
}
