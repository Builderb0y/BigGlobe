Chunk generators of type `bigglobe:scripted` allow data pack makers to place blocks in the world based on several scripts. Scripted chunk generators have the following properties:
* `reload_dimension` (optional) - Normally, chunk generators are stored in level.dat. This can be desirable, because if I change something small about the chunk generator in a new version, old worlds won't necessarily break. On the other hand, it makes development harder, since I need to remember to edit my level.dat file all the time. Also, if I change something bigger in a way which is incompatible with the old spec, or if you do legitimately want to update a world to a newer version of Big Globe for any other reason, you may have to resort to editing level.dat yourself, which is also undesirable. That's where `reload_dimension` comes in. If present, it specifies the dimension inside the world preset inside the Big Globe mod jar which contains the most up-to-date version of this chunk generator. If Big Globe can find this dimension inside the mod jar AND "Reload Big Globe chunk generators from mod jar" is enabled in Big Globe's config file, then Big Globe will use the chunk generator from the mod jar instead of the one from level.dat.
* `height` - the Y range where this chunk generator could potentially place blocks. It should match the range specified in the dimension_type which this chunk generator is attached to. I don't know why Minecraft has them separate.
	* `min_y` - the minimum Y level (inclusive) that this chunk generator can place blocks at.
	* `max_y` - the maximum Y level (exclusive) that this chunk generator can place blocks at.
	* `sea_level` (optional) - the sea level reported by this chunk generator. This can affect some game mechanics. If absent, the sea level defaults to min_y.
* `layer` - the root layer responsible for placing blocks in raw terrain. The root layer fills the entire column with a single block type. Placing additional blocks must be done by attaching children to the layer. See the documentation on layers for more info on how they work.
	* `state` - a script which returns a BlockState to fill the column with. This script has the following environments present:
		* MathScriptEnvironment
		* StatelessRandomScriptEnvironment
		* GridScriptEnvironment (with implicit seed)
		* MinecraftScriptEnvironment
		* BaseColumnScriptEnvironment
		* ColumnEntryRegistry (x and z are hard-coded to the position currently being generated)
	* `children` - an array of layers. Defaults to an empty array if absent.
	* `before_children` and `after_children` (optional) (added in V4.2.1 or V4.3.0, whichever one gets released first) - scripts which can place additional blocks before and after the children place blocks. See the documentation on layers for more info.

	Note: the root layer does not have a `valid` property. If you provide one anyway, it will be ignored.
* `feature_dispatcher` - has the following properties:
	* `rock_replacers` - array containing strings which represent either a configured feature, or a tag of configured features. All configured features in this array must have one of the following types:
		* `bigglobe:chunk_sprinkle`
		* `bigglobe:molten_rock`
		* `bigglobe:bedrock`
		* `bigglobe:rock_layer`
		* `bigglobe:ore`

		Any configured features in the array which do not have one of these types will be ignored. Rock replacers run after layers, and run in the order they are declared in this array. If an element in the array is a tag, then the replacers in that tag will run in alphabetical order. Sorted by path first, then by namespace.
	* `raw` and `normal` - a string representing the namespace and path of a feature dispatcher. Raw runs first, and has access to only one chunk at a time. Normal runs later, after a 3x3 area of chunks is available.

		Note: all configured features placed directly or indirectly by raw must be of type `bigglobe:single_block` or `bigglobe:scripted`. Any other configured feature types will not do anything during raw generation. This restriction does not apply to normal generation.
