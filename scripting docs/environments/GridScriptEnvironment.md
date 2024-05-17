This environment can be used to sample grids defined in `/data/modid/bigglobe_noise_sources/`.

# Fields

* `grid.minValue` - the smallest (closest to negative infinity) value that the grid can produce at any coordinate.
* `grid.maxValue` - the largest (closest to positive infinity) value that the grid can produce at any coordinate.
* `grid.dimensions` - the number of dimensions the grid has. This will always be between 1 and 3 (inclusive).

# Functions

* `NumberArray newBooleanArray(int length)` allocates a new NumberArray containing (length) booleans.
* `NumberArray newByteArray(int length)` allocates a new NumberArray containing (length) bytes.
* `NumberArray newShortArray(int length)` allocates a new NumberArray containing (length) shorts.
* `NumberArray newIntArray(int length)` allocates a new NumberArray containing (length) ints.
* `NumberArray newLongArray(int length)` allocates a new NumberArray containing (length) longs.
* `NumberArray newFloatArray(int length)` allocates a new NumberArray containing (length) floats.
* `NumberArray newDoubleArray(int length)` allocates a new NumberArray containing (length) doubles.

# Methods

* `grid1D.getValue(long seed, int x)` - samples the grid at the specified coordinate, and returns the value of the grid at that coordinate. Passing in a different seed will produce a different value.
* `grid2D.getValue(long seed, int x, int y)` - ditto, but for 2D grids. Note that 2D grids are usually used horizontally when doing anything related to terrain gen, but in Minecraft, x and z are the horizontal axes, not x and y. As such, if you're trying to use a 2D grid horizontally, consider passing the world Z coordinate into the grid's Y parameter.
* `grid3D.getValue(long seed, int x, int y, int z)` - ditto, but for 3D grids.
* `grid1D.getValuesX(long seed, int x, NumberArray samples)` - samples the grid starting at the provided coordinate, and continuing in the positive X direction until the provided samples array is full. the "prefix", "sliceFromTo", and "sliceOffsetLength" methods on NumberArray can be used to limit how many samples are collected, or change the indices in the array where the samples are stored.
* `grid2D.getValuesX(long seed, int x, int y, NumberArray samples)` - ditto for 2D grids.
* `grid2D.getValuesY(long seed, int x, int y, NumberArray samples)` - ditto, but continuing in the positive Y direction.
* `grid3D.getValuesX(long seed, int x, int y, int z, NumberArray samples)` - ditto, but for 3D grids.
* `grid3D.getValuesY(long seed, int x, int y, int z, NumberArray samples)` - ditto, but continuing in the positive Y direction.
* `grid3D.getValuesZ(long seed, int x, int y, int z, NumberArray samples)` - ditto, but continuing in the positive Z direction.

If an implicit seed is present (which it usually is), then the seed parameter of the above methods can be omitted.

* `numberArray.getBoolean(int index)` - casts the number at the provided index in the array to a boolean and returns it.
* `numberArray.getByte(int index)` - casts the number at the provided index in the array to a byte and returns it.
* `numberArray.getShort(int index)` - casts the number at the provided index in the array to a short and returns it.
* `numberArray.getInt(int index)` - casts the number at the provided index in the array to a int and returns it.
* `numberArray.getLong(int index)` - casts the number at the provided index in the array to a long and returns it.
* `numberArray.getFloat(int index)` - casts the number at the provided index in the array to a float and returns it.
* `numberArray.getDouble(int index)` - casts the number at the provided index in the array to a double and returns it.

* `numberArray.setBoolean(int index, boolean value)` - casts the provided value to the underlying type of the array, and stores it in the array at the provided index.
* `numberArray.setByte(int index, boolean value)` - casts the provided value to the underlying type of the array, and stores it in the array at the provided index.
* `numberArray.setShort(int index, boolean value)` - casts the provided value to the underlying type of the array, and stores it in the array at the provided index.
* `numberArray.setInt(int index, boolean value)` - casts the provided value to the underlying type of the array, and stores it in the array at the provided index.
* `numberArray.setLong(int index, boolean value)` - casts the provided value to the underlying type of the array, and stores it in the array at the provided index.
* `numberArray.setFloat(int index, boolean value)` - casts the provided value to the underlying type of the array, and stores it in the array at the provided index.
* `numberArray.setDouble(int index, boolean value)` - casts the provided value to the underlying type of the array, and stores it in the array at the provided index.

* `numberArray.(int index)` - attempts to infer the desired type from context, casts the value in the array at the provided index to that type, and returns it.
	* The desired type is inferred from what you attempt to cast the value to, either implicitly or explicitly.
		* For example, `int number = array.(0)` would infer that you want it to return an int.
	* If the type cannot be inferred, it defaults to double.
		* For example, `var number = array.(0)` would make number a double.
	* This method acts like an lvalue, meaning you can assign to it.
		* `array.(0) = 2` would attempt to cast 2 to the underlying type of the array, and store it in the array at index 0.

* `numberArray.prefix(int length)` - returns a view of the first (length) numbers in the array, as another array.
* `numberArray.sliceFromTo(int start, int end)` - returns a view of the numbers between start (inclusive) and end (exclusive) in the array, as another array.
* `numberArray.sliceOffsetLength(int offset, int length)` - returns a view of (length) numbers in the array, starting at (offset). In other words, the numbers whose indexes are between `offset` (inclusive) and `offset + length` (exclusive).
	* In all 3 of the above methods, changes to the old array are reflected in the new array, and vise versa.
	* This means that if you modify the original array at an index which the slice can access, then the slice will see the modified value.
	* Likewise, if you modify the slice, you will see the modified value in the original array too.

# Types

* Grid - the common super type of Grid1D, Grid2D, and Grid3D.
* Grid1D - a 1D grid.
* Grid2D - a 2D grid.
* Grid3D - a 3D grid.
* NumberArray - a wrapper for an array of numbers or booleans. This type of array will auto-cast any numbers you put in it to its underlying type, and can auto-cast any numbers you extract from it to the requested type. When number arrays are allocated, they use a shared backing array. This means there is almost no overhead from the allocation itself. This is about as close to stack-allocated arrays as you can get in java. There is a limit on the maximum size the backing array can be (defaults to 1 MB, but is configurable with java arguments), and therefore you should not allocate new NumberArray's over and over again. Instead, they should be re-used as much as possible. NumberArray's are automatically de-allocated when the script exits. Also, NumberArray's containing booleans will use one bit per boolean, but they are padded to the nearest byte at the end. This means that a NumberArray of 1 boolean and a NumberArray of 8 booleans will both take up 1 byte of space in the backing array, but a NumberArray of 9 booleans will take up 2 bytes.

# Casting

* `String -> Grid` - the string should be formatted as namespace:path
* `String -> Grid1D` - ditto.
* `String -> Grid2D` - ditto.
* `String -> Grid3D` - ditto.