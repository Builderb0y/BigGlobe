# Variables

* `double pi` - the double which is closest to the mathematical constant [π](https://en.wikipedia.org/wiki/Pi).
* `double tau` - the double which is closest to 2 * π.
* `double e` - the double closest to [Euler's constant](https://en.wikipedia.org/wiki/E_(mathematical_constant)).
* `float nan` - the floating point constant [NaN](https://en.wikipedia.org/wiki/NaN). This constant can be implicitly cast to type `double` as needed.
* `float inf` - the floating point constant [infinity](https://en.wikipedia.org/wiki/IEEE_754#Infinities). This constant can be implicitly cast to type `double` as needed.

# Functions

## Trigonometry

* `double sin(double radians)` and `double cos(double radians)` - [sine and cosine](https://en.wikipedia.org/wiki/Sine_and_cosine).
* `double tan(double radians)` - [tangent function](https://en.wikipedia.org/wiki/Trigonometric_functions).
* `double asin(double value)`, `double acos(double value)`, and `double atan(double value)` - [inverse sine, cosine, and tangent](https://en.wikipedia.org/wiki/Trigonometric_functions).
* `double sinh(double value)`, `double cosh(double value)`, and `double tanh(double value)` - [hyperbolic sine, cosine, and tangent](https://en.wikipedia.org/wiki/Hyperbolic_functions).
* `double asinh(double value)`, `double acosh(double value)`, and `double atanh(double value)` - [inverse hyperbolic sine, cosine, and tangent](https://en.wikipedia.org/wiki/Inverse_hyperbolic_functions).
* `double atan2(double x, double y)` - the [arc tangent function of 2 arguments](https://en.wikipedia.org/wiki/Atan2).

## Exponentials

* `double exp(double value)` - the [exponential function](https://en.wikipedia.org/wiki/Exponential_function).
* `double log(double value)` - the [natural logarithm function](https://en.wikipedia.org/wiki/Natural_logarithm)
* `double ln(double value)` - alias for `log(value)` coming in Big Globe 4.0.
* `double exp2(double value)` - 2 raised to the power of value.
* `double log2(double value)` - logarithm in base 2.
* `double sqrt(double value)` - the [square root function](https://en.wikipedia.org/wiki/Square_root).
* `double cbrt(double value)` - the [cube root function](https://en.wikipedia.org/wiki/Cube_root).
* `double pow(double value, double exponent)` - the [power function](https://en.wikipedia.org/wiki/Exponentiation). Slightly more accurate than typing `value ^ exponent` in some cases.

## Interpolation

* `float mixLinear(float min, float max, float value)` and `double mixLinear(double min, double max, double value)` - performs [linear interpolation](https://en.wikipedia.org/wiki/Linear_interpolation) between max.
	* These functions are equivalent to `(max - min) * value + min`.
	* Invariants:
		* When value is 0.0, the result is min.
		* When value is 1.0, the result is max.
		* When value is between 0.0 and 1.0, the result is between min and max.
		* When value is less than 0, the result is less than min.
		* When value is greater than 1, the result is greater than max.
		* When min, max, or value is NaN, the result is NaN.
	* As the name would imply, `mixLinear(constant1, constant2, x)` is a [linear function](https://en.wikipedia.org/wiki/Linear_function_(calculus)) of x.
		* This means that the function is [continuous](https://en.wikipedia.org/wiki/Continuous_function),  [smooth, and infinitely differentiable](https://en.wikipedia.org/wiki/Smoothness).
* `float mixClamp(float min, float max, float value)` and `double mixClamp(double min, double max, double value)` - similar to `mixLinear(min, max, value)`, except that the result is always guaranteed to be between min and max.
	* Invariants:
		* When value is less than 0.0, the result is min.
		* When value is greater than 1.0, the result is max.
		* When value is NaN, min is returned.
		* All other invariants of mixLinear() apply to mixClamp().
	* `mixClamp(constant1, constant2, x) is a continuous function of x, but it is not smooth.
* `mixSmooth(float min, float max, float value)` and `double mixSmoother(double min, double max, double value)` - similar to `mixClamp(min, max, value)` except that after value is clamped to the 0-1 range, it is then passed through the polynomial `-2x^3 + 3x^2` before performing linear interpolation on the min and max.
	* All the invariants of mixClamp() apply to mixSmooth().
	* `mixSmooth(constant1, constant2, x)` is [C^1 differentiable](https://en.wikipedia.org/wiki/Smoothness#Differentiability_classes) as a function of x.
* `float mixSmoother(float min, float max, float value)` and `double mixSmoother(double min, double max, double value)` - similar to `mixSmooth(min, max, value)` except that the polynomial used is `6x^5 - 15x^4 + 10x^3`.
	* All the invariants of mixSmooth() and, by extension, mixClamp() apply to mixSmoother().
	* `mixSmoother(constant1, constant2, x)` is [C^2 differentiable](https://en.wikipedia.org/wiki/Smoothness#Differentiability_classes) as a function of x.
* `float unmixLinear(float min, float max, float value)` and `double unmixLinear(double min, double max, double value)` - performs linear de-interpolation between min and max. In other words, instead of mapping the range [0-1] to [min-max], it maps the range [min-max] to the range [0-1].
	* These functions are equivalent to `(value - min) / (max - min)`.
	* Invariants:
		* When value equals min, the result is 0.0.
		* When value equals max, the result is 1.0.
		* When value is between min and max, the result is between 0.0 and 1.0.
		* When value is less than min, the result is less than 0.0.
		* When value is greater than max, the result is greater than 1.0.
	* `unmixLinear(constant1, constant2, x)` is a [linear function](https://en.wikipedia.org/wiki/Linear_function_(calculus)) of x.
		* This means that the function is [continuous](https://en.wikipedia.org/wiki/Continuous_function),  [smooth, and infinitely differentiable](https://en.wikipedia.org/wiki/Smoothness).
* `float unmixClamp(float min, float max, float value)` and `double unmixClamp(double min, double max, double value)` - similar to `unmixLinear(min, max, value)`, except that the result is always guaranteed to be between 0.0 and 1.0.
	* Invariants:
		* When value is less than min, the result is 0.0.
		* When value is greater than max, the result is 1.0.
		* When value is NaN, the result is min.
		* All other invariants of unmixLinear() remain unchanged.
	* `mixClamp(constant1, constant2, x)` is [continuous](https://en.wikipedia.org/wiki/Continuous_function), and therefore [C^0 differentiable](https://en.wikipedia.org/wiki/Smoothness#Differentiability_classes).
* `float unmixSmooth(float min, float max, float value)` and `double unmixSmooth(double min, double max, double value)` - similar to `unmixClamp(min, max, value)` except that the result is passed through the polynomial `-2x^3 + 3x^2` after being de-interpolated.
	* All the invariants of unmixClamp() apply to unmixSmooth().
	* `unmixSmooth(constant1, constant2, x)` is [C^1 differentiable](https://en.wikipedia.org/wiki/Smoothness#Differentiability_classes).
* `float unmixSmoother(float min, float max, float value)` and `double unmixSmoother(double min, double max, double value)` - similar to `unmixSmooth(min, max, value)` except that the polynomial used is `6x^5 - 15x^4 + 10x^3`.
	* All the invariants of unmixSmooth() and, by extension, unmixClamp() apply to unmixSmoother().
	* `unmixSmoother(constant1, constant2, x)` is [C^2 differentiable](https://en.wikipedia.org/wiki/Smoothness#Differentiability_classes) as a function of x.
* `float smooth(float value)` and `double smooth(double value)` - special case of `mixSmooth(0.0, 1.0, value)`. In other words, applies the polynomial `-2x^3 + 3x^2` to value.
* `float smoother(float value)` and `double smoother(double value)` - special case of `mixSmoother(0.0, 1.0, value)`. In other words, applies the polynomial `6x^5 - 15x^4 + 10x^3` to value.

## Misc

* `double toRadians(double degrees)` - converts degrees to radians.
* `double toDegrees(double radians)` - converts radians to degrees.
* `double floor(double value)` and `double ceil(double value)` - the [floor and ceiling functions](https://en.wikipedia.org/wiki/Floor_and_ceiling_functions).
* `int abs(int number)`, `long abs(long number)`, `float abs(float number)`, and `double abs(double number)` - the [absolute value](https://en.wikipedia.org/wiki/Absolute_value) of the number.
	* For ints and longs, if the number is the minimum possible value that an int or long can represent, then the result overflows and the minimum value is returned.
	* For floats and doubles, if the number is -0.0, then +0.0 is returned.
* `float copySign(float magnitude, float sign)` and `double copySign(double magnitude, double sign)` - returns a number with the provided magnitude and sign. The provided sign needn't be exactly 1 or -1, it can be any number. Only the sign of the number matters.
* `int sign(int number)` and `long sign(long number)` - returns -1, 0, or 1 if the number is negative, zero, or positive respectively.
* `float sign(float number)` and `double sign(double number)` - returns -1.0, -0.0, +0.0, +1.0, or NaN if the number is less than 0, has bitwise equivalence to -0.0, has bitwise equivalence to +0.0, is greater than 0.0, or is NaN respectively.
* `int mod(int a, int b)`, `long mod(long a, long b)`, `float mod(float a, float b)`, and `double mod(double a, double b)` is another way of computing `a % b`. There is no difference between these two notations.
* `boolean isNaN(float value)` and `boolean isNaN(double value)` - returns true if the value is NaN, false otherwise.
* `boolean isNotNaN(float value)` and `boolean isNotNaN(double value)` - returns true if the value is not NaN, false otherwise.
* `boolean isInfinite(float value)` and `boolean isInfinite(double value)` - returns true if the value is positive or negative infinity.
	* Returns false for NaN.
* `boolean isFinite(float value)` and `boolean isFinite(double value)` - returns true if the value is not positive infinity, negative infinity, or NaN.
* `int min(int... numbers)`, `long min(long... numbers)`, `float min(float... numbers)`, and `double min(double... numbers)` - returns the smallest (closest to negative infinity) of the provided numbers.
	* You must provide at least 2 arguments to these functions.
	* For floats and doubles, NaN is considered farthest away from negative infinity.
	* For floats and doubles, -0.0 is considered closer to negative infinity than +0.0.
* `int max(int... numbers)`, `long max(long... numbers)`, `float max(float... numbers)`, and `double max(double... numbers)` - returns the largest (closest to positive infinity) of the provided numbers.
	* You must provide at least 2 arguments to these functions.
	* For floats and doubles, NaN is considered farthest away from positive infinity.
	* For floats and doubles, +0.0 is considered closer to positive infinity than -0.0.
* `int clamp(int min, int max, int value)`, `long clamp(long min, long max, long value)`, `float clamp(float min, float max, float value)`, and `double clamp(double min, double max, double value)` - equivalent to `min(max(value, min), max)`, which is to say, it ensures that value is between min and max.
	* If value is less than min, then min is returned.
	* If value is greater than max, then max is returned.
	* For floats and doubles, if value is NaN, then min is returned.
		* Unless min is also NaN, in which case max is returned.

## Bitwise stuff

* `float intBitsToFloat(int bits)` - returns a float with the same bit pattern as the provided int.
* `int floatBitsToInt(float bits)` - returns an int with the same bit pattern as the provided float.
* `double longBitsToDouble(long bits)` - returns a double with the same bit pattern as the provided long.
* `long doubleBitsToLong(double bits)` - returns a long with the same bit pattern as the provided double.
* `int bitCount(int bits)` and `int bitCount(long bits)` - returns the number of 1's in the [2's compliment](https://en.wikipedia.org/wiki/Two%27s_complement) representation of the provided number.
* `int highestOneBit(int bits)` and `long highestOneBit(long bits)` - returns a number with a single bit set to 1. The chosen bit is the most significant 1 bit in the input number.
* `int lowestOneBit(int bits)` and `long lowestOneBit(long bits)` - returns a number with a single bit set to 1. The chosen bit is the least significant 1 bit in the input number.
* `int numberOfLeadingZeros(int bits)` and `int numberOfLeadingZeros(long bits)` - returns the number of zeros on the most significant end of the number before the first 1.
* `int numberOfTrailingZeros(int bits)` and `int numberOfTrailingZeros(long bits)` - returns the number of zeros on the least significant end of the number before the first 1.
* `int rotateLeft(int bits, int amount)` and `long rotateLeft(long bits, int amount)` - similar to `bits <<< amount` except that the bits discarded are re-inserted at the other end of the number. amount is modulo the size of bits.
* `int rotateRight(int bits, int amount)` and `long rotateRight(long bits, int amount)` - similar to `bits >>> amount` except that the bits discarded are re-inserted at the other end of the number. amount is modulo the size of bits.
* `int reverseBytes(int bits)` and `long reverseBytes(long bits)` - slices the number up into 4 bytes (for ints) or 8 bytes (for longs), reverses them, and re-assembles them.
* `int reverseBits(int bits)` and `long reverseBits(long bits)` - slices the number up into 32 bits (for ints) or 64 bits (for longs), reverses them, and re-assembles them.

# Notes

All of the above functions count as pure, which means their return value can be computed at compile-time if their argument is a constant value.