package builderb0y.bigglobe.platform.fabric.fabric.api.command.v2;

import java.util.ArrayList;
import java.util.List;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands.CommandSelection;

public interface CommandRegistrationCallback {

	void register(
		CommandDispatcher<CommandSourceStack> dispatcher,
		CommandBuildContext registryAccess,
		CommandSelection environment
	);

	class EventHolder {
		public final List<CommandRegistrationCallback> listeners = new ArrayList<>();

		public void register(CommandRegistrationCallback callback) {
			this.listeners.add(callback);
		}

		public void invoker(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, CommandSelection environment) {
			for (CommandRegistrationCallback listener : this.listeners) {
				listener.register(dispatcher, registryAccess, environment);
			}
		}

		public CommandRegistrationCallback invoker() {
			return this::invoker;
		}
	}

	EventHolder EVENT = new EventHolder();
}
