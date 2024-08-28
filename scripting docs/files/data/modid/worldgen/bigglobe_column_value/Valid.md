This is a (usually optional) object containing the following properties:
* `where` (script) - returns true/false if the column value is applicable to this x/z position.
* `min_y` (script) - for 3D column values, returns the lowest Y level (inclusive) that this column value is applicable at.
* `max_y` (script) - for 3D column values, returns the highest Y level (exclusive) that this column value is applicable at.
* `fallback` - the value to return for this column value if it is not valid. Defaults to NaN for floats and doubles, 0 for all other numbers, false for booleans, and null for objects.

The purpose of valid is to restrict where certain column values can be computed. For example, if you have expensive 3D noise which is only valid 512 blocks below the world surface (like cave noise), then it does not make sense to compute it outside that Y range.

`where`, `min_y`, and `max_y` have the following environments available:
* MathScriptEnvironment
* MinecraftScriptEnvironment
* StatelessRandomScriptEnvironment
* BaseColumnScriptEnvironment
* ColumnEntryRegistry (x and z are hard-coded to the position of the column)
* ColorScriptEnvironment
* ExternalImageScriptEnvironment
* ExternalDataScriptEnvironment