RandomScriptEnvironment is useful, but sometimes you'll need to create a Random object, use it very few times, and then repeat next time the script is executed, which could be very often. In these cases, it is inefficient to actually create a Random object for this purpose, which is why some contexts don't actually have the RandomScriptEnvironment present. Instead, they have the concept of "stateless" random instances, which are really just primitive longs. The StatelessRandomScriptEnvironment allows you to convert a long directly into a random number, without allocating a Random object to act as a middleman.

# Methods
* long.newSeed(int... salt) - returns a new random seed based on the existing one and the salt. Any non-negative number of salt parameters may be provided, including no salt at all. The same initial seed and the same salt will produce the same new seed.
* long.nextInt() - returns a random int. All ints, both positive and negative, are equally likely to be chosen. The same seed will always produce the same number.
* long.nextInt(int max) - returns a random int between 0 (inclusive) and max (exclusive). All ints in this range are equally likely to be chosen. The same seed and the same max will always produce the same number.
* long.nextInt(int*(min, max)) - returns a random int between min (inclusive) and max (exclusive). all ints in this range are equally likely to be chosen. The same seed and the same min and max will always produce the same number.
* long.nextLong() - returns a random long. All longs, both positive and negative, are equally likely to be chosen. The same seed will always produce the same number.
* long.nextLong(long max) - returns a random long between 0 (inclusive) and max (exclusive). All longs in this range are equally likely to be chosen. The same seed and the same max will always produce the same number.
* long.nextLong(long*(min, max)) - returns a random long between min (inclusive) and max (exclusive). all longs in this range are equally likely to be chosen. The same seed and the same min and max will always produce the same number.
* long.nextFloat() - returns a random float between 0 (inclusive) and 1 (exclusive). This method follows a uniform distribution, in the sense that the chance of the chosen number falling between A and B (for two floats A and B such that 0 <= A < B <= 1) is equal to B - A. The same seed will always produce the same number.
* long.nextFloat(float max) - returns a random float between 0 (inclusive) and max (exclusive). This method follows a uniform distribution, in the sense that the chance of the chosen number falling between A and B (for two floats A and B such that 0 <= A < B <= max) is equal to (B - A) / max. The same seed and the same max will always produce the same number.
* long.nextFloat(float*(min, max)) - returns a random float between min (inclusive) and max (exclusive). This method follows a uniform distribution, in the sense that the chance of the chosen number falling between A and B (for two floats A and B such that min <= A < B <= max) is equal to (B - A) / (max - min). The same seed and the same min and max will always produce the same number.
* long.nextDouble() - returns a random double between 0 (inclusive) and 1 (exclusive). This method follows a uniform distribution, in the sense that the chance of the chosen number falling between A and B (for two doubles A and B such that 0 <= A < B <= 1) is equal to B - A. The same seed will always produce the same number.
* long.nextDouble(double max) - returns a random double between 0 (inclusive) and max (exclusive). This method follows a uniform distribution, in the sense that the chance of the chosen number falling between A and B (for two doubles A and B such that 0 <= A < B <= max) is equal to (B - A) / max. The same seed and the same max will always produce the same number.
* long.nextDouble(double*(min, max)) - returns a random double between min (inclusive) and max (exclusive). This method follows a uniform distribution, in the sense that the chance of the chosen number falling between A and B (for two doubles A and B such that min <= A < B <= max) is equal to (B - A) / (max - min). The same seed and the same min and max will always produce the same number.
* long.nextBoolean() - returns a random boolean. True and false are both equally likely to be selected.
* long.nextBoolean(float trueChance) - returns a random boolean. The chance of true being selected is equal to the trueChance parameter, and the chance of false being selected is equal to 1 - trueChance. If trueChance is less than or equal to 0, false is selected unconditionally. If trueChance is greater than or equal to 1, true is returned unconditionally. If trueChance is NaN, false is returned unconditionally.
* long.nextBoolean(double trueChance) - returns a random boolean. The chance of true being selected is equal to the trueChance parameter, and the chance of false being selected is equal to 1 - trueChance. If trueChance is less than or equal to 0, false is selected unconditionally. If trueChance is greater than or equal to 1, true is returned unconditionally. If trueChance is NaN, false is returned unconditionally.
* long.roundInt(float value) - casts the value to an int, rounding either up or down randomly. The chance of rounding down is `1.0I - (value % 1.0I)`, and the chance of rounding up is `value % 1.0I`. If the value can already be represented as an int, it is returned as-is.
* long.roundInt(double value) - casts the value to an int, rounding either up or down randomly. The chance of rounding down is `1.0L - (value % 1.0L)`, and the chance of rounding up is `value % 1.0L`. If the value can already be represented as an int, it is returned as-is.
* long.roundLong(float value) - casts the value to a long, rounding either up or down randomly. The chance of rounding down is `1.0I - (value % 1.0I)`, and the chance of rounding up is `value % 1.0I`. If the value can already be represented as a long, it is returned as-is.
* long.roundLong(double value) - casts the value to a long, rounding either up or down randomly. The chance of rounding down is `1.0L - (value % 1.0L)`, and the chance of rounding up is `value % 1.0L`. If the value can already be represented as a long, it is returned as-is.

# Keywords
* long.if (body) - syntax sugar for `if (seed.nextBoolean(): body)`. May be combined with `else`, just like normal if statements.
* long.if (chance: body) - syntax sugar for `if (seed.nextBoolean(chance): body)`. May be combined with `else`, just like normal if statements.
* long.unless (body) - syntax sugar for `unless (seed.nextBoolean(): body)`. May be combined with `else`, just like normal if statements.
* long.unless (chance: body) - syntax sugar for `unless (seed.nextBoolean(chance): body)`. May be combined with `else`, just like normal if statements.
* long.switch (value1, value2, ...) - evaluates and returns a random value. All provided values are equally likely to be selected.
* long.switch (chance1: value1, chance2: value2, ...) - evaluates and returns a random value. The chance of any given value being selected is its chance divided by the sum of all chances. If all chances add up to 0, an exception is thrown.
* long.switch (chance1: value1, chance2: value2, ... default: defaultValue) - evaluates and returns a random value. The chance of any given value being selected is its chance divided by the sum of all chances. If all chances add up to 0, defaultValue is returned..

# Notes
* long.nextGaussian() and long.nextExponential() do not exist in this environment.

# Idioms
Quite often, more than one random number is desired. This can be a problem when the same seed always produces the same number or behavior. To "re-use" a seed, the following pattern is recommended:
```
long seed = ... ;compute your initial seed however you want.
int*(
	x = (seed := seed.newSeed()).nextInt(-1, 2)
	y = (seed := seed.newSeed()).nextInt(-1, 2)
	z = (seed := seed.newSeed()).nextInt(-1, 2)
)
```