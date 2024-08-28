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

			The script has the following additional environments available:
			* MathScriptEnvironment
			* MinecraftScriptEnvironment
			* StatelessRandomScriptEnvironment
			* BaseColumnScriptEnvironment
			* ColumnEntryRegistry (x and z are hard-coded to the position of the column, y has a default value if `params > is_3d` is true)
			* ColorScriptEnvironment
			* ExternalImageScriptEnvironment
			* ExternalDataScriptEnvironment

			If `params > is_3d` is set to true, then an implicit y level is also available, accessed via the variable `y`.
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