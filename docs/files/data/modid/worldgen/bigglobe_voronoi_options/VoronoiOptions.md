# This file contains documentation on an UPCOMING feature intended for Big Globe 6.0.0. It is not fully finalized yet, and may change before then.

Voronoi options work like tags, but specifically for `class/voronoi`-typed ElementSpec's. The primary difference from normal tags is that each element also specifies a weight. A secondary difference is that each element can either add to the results from the previous data pack, or remove from them. Voronoi options have the following properties:
* `replace` - if true, the contents of the previous data pack are discarded before new values are added (or removed). Defaults to false.
* `values` - an array of values to add or remove. Each value has the following properties:
	* `operation` - may be `add` or `remove`. Defaults to `add`. Controls whether this element is added to or removed from the final set.
	* `class` - a reference to an ElementSpec of type `class/voronoi`.
	* `weight` - the relative likelihood of this class being selected. The exact chance is this weight divided by the sum of all weights. The default weight is 1.0.

When multiple data packs provide voronoi options for the same column entry, they will be processed in the order that the data packs are enabled in.