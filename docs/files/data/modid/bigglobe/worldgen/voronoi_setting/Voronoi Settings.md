If you don't know what a voronoi diagram is, I suggest you [learn that first](https://en.wikipedia.org/wiki/Voronoi_diagram).

Voronoi settings are used by column entries with a type of `voronoi`. Every voronoi cell exposes a set of values to scripts, and may choose which column value is responsible for supplying each value. Different cells may delegate their values to different column values.

The `exports` property of voronoi settings maps values to column values. Every key in this property must match one of the exported values in the voronoi-typed column entry which uses it. The value associated with the key is the namespace and path of the column value which will compute this value.

But sometimes, the column value depends on other column values which aren't shared between voronoi cells, and aren't really sensible to be global on the column itself. This is where the `enables` property comes in. It's an array of strings, where each element is the namespace and path of a column value which is "part of" this voronoi settings, regardless of whether or not it is exported. The same column value can be enabled by multiple voronoi settings. If a column value is enabled by a voronoi setting, it will not be exposed to scripts EXCEPT when that script is part of the same voronoi cell. The enables property must contain every column value which is exported.

The last two properties on voronoi settings are considerably simpler:
* `weight` - the relative likeliness of this voronoi settings being selected by the column value which owns it. Must be greater than 0.
* `owner` - the column value which can select this voronoi settings. The column value must be of type voronoi.