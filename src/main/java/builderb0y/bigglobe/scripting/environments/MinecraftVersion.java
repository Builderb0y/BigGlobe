package builderb0y.bigglobe.scripting.environments;

import java.lang.invoke.MethodHandles;

import org.jetbrains.annotations.NotNull;

import net.minecraft.SharedConstants;

import builderb0y.scripting.bytecode.ConstantFactory;

public record MinecraftVersion(int major, int minor, int bugfix) implements Comparable<MinecraftVersion> {

	public static final MinecraftVersion CURRENT;

	static {
		MinecraftVersion current;
		try {
			current = of(
				SharedConstants.getGameVersion().getName()
			);
		}
		catch (Throwable throwable) {
			current = null; //probably in a unit test.
		}
		CURRENT = current;
	}

	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static MinecraftVersion of(MethodHandles.Lookup caller, String name, Class<?> type, String string) {
		return of(string);
	}

	public static MinecraftVersion of(String string) {
		int major = 0, minor = 0, bugfix = 0;
		int point = 0;
		for (int index = 0, length = string.length(); index < length; index++) {
			char c = string.charAt(index);
			if (c == '.') {
				if (++point > 2) {
					throw new IllegalArgumentException("More than 2 points in version: " + string);
				}
				major = minor;
				minor = bugfix;
				bugfix = 0;
			}
			else {
				int digit = Character.digit(c, 10);
				if (digit >= 0) bugfix = Math.addExact(Math.multiplyExact(bugfix, 10), digit);
				else throw new NumberFormatException("Non-digit character in version string: " + string);
			}
		}
		//1.21.1: fine.
		//1.21: fine.
		//1: not fine.
		if (point == 0) {
			throw new IllegalArgumentException("No points in version: " + string);
		}
		else if (point == 1) { //special handle versions with only one point.
			major = minor;
			minor = bugfix;
			bugfix = 0;
		}
		return new MinecraftVersion(major, minor, bugfix);
	}

	@Override
	public int compareTo(@NotNull MinecraftVersion that) {
		int compare = Integer.compare(this.major, that.major);
		if (compare != 0) return compare;
		compare = Integer.compare(this.minor, that.minor);
		if (compare != 0) return compare;
		return Integer.compare(this.bugfix, that.bugfix);
	}

	@Override
	public String toString() {
		return this.major + "." + this.minor + "." + this.bugfix;
	}
}