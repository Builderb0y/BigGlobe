int surfaceY = world_traits.`bigglobe:y_level_on_surface`
long seed = columnSeed(16x47B481311B32724DUL)
double slopeSquared = (
	+ dx(`bigglobe:islands/eroded_surface_height`) ^ 2
	+ dz(`bigglobe:islands/eroded_surface_height`) ^ 2
)

if ((seed := seed.newSeed()).nextDouble() < unmixSmooth(8.0L, 4.0L, `bigglobe:islands/eroded_surface_height`):
	int depth = (seed := seed.newSeed()).nextInt(3, 7)
	setBlockStates(surfaceY - depth, surfaceY, 'minecraft:gravel')
)

IslandSurfaceStates states = `bigglobe:islands/surface_states`
double rawDepth = floorInt(
	(seed := seed.newSeed()).nextDouble(3.0L, 7.0L)
	- slopeSquared * 3.0L
)

if (`bigglobe:islands/is_volcano`:
	int limit = surfaceY - (surfaceOnly ? 32 : (seed := seed.newSeed()).nextInt(16, 32))
	int segmentTop = surfaceY - 1
	while (segmentTop >= limit:
		int segmentBottom = getBottomOfSegment(segmentTop)
		if (getBlockState(segmentTop) ==. 'minecraft:stone':
			setBlockStates(max(segmentBottom, limit), segmentTop + 1, 'minecraft:basalt[axis=y]')
		)
		segmentTop = segmentBottom - 1
	)
)

if (states.surfaceState ==. 'minecraft:blackstone':
	int limit = surfaceY - int(rawDepth)
	int segmentTop = surfaceY - 1
	while (segmentTop >= limit:
		int segmentBottom = getBottomOfSegment(segmentTop)
		if (getBlockState(segmentTop).?hasFullCubeCollision():
			setBlockStates(max(segmentBottom, limit), segmentTop + 1, 'minecraft:blackstone')
		)
		segmentTop = segmentBottom - 1
	)
)
else (
	rawDepth -= surfaceY / 192.0L
	if (int*(depth := int(rawDepth)) > 0:
		int limit = surfaceY - depth
		int segmentTop = surfaceY - 1
		boolean hadBlock = false
		while (segmentTop >= limit:
			int segmentBottom = getBottomOfSegment(segmentTop)
			hadBlock = (getBlockState(segmentTop).?hasFullCubeCollision()).if (
				setBlockStates(max(segmentBottom, limit), segmentTop, states.subsurfaceState)
				unless (hadBlock: setBlockState(segmentTop, states.surfaceState))
			)
			segmentTop = segmentBottom - 1
		)
	)
)

if (surfaceY > world_traits.`bigglobe:sea_level` && !`bigglobe:islands/is_volcano` && (seed := seed.newSeed()).nextBoolean(`bigglobe:islands/snow_chance`):
	generateSnow(surfaceY, `bigglobe:islands/snow_height`)
)