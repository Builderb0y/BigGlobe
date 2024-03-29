package builderb0y.bigglobe.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

import builderb0y.autocodec.util.AutoCodecUtil;

public class SodiumCompat {

	public static final boolean SODIUM_INSTALLED;
	static {
		try {
			ModContainer sodium = FabricLoader.getInstance().getModContainer("sodium").orElse(null);
			SODIUM_INSTALLED = sodium != null && sodium.getMetadata().getVersion().compareTo(Version.parse("0.5.0")) >= 0;
		}
		catch (VersionParsingException exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}

	/** sodium swaps the order of the channels, for some reason. */
	public static int maybeSwapChannels(int color) {
		if (SODIUM_INSTALLED) return Integer.reverseBytes(Integer.rotateLeft(color, 8));
		else return color;
	}
}