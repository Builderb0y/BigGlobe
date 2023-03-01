package builderb0y.bigglobe.trees;

import java.util.random.RandomGenerator;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.trees.trunks.SlantedTrunkConfig;
import builderb0y.bigglobe.trees.trunks.StraightTrunkConfig;
import builderb0y.bigglobe.trees.trunks.TrunkConfig;
import builderb0y.bigglobe.trees.trunks.TwistedTrunkConfig;

@UseCoder(name = "REGISTRY", usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface TrunkFactory extends CoderRegistryTyped {

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
		double radius,
		RandomGenerator random
	);

	public static abstract class AbstractTrunkFactory implements TrunkFactory {

		public final boolean require_natural_ground;
		public final boolean can_generate_in_liquid;

		public AbstractTrunkFactory(
			boolean require_natural_ground,
			boolean can_generate_in_liquid
		) {
			this.require_natural_ground = require_natural_ground;
			this.can_generate_in_liquid = can_generate_in_liquid;
		}
	}

	public static class StraightTrunkFactory extends AbstractTrunkFactory {

		public StraightTrunkFactory(
			boolean require_natural_ground,
			boolean can_generate_in_liquid
		) {
			super(require_natural_ground, can_generate_in_liquid);
		}

		@Override
		public TrunkConfig create(double startX, int startY, double startZ, int height, double radius, RandomGenerator random) {
			return new StraightTrunkConfig(
				startX,
				startY,
				startZ,
				height,
				radius,
				this.require_natural_ground,
				this.can_generate_in_liquid
			);
		}
	}

	public static class SlantedTrunkFactory extends AbstractTrunkFactory {

		public final RandomSource slant;

		public SlantedTrunkFactory(
			boolean require_natural_ground,
			boolean can_generate_in_liquid,
			RandomSource slant
		) {
			super(require_natural_ground, can_generate_in_liquid);
			this.slant = slant;
		}

		@Override
		public TrunkConfig create(double startX, int startY, double startZ, int height, double radius, RandomGenerator random) {
			double slant = this.slant.get(random);
			double angle = random.nextDouble(BigGlobeMath.TAU);
			return new SlantedTrunkConfig(
				startX,
				startY,
				startZ,
				height,
				radius,
				Math.cos(angle) * slant,
				Math.sin(angle) * slant,
				this.require_natural_ground,
				this.can_generate_in_liquid
			);
		}
	}

	public static class TwistedTrunkFactory extends AbstractTrunkFactory {

		public TwistedTrunkFactory(
			boolean require_natural_ground,
			boolean can_generate_in_liquid
		) {
			super(require_natural_ground, can_generate_in_liquid);
		}

		@Override
		public TrunkConfig create(double startX, int startY, double startZ, int height, double radius, RandomGenerator random) {
			double speed = Permuter.nextUniformDouble(random);
			speed *= 12.0D - 4.0D * speed * speed;
			return new TwistedTrunkConfig(
				startX,
				startY,
				startZ,
				height,
				radius,
				random.nextDouble(BigGlobeMath.TAU),
				speed,
				this.require_natural_ground,
				this.can_generate_in_liquid
			);
		}
	}
}