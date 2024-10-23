# Variables

* `x` - the x coordinate of the current column.
* `z` - the z coordinate of the current column.
* `minCachedYLevel` - the minimum Y level (inclusive) that this column will ever cache. Cached column values will only ever be cached inside this range, even if they are valid outside this range.
* `maxCachedYLevel` - the maximum Y level (exclusive) that this column will ever cache. Cached column values will only ever be cached inside this range, even if they are valid outside this range.
* `purpose` (deprecated, replaced with `hints.usage`) - a String representing what this column is being used for. It may be one of the following:
	* "heightmap" - the user of this column only cares about the top block in the column. Scripts which compute other values (like caves) can skip them when purpose == 'heightmap'. Scripts can also choose to omit certain blocks from generation when only the heightmap is cared about. For example, the overworld skips skylands when only the heightmap is cared about, and the end skips void clouds.
	* "raw_generation" - the column is being used for the raw generation stage of terrain generation.
	* "features" - the column is being used for the features stage of terrain generation.
	* "generic" - the column is being used for something else.
* `distantHorizons` (deprecated, replaced with `hints.isLod`) - true when this column is being used to generate Distant Horizons or Voxy terrain, false for normal terrain.
	* Scripts can use this variable to skip finer details of terrain gen when they won't be as visible. In particular, underground features are a good candidate for being skipped in DH/voxy terrain.
* `surfaceOnly` (deprecated, replaced with `!hints.fill`) - same as `purpose == 'heightmap'`. I'm not sure why I have this. Might be for legacy reasons or something and it never got removed.
* `worldSeed` - a long derived from the world seed. This is not the actual seed because some noise grids and scripts are sent to the client for block tinting, and they won't work properly without the seed. But I don't want to send the actual world seed to the client because that would allow anyone to find the seeds of multiplayer servers and gain an unfair advantage. So, the seed exposed to scripts is a secure hash of the actual world seed.
* `columnSeed` - a long derived from the hashed world seed and the column's x and z position.
* `hints` (upcoming) - an object of type Hints which provides some hints about what worldgen is desired and what can be skipped.

# Fields

* `hints.fill` (upcoming) - If true, the entire column is desired. If false, only the surface is desired.
* `hints.carve` (upcoming) - If true, caves and other things that carve through the terrain are desired. If false, only the terrain itself is desired.
* `hints.decorate` (upcoming) - If true, structures and features that spawn underground are desired. If false, they are not desired. Note that this only applies to UNDERGROUND structures and features. Above-ground structures and features should always spawn.
* `hints.isLod` (upcoming) - If true, this terrain is being generated for Distant Horizons or Voxy. If false, this terrain is being generated for normal Minecraft.
* `hints.lod` (upcoming) - The detail level currently being generated. For normal Minecraft terrain, this is 0. For Distant Horizons or Voxy terrain, this could be 0 or another number greater than 0. It will never be negative.
* `hints.distanceBetweenColumns` (upcoming) - Equal to `1 << hints.lod`. For normal Minecraft terrain, every column is generated, so the distance between columns is 1. But Big Globe cuts corners for Distant Horizons and Voxy for terrain that's really far away by only generating every other colum, or every 4'th column, or every 8'th column, and pretending everything between matches the nearest column generated. When you get closer, more columns are generated. Anyway, this field tells you the exact distance between columns being generated right now.
* `hints.usage` (upcoming) - Mostly equal to the old `purpose` variable, but it's an enum instead of a string. It has auto-casting though, so you just need to use `==.` or `!=.` instead of `==` or `!=` when comparing it to a string. The benefit is that you get a compile-time error for unknown usages instead of things just not doing what you want at runtime.

# Functions

* `columnSeed(long salt)` returns a long derived from the hashed world seed, the salt, and the column's x and z position.

# Types

* `Hints` (upcoming) - hints for what generation is desired, and what generation can be skipped.

# Casting

* `String -> Usage` - intended for comparing `hints.usage` to things.