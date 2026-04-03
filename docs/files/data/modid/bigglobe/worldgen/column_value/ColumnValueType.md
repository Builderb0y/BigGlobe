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

If it's an object, then the `type` property inside the object may be one of the following:
* `class` has the following additional properties:
	* `name` (string) - controls the name of the type associated with this class.
	* `fields` (list) - the list of fields in the class. Every element in the list has the following properties:
		* `name` - the name of the field.
		* `type` - a ColumnValueType representing the type of this field.
* `voronoi` has the following additional properties:
	* `name` (string) - controls the name of the type associated with this voronoi cell. This is the name that will be exposed to scripts. For example, if the name was "ExampleCell", then a script would be able to do `ExampleCell cell = ...`
	* `exports` (object) - controls the list of column values which will be present on all voronoi cells.
		* (key) - the name of the exported value.
		* (value) - an AccessSchema describing the type of the value and how it should be accessed.