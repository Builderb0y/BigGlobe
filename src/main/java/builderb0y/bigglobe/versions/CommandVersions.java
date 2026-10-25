package builderb0y.bigglobe.versions;

import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;

public class CommandVersions {

	public static Predicate<CommandSourceStack> levelPredicate(int level) {
		return source -> source.hasPermission(level);
	}
}
