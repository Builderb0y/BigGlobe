Chunk generators of type `bigglobe:scripted` allow data pack makers to place blocks in the world based on several scripts. Scripted chunk generators have the following properties:
* `biome_source` - can be a [vanilla biome source](https://minecraft.wiki/w/Dimension_definition#Biome_sources) or Big Globe's "scripted" biome source. Scripted biome sources have the following properties:
	* `type` - must be set to `bigglobe:scripted`.
	* `script` - a script which returns the biome for the current position. This script has the following environments available:
		* MathScriptEnvironment
		* StatelessRandomScriptEnvironment
		* GridScriptEnvironment
		* MinecraftScriptEnvironment
		* BaseColumnScriptEnvironment
		* ColumnEntryRegistry
		* ColorScriptEnvironment
		* ExternalImageScriptEnvironment
		* ExternalDataScriptEnvironment

		And a variable `y` which represents the Y level that the biome is being queried at.
	* `all_possible_biomes` - a biome tag containing all the biomes that this script could potentially return. The `#` is implicit, and you only need to provide the namespace and path of the tag.
* `reload_dimension` (optional) - Normally, chunk generators are stored in level.dat. This can be desirable, because if I change something small about the chunk generator in a new version, old worlds won't necessarily break. On the other hand, it makes development harder, since I need to remember to edit my level.dat file all the time. Also, if I change something bigger in a way which is incompatible with the old spec, or if you do legitimately want to update a world to a newer version of Big Globe for any other reason, you may have to resort to editing level.dat yourself, which is also undesirable. That's where `reload_dimension` comes in. If present, it specifies the dimension inside the world preset inside the Big Globe mod jar which contains the most up-to-date version of this chunk generator. If Big Globe can find this dimension inside the mod jar, then Big Globe will use the chunk generator from the mod jar instead of the one from level.dat.

	In 3.x, the reload dimension was automatically inferred from the chunk generator type. But this posed a problem: how would datapack makers indicate that their own custom world presets should NOT be overridden by the default ones? At the time, I simply made this a config option. In 4.0, all 3 dimensions started using the same chunk generator type, so I now needed a way to distinguish them. Hence why this property was added. And in 4.3.0, I broke existing worlds again, which would've required users to enable that config option. But then I realized something: now that I have this property, data pack makers can just remove it if they don't want their world presets to be overridden. And that meant I didn't need the config option anymore. So, the config option was removed, and users do not need to enable it to update old worlds anymore.

* `reload_preset` (added in V4.3.2) (optional) - Similar to `reload_dimension` but specifies which preset to use, rather than the dimension within the preset. This should be removed in world presets added by data packs too.

	This was added to make my "experiments" preset easier to use.

* `height` - the Y range where this chunk generator could potentially place blocks. It should match the range specified in the dimension_type which this chunk generator is attached to. I don't know why Minecraft has them separate.
	* `min_y` - the minimum Y level (inclusive) that this chunk generator can place blocks at.
	* `max_y` - the maximum Y level (exclusive) that this chunk generator can place blocks at.
	* `sea_level` (optional) - the sea level reported by this chunk generator. This can affect some game mechanics. If absent, the sea level defaults to min_y.
* `world_traits` (optional, added in V4.3.3) - a map containing the traits this generator implements. The keys and values of this map are as follows:
	* (key) - the namespace and path of a trait.
	* (value) - a specification for how to compute the value of this trait, and (optionally) how to set it from an overrider. The value can take one of two different forms:
		* The short way: if the value is a string or list of strings, then it is treated as a script which dictates how to get the value associated with this trait.
		* The long way: if the value is another map, then it may contain the following properties:
			* `get` - a script that dictates how to get the value associated with this trait. Basically the short way goes here.
			* `set` (optional) - a script that dictates how to set the value associated with this trait, for overriders.

		In any case, the scripts have the following environments available:
		* MathScriptEnvironment
		* StatelessRandomScriptEnvironment
		* MinecraftScriptEnvironment
		* BaseColumnScriptEnvironment
		* ColumnEntryRegistry

		If the trait is 3D, then a variable named `y` of type `int` is also available, which contains the Y level being requested.

		If the script is for the `set` property, then a variable named `value` of the same type as the trait is also available, which represents the value to be assigned to whatever underlying column value this trait delegates to.

	**New in V4.4.0:** world_traits is now the namespace and path of a file containing the traits. `modid:example` would load the traits from `/data/modid/worldgen/bigglobe_world_traits/example.json`. If multiple data packs provide this file, they will be merged at runtime. If multiple data packs provide the same trait within the file, the top data pack takes priority.
* `layer` - the root layer responsible for placing blocks in raw terrain. The root layer fills the entire column with a single block type. Placing additional blocks must be done by attaching children to the layer. See the documentation on layers for more info on how they work.
	* `state` - a script which returns a BlockState to fill the column with. This script has the following environments present:
		* MathScriptEnvironment
		* StatelessRandomScriptEnvironment
		* GridScriptEnvironment (with implicit seed)
		* MinecraftScriptEnvironment
		* BaseColumnScriptEnvironment
		* ColumnEntryRegistry (x and z are hard-coded to the position currently being generated)
		* ColorScriptEnvironment
		* ExternalImageScriptEnvironment
		* ExternalDataScriptEnvironment
	* `children` - an array of layers. Defaults to an empty array if absent.
	* `before_children` and `after_children` (optional) (added in V4.3.0) - scripts which can place additional blocks before and after the children place blocks. See the documentation on layers for more info.

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
	* ColorScriptEnvironment
	* ExternalImageScriptEnvironment
	* ExternalDataScriptEnvironment
* `colors` (optional) - controls the colors of some blocks.
	* `grass` (optional) - controls the color of grass, tallgrass, and related blocks.
	* `foliage` (optional) - controls the colors of leaves (I think).
	* `water` (optional) - controls the color of water.

	All 3 of the above scripts have the following environments available:
	* MathScriptEnvironment
	* StatelessRandomScriptEnvironment
	* MinecraftScriptEnvironment
	* BaseColumnScriptEnvironment
	* ColorScriptEnvironment
	* ColumnEntryRegistry (x and z are hard-coded at the position of the block being colored, y defaults to the Y level of the block being colored)

	They also have the following variables:
	* `y` - the Y level of the block being colored.

	And the following functions:
	* `getDefaultGrassColor(double temperature, double foliage)` - returns the color that minecraft would normally use for grass if this script were absent and the block were in a biome with the provided temperature and foliage. temperature foliage should be in the range 0 to 1.
	* `getDefaultFoliageColor(double temperature, double foliage)` - same as getDefaultGrassColor(), but for foliage.

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
			* ColorScriptEnvironment
			* ExternalImageScriptEnvironment
			* ExternalDataScriptEnvironment