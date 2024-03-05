**This content is exclusive to Big Globe 4.0, and may change between now and when 4.0 is released!**

This environment is data-driven, so all the variables and functions it provides to scripts are defined by you! This is done by adding json files to the `/data/(modid)/worldgen/bigglobe_column_value/` directory. All files in this directory are expected to be formatted as a ColumnEntry.

# ColumnValueType

A ColumnValueType may be a string or an object. If it's a string, then it may be one of the following:
* `byte`
* `short`
* `int`
* `long`
* `float`
* `double`
* `boolean`
* `block`
* `block_state`
* `biome`
* `configured_feature`
* `wood_palette`

If it's an object, then `type` may be one of the following:
* `class` has the following additional properties:
	* `name` (string) - controls the name of the type associated with this class.
	* `fields` (object) - the list of fields in the class.
		* (key) - the name of the field.
		* (value) - a ColumnValueType representing the type of this field.
* `voronoi` has the following additional properties:
	* `name` (string) - controls the name of the type associated with this voronoi cell. This is the name that will be exposed to scripts. For example, if the name was "ExampleCell", then a script would be able to do `ExampleCell cell = ...`
	* `exports` (object) - controls the list of column values which will be present on all voronoi cells.
		* (key) - the name of the exported value.
		* (value) - an AccessSchema describing the type of the value and how it should be accessed.

# AccessSchema

This is an object with the following properties:
* `type` - a ColumnValueType describing the type accessed by this schema.
* `is_3d` - a boolean controlling whether or not the value is allowed to be different at different Y levels.

# Valid

This is a (usually optional) object containing the following properties:
* `where` (script) - returns true/false if the column value is applicable to this x/z position.
* `min_y` (script) - for 3D column values, returns the lowest Y level (inclusive) that this column value is applicable at.
* `max_y` (script) - for 3D column values, returns the highest Y level (exclusive) that this column value is applicable at.
* `fallback` - the value to return for this column value if it is not valid. Defaults to NaN for floats and doubles, 0 for all other numbers, false for booleans, and null for objects.

The purpose of valid is to restrict where certain column values can be computed. For example, if you have expensive 3D noise which is only valid 512 blocks below the world surface (like cave noise), then it does not make sense to compute it outside that Y range.

# ColumnEntry

This is the structure for the actual json files in `/data/(modid)/worldgen/bigglobe_column_value/`. It has the following properties:

* `type` - defines how the column value behaves, or how it's calculated, and may be one of the following:
	* `constant` - the value is constant. Has the following additional properties:
		* `value` - the value.
		
		Note: constant types is always valid, and never cached.
	* `noise` - the value is computed from noise. Has the following additional properties:
		* `grid` - if `params > is_3d` is set to true, then this must be a 3D grid. Otherwise, it must be a 2D grid.
		* `cache` - if set to true, the value will be stored so that if it is queried again, it is not computed again. If this column value is 3D according to its params, then all valid Y levels are computed in bulk on first access. Computing values in bulk is faster than computing all valid Y levels individually, but slower than computing only a single Y level. It is recommended to use caching if you plan on accessing many different Y levels or, if the value is 2D, if you plan on accessing the value  more than once.
		* `valid` - declares where this column value is valid.

		A noise type has the following additional restrictions:
		* `params > type` must be either `float` or `double`.
	* `script` - the value is computed from a script. Has the following additional properties:
		* `script` - the script which computes the value.
		* `cache` - if set to true, the value will be stored so that if it is queried again, it is not computed again. If this column value is 3D according to its params, then all valid Y levels are computed in bulk on first access. Computing values in bulk is the same speed as computing them individually. It is recommended to use caching if you plan on accessing the value many times, and the script which computes those values is non-trivial. For example, you would probably NOT want to use caching if the script simply returns the sum of 2 other cached values.
	* `decision_tree` - the value is calculated from a (possibly nested or very long) if-else chain, which itself is defined by more json files in the `/data/(modid)/worldgen/bigglobe_decision_tree/` directory. Has the following additional properties:
		* `root` - the namespace and path of the first decision tree element which should be checked.
		* `valid` - declares where this column value is valid.
		* `cache` - if set to true, the value will be stored so that if it is queried again, it is not computed again. If this column value is 3D according to its params, then all valid Y levels are computed in bulk on first access. Computing values in bulk is the same speed as computing them individually. It is recommended to use caching if you plan on accessing the value more than once.
	* `voronoi` - the world is split into voronoi cells, and each cell may calculate the value differently. Has the following additional properties:
		* `diagram` - object containing the distance and variation between seed points.
		* `valid` - declares where this column value is valid.

		A voronoi type has the following additional restrictions:
		* `params > type` must be `voronoi`.
		* `params > is_3d` must be `false`.

		Note: voronoi types are always cached.
* `params` - an AccessSchema defining what type the value is, and how to access it.

todo: document decision tree json files, voronoi json files, and the environments present on scripts.