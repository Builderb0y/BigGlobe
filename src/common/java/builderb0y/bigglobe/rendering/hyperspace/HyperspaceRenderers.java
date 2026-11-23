package builderb0y.bigglobe.rendering.hyperspace;

import java.util.Random;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.randomLists.RandomList;

public class HyperspaceRenderers {

	public static final Random RANDOM = new Random();
	public static final RandomList<HyperspaceRenderer.Factory> FACTORIES = new RandomList<>();

	public static @Nullable HyperspaceRenderer getRandom() {
		try {
			return FACTORIES.getRandomElement(RANDOM).create();
		}
		catch (Throwable throwable) {
			BigGlobeMod.LOGGER.error("Failed to create hyperspace renderer this time", throwable);
			return null;
		}
	}

	static {
		FACTORIES.add(JuliaVolumeHyperspaceRenderer::new, 1.0D);
		FACTORIES.add(     CloudyHyperspaceRenderer::new, 1.0D);
		FACTORIES.add(     AuroraHyperspaceRenderer::new, 1.0D);
	}
}