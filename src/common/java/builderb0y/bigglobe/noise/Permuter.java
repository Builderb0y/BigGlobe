package builderb0y.bigglobe.noise;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.random.RandomGenerator;

import it.unimi.dsi.fastutil.HashCommon;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Range;

import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;

import builderb0y.scripting.bytecode.CastingSupport;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.util.InfoHolder;

/**
the primary intention of this class is to use the
static methods which take a seed as a parameter.
these methods allow you to generate pseudorandom values without
needing to allocate a new {@link RandomGenerator} instance.
however, since the seed is pass-by-value, it will
not be updated by calling these static methods.
therefore, the caller will need to provide a different
seed for every random value they wish to generate.
additional {@link #permute(long, int) permute}
methods are provided to create new seeds from an
existing seed and some magic number salt value.

if a sequence of pseudorandom values is desired,
the recommended way of generating them is to add {@link #PHI64}
to the seed every time a new pseudorandom value is generated.
for example:
float  firstValue = Permuter.nextPositiveFloat(seed += Permuter.PHI64);
float secondValue = Permuter.nextPositiveFloat(seed += Permuter.PHI64);

in case an actual {@link RandomGenerator} instance is needed,
this class implements that too, and the seed is updated as described above.
the period of this implementation is 2^64.
this implementation is NOT thread-safe, but it is faster than {@link Random}.
this implementation is NOT cryptographically secure.
if cryptographically secure random numbers are desired,
consider using {@link SecureRandom} instead.
*/
@SuppressWarnings("unused")
public class Permuter implements RandomGenerator {

	/**
	same as {@link HashCommon#LONG_PHI} and
	{@link jdk.internal.util.random.RandomSupport#GOLDEN_RATIO_64}.
	*/
	public static final long PHI64 = 0x9E3779B97F4A7C15L;

	public long seed;

	public Permuter(long seed) {
		this.seed = seed;
	}

	public static Permuter from(RandomSource random) {
		return random instanceof MojangPermuter mojangPermuter ? mojangPermuter.permuter : new Permuter(random.nextLong());
	}

