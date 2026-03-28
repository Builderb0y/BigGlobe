package builderb0y.bigglobe.noise;

import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

/**
using delegation instead of making Permuter implement both
interfaces because I don't trust the compiler to duplicate
methods which should be obfuscated due to inheriting from Random,
but also not obfuscated due to inheriting from RandomGenerator.
and also it just feels icky to have mojang methods in my Permuter class.
*/
public class MojangPermuter implements RandomSource {

	public final Permuter permuter;

	public MojangPermuter(Permuter permuter) {
		this.permuter = permuter;
	}

	public MojangPermuter(long seed) {
		this(new Permuter(seed));
	}

	public static MojangPermuter from(RandomGenerator random) {
		return random instanceof Permuter permuter ? new MojangPermuter(permuter) : new MojangPermuter(random.nextLong());
	}

	@Override
	public RandomSource fork() {
		return new MojangPermuter(this.nextLong());
	}

	@Override
	public PositionalRandomFactory forkPositional() {
		return new Splitter(this.nextLong());
	}

	public long getSeed() {
		return this.permuter.seed;
	}

	@Override
	public void setSeed(long seed) {
		this.permuter.setSeed(seed);
	}

	@Override
	public int nextInt() {
		return this.permuter.nextInt();
	}

	@Override
	public int nextInt(int bound) {
		return this.permuter.nextInt(bound);
	}

	@Override
	public long nextLong() {
		return this.permuter.nextLong();
	}

	@Override
	public boolean nextBoolean() {
		return this.permuter.nextBoolean();
	}

	@Override
	public float nextFloat() {
		return this.permuter.nextFloat();
	}

	@Override
	public double nextDouble() {
		return this.permuter.nextDouble();
	}

	@Override
	public double nextGaussian() {
		return this.permuter.nextGaussian();
	}

	@Override
	public void consumeCount(int count) {
		this.permuter.skip(count);
	}

	public static class Splitter implements PositionalRandomFactory {

		public final long seed;

		public Splitter(long seed) {
			this.seed = seed;
		}

		@Override
		public RandomSource fromSeed(long seed) {
			return new MojangPermuter(Permuter.permute(this.seed, seed));
		}

		@Override
		public RandomSource fromHashOf(String seed) {
			return new MojangPermuter(Permuter.permute(this.seed, seed));
		}

		@Override
		public RandomSource at(int x, int y, int z) {
			return new MojangPermuter(Permuter.permute(this.seed, x, y, z));
		}

		@Override
		public void parityConfigString(StringBuilder info) {
			info.append("MojangPermuter$Splitter: { seed: ").append(this.seed).append(" }");
		}

		@Override
		public RandomSource at(BlockPos pos) {
			return new MojangPermuter(Permuter.permute(this.seed, pos));
		}

		@Override
		public RandomSource fromHashOf(Identifier seed) {
			return new MojangPermuter(Permuter.permute(this.seed, seed));
		}
	}
}