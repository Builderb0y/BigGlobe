This environment automatically includes everything in the TraitManager environment.

This environment is data-driven, so all the variables and functions it provides to scripts are defined by you! This is done by adding json files to the `/data/(modid)/worldgen/bigglobe_column_value/` directory. All files in this directory are expected to be formatted as a ColumnEntry.
* If a column entry is 3D, then it will be exposed to scripts as a function taking a parameter for the Y level.
	* If an implicit Y level is available, then it will *also* be exposed to scripts as a variable.
* If a column entry is 2D, then it will be exposed to scripts as a variable.

If the column entry's type is `class`, then all fields declared on the class will be exposed as fields.

If the column entry's type is `voronoi`, then all exports declared on the voronoi cell will be exposed as fields or methods, depending on whether or not they are 3D and whether or not an implicit Y level is available.
* If the export is 3D then it will be exposed as a method taking Y as a parameter.
	* If an implicit Y level is available, then at will *also* be exposed as a field.
* If the export is 2D then it will be exposed as a field.

The following fields are also available on all voronoi cells:
* `id` - the namespace and path of the voronoi settings which defines this cell.
* `cell_x`, `cell_z`, `center_x`, and `center_z` - the voronoi diagram is setup so that the world is split up into square areas `distance` blocks wide. Every square has a single seed point somewhere inside it. The cell position is the index of the square itself, incrementing or decrementing by 1 across adjacent squares. The center position is the position of the seed point inside the square, measured in blocks. The center position is relative to the world origin, not the start of the square.
* `hard_distance` and `hard_distance_squared` - how close the current position is to the nearest edge of the cell, from 0 to 1, where 0 is in the center, and 1 is on the edge. The "squared" version is the normal version raised to the power of 2.
* `soft_distance` and `soft_distance_squared` - roughly how close the current position is to the edge of the cell, from 0 to 1, where 0 is in the center, and 1 is on the edge. The difference between soft distance and hard distance is made clear when you consider the set of points whose distance is a constant value. For example, 0.5. These points will form a smaller copy of the original voronoi cell shape with hard distance. With soft distance, this shape will have rounded corners. The corners are more rounded when soft distance is smaller, and less rounded when soft distance is larger. Internally, this value is actually calculated in terms of the squared variant, and the normal variant is the square root of that. As such, it is faster to use `soft_distance_squared` than it is to use `soft_distance ^ 2`.
* `euclidean_distance` and `euclidean_distance_squared` - the number of blocks between the center position and the current position. Like soft distance, this is computed in terms of the square, so `euclidean_distance_squared` is faster to use than `euclidean_distance ^ 2`.

If the script is itself used to compute one of the values exported by a voronoi settings, then the above properties will be exposed as variables AND fields. Otherwise, they will be exposed as fields only.

If the script is itself used to compute one of the values enabled by a voronoi settings, then every column value enabled by the voronoi settings is also exposed to the script in the same way as any other column value. Additionally, the exports of the voronoi cell are made available as variables too, not just fields.

If the script does not have an implicit x or z coordinate (for example, feature dispatchers), then:
* All column values exposed as variables are instead exposed as functions which take x and z as parameters.
* All column values exposed as fields are instead exposed as methods which take x and z as parameters.
* All column values exposed as functions taking y as a parameter are instead exposed as functions taking x, y, and z as parameters.
* All column values exposed as methods taking y as a parameter are instead exposed as methods taking x, y, and z as parameters.

If the script DOES have an implicit X and Z coordinate, it may or may not be possible to access column values at other coordinates. If my documentation states that a coordinate is hard-coded, then it is not possible to access column values at other coordinates. If my documentation states that a coordinate "defaults to" something, then it is possible to access column values at other coordinates by providing them when accessing the column value. For example:
```
float value = `some_mod:some_column_value`(y) ;uses the default coordinates.
value = `some_mod:some_column_value`(x, y, z) ;uses the provided coordinates.
```

New in V4.3.0: If the script which is making use of this column value is itself part of a column value, then this column value will have an alias which skips the parts of its name that are shared with the script which is using this column value. Examples might help:
* `mod:a` can reference `mod:b` with the alias `b`. In this case the common part is just the namespace.
* `mod:dim/a` can reference `mod:dim/b` with the alias `b`. In this case the common part includes the namespace and the "dim" folder.
* `mod:a` can reference `mod:dim/a` with the alias `dim/a`. In this case the common part is just the namespace again.
* `mod1:a` cannot reference `mod2:a` with an alias because there is nothing in common between these two names.
* `mod:dim/a` cannot reference `mod:a` with an alias because it would require back-tracking out of the "dim" folder.
	* To illustrate why this is a problem, consider the case where both `mod:a` and `mod:dim/a` both exist, and `mod:dim/b` tries to reference just `a`. In this case it is ambiguous which column value `a` should refer to.
* `mod:dim1/a` cannot reference `mod:dim2/a` with an alias because it would also require back-tracking.

If an alias is available, then it will be available *in addition to* the full name, not *instead of* the full name. If an alias is not available, then the column value can only be referred to by its full name.