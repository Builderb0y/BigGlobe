# Variables

No variables on the "base" MinecraftScriptEnvironment, *however*...

## With world

When a world is present, the following variables can be referenced:

* `worldSeed` - the seed of the world, as a long.
* `minValidYLevel` - the minimum Y level of the underlying world. Note that there may be additional limitations about where you can query or place blocks which may differ from this value.
* `maxValidYLevel` - the maximum Y level of the underlying world. Note that there may be additional limitations about where you can query or place blocks which may differ from this value.

# Fields

* `id` - applicable to Block, Biome, ConfiguredFeature, and Tag; returns a String representation of the object's name which includes its namespace and its path, separated by a colon.
* `Biome.temperature` and `Biome.downfall` - the "temperature" and "downfall" properties in the biome json file.
* `BlockState.(any name here)` - returns the value of the property with the given name, as a *generic* instance of Comparable, or null if no such property exists with the given name. Example:
	```
	BlockState state = 'minecraft:snow[layers=1]'
	int layers = state.layers ;1
	```

# Functions

No functions on the "base" MinecraftScriptEnvironment, *however*...

## With world

When a world is present, the following functions can be called:

* `getBlockState(int x, int y, int z)` - returns the BlockState at the provided position.
* `setBlockState(int x, int y, int z, BlockState state)` - changes the BlockState at the provided position.
* `setBlockStateReplaceable(int x, int y, int z, BlockState state)` - changes the BlockState at the provided position if, and only if, the current BlockState that's already at that position is replaceable.
* `setBlockStateNonReplaceable(int x, int y, int z, BlockState state)` - changes the BlockState at the provided position if, and only if, the current BlockState that's already at that position is NOT replaceable.
* `placeBlockState(int x, int y, int z, BlockState state)` - similar to `setBlockState(x, y, z, state)`, with 3 differences:
	* `state.canPlaceAt(x, y, z)` is called for you automatically, and the block will not be set if it isn't able to be placed at the provided position.
	* If the block is meant to be 2 blocks tall (doors, sunflowers, etc...), then both halves are placed. With setBlockState(), you only get one half (unless you call the function twice).
	* For waterloggable states, the state's waterlogged status will be updated to match whether or not there's already water at the given position before being placed.
	
	These two differences stack with each other btw. If the block is 2 blocks tall, and is waterloggable, then both halves will check for water and update their states accordingly.
* `fillBlockState(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state)` - similar to the `/fill` command; this function sets block states in a cuboid region of blocks.
* `fillBlockStateReplaceable(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state)` - similar to `fillBlockState(x1, y1, z1, x2, y2, z2, state)`, but will only place blocks where the existing block already at that position is replaceable.
* `fillBlockStateNonReplaceable(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state)` - similar to `fillBlockState(x1, y1, z1, x2, y2, z2, state)`, but will only place blocks where the existing block already at that position is NOT replaceable.
* `placeFeature(int x, int y, int z, ConfiguredFeature feature)` - works like the `/place feature` command; places the feature at the provided coordinates.
* `isYLevelValid(int y)` - returns true if y is between `minValidYLevel` and `maxValidYLevel`, false otherwise.
* `isPositionValid(int x, int y, int z)` - returns true if y is between `minValidYLevel` and `maxValidYLevel`, and the chunk at the provided x/z position is loaded, false otherwise.
* `getBlockData(int x, int y, int z)` - acts like the `/data get` command; returns an NbtCompound containing the block data at the given position, or null if there is no block entity at the given position.
* `setBlockData(int x, int y, int z, NbtCompound data)` - sets the NBT data of the block at the given position. Any tags that the block needs, but you don't provide, are regenerated. Does nothing if the block at the given position is not a block entity.
* `mergeBlockData(int x, int y, int z, NbtCompound data)` - acts like the `/data merge` command; merges the existing NBT data at the given position with the provided NBT data, then sets it on the block at the given position. Does nothing if the block at the given position is not a block entity.
* `summon(double x, double y, double z, String entityType)` - acts like the `/summon` command; spawns the given entity at the given position.
* `summon(double x, double y, double z, String entityType, NbtCompound nbt)` - also acts like `/summon`, with custom NBT data to apply to the entity when spawning it.

# Methods

