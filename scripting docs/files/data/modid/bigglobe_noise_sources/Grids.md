Files in this directory are Grid instances. Fields:

* `dimensions` - the number of dimensions in the grid. Can be anywhere from 1 to 3 (inclusive).
	* Some grids have a "scale" parameter, and of these, most allow you to tweak the scale on the X, Y, and Z axis independently. To make writing this documentation easier, whenever I write `scaleX/Y/Z`, I mean there are separate fields for `scaleX`, `scaleY`, and `scaleZ`, up to the number of dimensions in the grid. For example, if the grid has 2 dimensions, then only `scaleX` and `scaleY` are valid, and `scaleZ` will be ignored. Note that most 2D grids are used horizontally in Minecraft, but X and Z are the horizontal axes in Minecraft, not X and Y. Despite this, 2D grids do NOT use `scaleX` and `scaleZ`. It is always `scaleX` and `scaleY`. Note also that `scaleY` is typically up/down for 3D grids.

		**New in V4.4.0:** you can now also provide a single `scale` property, which applies to all X, Y, and Z. If `scale` and `scaleX/Y/Z` are both provided, `scaleX/Y/Z` takes priority.
	* In most cases, the number of dimensions is implicit depending on where you're defining the grid. At the time of writing this, only grids in the "bigglobe_noise_sources" folder need to specify the number of dimensions.
* `type` - how the values in this grid are computed.

The following other fields are also available when `type` is...
* "constant" - the value is the same everywhere.
	* `value` - the value to use everywhere.
* "white_noise" - the value is completely random everywhere. All values between -amplitude and +amplitude are equally likely.
	* `amplitude` - the maximum possible value that the value could be. The minimum possible value is -amplitude.
* "binary" - the value is always either -amplitude or +amplitude. Nothing in-between.
	* `amplitude` - the maximum value this grid can supply. The minimum value is -amplitude.
	* `chance` - the probability (from 0 to 1) of choosing amplitude as the value. If probability is near 1, then amplitude is very likely to be chosen. If probability is near 0, then -amplitude is very likely to be chosen. This field is optional and defaults to 0.5.
* "gaussian" - the value is the average of several other values.
	* `amplitude` - the maximum possible value that this grid can supply.
	* `iterations` - the number of random numbers that are averaged together.
* "linear", "smooth", "smoother", or "cubic"
	```json
	{
		"type": "smooth",
		"amplitude": 1.0,
		"scaleX/Y/Z": 123
	}
	```
	is shorthand for
	```json
	{
		"type": "smooth_resample",
		"source": {
			"type": "white_noise",
			"amplitude": 1.0
		},
		"scaleX/Y/Z": 123
	}
	```
	These types pre-date resample grids, but are left around for backwards-compatibility reasons.
* "worley" - a "center point" exists, on average, every `scale` blocks, and the value is the square of the distance to the nearest center point.
	* `scale` - the average distance between center points. Note that there is only one scale for this type, not one per axis.
	* `amplitude` - a multiplier for the value. The value is always in the range 0 to amplitude.
* "linear_resample", "smooth_resample", "smoother_resample", or "cubic_resample"
	* `source` - another grid with the same number of dimensions as this one.

		It is recommended (but not required) to use one of the following grid types for the source, because other grid types not on this list may be slower to sample.
		* white_noise
		* binary
		* gaussian
	* `scaleX/Y/Z` - distance between sample points.

	These types will sample `source` every `scaleX/Y/Z` blocks, and interpolate between these numbers for all blocks that do not lie on a lattice point. The difference between them is the curve used for interpolation.
	* linear - no curve. f(x) = x for 0 <= x <= 1.
	* smooth - a degree 3 polynomial. f(x) = -2x^3 + 3x^2 for 0 <= x <= 1. Unlike linear, the derivative of this curve is 0 at x = 0 and at x = 1.
	* smoother - a degree 5 polynomial. f(x) = 6x^5 - 15x^4 + 10x^3 for 0 <= x <= 1. Unlike smooth, the 2nd derivative of this curve is 0 at x = 0 and at x = 1.
	* cubic - this one's a bit special. Instead of interpolating between 2, 4, or 8 sample points (for 1, 2, or 3 dimensions respectively), it interpolates between 4, 16, or 64 sample points using cubic, bicubic, or tricubic interpolation. For example, in just 1 dimension, we have 4 sample points. Let's name them a, b, c, and d. A cubic curve will satisfy the following properties:
		* f(0) = b.
		* f(1) = c.
		* f'(0) = (c - a) / 2.
		* f'(1) = (d - b) / 2.
		* f is a degree 3 polynomial.

		Note that the resulting curve can sometimes, in rare circumstances, result in values that are greater than source's max value, or less than source's min value.
