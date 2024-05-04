package builderb0y.bigglobe.trees.trunks;

import java.util.random.RandomGenerator;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomSources.RandomSource;

@UseCoder(name = "REGISTRY", usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface TrunkFactory extends CoderRegistryTyped<TrunkFactory> {

	public static final CoderRegistry<TrunkFactory> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("tree_trunk_type"));
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("straight"), StraightTrunkFactory.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("slanted"),   SlantedTrunkFactory.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("twisted"),   TwistedTrunkFactory.class);
	}};

	public abstract TrunkConfig create(
		double startX,
		int startY,
		double startZ,
		int height,
		RandomGenerator random
	);

	public static abstract class AbstractTrunkFactory implements TrunkFactory {

		public final TrunkThicknessScript.Holder thickness;
		public final boolean require_natural_ground;
		public final boolean can_generate_in_liquid;

		public AbstractTrunkFactory(
			TrunkThicknessScript.Holder thickness,
			boolean require_natural_ground,
			boolean can_generate_in_liquid
		) {
			this.thickness = thickness;
			this.require_natural_ground = require_natural_ground;
			this.can_generate_in_liquid = can_generate_in_liquid;
		}
	}

	public static class StraightTrunkFactory extends AbstractTrunkFactory {

		public StraightTrunkFactory(
			TrunkThicknessScript.Holder thickness,
			boolean require_natural_ground,
			boolean can_generate_in_liquid
		) {
			super(thickness, require_natural_ground, can_generate_in_liquid);
		}

		@Override
		public TrunkConfig create(double startX, int startY, double startZ, int height, RandomGenerator random) {
			return new StraightTrunkConfig(
				startX,
				startY,
				startZ,
				height,
				this.thickness,
				this.require_natural_ground,
				this.can_generate_in_liquid
			);
		}
	}

	public static class SlantedTrunkFactory extends AbstractTrunkFactory {

		public final RandomSource slant;

		public SlantedTrunkFactory(
			TrunkThicknessScript.Holder thickness,
			boolean require_natural_ground,
			boolean can_generate_in_liquid,
			RandomSource slant
		) {
			super(thickness, require_natural_ground, can_generate_in_liquid);
			this.slant = slant;
		}

		@Override
		public TrunkConfig create(double startX, int startY, double startZ, int height, RandomGenerator random) {
			double slant = this.slant.get(random);
			double angle = random.nextDouble(BigGlobeMath.TAU);
			return new SlantedTrunkConfig(
				startX,
				startY,
				startZ,
				height,
				Math.cos(angle) * slant,
				Math.sin(angle) * slant,
				this.thickness,
				this.require_natural_ground,
				this.can_generate_in_liquid
			);
		}
	}

	public static class TwistedTrunkFactory extends AbstractTrunkFactory {

		public TwistedTrunkFactory(
			TrunkThicknessScript.Holder thickness,
			boolean require_natural_ground,
			boolean can_generate_in_liquid
		) {
			super(thickness, require_natural_ground, can_generate_in_liquid);
		}

		@Override
		public TrunkConfig create(double startX, int startY, double startZ, int height, RandomGenerator random) {
			double speed = Permuter.nextUniformDouble(random);
			speed *= 12.0D - 4.0D * speed * speed;
			return new TwistedTrunkConfig(
				startX,
				startY,
				startZ,
				height,
				random.nextDouble(BigGlobeMath.TAU),
				speed,
				this.thickness,
				this.require_natural_ground,
				this.can_generate_in_liquid
			);
		}
	}
}