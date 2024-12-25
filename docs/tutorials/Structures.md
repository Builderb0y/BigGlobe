Structures are stored in `/data/modid/worldgen/structure/`, so the first thing you'll want to do when creating a compatibility data pack for another mod's structures is to look at what structures the mod actually has. The 2nd thing to do is look at what biomes each structure can spawn in. This is the "biomes" property in the structure json file. If the mod is doing things correctly, it will say `"biomes": "#modid:has_structure/(name of structure)"`. In this case, all you need to do to make the structure spawn is add the relevant Big Globe biomes to that tag. Create the file `/data/modid/tags/worldgen/biome/has_structure/(name of structure).json` and put the following in it:
```json
{
	"replace": false,
	"values": [
		
	]
}
```
and put the biomes you want the structure to spawn in between the square brackets. For example, if you wanted the structure to spawn in the temperate plains biome and the warm plains biome, your file should look like this:
```json
{
	"replace": false,
	"values": [
		"bigglobe:temperate_plains",
		"bigglobe:warm_plains"
	]
}
```
You can list as many biomes as you want here.

# What if the mod doesn't have its own tag for its structure?

If the mod is not doing things correctly, another common thing you might find in the structure json is `"biomes": "#minecraft:is_forest"` or something else from the minecraft namespace. If this happens, you have 2 options: either override the structure, or delegate the structure.

Overriding is pretty simple: you simply copy-paste the structure json file into your own data pack and change "biomes" to point to a tag you control. There are some down sides to this method though:
* This can change where the structure spawns in vanilla worlds. Though if you're making a compatibility pack, then I'm assuming you only plan on using it inside a Big Globe world.
* If the mod updates and changes the structure, your compatibility pack may need to be updated too.
* If the mod has a restrictive license, copying its files could get you into legal trouble. Always check the license before copying things from other mods.

Delegating solves all of these problems: a delegating structure is one which is identical to another structure, but with some slightly different properties. A delegating structure looks like this:
```json
{
	"type": "bigglobe:delegating",
	"delegate": "modid:example_structure",
	"biomes": "#modid:has_structure/example_structure"
}
```
Simply add a structure like this to your compatibility pack, along with a biome tag for it to spawn in, and a structure set to control how often it spawns, and you should be good to go.

Delegating structures can also tweak the spawn_overrides, the step, and the terrain_adaptation, just like for any vanilla structures. Delegating structures cannot override any other properties of the structure they're delegating to.

# What if the structure spawns weirdly or at a bad Y level?

For this, I have structure overriders. This is a slightly more advanced topic which requires a bit of scripting knowledge. If you don't know how Big Globe's scripting language works, go read the docs for that first.

Structure overriders are found in `/data/modid/worldgen/bigglobe_overrider/`, and **must be added to the current chunk generator's overrider tag to function**. There are currently 3 types of them: collision, structure, and column_value.

## Collision overriders

Collision overriders are used when 2 structures intersect with each other. Big Globe's normal logic says that when this happens, the larger of the two structures (by volume) should spawn in the world, and the smaller should not. Collision overriders allow you to tweak this logic.

Collision overriders have the following json properties:
* `type` - must be `collision`.
* `script` - a script which decides which of the two structures (if either) should spawn in the world. This script has the following environments present:
	* JavaUtilScriptEnvironment
	* MathScriptEnvironment
	* RandomScriptEnvironment
	* StatelessRandomScriptEnvironment
	* GridScriptEnvironment
	* MinecraftScriptEnvironment
	* StructureScriptEnvironment
	* NbtScriptEnvironment
	* ColumnEntryRegistry

	And the following additional fields:
	* ScriptStructurePiece.data

	And the following additional variables:
	* currentStart - a StructureStart representing the structure that is trying to spawn right now.
	* otherStart - a StructureStart representing the other structure that currentStart collided with.
	* hints - the current terrain generation hints.

	This script returns one of the following:
	* A positive int, to indicate that the current start should spawn.
	* A negative int, to indicate that the current start should not spawn.
	* Zero, to indicate that this overrider has no preference for this specific pair of structures. Control then falls upon the next collision overrider, or default size-based logic if there are no more collision overriders remaining.

## Structure overriders

Structure overriders are used when a structure spawns at a bad location. Structure overriders can then decide to move the structure to a better location, or prevent it from spawning entirely if the spawn location cannot be corrected. Movement is limited to vertical-only.

Structure overriders have the following json properties:
* `type` - must be `structure`
* `script` - a script which can move the structure vertically, or prevent it from spawning. This script has the following environments present:
	* JavaUtilScriptEnvironment (with implicit random)
	* MathScriptEnvironment
	* RandomScriptEnvironment (with implicit random)
	* StatelessRandomScriptEnvironment
	* GridScriptEnvironment (with implicit seed)
	* MinecraftScriptEnvironment (with implicit random)
	* StructureScriptEnvironment
	* NbtScriptEnvironment
	* ColumnEntryRegistry

	And the following additional variables:
	* `start` - the current StructureStart being overridden.
	* `hints` - the current worldgen hints.

	And the following additional fields:
	* ScriptStructurePiece.data

	And the following additional methods:
	* `StructureStart.move(int yOffset)` - moves the start yOffset blocks up from its current position. If yOffset is negative, the structure moves down instead.
	* `StructureStart.moveToRange(optional Random random, int*(minY, maxY))` - moves the start to a random Y level between minY and maxY. Returns true if the structure fits in this range, and false otherwise.

	The overrider should return true if the structure is allowed to spawn at this location, and false if it should not spawn.

## Column value overriders

Column value overriders are used when a structure spawns fine, but the terrain around it doesn't look quite right. For example, when a house spawns at ground level, but the ground isn't flat, so the house looks like it's floating on one side.

Column value overriders have the following json properties:
* `type` - must be `column_value`
* `script` - a script which modifies column values to fit nearby structures better. This script has the following environments present:
	* MathScriptEnvironment
	* StatelessRandomScriptEnvironment
	* GridScriptEnvironment
	* MinecraftScriptEnvironment
	* ScriptedColumnBaseEnvironment
	* ColumnEntryRegistry
	* ColorScriptEnvironment
	* ExternalImageScriptEnvironment
	* ExternalDataScriptEnvironment
	* StructureScriptEnvironment
	* NbtScriptEnvironment (immutable)
	* JavaUtilScriptEnvironment

	And the following additional variable:
	* `structures` - the List of structures intersecting the current chunk. Not all of them will have pieces intersecting the current column. Check for this and handle it sanely.

	And the following additional functions:
	* `distanceToSquare()` returns the distance from the current column to a square defined by one of the following:
		* A StructureStart
		* A StructurePiece
		* 4 doubles (minX, minZ, maxX, maxZ)
	* `distanceToCircle()` returns the distance from the current column to the edge of a circle. This does NOT return the distance to the circle's center point. If the current column is inside the circle, this function returns 0. The circle may be defined by one of the following:
		* A StructureStart - the circle has its center at the center of the start, and is as big as it can be while still being fully contained within the start. An optional extra double parameter can be provided to set the radius of the circle.
		* A StructurePiece - the circle has its center at the center of the piece, and is as big as it can be while still being fully contained within the piece. An optional extra double parameter can be provided to set the radius of the circle.
		* 3 doubles (centerX, centerZ, radius)

	And the following additional field:
	* ScriptStructurePiece.data

	This script returns nothing.