* (dx|dy|dz)_(linear|smooth|smoother|cubic)_resample (I didn't feel like typing out all the combinations here, but one example is dy_smoother_resample) interpolates values from another grid just like described above, but then computes the derivative of the curve they use for interpolation and returns that. Like the other resample grids above, the dx/y/z resample grids have the following additional properties:
	* `source`
	* `scaleX/Y/Z`

	They control the same thing as the normal resample grids.

	Note: The "linear" derivative grids will be discontinuous every scaleX/Y/Z blocks!
* "offset" (upcoming) - samples another grid at a different position. Has the following additional properties:
	* `source` - the other grid to sample.
	* `offsetX/Y/Z` - the offset from the current position to the sampled position. In other words, the sampled position is the requested position + offset.
* "negate" - negates another grid. The resulting value at every position will be the negative of the wrapped grid's value.
	* `grid` - the grid to negate.
* "abs" - takes the absolute value of another grid.
	* `grid` - the grid to take the absolute value of.
* "square" - squares another grid. In other words, raises its values to the power of 2.
	* `grid` - the grid to square.
* "change_range" - changes the output range of another grid.
	* `grid` - the grid to change the range of.
	* `min` - the new minimum value of the grid.
	* `max` - the new maximum value of the grid.
* "sum" - sums up the values of 2 or more other grids.
	* `layers` - array of grids to sum up. They don't all need to be the same type, but they do all need to have the same number of dimensions.
* "product" - multiplies the values of 2 or more other grids together.
	* `layers` - array of grids to multiply together. Like sum, they don't all need to be the same type, but they do need to have the same number of dimensions.
* "template" - delegates to another grid provided in the "bigglobe_noise_sources" folder.
	* `template` - the grid to delegate to, specified as "namespace:path", where namespace is the name of the folder immediately inside your "data" folder, and path is the name of the file inside the "bigglobe_noise_sources" folder.
* "script" - uses a script to compute noise.
	* `script` - string or array of strings defining the script's source code.
		
		This is one place where script templates cannot (currently) be used.

		The available environments for this script include MathScriptEnvironment, and StatelessRandomScriptEnvironment. Also, the following additional variables are available:
		* `x/y/z` - the position being sampled.
		* anything defined in inputs.
	* `inputs` - an object containing other grids that the script depends on.
		* (key) - the name of an input.
		* (value) - another grid with the same number of dimensions as this grid.
	* `min` - the smallest value (closest to negative infinity) that this script can return.
	* `max` - the largest value (closest to positive infinity) that this script can return.

	Unlike most grids, min and max are not implicit, and cannot be inferred from the script's source code or from its inputs. So, it relies on you to specify what they are.
* "project_x", "project_y", or "project_z" - converts a 1D grid to a 2D grid or a 3D grid.
	* `grid` - the 1D grid to convert.

	For project_x, the sampled coordinate in the 1D grid is the X coordinate of the resulting 2D or 3D grid. For project_y, it's the Y coordinate, and for project_z it's the Z coordinate.

	project_z is only a valid type in 3D. project_z cannot be used to convert a 1D grid to a 2D grid.
* "project_xy", "project_xz", or "project_yz" - converts a 2D grid to a 3D grid.
	* `grid` - the 2D grid to convert.
	
	For project_xy, the sampled coordinates in the 2D grid are the X and Y coordinates of the resulting 3D grid. For project_xz, they're the x and z coordinates, and for project_yz, they're the y and z coordinates.
* "stalactites" - only valid in 2 dimensions, used to generate values which resemble a heightmap of stalactites. Or stalagmites. One of those.
	* `scale` - the average distance between potential stalactite center points.
	* `amplitude` - a multiplier for the height of the stalactites. Can be negative.
	* `chance` - the probability from 0 to 1 that a stalactite will spawn at each center point.

	This grid type was added solely because I didn't want to write a script for it. Its format is subject to change, and in general is discouraged from being used.
* "sine_sum" - only valid in 2 dimensions, sums up many sine waves with random angles and phases.
	* `scale` - the wavelength of the sine waves. Sort of. The actual wavelength is this value multiplied by tau.
	* `amplitude` - a multiplier for the amplitude of the sine waves. Values produced by this grid will be between -amplitude * sqrt(iterations) and +amplitude * sqrt(iterations).
	* `iterations` - the number of sine waves to sum up. Must be between 0 and 64.

	Note: unlike most other grids, this grid is not stateless. It keeps a cache of angles and phases to use for the sine waves which depends on the seed passed into it. As such, it should NOT be used with the "stacked" 3D grid types, because this will result in very poor performance.
* "stacked_xy", "stacked_xz", and "stacked_yz" - converts a 2D grid to a 3D grid, but unlike the "project" types, the seed passed into the 2D grid is modified based on the unused coordinate. For example, for stacked_xz, the sampled coordinates of the 2D grid are the X and Z coordinates of the resulting 3D grid, and the seed passed into the 2D grid depends on the Y coordinate of the resulting 3D grid.
	* `2D_grid` - the 2D grid to convert to 3D.
	
	Note: these types should NOT wrap a 2D grid of type "sine_sum", because this will result in very poor performance.