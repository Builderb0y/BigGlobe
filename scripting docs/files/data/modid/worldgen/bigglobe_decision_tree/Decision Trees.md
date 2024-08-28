Decision trees are used by column entries with type `decision_tree`. Each decision tree node can choose to either provide a value, or check a condition and delegate to a different decision tree node. The json spec of a decision tree takes one of two forms:

Option 1: the node provides a result. In this case, the file has only one property:
* `result` - an object representing the result.
	* `type` may be one of the following:
		* `constant` - the result is a simple constant. In this case, there is another property:
			* `value` - something which represents the value. The decision tree doesn't actually specify what the type of the value (float, boolean, class, etc...) is; the column entry using it does.
				* When the type is byte, short, int, long, float, or double, the value must be a number.
				* When the type is voronoi, an exception is thrown unconditionally; you cannot have a decision tree that returns a voronoi cell.
				* When the type is block, block_state, biome, configured_feature, or wood_palette, the value must be a string containing the namespace and path of the relevant object.
				* When the type is class, the value must be an object. Every key of the object should match the name of one of the fields in the class, and every value should be another constant representing the value of that field.
		* `scripted` - the result is chosen by another script. In this case, there is another property:
			* `script` - the script which returns the value. The script has the following environments present:
				* MathScriptEnvironment
				* MinecraftScriptEnvironment
				* StatelessRandomScriptEnvironment
				* BaseColumnScriptEnvironment
				* ColumnEntryRegistry (x and z are hard-coded at the position of the column, y has a default value if the column value using this decision tree is 3D)
				* ColorScriptEnvironment
				* ExternalImageScriptEnvironment
				* ExternalDataScriptEnvironment

				If the column value referencing this decision tree is 3D and therefore has an implicit Y level, that Y level can be accessed via the `y` variable.

Option 2: the node checks a condition and delegates to a different decision tree node. In this case, the file has 3 properties:
* `condition` - the condition to check. This object has the following properties:
	* `type` - how to evaluate the condition. May be one of the following:
		* `threshold` has the following additional properties:
			* `column_value` - the column value that this condition is based on.
			* `min` - the value at which this condition evaluates to false.
			* `max` - the value at which this condition evaluates to true.

				If max is greater than min, then:
				* If the column value is less than min, then the condition evaluates to false.
				* If the column value is greater than max, then the condition evaluates to true.
				* If the column value is between min and max, then the condition evaluates to either true or false, randomly, based on how close the value was to min or max.
					* If the column value was close to min, then the condition is likely to evaluate to false.
					* If the column value was close to max, then the condition is likely to evaluate to true.

				If max is less than min, then the above invariants are negated:
				* If the column value is less than max, then the condition evaluates to true.
				* If the column value is greater than min, then the condition evaluates to false.
				* If the column value is between min and max, then the condition evaluates to either true or false, randomly, based on how close the value was to min or max.
					* If the column value was close to min, then the condition is likely to evaluate to false.
					* If the column value was close to max, then the condition is likely to evaluate to true.

				If max equals min, then an exception is thrown and data packs will fail to load.
			* `smooth_min` and `smooth_max` - if both of these are false, then the chance of choosing true or false is equal to `(value - min) / (max - min)` (clamped to the 0-1 range). Since min and max are constant values, this is a linear function of value. But sometimes a linear function can make it obvious where the transition starts and ends, which may not be desirable. smooth_min applies a curve to the chance to make it less obvious where the transition begins (or ends, depending on your perspective) near min, and smooth_max does the same for max. Both of these properties are optional and default to true.
		* `script` has the following additional properties:
			* `script` - a script which returns true or false. This script has the following environments available:
				* MathScriptEnvironment
				* MinecraftScriptEnvironment
				* StatelessRandomScriptEnvironment
				* BaseColumnScriptEnvironment
				* ColumnEntryRegistry (x and z are hard-coded to the position of the column, y has a default value if the column value using this decision tree is 3D)
				* ColorScriptEnvironment
				* ExternalImageScriptEnvironment
				* ExternalDataScriptEnvironment

				If the column value referencing this decision tree is 3D and therefore has an implicit Y level, that Y level can be accessed via the `y` variable.
		* `and` - evaluates to true if ALL wrapped conditions evaluate to true.
			* `conditions` - an array of other conditions. Must contain at least 2 conditions.
		* `or` - evaluates to true if ANY wrapped conditions evaluate to true.
			* `conditions` - an array of other conditions. Must contain at least 2 conditions.
		* `not` - evaluates to true if the wrapped condition evaluates to false, and vise versa.
			* `condition` - the condition to negate.
* `if_true` - the namespace and path of the decision tree node to delegate to if the condition evaluates to true.
* `if_false` - the namespace and path of the decision tree node to delegate to if the condition evaluates to false.