# Methods
* Random.nextInt() - returns a random int. All ints, both positive and negative, are equally likely to be selected.
* Random.nextInt(int max) - returns a random int between 0 (inclusive) and max (exclusive). All ints in this range are equally likely to be selected.
* Random.nextInt(int*(min, max)) - returns a random int between min (inclusive) and max (exclusive). All ints in this range are equally likely to be selected.
* Random.nextLong() - returns a random long. All longs, both positive and negative, are equally likely to be selected.
* Random.nextLong(long max) - returns a random long between 0 (inclusive) and max (exclusive). All longs in this range are equally likely to be selected.
* Random.nextLong(long*(min, max)) - returns a random long between min (inclusive) and max (exclusive). All longs in this range are equally likely to be selected.
* Random.nextFloat() - returns a random float between 0 (inclusive) and 1 (exclusive). This method follows a uniform distribution, in the sense that the chance of the chosen number falling between A and B (for two floats A and B such that 0 <= A < B <= 1) is equal to B - A.
* Random.nextFloat(float max) - returns a random float between 0 (inclusive) and max (exclusive). This method follows a uniform distribution, in the sense that the chance of the chosen number falling between A and B (for two floats A and B such that 0 <= A < B <= max) is equal to (B - A) / max.
* Random.nextFloat(float*(min, max)) - returns a random float between min (inclusive) and max (exclusive). This method follows a uniform distribution, in the sense that the chance of the chosen number falling between A and B (for two floats A and B such that min <= A < B <= max) is equal to (B - A) / (max - min).
* Random.nextDouble() - returns a random double between 0 (inclusive) and 1 (exclusive). This method follows a uniform distribution, in the sense that the chance of the chosen number falling between A and B (for two doubles A and B such that 0 <= A < B <= 1) is equal to B - A.
* Random.nextDouble(double max) - returns a random double between 0 (inclusive) and max (exclusive). This method follows a uniform distribution, in the sense that the chance of the chosen number falling between A and B (for two doubles A and B such that 0 <= A < B <= max) is equal to (B - A) / max.
* Random.nextDouble(double*(min, max)) - returns a random double between min (inclusive) and max (exclusive). This method follows a uniform distribution, in the sense that the chance of the chosen number falling between A and B (for two doubles A and B such that min <= A < B <= max) is equal to (B - A) / (max - min).
* Random.nextBoolean() - returns a random boolean. True and false are both equally likely to be selected.
* Random.nextBoolean(float trueChance) - returns a random boolean. The chance of true being selected is equal to the trueChance parameter, and the chance of false being selected is equal to 1 - trueChance. If trueChance is less than or equal to 0, false is selected unconditionally. If trueChance is greater than or equal to 1, true is returned unconditionally. If trueChance is NaN, false is returned unconditionally.
* Random.nextBoolean(double trueChance) - returns a random boolean. The chance of true being selected is equal to the trueChance parameter, and the chance of false being selected is equal to 1 - trueChance. If trueChance is less than or equal to 0, false is selected unconditionally. If trueChance is greater than or equal to 1, true is returned unconditionally. If trueChance is NaN, false is returned unconditionally.
* Random.nextGaussian() - returns a random double. The chance of any given double being selected follows a gaussian distribution with a mean of 0 and a standard deviation of 1.
* Random.nextGaussian(double*(mean, deviation))  - returns a random double. The chance of any given double being selected follows a gaussian distribution with a mean and standard deviation determined by the parameters to this method.
* Random.nextExponential() - returns a random double. The chance of any given double being selected follows an exponential decay curve.
* Random.roundInt(float value) - casts the value to an int, rounding either up or down randomly. The chance of rounding down is `1.0I - (value % 1.0I)`, and the chance of rounding up is `value % 1.0I`. If the value can already be represented as an int, it is returned as-is.
* Random.roundInt(double value) - casts the value to an int, rounding either up or down randomly. The chance of rounding down is `1.0L - (value % 1.0L)`, and the chance of rounding up is `value % 1.0L`. If the value can already be represented as an int, it is returned as-is.
* Random.roundLong(float value) - casts the value to a long, rounding either up or down randomly. The chance of rounding down is `1.0I - (value % 1.0I)`, and the chance of rounding up is `value % 1.0I`. If the value can already be represented as a long, it is returned as-is.
* Random.roundLong(double value) - casts the value to a long, rounding either up or down randomly. The chance of rounding down is `1.0L - (value % 1.0L)`, and the chance of rounding up is `value % 1.0L`. If the value can already be represented as a long, it is returned as-is.

# Member keywords
* Random.if (body) - syntax sugar for `if (random.nextBoolean(): body)`. May be combined with `else`, just like normal if statements.
* Random.if (chance: body) - syntax sugar for `if (random.nextBoolean(chance): body)`. May be combined with `else`, just like normal if statements.
* Random.unless (body) - syntax sugar for `unless (random.nextBoolean(): body)`. May be combined with `else`, just like normal if statements.
* Random.unless (chance: body) - syntax sugar for `unless (random.nextBoolean(chance): body)`. May be combined with `else`, just like normal if statements.
* Random.switch (value1, value2, ...) - evaluates and returns a random value. All provided values are equally likely to be selected.
* Random.switch (chance1: value1, chance2: value2, ...) - evaluates and returns a random value. The chance of any given value being selected is its chance divided by the sum of all chances. If all chances add up to 0, an exception is thrown.
* Random.switch (chance1: value1, chance2: value2, ... default: defaultValue) - evaluates and returns a random value. The chance of any given value being selected is its chance divided by the sum of all chances. If all chances add up to 0, defaultValue is returned..

# Types
* Random - an object capable of supplying random numbers, or otherwise behaving randomly, as described above.

# Type methods
* Random.new(long seed, int... salt) - creates a new Random object. The seed must be provided, but any non-negative number of salt parameters (including none) may be provided. If two Random instances are created with the same seed and the same salt, they will behave identically when asked to produce the same types of behavior. For example, it will always be the case that `Random.new(123L).nextInt() == Random.new(123L).nextInt()`.