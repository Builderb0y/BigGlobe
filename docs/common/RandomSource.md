A RandomSource is an object which supplies random numbers in a specific range. Unlike grids, random sources do not interpolate between different values at different locations. Instead, they simply receive a seed, and return a random number based on that seed.

A random source consists of a `type`, and (usually) a `min` and `max`. `min` specifies the smallest (closest to negative infinity) value the source can supply, and `max` specifies the largest (closet to positive infinity) value the source can supply. If the type requires a `min` and `max` property, then `max` must be greater than `min`.

`type` can be one of the following:
* `constant` - returns the same value for each seed. Has the following additional property:
	* `value` - the value to return.

	This type does NOT have a `min` or `max` property.
* `uniform` - all real numbers between `min` and `max` are equally likely to be selected.
* `linear_low` - numbers closer to `min` are more likely to be selected than numbers closer to `max`.
* `linear_high` - numbers closer to `max` are more likely to be selected than numbers closer to `min`.
* `linear_centered` - numbers closer to the middle are more likely to be selected than numbers closer to `min` or `max`.
* `gaussian` - generates several different numbers and returns the average value of the numbers it generated. Has the following additional property:
	* `samples` - the number of numbers to average. Must be greater than 0.

		When samples is 1, this type produces the same distribution of values as `uniform`.

		When samples is 2, this type produces the same distribution of values as `linear_centered`.
* `exponential` - produces values between `min` and `max` on a logarithmic scale. Another way to think about this type is that all orders of magnitude between `min` and `max` are equally likely to be selected. The greater the difference in number of orders of magnitude covered between `min` and `max`, the greater the bias towards `min` becomes. In this type, `min` and `max` must be greater than 0.
* `average` - produces values between `min` and `max`, but biased to produce a specific average across samples. This type has the following additional property:
	* `average` - the average value that should be returned. If `average` is closer to `min` than `max`, then values less than `average` are more likely to be returned than values greater than `average`. If `average` is closer to `max` than `min`, then values greater than `average` are more likely to be returned than values less than `average`. `average` must be greater than `min`, and less than `max`.
* `scripted` - allows a script to return values any way it wants to. This mode still requires a `min` and `max` property, but they do not control how the script works. Instead, they communicate to the rest of Big Globe what range the script provides values in. This type has the following additional property:
	* `script` - the script which supplies values. This script has the following environments available:
		* MathScriptEnvironment
		* StatelessRandomScriptEnvironment
		* GridScriptEnvironment
		* MinecraftScriptEnvironment
		* BaseColumnScriptEnvironment
		* ColumnEntryRegistry
		* ColorScriptEnvironment

		And the following additional variables:
		* `y` - the Y level of the thing using this random source; usually a structure or a feature.
		* `randomSeed` - the seed to use for generating the random number.

		Note that `worldSeed` and `columnSeed` are also available, but they should not be used for generating random numbers. Only `randomSeed` should be used.

	Some things which use random sources could, in theory, be used in other world types that aren't scripted. In this case, if the script uses column values in any way, the thing which used the random source (usually a structure or a feature) will simply do nothing. Though some of the things that use random sources themselves require a Big Globe world type, and will do nothing in other world types regardless of whether or not the script uses column values.