	public static Permuter from(RandomGenerator random) {
		return random instanceof Permuter permuter ? permuter : new Permuter(random.nextLong());
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	public Permuter withSeed(long seed) {
		this.seed = seed;
		return this;
	}

	public MojangPermuter mojang() {
		return new MojangPermuter(this);
	}

	@Override
	public int nextInt() {
		return (int)(stafford(this.seed += PHI64));
	}

	@Override
	public long nextLong() {
		return stafford(this.seed += PHI64);
	}

	@Override
	public float nextFloat() {
		return toPositiveFloat(this.nextLong());
	}

	@Override
	public double nextDouble() {
		return toPositiveDouble(this.nextLong());
	}

	/**
	returns an int where the least significant (numberOfBits) bits are randomized,
	and the most significant (32 - numberOfBits) bits are all 0.
	if numberOfBits is 32, then this method is equivalent to {@link #nextInt()}.
	*/
	public int nextIntBits(@Range(from = 0, to = 32) int numberOfBits) {
		if (numberOfBits == 32) return this.nextInt();
		return this.nextInt() & ((1 << numberOfBits) - 1);
	}

	/**
	returns a long where the least significant (numberOfBits) bits are randomized,
	and the most significant (64 - numberOfBits) bits are all 0.
	if numberOfBits is 64, then this method is equivalent to {@link #nextLong()}.
	*/
	public long nextLongBits(@Range(from = 0, to = 64) int numberOfBits) {
		if (numberOfBits == 64) return this.nextLong();
		return this.nextLong() & ((1L << numberOfBits) - 1);
	}

	/**
	simulates calling {@link #nextInt()} (distance) times, but faster.
	*/
	public void skip(int distance) {
		this.seed += PHI64 * distance;
	}

	/**
	simulates calling {@link #nextInt()} (distance) times, but faster.
	*/
	public void skip(long distance) {
		this.seed += PHI64 * distance;
	}

	//////////////////////////////// Now entering static method territory! ////////////////////////////////
	//these methods can be used to get a single random
	//value without instantiating a new Permuter object.
	//however, these methods are NOT guaranteed to be consistent
	//with the values produced by similarly-named instance methods
	//on any implementation of RandomGenerator, INCLUDING Permuter!

	/**
	same as {@link jdk.internal.util.random.RandomSupport#mixStafford13(long)}.
	*/
	public static long stafford(long z) {
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	/**
	"permutes" the seed with no salt.
	in other words, does nothing.
	the seed is returned as-is.

	this method is equivalent to calling one of the
	{@link #permute(long, int...) vararg-based}
	permute methods with no extra arguments, or a 0-length array.
	this method is provided as a deprecated overload
	to indicate that the caller may have forgotten
	to provide the necessary salt for permutation.
	*/
	@Deprecated //you probably don't want this overload.
	public static long permute(
		long seed
	) {
		return seed;
	}

	/**
	permutes the seed using the given salt.
	the returned seed will depend on the current
	seed and the salt, and will be pseudo-random.
	*/
	public static long permute(
		long seed,
		int salt1
	) {
		return stafford(seed + salt1 * PHI64);
	}

	/**
	permutes the seed with 2 salt values.

	@see #permute(long, int)
	*/
	public static long permute(
		long seed,
		int salt1,
		int salt2
	) {
		seed = permute(seed, salt1);
		seed = permute(seed, salt2);
		return seed;
	}

	/**
	permutes the seed with 3 salt values.

	@see #permute(long, int)
	*/
	public static long permute(
		long seed,
		int salt1,
		int salt2,
		int salt3
	) {
		seed = permute(seed, salt1);
		seed = permute(seed, salt2);
		seed = permute(seed, salt3);
		return seed;
	}

	/**
	permutes the seed with 4 salt values.

	@see #permute(long, int)
	*/
	public static long permute(
		long seed,
		int salt1,
		int salt2,
		int salt3,
		int salt4
	) {
		seed = permute(seed, salt1);
		seed = permute(seed, salt2);
		seed = permute(seed, salt3);
		seed = permute(seed, salt4);
		return seed;
	}

	/**
	permutes the seed with 5 salt values.

	@see #permute(long, int)
	*/
	public static long permute(
		long seed,
		int salt1,
		int salt2,
		int salt3,
		int salt4,
		int salt5
	) {
		seed = permute(seed, salt1);
		seed = permute(seed, salt2);
		seed = permute(seed, salt3);
		seed = permute(seed, salt4);
		seed = permute(seed, salt5);
		return seed;
	}

	/**
	permutes the seed with 6 salt values.

	@see #permute(long, int)
	*/
	public static long permute(
		long seed,
		int salt1,
		int salt2,
		int salt3,
		int salt4,
		int salt5,
		int salt6
	) {
		seed = permute(seed, salt1);
		seed = permute(seed, salt2);
		seed = permute(seed, salt3);
		seed = permute(seed, salt4);
		seed = permute(seed, salt5);
		seed = permute(seed, salt6);
		return seed;
	}

	/**
	permutes the seed with 7 salt values.

	@see #permute(long, int)
	*/
	public static long permute(
		long seed,
		int salt1,
		int salt2,
		int salt3,
		int salt4,
		int salt5,
		int salt6,
		int salt7
	) {
		seed = permute(seed, salt1);
		seed = permute(seed, salt2);
		seed = permute(seed, salt3);
		seed = permute(seed, salt4);
		seed = permute(seed, salt5);
		seed = permute(seed, salt6);
		seed = permute(seed, salt7);
		return seed;
	}

	/**
	permutes the seed with 8 salt values.

	@see #permute(long, int)
	*/
	public static long permute(
		long seed,
		int salt1,
		int salt2,
		int salt3,
		int salt4,
		int salt5,
		int salt6,
		int salt7,
		int salt8
	) {
		seed = permute(seed, salt1);
		seed = permute(seed, salt2);
		seed = permute(seed, salt3);
		seed = permute(seed, salt4);
		seed = permute(seed, salt5);
		seed = permute(seed, salt6);
		seed = permute(seed, salt7);
		seed = permute(seed, salt8);
		return seed;
	}

	/**
	permutes the seed with many salt values.
	every element in the provided vararg array is
	used as a salt value to permute the seed with.
	if the provided vararg array is empty,
	then the seed will be returned as-is.

	@see #permute(long, int)
	*/
	public static long permute(long seed, byte... salts) {
		for (byte salt : salts) seed = permute(seed, salt);
		return seed;
	}

	/**
	permutes the seed with many salt values.
	every element in the provided vararg array is
	used as a salt value to permute the seed with.
	if the provided vararg array is empty,
	then the seed will be returned as-is.

	@see #permute(long, int)
	*/
	public static long permute(long seed, short... salts) {
		for (short salt : salts) seed = permute(seed, salt);
		return seed;
	}

	/**
	permutes the seed with many salt values.
	every element in the provided vararg array is
	used as a salt value to permute the seed with.
	if the provided vararg array is empty,
	then the seed will be returned as-is.

	@see #permute(long, int)
	*/
	public static long permute(long seed, int... salts) {
		for (int salt : salts) seed = permute(seed, salt);
		return seed;
	}

	/**
	permutes the seed with many salt values.
	every element in the provided vararg array is
	used as a salt value to permute the seed with.
	if the provided vararg array is empty,
	then the seed will be returned as-is.

	@see #permute(long, int)
	*/
	public static long permute(long seed, char... salts) {
		for (char salt : salts) seed = permute(seed, salt);
		return seed;
	}

	/**
	permutes the seed with a CharSequence.
	every character in the provided CharSequence is
	used as a salt value to permute the seed with.
	if the provided CharSequence has a
	{@link CharSequence#length() length} of 0,
	then the seed will be returned as-is.

	@see #permute(long, int)
	*/
	public static long permute(long seed, CharSequence saltSequence) {
		for (int index = 0, length = saltSequence.length(); index < length; index++) {
			seed = permute(seed, saltSequence.charAt(index));
		}
		return seed;
	}

	/**
	permutes the seed with an Identifier.
	first, the seed is permuted with the identifier's
	{@link Identifier#getNamespace() namespace}
	via {@link #permute(long, CharSequence)}.
	next, the seed is permuted with the literal
	char value ':' via {@link #permute(long, int)}.
	lastly, the seed is permuted with the identifier's
	{@link Identifier#getPath() path}, again using
	{@link #permute(long, CharSequence)}.
	the reason for permuting with ':' is to ensure that
	permute(seed, identifier) == permute(seed, identifier.toString()).
	note however that the reverse may not be true. in other words,
	permute(seed, string) may not be equal to permute(seed, new Identifier(string)),
	even when the string encodes a valid Identifier.
	in particular, these two will not be equal
	when the string does not contain a namespace,
	as the default namespace "minecraft" is used in this case.
	*/
	public static long permute(long seed, Identifier identifier) {
		seed = permute(seed, identifier.getNamespace());
		seed = permute(seed, ':');
		seed = permute(seed, identifier.getPath());
		return seed;
	}

	/**
	permutes the seed with the vector's {@link Vec3i#getX() x},
	{@link Vec3i#getY() y}, and {@link Vec3i#getZ() z} values.

	@see #permute(long, int)
	*/
	public static long permute(long seed, Vec3i vector) {
		return permute(seed, vector.getX(), vector.getY(), vector.getZ());
	}

	/**
	permutes the seed with the position's {@link ColumnPos#x() x}
	and {@link ColumnPos#z() z} values.

	@see #permute(long, int)
	*/
	public static long permute(long seed, ColumnPos pos) {
		return permute(seed, pos.x(), pos.z());
	}

	/**
	permutes the seed with the position's {@link ChunkPos#x() x}
	and {@link ChunkPos#z() z} values.

	@see #permute(long, int)
	*/
	public static long permute(long seed, ChunkPos chunkPos) {
		return permute(seed, chunkPos.x(), chunkPos.z());
	}

	public static long permute(long seed, long salt1) {
		seed = stafford(seed + salt1);
		return seed;
	}

	public static long permute(long seed, long salt1, long salt2) {
		seed = stafford(seed + salt1);
		seed = stafford(seed + salt2);
		return seed;
	}

	public static long permute(long seed, long salt1, long salt2, long salt3) {
		seed = stafford(seed + salt1);
		seed = stafford(seed + salt2);
		seed = stafford(seed + salt3);
		return seed;
	}

	public static long permute(long seed, long salt1, long salt2, long salt3, long salt4) {
		seed = stafford(seed + salt1);
		seed = stafford(seed + salt2);
		seed = stafford(seed + salt3);
		seed = stafford(seed + salt4);
		return seed;
	}

	public static long permute(long seed, UUID uuid) {
		return permute(seed, uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
	}

	public static long permute(long seed, double salt1) {
		return permute(seed, Double.doubleToRawLongBits(salt1));
	}

	public static long permute(long seed, double salt1, double salt2) {
		return permute(
			seed,
			Double.doubleToRawLongBits(salt1),
			Double.doubleToRawLongBits(salt2)
		);
	}

	public static long permute(long seed, double salt1, double salt2, double salt3) {
		return permute(
			seed,
			Double.doubleToRawLongBits(salt1),
			Double.doubleToRawLongBits(salt2),
			Double.doubleToRawLongBits(salt3)
		);
	}

	public static long permute(long seed, double salt1, double salt2, double salt3, double salt4) {
		return permute(
			seed,
			Double.doubleToRawLongBits(salt1),
			Double.doubleToRawLongBits(salt2),
			Double.doubleToRawLongBits(salt3),
			Double.doubleToRawLongBits(salt4)
		);
	}

	/**
	returns a pseudorandom int between {@link Integer#MIN_VALUE} and
	{@link Integer#MAX_VALUE} (both inclusive) based on the given seed.
	*/
	public static int nextUniformInt(long seed) {
		return (int)(stafford(seed));
	}

	public static int toUniformInt(long permutedSeed) {
		return (int)(permutedSeed);
	}

	/**
	returns a pseudorandom int between 0 and {@link Integer#MAX_VALUE}
	(both inclusive) based on the given seed.
	*/
	public static int nextPositiveInt(long seed) {
		return nextUniformInt(seed) & 0x7FFF_FFFF;
	}

	public static int toPositiveInt(long permutedSeed) {
		return ((int)(permutedSeed)) & 0x7FFF_FFFF;
	}

	/**
	returns a pseudorandom int between 0 (inclusive)
	and max (exclusive) based on the given seed.

	@throws IllegalArgumentException if max is less than or equal to 0.
	*/
	public static int nextBoundedInt(long seed, int max) {
		if (max <= 0) throw new IllegalArgumentException("bound must be positive.");

		int mask = max - 1;
		if ((max & mask) == 0) {
			return nextUniformInt(seed) & mask;
		}
		else {
			while (true) {
				int bits = nextPositiveInt(seed += PHI64);
				int result = bits % max;
				if (bits - result + mask >= 0) return result;
			}
		}
	}

	/**
	returns a pseudorandom int between min (inclusive)
	and max (exclusive) based on the given seed.

	@throws IllegalArgumentException if max is less than or equal to min.
	*/
	public static int nextBoundedInt(long seed, int min, int max) {
		if (max <= min) throw new IllegalArgumentException("max must be greater than min");
		int bound = max - min;
		if (bound > 0) {
			return nextBoundedInt(seed, bound) + min;
		}
		else {
			while (true) {
				int result = nextUniformInt(seed += PHI64);
				if (result >= min && result < max) return result;
			}
		}
	}

	/**
	returns a pseudorandom long between {@link Long#MIN_VALUE} and
	{@link Long#MAX_VALUE} (both inclusive) based on the given seed.
	*/
	public static long nextUniformLong(long seed) {
		return stafford(seed);
	}

	public static long toUniformLong(long seed) {
		return seed;
	}

	/**
	returns a pseudorandom long between 0 and {@link Long#MAX_VALUE}
	(both inclusive) based on the given seed.
	*/
	public static long nextPositiveLong(long seed) {
		return nextUniformLong(seed) & 0x7FFF_FFFF_FFFF_FFFFL;
	}

	public static long toPositiveLong(long seed) {
		return seed & 0x7FFF_FFFF_FFFF_FFFFL;
	}

	/**
	returns a pseudorandom long between 0 (inclusive)
	and max (exclusive) based on the given seed.

	@throws IllegalArgumentException if max is less than or equal to 0.
	*/
	public static long nextBoundedLong(long seed, long max) {
		if (max <= 0L) throw new IllegalArgumentException("bound must be positive.");
		if (max <= Integer.MAX_VALUE) return (long)(nextBoundedInt(seed, (int)(max)));
		long mask = max - 1;
		if ((max & mask) == 0L) {
			return nextUniformLong(seed) & mask;
		}
		else {
			while (true) {
				long bits = nextPositiveLong(seed += PHI64);
				long result = bits % max;
				if (bits - result + mask >= 0) return result;
			}
		}
	}

	/**
	returns a pseudorandom long between min (inclusive)
	and max (exclusive) based on the given seed.

	@throws IllegalArgumentException if max is less than or equal to min.
	*/
	public static long nextBoundedLong(long seed, long min, long max) {
		if (max <= min) throw new IllegalArgumentException("max must be greater than min");
		long bound = max - min;
		if (bound > 0) {
			return nextBoundedLong(seed, bound) + min;
		}
		else {
			while (true) {
				long result = nextUniformLong(seed += PHI64);
				if (result >= min && result < max) return result;
			}
		}
	}

	/**
	returns a pseudorandom float between 0.0 (inclusive)
	and 1.0 (exclusive) based on the given seed.
	*/
	public static float nextPositiveFloat(long seed) {
		return (nextUniformLong(seed) >>> (64 - 24)) * 0x1.0p-24F;
	}

	public static float toPositiveFloat(long permutedSeed) {
		return (permutedSeed >>> (64 - 24)) * 0x1.0p-24F;
	}

	/**
	returns a pseudorandom float between -1.0 (inclusive)
	and +1.0 (exclusive) based on the given seed.
	*/
	public static float nextUniformFloat(long seed) {
		return (nextUniformLong(seed) >> (64 - 25)) * 0x1.0p-24F;
	}

	public static float toUniformFloat(long permutedSeed) {
		return (permutedSeed >> (64 - 25)) * 0x1.0p-24F;
	}

	public static float nextUniformFloat(RandomGenerator random) {
		return (random.nextLong() >> (64 - 25)) * 0x1.0p-24F;
	}

	/**
	returns a pseudorandom float between 0.0 (inclusive)
	and max (exclusive) based on the given seed.

	@throws IllegalArgumentException if:
	max is {@link Float#POSITIVE_INFINITY},
	max {@link Float#isNaN() is NaN},
	or max is less than or equal to 0.0.
	*/
	public static float nextBoundedFloat(long seed, float max) {
		if (!(max > 0.0F && max < Float.POSITIVE_INFINITY)) {
			throw new IllegalArgumentException("bound must be positive and finite.");
		}
		return nextPositiveFloat(seed) * max;
	}

	public static float toBoundedFloat(long seed, float max) {
		if (!(max > 0.0F && max < Float.POSITIVE_INFINITY)) {
			throw new IllegalArgumentException("bound must be positive and finite.");
		}
		return toPositiveFloat(seed) * max;
	}

	/**
	returns a pseudorandom float between min (inclusive)
	and max (exclusive) based on the given seed.

	@throws IllegalArgumentException if:
	min is {@link Float#NEGATIVE_INFINITY},
	max is {@link Float#POSITIVE_INFINITY},
	min {@link Float#isNaN() is NaN},
	max {@link Float#isNaN() is NaN},
	or max is less than or equal to min.
	*/
	public static float nextBoundedFloat(long seed, float min, float max) {
		return nextBoundedFloat(seed, max - min) + min;
	}

	public static float toBoundedFloat(long seed, float min, float max) {
		return toBoundedFloat(seed, max - min) + min;
	}

	/**
	returns a pseudorandom double between 0.0 (inclusive)
	and 1.0 (exclusive) based on the given seed.
	*/
	public static double nextPositiveDouble(long seed) {
		return (nextUniformLong(seed) >>> (64 - 53)) * 0x1.0p-53D;
	}

	public static double toPositiveDouble(long permutedSeed) {
		return (permutedSeed >>> (64 - 53)) * 0x1.0p-53D;
	}

	/**
	returns a pseudorandom double between -1.0 (inclusive)
	and +1.0 (exclusive) based on the given seed.
	*/
	public static double nextUniformDouble(long seed) {
		return (nextUniformLong(seed) >> (64 - 54)) * 0x1.0p-53D;
	}

	public static double toUniformDouble(long permutedSeed) {
		return (permutedSeed >> (64 - 54)) * 0x1.0p-53D;
	}

	public static double nextUniformDouble(RandomGenerator random) {
		return (random.nextLong() >> (64 - 54)) * 0x1.0p-53D;
	}

	/**
	returns a pseudorandom double between 0.0 (inclusive)
	and max (exclusive) based on the given seed.

	@throws IllegalArgumentException if:
	max is {@link Double#POSITIVE_INFINITY},
	max {@link Double#isNaN() is NaN},
	or max is less than or equal to 0.0.
	*/
	public static double nextBoundedDouble(long seed, double max) {
		if (!(max > 0.0D && max < Double.POSITIVE_INFINITY)) {
			throw new IllegalArgumentException("bound must be positive and finite.");
		}
		return nextPositiveDouble(seed) * max;
	}

	public static double toBoundedDouble(long seed, double max) {
		if (!(max > 0.0D && max < Double.POSITIVE_INFINITY)) {
			throw new IllegalArgumentException("bound must be positive and finite.");
		}
		return toPositiveDouble(seed) * max;
	}

	/**
	returns a pseudorandom double between min (inclusive)
	and max (exclusive) based on the given seed.

	@throws IllegalArgumentException if:
	min is {@link Double#NEGATIVE_INFINITY},
	max is {@link Double#POSITIVE_INFINITY},
	min {@link Double#isNaN() is NaN},
	max {@link Double#isNaN() is NaN},
	or max is less than or equal to min.
	*/
	public static double nextBoundedDouble(long seed, double min, double max) {
		return nextBoundedDouble(seed, max - min) + min;
	}

	public static double toBoundedDouble(long seed, double min, double max) {
		return toBoundedDouble(seed, max - min) + min;
	}

	/**
	returns a pseudorandom boolean with a
	50% chance of being true and a 50% chance
	of being false based on the given seed.
	*/
	public static boolean nextBoolean(long seed) {
		return nextUniformInt(seed) < 0;
	}

	public static boolean toBoolean(long permutedSeed) {
		return toUniformInt(permutedSeed) < 0;
	}

	/**
	returns a pseudorandom boolean with a (chance)
	chance of being true and a (1.0 - chance)
	chance of being false based on the given seed.
	*/
	public static boolean nextChancedBoolean(long seed, float chance) {
		return chance > 0.0F && (chance >= 1.0F || nextPositiveFloat(seed) < chance);
	}

	public static boolean toChancedBoolean(long permutedSeed, float chance) {
		return chance > 0.0F && (chance >= 1.0F || toPositiveFloat(permutedSeed) < chance);
	}

	public static boolean nextChancedBoolean(RandomGenerator random, float chance) {
		return chance > 0.0F && (chance >= 1.0F || random.nextFloat() < chance);
	}

	/**
	returns a pseudorandom boolean with a (chance)
	chance of being true and a (1.0 - chance)
	chance of being false based on the given seed.
	*/
	public static boolean nextChancedBoolean(long seed, double chance) {
		return chance > 0.0D && (chance >= 1.0D || nextPositiveDouble(seed) < chance);
	}

	public static boolean toChancedBoolean(long permutedSeed, double chance) {
		return chance > 0.0D && (chance >= 1.0D || toPositiveDouble(permutedSeed) < chance);
	}

	public static boolean nextChancedBoolean(RandomGenerator random, double chance) {
		return chance > 0.0D && (chance >= 1.0D || random.nextDouble() < chance);
	}

	/**
	rounds the number either up or down, randomly, based on the given seed.
	the number is more likely to round up the closer it is to ceil(number).
	likewise, the number is more likely to round down the closer it is to floor(number).
	for example, if the number is 1.25, then it has a 25% chance of rounding up to 2,
	and a 75% chance of rounding down to 1. the rounded value is returned as an int.
	if the number is already an integer, it will be cast to an int and returned as-is.
	in other words, the number 3.0 has a 100% chance of rounding to 3,
	and a 0% chance of rounding to 2 or 4.
	*/
	public static int roundRandomlyI(long seed, float number) {
		return CastingSupport.floorInt(number + nextPositiveFloat(seed));
	}

	public static int roundRandomlyI(RandomGenerator random, float number) {
		return CastingSupport.floorInt(number + random.nextFloat());
	}

	/**
	rounds the number either up or down, randomly, based on the given seed.
	the number is more likely to round up the closer it is to ceil(number).
	likewise, the number is more likely to round down the closer it is to floor(number).
	for example, if the number is 1.25, then it has a 25% chance of rounding up to 2,
	and a 75% chance of rounding down to 1. the rounded value is returned as an int.
	if the number is already an integer, it will be cast to an int and returned as-is.
	in other words, the number 3.0 has a 100% chance of rounding to 3,
	and a 0% chance of rounding to 2 or 4.
	*/
	public static int roundRandomlyI(long seed, double number) {
		return CastingSupport.floorInt(number + nextPositiveDouble(seed));
	}

	public static int roundRandomlyI(RandomGenerator random, double number) {
		return CastingSupport.floorInt(number + random.nextDouble());
	}

	/**
	rounds the number either up or down, randomly, based on the given seed.
	the number is more likely to round up the closer it is to ceil(number).
	likewise, the number is more likely to round down the closer it is to floor(number).
	for example, if the number is 1.25, then it has a 25% chance of rounding up to 2,
	and a 75% chance of rounding down to 1. the rounded value is returned as a long.
	if the number is already an integer, it will be cast to a long and returned as-is.
	in other words, the number 3.0 has a 100% chance of rounding to 3,
	and a 0% chance of rounding to 2 or 4.
	*/
	public static long roundRandomlyL(long seed, float number) {
		return CastingSupport.floorLong(number + nextPositiveFloat(seed));
	}

	public static long roundRandomlyL(RandomGenerator random, float number) {
		return CastingSupport.floorLong(number + random.nextFloat());
	}

	/**
	rounds the number either up or down, randomly, based on the given seed.
	the number is more likely to round up the closer it is to ceil(number).
	likewise, the number is more likely to round down the closer it is to floor(number).
	for example, if the number is 1.25, then it has a 25% chance of rounding up to 2,
	and a 75% chance of rounding down to 1. the rounded value is returned as a long.
	if the number is already an integer, it will be cast to a long and returned as-is.
	in other words, the number 3.0 has a 100% chance of rounding to 3,
	and a 0% chance of rounding to 2 or 4.
	*/
	public static long roundRandomlyL(long seed, double number) {
		return CastingSupport.floorLong(number + nextPositiveDouble(seed));
	}

	public static long roundRandomlyL(RandomGenerator random, double number) {
		return CastingSupport.floorLong(number + random.nextDouble());
	}

	/**
	returns a random element from the provided array, based on the provided seed.
	all elements of the array are equally likely to be selected.
	*/
	public static <T> T choose(long seed, T[] values) {
		return values[nextBoundedInt(seed, values.length)];
	}

	public static <T> T choose(RandomGenerator random, T[] values) {
		return values[random.nextInt(values.length)];
	}

	/**
	returns a random element from the provided list, based on the provided seed.
	all elements of the list are equally likely to be selected.
	*/
	public static <T> T choose(long seed, List<T> values) {
		return values.get(nextBoundedInt(seed, values.size()));
	}

	public static <T> T choose(RandomGenerator random, List<T> values) {
		return values.get(random.nextInt(values.size()));
	}

	public static final BetweenInfo BETWEEN_INFO = new BetweenInfo();
	public static class BetweenInfo extends InfoHolder {

		public static final int
			FLAG_MIN_INCLUSIVE   = 1 << 0,
			FLAG_MAX_INCLUSIVE   = 1 << 1,
			FLAG_SEED_RECEIVER   = 0 << 2,
			FLAG_RANDOM_RECEIVER = 1 << 2,
			FLAG_INT_DESIRED     = 0 << 3,
			FLAG_LONG_DESIRED    = 1 << 3,
			FLAG_FLOAT_DESIRED   = 2 << 3,
			FLAG_DOUBLE_DESIRED  = 3 << 3;

		public MethodInfo
			betweenSeedExclusiveExclusiveInt,
			betweenSeedInclusiveExclusiveInt,
			betweenSeedExclusiveInclusiveInt,
			betweenSeedInclusiveInclusiveInt,
			betweenRandomExclusiveExclusiveInt,
			betweenRandomInclusiveExclusiveInt,
			betweenRandomExclusiveInclusiveInt,
			betweenRandomInclusiveInclusiveInt,
			betweenSeedExclusiveExclusiveLong,
			betweenSeedInclusiveExclusiveLong,
			betweenSeedExclusiveInclusiveLong,
			betweenSeedInclusiveInclusiveLong,
			betweenRandomExclusiveExclusiveLong,
			betweenRandomInclusiveExclusiveLong,
			betweenRandomExclusiveInclusiveLong,
			betweenRandomInclusiveInclusiveLong,
			betweenSeedExclusiveExclusiveFloat,
			betweenSeedInclusiveExclusiveFloat,
			betweenSeedExclusiveInclusiveFloat,
			betweenSeedInclusiveInclusiveFloat,
			betweenRandomExclusiveExclusiveFloat,
			betweenRandomInclusiveExclusiveFloat,
			betweenRandomExclusiveInclusiveFloat,
			betweenRandomInclusiveInclusiveFloat,
			betweenSeedExclusiveExclusiveDouble,
			betweenSeedInclusiveExclusiveDouble,
			betweenSeedExclusiveInclusiveDouble,
			betweenSeedInclusiveInclusiveDouble,
			betweenRandomExclusiveExclusiveDouble,
			betweenRandomInclusiveExclusiveDouble,
			betweenRandomExclusiveInclusiveDouble,
			betweenRandomInclusiveInclusiveDouble;

		public MethodInfo getMethodFor(@MagicConstant(flagsFromClass = BetweenInfo.class) int flags) {
			//rust can't do this hehe
			return switch (flags) {
				case FLAG_INT_DESIRED                                                                     -> this.betweenSeedExclusiveExclusiveInt;
				case FLAG_INT_DESIRED                                                | FLAG_MIN_INCLUSIVE -> this.betweenSeedInclusiveExclusiveInt;
				case FLAG_INT_DESIRED                           | FLAG_MAX_INCLUSIVE                      -> this.betweenSeedExclusiveInclusiveInt;
				case FLAG_INT_DESIRED                           | FLAG_MAX_INCLUSIVE | FLAG_MIN_INCLUSIVE -> this.betweenSeedInclusiveInclusiveInt;
				case FLAG_INT_DESIRED    | FLAG_RANDOM_RECEIVER                                           -> this.betweenRandomExclusiveExclusiveInt;
				case FLAG_INT_DESIRED    | FLAG_RANDOM_RECEIVER                      | FLAG_MIN_INCLUSIVE -> this.betweenRandomInclusiveExclusiveInt;
				case FLAG_INT_DESIRED    | FLAG_RANDOM_RECEIVER | FLAG_MAX_INCLUSIVE                      -> this.betweenRandomExclusiveInclusiveInt;
				case FLAG_INT_DESIRED    | FLAG_RANDOM_RECEIVER | FLAG_MAX_INCLUSIVE | FLAG_MIN_INCLUSIVE -> this.betweenRandomInclusiveInclusiveInt;
				case FLAG_LONG_DESIRED                                                                    -> this.betweenSeedExclusiveExclusiveLong;
				case FLAG_LONG_DESIRED                                               | FLAG_MIN_INCLUSIVE -> this.betweenSeedInclusiveExclusiveLong;
				case FLAG_LONG_DESIRED                          | FLAG_MAX_INCLUSIVE                      -> this.betweenSeedExclusiveInclusiveLong;
				case FLAG_LONG_DESIRED                          | FLAG_MAX_INCLUSIVE | FLAG_MIN_INCLUSIVE -> this.betweenSeedInclusiveInclusiveLong;
				case FLAG_LONG_DESIRED   | FLAG_RANDOM_RECEIVER                                           -> this.betweenRandomExclusiveExclusiveLong;
				case FLAG_LONG_DESIRED   | FLAG_RANDOM_RECEIVER                      | FLAG_MIN_INCLUSIVE -> this.betweenRandomInclusiveExclusiveLong;
				case FLAG_LONG_DESIRED   | FLAG_RANDOM_RECEIVER | FLAG_MAX_INCLUSIVE                      -> this.betweenRandomExclusiveInclusiveLong;
				case FLAG_LONG_DESIRED   | FLAG_RANDOM_RECEIVER | FLAG_MAX_INCLUSIVE | FLAG_MIN_INCLUSIVE -> this.betweenRandomInclusiveInclusiveLong;
				case FLAG_FLOAT_DESIRED                                                                   -> this.betweenSeedExclusiveExclusiveFloat;
				case FLAG_FLOAT_DESIRED                                              | FLAG_MIN_INCLUSIVE -> this.betweenSeedInclusiveExclusiveFloat;
				case FLAG_FLOAT_DESIRED                         | FLAG_MAX_INCLUSIVE                      -> this.betweenSeedExclusiveInclusiveFloat;
				case FLAG_FLOAT_DESIRED                         | FLAG_MAX_INCLUSIVE | FLAG_MIN_INCLUSIVE -> this.betweenSeedInclusiveInclusiveFloat;
				case FLAG_FLOAT_DESIRED  | FLAG_RANDOM_RECEIVER                                           -> this.betweenRandomExclusiveExclusiveFloat;
				case FLAG_FLOAT_DESIRED  | FLAG_RANDOM_RECEIVER                      | FLAG_MIN_INCLUSIVE -> this.betweenRandomInclusiveExclusiveFloat;
				case FLAG_FLOAT_DESIRED  | FLAG_RANDOM_RECEIVER | FLAG_MAX_INCLUSIVE                      -> this.betweenRandomExclusiveInclusiveFloat;
				case FLAG_FLOAT_DESIRED  | FLAG_RANDOM_RECEIVER | FLAG_MAX_INCLUSIVE | FLAG_MIN_INCLUSIVE -> this.betweenRandomInclusiveInclusiveFloat;
				case FLAG_DOUBLE_DESIRED                                                                  -> this.betweenSeedExclusiveExclusiveDouble;
				case FLAG_DOUBLE_DESIRED                                             | FLAG_MIN_INCLUSIVE -> this.betweenSeedInclusiveExclusiveDouble;
				case FLAG_DOUBLE_DESIRED                        | FLAG_MAX_INCLUSIVE                      -> this.betweenSeedExclusiveInclusiveDouble;
				case FLAG_DOUBLE_DESIRED                        | FLAG_MAX_INCLUSIVE | FLAG_MIN_INCLUSIVE -> this.betweenSeedInclusiveInclusiveDouble;
				case FLAG_DOUBLE_DESIRED | FLAG_RANDOM_RECEIVER                                           -> this.betweenRandomExclusiveExclusiveDouble;
				case FLAG_DOUBLE_DESIRED | FLAG_RANDOM_RECEIVER                      | FLAG_MIN_INCLUSIVE -> this.betweenRandomInclusiveExclusiveDouble;
				case FLAG_DOUBLE_DESIRED | FLAG_RANDOM_RECEIVER | FLAG_MAX_INCLUSIVE                      -> this.betweenRandomExclusiveInclusiveDouble;
				case FLAG_DOUBLE_DESIRED | FLAG_RANDOM_RECEIVER | FLAG_MAX_INCLUSIVE | FLAG_MIN_INCLUSIVE -> this.betweenRandomInclusiveInclusiveDouble;
				default -> throw new IllegalArgumentException(Integer.toBinaryString(flags));
			};
		}
	}

	public static int betweenRandomInclusiveExclusiveInt(RandomGenerator random, int min, int max) {
		if (max <= min) throw new IllegalArgumentException("empty interval: [" + min + ", " + max + ")");
		return random.nextInt(min, max);
	}

	public static int betweenRandomInclusiveInclusiveInt(RandomGenerator random, int min, int max) {
		if (max < min) throw new IllegalArgumentException("empty interval: [" + min + ", " + max + "]");
		int bound = max - min + 1;
		if (bound > 0) {
			return random.nextInt(bound) + min;
		}
		else {
			while (true) {
				int result = random.nextInt();
				if (result >= min && result <= max) return result;
			}
		}
	}

	public static int betweenRandomExclusiveExclusiveInt(RandomGenerator random, int min, int max) {
		if (min == Integer.MAX_VALUE || max <= ++min) throw new IllegalArgumentException("empty interval: (" + min + ", " + max + ")");
		int bound = max - min;
		if (bound > 0) {
			return random.nextInt(bound) + min;
		}
		else {
			while (true) {
				int result = random.nextInt();
				if (result > min && result < max) return result;
			}
		}
	}

	public static int betweenRandomExclusiveInclusiveInt(RandomGenerator random, int min, int max) {
		if (max <= min) throw new IllegalArgumentException("empty interval: (" + min + ", " + max + "]");
		int bound = max - min;
		if (bound > 0) {
			return random.nextInt(bound) + min + 1;
		}
		else {
			while (true) {
				int result = random.nextInt();
				if (result > min && result <= max) return result;
			}
		}
	}

	public static int betweenSeedInclusiveExclusiveInt(long seed, int min, int max) {
		if (max <= min) throw new IllegalArgumentException("empty interval: [" + min + ", " + max + ")");
		return nextBoundedInt(seed, min, max);
	}

	public static int betweenSeedInclusiveInclusiveInt(long seed, int min, int max) {
		if (max < min) throw new IllegalArgumentException("empty interval: [" + min + ", " + max + "]");
		int bound = max - min + 1;
		if (bound > 0) {
			return nextBoundedInt(seed, bound) + min;
		}
		else {
			while (true) {
				int result = nextUniformInt(seed += PHI64);
				if (result >= min && result <= max) return result;
			}
		}
	}

	public static int betweenSeedExclusiveExclusiveInt(long seed, int min, int max) {
		if (min == Integer.MAX_VALUE || max <= ++min) throw new IllegalArgumentException("empty interval: (" + min + ", " + max + ")");
		int bound = max - min;
		if (bound > 0) {
			return nextBoundedInt(seed, bound) + min;
		}
		else {
			while (true) {
				int result = nextUniformInt(seed);
				if (result > min && result < max) return result;
			}
		}
	}

	public static int betweenSeedExclusiveInclusiveInt(long seed, int min, int max) {
		if (max <= min) throw new IllegalArgumentException("empty interval: (" + min + ", " + max + "]");
		int bound = max - min;
		if (bound > 0) {
			return nextBoundedInt(seed, bound) + min + 1;
		}
		else {
			while (true) {
				int result = nextUniformInt(seed);
				if (result > min && result <= max) return result;
			}
		}
	}

	public static long betweenRandomInclusiveExclusiveLong(RandomGenerator random, long min, long max) {
		if (max <= min) throw new IllegalArgumentException("empty interval: [" + min + ", " + max + ")");
		return random.nextLong(min, max);
	}

	public static long betweenRandomInclusiveInclusiveLong(RandomGenerator random, long min, long max) {
		if (max < min) throw new IllegalArgumentException("empty interval: [" + min + ", " + max + "]");
		long bound = max - min + 1L;
		if (bound > 0L) {
			return random.nextLong(bound) + min;
		}
		else {
			while (true) {
				long result = random.nextLong();
				if (result >= min && result <= max) return result;
			}
		}
	}

	public static long betweenRandomExclusiveExclusiveLong(RandomGenerator random, long min, long max) {
		if (min == Long.MAX_VALUE || max <= ++min) throw new IllegalArgumentException("empty interval: (" + min + ", " + max + ")");
		long bound = max - min;
		if (bound > 0L) {
			return random.nextLong(bound) + min;
		}
		else {
			while (true) {
				long result = random.nextLong();
				if (result > min && result < max) return result;
			}
		}
	}

	public static long betweenRandomExclusiveInclusiveLong(RandomGenerator random, long min, long max) {
		if (max <= min) throw new IllegalArgumentException("empty interval: (" + min + ", " + max + "]");
		long bound = max - min;
		if (bound > 0L) {
			return random.nextLong(bound) + min + 1L;
		}
		else {
			while (true) {
				long result = random.nextLong();
				if (result > min && result <= max) return result;
			}
		}
	}

	public static long betweenSeedInclusiveExclusiveLong(long seed, long min, long max) {
		if (max <= min) throw new IllegalArgumentException("empty interval: [" + min + ", " + max + ")");
		return nextBoundedLong(seed, min, max);
	}

	public static long betweenSeedInclusiveInclusiveLong(long seed, long min, long max) {
		if (max < min) throw new IllegalArgumentException("empty interval: [" + min + ", " + max + "]");
		long bound = max - min + 1L;
		if (bound > 0L) {
			return nextBoundedLong(seed, bound) + min;
		}
		else {
			while (true) {
				long result = nextUniformLong(seed += PHI64);
				if (result >= min && result <= max) return result;
			}
		}
	}

	public static long betweenSeedExclusiveExclusiveLong(long seed, long min, long max) {
		if (min == Long.MAX_VALUE || max <= ++min) throw new IllegalArgumentException("empty interval: (" + min + ", " + max + ")");
		long bound = max - min;
		if (bound > 0L) {
			return nextBoundedLong(seed, bound) + min;
		}
		else {
			while (true) {
				long result = nextUniformLong(seed += PHI64);
				if (result > min && result < max) return result;
			}
		}
	}

	public static long betweenSeedExclusiveInclusiveLong(long seed, long min, long max) {
		if (max <= min) throw new IllegalArgumentException("empty interval: (" + min + ", " + max + "]");
		long bound = max - min;
		if (bound > 0L) {
			return nextBoundedLong(seed, bound) + min + 1L;
		}
		else {
			while (true) {
				long result = nextUniformLong(seed);
				if (result > min && result <= max) return result;
			}
		}
	}

	public static float scaleBetween(float value, float min, float max) {
		float range = max - min;
		if (range < Float.POSITIVE_INFINITY) {
			value = value * range + min;
		}
		else {
			//if min is sufficiently close to -Float.MAX_VALUE
			//and max is sufficiently close to +Float.MAX_VALUE,
			//then the difference between them would overflow to infinity.
			float halfMin = min * 0.5F;
			float halfMax = max * 0.5F;
			value = (value * (halfMax - halfMin) + halfMin) * 2.0F;
		}
		return value;
	}

	public static float nextInclusiveFloat(RandomGenerator random, float min, float max) {
		return scaleBetween(random.nextInt((1 << 24) + 1) * 0x1.0p-24F, min, max);
	}

	public static float nextInclusiveFloat(long seed, float min, float max) {
		return scaleBetween(nextBoundedInt(seed, (1 << 24) + 1) * 0x1.0p-24F, min, max);
	}

	public static float betweenRandomInclusiveInclusiveFloat(RandomGenerator random, float min, float max) {
		if (!(max >= min)) throw new IllegalArgumentException("Empty interval: [" + min + ", " + max + "]");
		return nextInclusiveFloat(random, min, max);
	}

	public static float betweenRandomInclusiveExclusiveFloat(RandomGenerator random, float min, float max) {
		float max2 = Math.nextDown(max);
		if (!(max2 >= min)) throw new IllegalArgumentException("Empty interval: [" + min + ", " + max + ")");
		return nextInclusiveFloat(random, min, max2);
	}

	public static float betweenRandomExclusiveInclusiveFloat(RandomGenerator random, float min, float max) {
		float min2 = Math.nextUp(min);
		if (!(max >= min2)) throw new IllegalArgumentException("Empty interval: (" + min + ", " + max + "]");
		return nextInclusiveFloat(random, min2, max);
	}

	public static float betweenRandomExclusiveExclusiveFloat(RandomGenerator random, float min, float max) {
		float min2 = Math.nextUp(min);
		float max2 = Math.nextDown(max);
		if (!(max2 >= min2)) throw new IllegalArgumentException("Empty interval: (" + min + ", " + max + ")");
		return nextInclusiveFloat(random, min2, max2);
	}

	public static float betweenSeedInclusiveInclusiveFloat(long seed, float min, float max) {
		if (!(max >= min)) throw new IllegalArgumentException("Empty interval: [" + min + ", " + max + "]");
		return nextInclusiveFloat(seed, min, max);
	}

	public static float betweenSeedInclusiveExclusiveFloat(long seed, float min, float max) {
		float max2 = Math.nextDown(max);
		if (!(max2 >= min)) throw new IllegalArgumentException("Empty interval: [" + min + ", " + max + ")");
		return nextInclusiveFloat(seed, min, max2);
	}

	public static float betweenSeedExclusiveInclusiveFloat(long seed, float min, float max) {
		float min2 = Math.nextUp(min);
		if (!(max >= min2)) throw new IllegalArgumentException("Empty interval: (" + min + ", " + max + "]");
		return nextInclusiveFloat(seed, min2, max);
	}

	public static float betweenSeedExclusiveExclusiveFloat(long seed, float min, float max) {
		float min2 = Math.nextUp(min);
		float max2 = Math.nextDown(max);
		if (!(max2 >= min2)) throw new IllegalArgumentException("Empty interval: (" + min + ", " + max + ")");
		return nextInclusiveFloat(seed, min2, max2);
	}

	public static double scaleBetween(double value, double min, double max) {
		double range = max - min;
		if (range < Double.POSITIVE_INFINITY) {
			value = value * range + min;
		}
		else {
			//if min is sufficiently close to -Double.MAX_VALUE
			//and max is sufficiently close to +Double.MAX_VALUE,
			//then the difference between them would overflow to infinity.
			double halfMin = min * 0.5D;
			double halfMax = max * 0.5D;
			value = (value * (halfMax - halfMin) + halfMin) * 2.0D;
		}
		return value;
	}

	public static double nextInclusiveDouble(RandomGenerator random, double min, double max) {
		return scaleBetween(random.nextLong((1L << 53) + 1L) * 0x1.0p-53D, min, max);
	}

	public static double nextInclusiveDouble(long seed, double min, double max) {
		return scaleBetween(nextBoundedLong(seed, (1L << 53) + 1L) * 0x1.0p-53D, min, max);
	}

	public static double betweenRandomInclusiveInclusiveDouble(RandomGenerator random, double min, double max) {
		if (!(max >= min)) throw new IllegalArgumentException("Empty interval: [" + min + ", " + max + "]");
		return nextInclusiveDouble(random, min, max);
	}

	public static double betweenRandomInclusiveExclusiveDouble(RandomGenerator random, double min, double max) {
		double max2 = Math.nextDown(max);
		if (!(max2 >= min)) throw new IllegalArgumentException("Empty interval: [" + min + ", " + max + ")");
		return nextInclusiveDouble(random, min, max2);
	}

	public static double betweenRandomExclusiveInclusiveDouble(RandomGenerator random, double min, double max) {
		double min2 = Math.nextUp(min);
		if (!(max >= min2)) throw new IllegalArgumentException("Empty interval: (" + min + ", " + max + "]");
		return nextInclusiveDouble(random, min2, max);
	}

	public static double betweenRandomExclusiveExclusiveDouble(RandomGenerator random, double min, double max) {
		double min2 = Math.nextUp(min);
		double max2 = Math.nextDown(max);
		if (!(max2 >= min2)) throw new IllegalArgumentException("Empty interval: (" + min + ", " + max + ")");
		return nextInclusiveDouble(random, min2, max2);
	}

	public static double betweenSeedInclusiveInclusiveDouble(long seed, double min, double max) {
		if (!(max >= min)) throw new IllegalArgumentException("Empty interval: [" + min + ", " + max + "]");
		return nextInclusiveDouble(seed, min, max);
	}

	public static double betweenSeedInclusiveExclusiveDouble(long seed, double min, double max) {
		double max2 = Math.nextDown(max);
		if (!(max2 >= min)) throw new IllegalArgumentException("Empty interval: [" + min + ", " + max + ")");
		return nextInclusiveDouble(seed, min, max2);
	}

	public static double betweenSeedExclusiveInclusiveDouble(long seed, double min, double max) {
		double min2 = Math.nextUp(min);
		if (!(max >= min2)) throw new IllegalArgumentException("Empty interval: (" + min + ", " + max + "]");
		return nextInclusiveDouble(seed, min2, max);
	}

	public static double betweenSeedExclusiveExclusiveDouble(long seed, double min, double max) {
		double min2 = Math.nextUp(min);
		double max2 = Math.nextDown(max);
		if (!(max2 >= min2)) throw new IllegalArgumentException("Empty interval: (" + min + ", " + max + ")");
		return nextInclusiveDouble(seed, min2, max2);
	}
}