* `Block.getDefaultState()` - returns the default BlockState associated with the Block. All properties on the BlockState will be their default values.
* `Block.getRandomState(RandomGenerator random)` - returns a random BlockState associated with the Block. All properties on the BlockState will have random values.
* `Block.getRandomState(long seed)` - returns a random BlockState associated with the Block based on the provided seed. All properties on the BlockState will have random values.
* `Block.isIn(BlockTag tag)` - returns true if the block is in the specified tag, false otherwise.
* `BlockTag.random(RandomGenerator random)` - returns a random Block in this tag. All blocks in the tag are equally likely to be selected.
* `BlockTag.random(long seed)` - returns a random Block in this tag based on the provided seed. All blocks in the tag are equally likely to be selected.
* `BlockState.isIn(BlockTag tag)` - returns true if the state's block is in the provided tag, false otherwise.
* `BlockState.getBlock()` - returns the Block associated with the state.
* `BlockState.isAir()` - returns true for `minecraft:air`, `minecraft:void_air`, and `minecraft:cave_air`, and false for all other block states. Though it's not impossible for other mods to register their own air blocks I guess.
* `BlockState.isReplaceable()` - returns true if attempting to place a block on this block will instead place it inside the block. For example, short grass and water are both replaceable.
* `BlockState.hasWater()` - returns true if the state is water or waterlogged, false otherwise. This method **will be removed** in Big Globe 4.0.
* `BlockState.hasLava()` - returns true for lava blocks, false for all other vanilla blocks, because lavalogging is not a thing in vanilla. Modded blocks may be lavaloggable. This method **will be removed** in Big Globe 4.0.
* `BlockState.hasSoulLava()` - returns true for soul lava blocks, false for all other vanilla blocks, because soullavalogging is not a thing in vanilla. Modded blocks may be soullavaloggable. This method **will be removed** in Big Globe 4.0.
* `BlockState.hasFluid()` - returns true if the state has an associated fluid, false otherwise. This method **will be removed** in Big Globe 4.0.
* `BlockState.blocksLight()` - returns true if you can build a roof out of this block and have it be dark under the roof.
* `BlockState.hasCollision()` - returns true if entities can collide with this block, false otherwise.
* `BlockState.hasFullCubeCollision()` - returns true if the collision shape for this block is a full cube, false otherwise.
* `BlockState.hasFullCubeOutline()` - returns true if the outline shape you get when looking at the block si a full cube, false otherwise.
* `BlockState.rotate(int degrees)` - returns the state rotated by the number of degrees. The number of degrees must be divisible by 90. If the number of degrees is not divisible by 90, then the state is returned as-is.
* `BlockState.mirror(String axis)` - mirrors the state about the given axis. The axis must be the String `"x"` or `"z"`. All other String's will return the state as-is.
* `BlockState.with(String propertyName, Comparable value)` - returns a new BlockState which is identical to the current one, but with the provided property set to the provided value. If the state has no such property with the given name, the state is returned as-is. Example:
	```
	BlockState oneLayer = 'minecraft:snow[layers=1]'
	BlockState twoLayers = oneLayer.with('layers', 2)
	```
* `Biome.isIn(BiomeTag tag)` - returns true if the biome is in the provided tag, false otherwise.
* `BiomeTag.random(RandomGenerator random)` - returns a random Biome in this tag. All biomes in the tag are equally likely to be selected.
* `BiomeTag.random(long seed)` - returns a random Biome in this tag based on the provided seed. All biomes in the tag are equally likely to be selected.
* `ConfiguredFeature.isIn(ConfiguredFeatureTag tag)` - returns true if the biome is in the provided tag, false otherwise.
* `ConfiguredFeatureTag.random(RandomGenerator random)` - returns a random ConfiguredFeature in this tag. All configured features in the tag are equally likely to be selected.
* `ConfiguredFeatureTag.random(long seed)` - returns a random ConfiguredFeature in this tag based on the provided seed. All configured features in the tag are equally likely to be selected.

## With random

When an implicit RandomGenerator object is provided, the following additional methods can be called:

* `Block.getRandomState()` - identical to `Block.getRandomState(implicit random)`
* `BlockTag.random()` - identical to `BlockTag.random(implicit random)`
* `BiomeTag.random()` - identical to `BiomeTag.random(implicit random)`
* `ConfiguredFeatureTag.random()` - identical to `ConfiguredFeatureTag.random(implicit random)`

## With world

When a world is present, the following additional methods can be called:

* `BlockState.canPlaceAt(int x, int y, int z)` - returns true if the block at the provided position is replaceable and the state can exist at the provided position. For example, most plants require grass or dirt below them.
* `BlockState.canStayAt(int x, int y, int z)` - returns true if the block can exist at this location without popping off. For example, most plants require grass or dirt below them.

# Keywords

* `BlockState` takes one of two forms:
	* `BlockState('minecraft:snow[layers=1]')` works identical to explicit casting.
	* `BlockState('minecraft:snow', layers: 1)` alternate syntax allows you to specify properties with non-constant values.
		* Note that you can also provide a Block instead of a String:
			```
			Block block = 'minecraft:snow'
			BlockState state = BlockState(block, layers: 1)
			```

# Types

* Tag - common super class of BlockTag, BiomeTag, ConfiguredFeatureTag, etc... (coming in the next version)
* Block
* BlockState
* BlockTag
* Biome
* BiomeTag
* ConfiguredFeature
* ConfiguredFeatureTag

# Casting

* `String -> Block` - use the namespace and path of the block. Example: `Block stone = 'minecraft:stone'`.
* `String -> BlockState` - use the same text you would in a `/setblock` command. Example: `BlockState snow = 'minecraft:snow[layers=1]'`
* `String -> BlockTag` - use the namespace and path of the tag, without a `#` prefix. Example: `BlockTag wool = 'minecraft:wool'` references `/data/minecraft/tags/blocks/wool.json`
* `String -> Biome` - use the namespace and path of the biome. Example: `Biome plains = 'minecraft:plains'`
* `String -> BiomeTag` - use the namespace and path of the tag, without a `#` prefix. Example: `BiomeTag overworld = 'minecraft:overworld'` references `/data/minecraft/tags/worldgen/biome/overworld.json`
* `String -> ConfiguredFeature` - use the namespace and path of the configured feature. Example: `ConfiguredFeature oakTree = 'minecraft:oak'`
* `String -> ConfiguredFeatureTag` - use the namespace and path of the tag, without a `#` prefix.