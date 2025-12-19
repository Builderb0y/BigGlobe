package builderb0y.bigglobe.versions;

import java.util.function.Predicate;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class CommandVersions {

	public static Predicate<ServerCommandSource> levelPredicate(int level) {
		#if MC_VERSION >= MC_1_21_11
			return CommandManager.requirePermissionLevel(
				switch (level) {
					case 0 -> CommandManager.ALWAYS_PASS_CHECK;
					case 1 -> CommandManager.MODERATORS_CHECK;
					case 2 -> CommandManager.GAMEMASTERS_CHECK;
					case 3 -> CommandManager.ADMINS_CHECK;
					case 4 -> CommandManager.OWNERS_CHECK;
					default -> throw new IllegalArgumentException(String.valueOf(level));
				}
			);
		#else
			return (ServerCommandSource source) -> source.hasPermissionLevel(level);
		#endif
	}
}