package builderb0y.bigglobe.versions;

import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class CommandVersions {

	public static Predicate<CommandSourceStack> levelPredicate(int level) {

		return Commands.hasPermission(
			switch (level) {
				case 0 -> Commands.LEVEL_ALL;
				case 1 -> Commands.LEVEL_MODERATORS;
				case 2 -> Commands.LEVEL_GAMEMASTERS;
				case 3 -> Commands.LEVEL_ADMINS;
				case 4 -> Commands.LEVEL_OWNERS;
				default -> throw new IllegalArgumentException(String.valueOf(level));
			}
		);
	}
}