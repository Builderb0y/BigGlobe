# Variables

* `x` - the x coordinate of the current column.
* `z` - the z coordinate of the current column.
* `minCachedYLevel` - the minimum Y level (inclusive) that this column will ever cache. Cached column values will only ever be cached inside this range, even if they are valid outside this range.
* `maxCachedYLevel` - the maximum Y level (exclusive) that this column will ever cache. Cached column values will only ever be cached inside this range, even if they are valid outside this range.
* `purpose` - a String representing what this column is being used for. It may be one of the following:
	* "heightmap" - the user of this column only cares about the top block in the column. Scripts which compute other values (like caves) can skip them when purpose == 'heightmap'. Scripts can also choose to omit certain blocks from generation when only the heightmap is cared about. For example, the overworld skips skylands when only the heightmap is cared about, and the end skips void clouds.
	* "raw_generation" - the column is being used for the raw generation stage of terrain generation.
	* "features" - the column is being used for the features stage of terrain generation.
	* "generic" - the column is being used for something else.
* `distantHorizons` - true when this column is being used to generate Distant Horizons or Voxy terrain, false for normal terrain.
	* Scripts can use this variable to skip finer details of terrain gen when they won't be as visible. In particular, underground features are a good candidate for being skipped in DH/voxy terrain.
* `surfaceOnly` - same as `purpose == 'heightmap'`. I'm not sure why I have this. Might be for legacy reasons or something and it never got removed.
* `worldSeed` - a long derived from the world seed. This is not the actual seed because some noise grids and scripts are sent to the client for block tinting, and they won't work properly without the seed. But I don't want to send the actual world seed to the client because that would allow anyone to find the seeds of multiplayer servers and gain an unfair advantage. So, the seed exposed to scripts is a secure hash of the actual world seed.
* `columnSeed` = a long derived from the hashed world seed and the column's x and z position.

# Functions

* `columnSeed(long salt)` returns a long derived from the hashed world seed, the salt, and the column's x and z position.