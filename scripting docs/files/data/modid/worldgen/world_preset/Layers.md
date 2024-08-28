Layers are responsible for filling a column with blocks. They are the first step in block placement. All layer types have the following properties:
* `valid` (optional) - where this layer should place blocks.
	* `where` (optional) - a script which returns true or false based on whether or not it should place blocks in the current column. If not present, this layer will attempt to place blocks in every column.
	* `min_y` (optional) - a script which returns an int representing the minimum Y level (inclusive) that this layer should place blocks at. If not present, the layer will start placing blocks at the lowest Y level in the chunk.
	* `max_y` (optional) - a script which returns an int representing the maximum Y level (exclusive) that this layer should place blocks at. If not present, the layer will finish placing blocks at the highest Y level in the chunk.

		`where`, `min_y`, and `max_y` have the following environments present:
		* MathScriptEnvironment
		* StatelessRandomScriptEnvironment
		* GridScriptEnvironment (with implicit seed)
		* MinecraftScriptEnvironment
		* BaseColumnScriptEnvironment
		* ColumnEntryRegistry (x and z are hard-coded at the current position being generated)
		* ColorScriptEnvironment
		* ExternalImageScriptEnvironment
		* ExternalDataScriptEnvironment
* `children` - an array of other layers to generate. Every child in the array will only be able to replace blocks that this layer placed. Additionally, each child will only be able to place blocks where the previous child didn't.
* `before_children` - a script which can place blocks in this column before our children do.
* `after_children` - a script which can place blocks in this column after our children do.

	`before_children` and `after_children` have the following environments present:
	* MathScriptEnvironment
	* StatelessRandomScriptEnvironment
	* GridScriptEnvironment (with implicit seed)
	* MinecraftScriptEnvironment
	* BaseColumnScriptEnvironment
	* ColumnEntryRegistry (x and z are hard-coded at the current position being generated, unless you're in a dx or dz block (see below), in which case they're hard-coded at the position of an adjacent column)
	* ColorScriptEnvironment
	* ExternalImageScriptEnvironment
	* ExternalDataScriptEnvironment

	And the following extra variables:
	* `minY` - the minimum Y level (inclusive) of the chunk being generated.
	* `maxY` - the maximum Y level (exclusive) of the chunk being generated.

	And the following functions:
	* `getBlockState(int y)` - returns the BlockState at the X and Z position of the column being generated, and the Y level specified as an argument. Note however that this function does not know about blocks placed by the parent layer. If this layer has not placed a block at the provided Y level yet, and neither have its children, then this function returns null!
	* `setBlockState(int y, BlockState state)` - sets the BlockState at the X and Z position of the column being generated, and the Y level specified as an argument to the provided state. This function will have no effect if...
		* The parent layer did not place a block at this Y level, or
		* An earlier child already placed a block at this Y level.
	* `setBlockStates(int minY, int maxY, BlockSTate state)` - Sets the BlockState at the X and Z position of the column being generated, and all the Y levels between minY (inclusive) and maxY (exclusive) to state. Like `setBlockState()`, this function will not place blocks at any Y level where...
		* The parent layer did not place a block at the Y level, or
		* An earlier child already placed a block at the Y level.
	* `getTopOfSegment(int y)` - returns the highest Y level (exclusive) of the segment of blocks at the provided Y level. In other words, if you started at the provided Y level, and worked your way upwards until you found a different block than the one you started with, the Y level of the different block would be the same as the Y level returned by this function. Note however that because of the internal data structure Big Globe uses to store blocks during this part of terrain gen, calling getTopOfSegment() will be faster than having a loop in your script that checks getBlockState() at lots of different Y levels.
	* `getBottomOfSegment(int y)` - returns the lowest Y level (inclusive) of the segment of blocks at the provided Y level. In other words, this function is similar to getTopOfSegment(), but behaves as if it iterates downward instead of upward. And also returns the last matching block, not the first non-matching block.

	And the following keywords:
	* `dx(expression)` - evaluates expression on the current column and an adjacent column on the X axis, and returns the difference between the two results. Which adjacent column is used is not specified. It could be the adjacent column in the positive X direction, or in the negative X direction. What is guaranteed however is that if the positive X direction is used, then the result is `(value at +x) - (value at current)`, and if the negative X direction is used, then the result is `(value at current) - (value at -x)`.
	* `dz(expression)` - same as dx, but for the Z axis.

	Note: you cannot nest dx and dz expressions to get higher-order derivatives. In fact, you can't nest dx and dz expressions at all. The reason for this has to do with the internal details of how adjacent columns are selected, but the tl;dr is that if you tried to get a higher-order derivative by nesting dx or dz calls, the result would always be 0. This is not usually helpful, and I wanted to avoid confusion about why you get 0 instead of the higher-order derivative you were probably expecting, so I just made it a compile-time error instead telling you that this is not supported.
