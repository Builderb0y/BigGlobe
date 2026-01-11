Column restrictions supply a number between 0 and 1 which controls the frequency or amount that something else spawns. For example, rock layers use use column restrictions as part of their thickness calculation.

Column restrictions have the following properties:
* `type` - may be one of:
	* `constant` - the restriction returns a constant number. Has the following additional properties:
		* `value` - the value to return.

		As a shortcut, you can also inline a number instead of an object for this type. For example:
		```json
		{
			"restriction": {
				"type": "constant",
				"value": 0.5
			}
		}
		```
		can be replaced with
		```json
		{
			"restriction": 0.5
		}
		```
	* `threshold` - Applies linear or smooth de-interpolation of the provided column value. Has the following additional properties:
		* `property` - the column value to compare to min and max.
		* `min` - the value for this property where the column restriction should return 0.
		* `max` - the value for this property where the column restriction should return 1.
		* `smooth_min` - when true, smoothens the transition between 0 and "not 0". When false, the transition can appear more abrupt.
		* `smooth_max` - when true, smoothens the transition between 1 and "not 1". When false, the transition can appear more abrupt.

		If the property returns a value between `min` and `max`, then the restriction will return a value between 0 and 1. If max is greater than min, then the restriction will return 0 when the property is less than min, and 1 when the property is greater than max. If max is less than min, then these are reversed.
	* `range` - Applies a very funky curve to the column value to ensure that the restriction returns 0 outside the range, and 1 at the mid point. Has the following additional properties:
		* `property` - the column value to compare to min, mid, and max.
		* `min` - the lower bound of the range. The restriction will return 0 when `property` is less than or equal to `min`.
		* `mid` - the middle of the range. This value must be greater than min and less than max, but it does not necessarily need to be equal to `(min + max) / 2`. The restriction will return 1 when `property` is equal to `mid`.
		* `max` - the upper bound of the range. The restriction will return 0 when `property` is greater than or equal to `max`.
		* `smooth` - when true, smoothens the transition between 0 and "not 0". When false, the transition can appear more abrupt.
	* `and` - Returns the product of 2 or more other restrictions. It is called 'and' because if you interpret the restrictions as chance values, then the chance of all of them passing is the same as their product. Has the following additional property:
		* `restrictions` - the list of restrictions to multiply together.
	* `or` - Returns one minus the product of one minus 2 or more other restrictions. In other words, for example, given restrictions A, B, and C, the `or` of them would be `1 - ((1 - A) * (1 - B) * (1 - C))`. This restriction is called 'or' because if you interpret the restrictions as chance values, then the chance of at least one passing is the same as 1 minus the product of one minus the chances. Has the following additional property:
		* `restrictions` - the list of restrictions to or-ify together.
	* `not` - Returns one minus another restriction. It is called 'not' because if you interpret the restriction as a chance value, then the chance of it *not* passing is one minus the chance of it passing.
		* `restriction` - the restriction to invert.
	* `skip_distant_horizons` - returns 0 when generating terrain for distant horizons, 1 otherwise. Has no additional properties.
	* `script` - returns a value from a script. Has the following additional property:
		* `script` - the script to evaluate. This script has the following environments available:
			* MathScriptEnvironment
			* StatelessRandomScriptEnvironment
			* GridScriptEnvironment (with implicit seed)
			* MinecraftScriptEnvironment
			* BaseColumnScriptEnvironment
			* ColumnEntryRegistry
			* ColorScriptEnvironment

			And the following additional variables:
			* `y` - the Y level where this script is being evaluated.

			And the following additional functions:
			* `double bandLinear(double*(min, mid, max, value))` - mimics a `range` restriction, with `smooth` set to false.
			* `double bandSmooth(double*(min, mid, max, value))` - mimics a `range` restriction, with `smooth` set to true.