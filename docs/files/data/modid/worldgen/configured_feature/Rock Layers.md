Rock layers are things like andesite, diorite, granite, etc. They spawn underground and replace stone. Rock layers are configured features which must be added to the rock replacers section in a scripted chunk generator to function. For convenience, a tag for each dimension already exists for you to add rock layers to. Rock layer json files have the following properties:

* `type` - must be `bigglobe:rock_layer`.
* `config` - has the following properties:
	* `seed` - the random number generator (RNG) seed used for querying the noise to determine what Y level the rock spawns at.
	* `repeat` - the average distance between rock layers of this type.
	* `entries` - the blocks that can be placed by this rock layer. This is a variations list, which means it supports defaults/variations for reducing duplicated text. Each object in this list contains the following properties:
		* `weight` - the relative likelihood of this entry being selected for a given base Y level.
		* `center` - a 2D noise source that specifies how far above or below the base Y level the center of the layer should be.
		* `thickness` - another 2D noise source that specifies how far above or below the center Y level to actually place blocks.
		* `blocks` - an object containing the blocks to place. The keys of this object are the names of blocks to find, and the values are names of blocks to replace them with.
		* `restrictions` (optional) - a column restriction specifying where this layer should appear in the world. Sandstone uses this to only spawn below deserts.