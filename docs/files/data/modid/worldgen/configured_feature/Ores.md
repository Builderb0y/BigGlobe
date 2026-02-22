# Regular ores

## Json structure

* `type` - must be `bigglobe:ore`
* `config`:
	* `seed` - a unique number or string. I recommend setting this to the name of the file.
	* `blocks` - a map of which blocks to find, and which blocks to replace them with. The keys and values of this map use the same syntax as /setblock. The keys may omit block properties. The values will log a warning on missing properties, and choose the default value for each missing property.
	* `chance` - a script returning a double between 0 and 1 which determines how likely the ore is to spawn at any given location. Default generation will attempt to place one ore inside every chunk section (16x16x16 area), and fail randomly based on chance. If the script returns a number outside the 0-1 range, the ore will behave as if the number was clamped to the 0-1 range first. If the script returns NaN, it will be treated as 0. This script has the following environments available:
		* MathScriptEnvironment
		* StatelessRandomScriptEnvironment
		* GridScriptEnvironment (with an implicit seed)
		* MinecraftScriptEnvironment
		* BaseColumnScriptEnvironment
		* ColumnEntryRegistry
		* ColorScriptEnvironment

		And the following additional variable:
		* y - the Y level the ore is attempting to spawn at.

		Note that x and z are provided by BaseColumnScriptEnvironment.
	* `core_chance` (upcoming) (optional) - a script returning a double between 0 and 1 which determines how likely the ore is to spawn when a molten rock is cooled by water.
		* Has the same environments and variables as `chance`.
		* When this value is absent, the regular chance is used by molten rock blocks.
		* Note that molten rock uses a weighted random system, so the returned chance is effectively divided by the total core chance of all ores.
			* This has the implication that even if every ore has a very low chance of spawning, one of them will still be selected anyway.
	* `radius` - a RandomSource returning a number between 0 and 16 which determines how big the ore is.

# Other requirements

The chunk generator must know to place this ore in the world, otherwise it won't spawn. This feature is a rock replacer, and must be directly or indirectly present in the `rock_replacers` section of the feature dispatcher. All built-in scripted chunk generators have a tag for this purpose, meaning that you can add this feature to the tag that your chunk generator uses for ores, and it'll work.

# Generic ores

If the above requirements are too strict and you need to place an ore feature in a non-scripted world, or with commands, you can change the `type` to `bigglobe:generic_ore`, and it will behave just like any other configured feature. Generic ores have the following json differences compared to normal ores:
* `seed` is ignored.
* `chance` is ignored. If you tell the ore to spawn somewhere with a placed feature or command, it'll always spawn there.
* `blocks` is renamed to `states`.

## Notes

Regular ores, when used as a rock replacer, are more efficient than generic ores, and are multi-threaded. Generic ores are less efficient, and single-threaded.

# Scripted ores (New in V4.8.3)

When `type` is set to `bigglobe:scripted_ore`, a script determines what blocks to replace with what other blocks. In this type, the `blocks` property is removed, and a new `replacer_script` property is added. The replacer script has the following environments available:
* MathScriptEnvironment
* StatelessRandomScriptEnvironment
* GridScriptEnvironment
* MinecraftScriptEnvironment
* BaseColumnScriptEnvironment
* ColumnEntryRegistry
* ColorScriptEnvironment

And the following additional variables:
* `BlockState oldState` - the state being replaced.
* `int*(blockX, blockY, blockZ)` - the position of the block being replaced.
* `long blockSeed` - a random number based on the block position and the seed of the ore.
* `double*(centerX, centerY, centerZ)` - the center of the ore vein.
* `double radius` - the radius of the ore vein.
* `double radialFraction` - how close the block is to the center of the vein. 0 indicates that the block is at the center of the vein, while 1 indicates that the block is on the edge of the vein.

The script will be called for every block inside the vein, and is expected to return a BlockState to replace oldState with. If the script returns null, oldState is left unchanged.

## Notes

The other ore types have a `(1.0 - radialFraction ^ 2) ^ 2` chance of modifying the current block, so if you want to match that, now you know the curve for it.

Column values are available for scripted ores, but the column used will be positioned at the center of the vein, not at the current block being replaced. The `x` and `z` variables typically obtained from the column will reflect this. Despite this, the Y level of 3D column values defaults to the Y level of the block being replaced, not the Y level of the center of the vein. The Y level of 3D column values can also be manually specified as usual.

Scripted ores are slightly less efficient than regular ores, but are still multi-threaded.

# Interactions with molten rock

When cooling molten rock, it can turn into a random ore. But you might wonder, how does it decide which ore to turn into? Well, the answer is simple: it can turn into any ore present in the feature dispatcher for the current dimension, weighted based on the chance of the ore spawning at the molten rock's location. This also means that it will never turn into a generic ore, and it will never turn into anything except stone in non-scripted worlds.

## Scripted ore interactions with molten rock

Scripted ores work mostly the same as regular ores when it comes to molten rocks turning into them, but there are a few important differences:

* `oldState` will always be stone.
* `blockSeed` will be completely random and unrelated to position or the ore's seed.
* `centerX`, `centerY`, and `centerZ` will be set to `blockX`, `blockY`, and `blockZ` respectively.
* `radius` will be set to 1.0, regardless of whether or not this is in the range specified by the feature's config.
* `radialFraction` will be set to a random number between 0 and 1.