* `type` - may be one of the following:
	* `simple_2d` - fills the Y range between `valid > min_y` and `valid > max_y` (or the bounds of the chunk, if either of those values are not specified) with a single block state. This type has the following additional properties:
		* `state` - a script returning the BlockState that this layer will attempt to place. This script has the following environments available:
			* MathScriptEnvironment
			* StatelessRandomScriptEnvironment
			* GridScriptEnvironment (with implicit seed)
			* MinecraftScriptEnvironment
			* BaseColumnScriptEnvironment
			* ColumnEntryRegistry
			* ColorScriptEnvironment
			* ExternalImageScriptEnvironment
			* ExternalDataScriptEnvironment
	* `simple_3d` - fills only some blocks between `valid > min_y` and `valid > max_y` (or the chunk bounds) with a single block state. This type has the following additional properties:
		* `state` - a script returning the BlockState that this layer will attempt to place. This script has the following environments available:
			* MathScriptEnvironment
			* StatelessRandomScriptEnvironment
			* GridScriptEnvironment (with implicit seed)
			* MinecraftScriptEnvironment
			* BaseColumnScriptEnvironment
			* ColumnEntryRegistry
			* ColorScriptEnvironment
			* ExternalImageScriptEnvironment
			* ExternalDataScriptEnvironment

		* `condition` - a script returning true or false if the state should be placed at the current Y level. This script has the following environments available:
			* MathScriptEnvironment
			* StatelessRandomScriptEnvironment
			* GridScriptEnvironment (with implicit seed)
			* MinecraftScriptEnvironment
			* BaseColumnScriptEnvironment
			* ColumnEntryRegistry (x and z are hard-coded at the current position being generated, unless you're in a dx or dz block (see below), in which case they're hard-coded at the position of an adjacent column)
			* ColorScriptEnvironment
			* ExternalImageScriptEnvironment
			* ExternalDataScriptEnvironment

			The Y level itself can also be accessed via the `y` variable.
	* `multi_state_3d` (new in V4.3.0) - fills the Y range between `valid > min_y` and `valid > max_y` (or the chunk bounds) with a block state which depends on Y level. This type has the following additional properties:
		* `state` - a script returning the BlockState that this layer will attempt to place at the current Y level. If this script returns null, no block is placed at that Y level. This script has the following environments available:
			* MathScriptEnvironment
			* StatelessRandomScriptEnvironment
			* GridScriptEnvironment (with implicit seed)
			* MinecraftScriptEnvironment
			* BaseColumnScriptEnvironment
			* ColumnEntryRegistry (x and z are hard-coded at the current position being generated, unless you're in a dx or dz block (see below), in which case they're hard-coded at the position of an adjacent column)
			* ColorScriptEnvironment
			* ExternalImageScriptEnvironment
			* ExternalDataScriptEnvironment

			The Y level itself can also be accessed via the `y` variable.
	* `scripted` (new in V4.3.0) - allows a script to place any block wherever it wants (as long as it's within the valid range and the chunk bounds). This type has the following additional properties:
		* `script` - a script which places blocks. This script has the following environments available:
			* MathScriptEnvironment
			* StatelessRandomScriptEnvironment
			* GridScriptEnvironment (with implicit seed)
			* MinecraftScriptEnvironment
			* GridScriptEnvironment (with implicit seed)
			* BaseColumnScriptEnvironment
			* ColumnEntryRegistry (x and z are hard-coded at the current position being generated, unless you're in a dx or dz block (see below), in which case they're hard-coded at the position of an adjacent column)
			* ColorScriptEnvironment
			* ExternalImageScriptEnvironment
			* ExternalDataScriptEnvironment
		
			And the following extra variables:
			* `minY` - the minimum Y level (inclusive) of the chunk being generated.
			* `maxY` - the maximum Y level (exclusive) of the chunk being generated.
		
			And the following functions:
			* `getBlockState(int y)` - returns the BlockState at the X and Z position of the column being generated, and the Y level specified as an argument. Note however that this function does not know about blocks placed by the parent layer. If this layer has not placed a block at the provided Y level yet, and neither have its children, then this function returns null!
			* `setBlockState(int y, BlockState state)` - sets the BlockState at the X and Z position of the column being generated, and the Y level specified as an argument to the provided state. This function will have no effect if...
				* The parent layer did not place a block at this Y level, or
				* An earlier child already placed a block at this Y level.
			* `setBlockStates(int minY, int maxY, BlockSTate state)` - Sets the BlockState at the X and Z position of the column being generated, and all the Y levels between minY (inclusive) and maxY (exclusive) to state. Like `setBlockState()`, this function will not place blocks at any Y level where...
				* The parent layer did not place a block at the Y level, or
				* An earlier child already placed a block at the Y level.
			* `getTopOfSegment(int y)` - returns the highest Y level (exclusive) of the segment of blocks at the provided Y level. In other words, if you started at the provided Y level, and worked your way upwards until you found a different block than the one you started with, the Y level of the different block would be the same as the Y level returned by this function. Note however that because of the internal data structure Big Globe uses to store blocks during this part of terrain gen, calling getTopOfSegment() will be faster than having a loop in your script that checks getBlockState() at lots of different Y levels.
			* `getBottomOfSegment(int y)` - returns the lowest Y level (inclusive) of the segment of blocks at the provided Y level. In other words, this function is similar to getTopOfSegment(), but behaves as if it iterates downward instead of upward. And also returns the last matching block, not the first non-matching block.

			Note: this script does NOT have dx or dz available.