* `overriders` - a tag containing all the overriders which should be applied to structures or column values. Overriders are applied in alphabetical order, sorted by path first, then by namespace.
* `spawn_point` (optional) - a script which returns true if a player can spawn at some position. Big Globe will generate (potentially) many different random positions within (.minecraft/config/bigglobe/Big Globe.json5 > Player Spawning > Max Spawn Radius) blocks of (0, 0), and evaluate this script on them until it returns true, or the maximum number of attempts (currently 1024) is reached. If this script is absent or returns false more than 1024 times in a row, then vanilla spawning logic is used instead, which may place players at undesired locations. This script has the following environments available:
	* MathScriptEnvironment
	* StatelessRandomScriptEnvironment
	* GridScriptEnvironment (with implicit seed)
	* MinecraftScriptEnvironment (with random)
	* BaseColumnScriptEnvironment
	* ColumnEntryRegistry (x and z are hard-coded at the current position being tested to see if it's a good spawn position or not)
	* RandomScriptEnvironment (with random)
* `colors` (optional) - controls the colors of some blocks.
	* `grass` (optional) - controls the color of grass, tallgrass, and related blocks.
	* `foliage` (optional) - controls the colors of leaves (I think).
	* `water` (optional) - controls the color of water.

	All 3 of the above scripts have the following environments available:
	* MathScriptEnvironment
	* StatelessRandomScriptEnvironment
	* MinecraftScriptEnvironment
	* BaseColumnScriptEnvironment
	* ColumnEntryRegistry (x and z are hard-coded at the position of the block being colored, y defaults to the Y level of the block being colored)

	They also have the following variables:
	* `y` - the Y level of the block being colored.

	And the following functions:
	* `getDefaultGrassColor(double temperature, double foliage)` - returns the color that minecraft would normally use for grass if this script were absent and the block were in a biome with the provided temperature and foliage. temperature foliage should be in the range 0 to 1.
	* `getDefaultFoliageColor(double temperature, double foliage)` - same as getDefaultGrassColor(), but for foliage.
	* `packI(int red, int green, int blue)` - packs the 3 values into a single color. red, green, and blue are expected to be between 0 and 255 (inclusive).
	* `packF(float red, float green, float blue)` - packs the 3 values into a single color. red, green, and blue are expected to be between 0 and 1.
	* `packD(double red, double green, double blue)` packs the 3 values into a single color. red, green, and blue are expected to be between 0 and 1.

	And the following fields:
	* `color.redI`, `color.greenI`, and `color.blueI` - the red, green, or blue channel of the color, as an int, in the range 0 to 255 (inclusive).
	* `color.redF`, `color.greenF`, and `color.blueF` - the red, green, or blue channel of the color, as a float, in the range 0 to 1.
	* `color.redD`, `color.greenD`, and `color.blueD` - the red, green, or blue channel of the color, as a double, in the range 0 to 1.

	Internally, colors are represented as ints, with 8 bits used for each channel, and 8 bits leftover which are unused. But the order of the channels is un-specified. The above packing functions should be used to create colors, and the above unpacking fields should be used to retrieve a specific channel.
* `nether_overrides` (optional) - has the following properties:
	* `place_portal_at_high_y_level` - if true, Big Globe will override nether portal placement logic to place the portal as high as possible. If Big Globe fails to find a suitable nether portal location, vanilla logic will be used as a fallback.
* `end_overrides` (optional) - has the following properties:
	* `spawning`- has the following properties:
		* `location` - an array of 3 ints which overrides the obsidian platform position.
		* `obsidian_platform` - true to spawn the platform itself, false to not do that.
	* `inner_gateways` - has the following properties:
		* `radius` - distance from the origin to spawn gateways at when the dragon dies.
		* `height` - the Y level to spawn gateways at when the dragon dies.
	* `outer_gateways` - has the following properties:
		* `min_radius` - the minimum distance from the origin that the game will try to place an exit gateway at when you throw an ender pearl at one of the inner ones.
		* `max_radius` - the maximum distance from the origin that the game will try to place an exit gateway at before giving up.
		* `step` - the distance between attempts to spawn an exit gateway.
		* `condition` - a script which returns true if an exit gateway can be placed at a given x/z position, false otherwise. This script has the following environments present:
			* MathScriptEnvironment
			* StatelessRandomScriptEnvironment
			* GridScriptEnvironment (with implicit seed)
			* MinecraftScriptEnvironment
			* BaseColumnScriptEnvironment
			* ColumnEntryRegistry (x and z are hard-coded at the position being tested)