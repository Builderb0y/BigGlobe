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

if (surfaceY > 0 && !`bigglobe:islands/is_volcano` && (seed := seed.newSeed()).nextBoolean(`bigglobe:islands/snow_chance`):
	int snowStart = surfaceY
	if (getBlockState(snowStart - 1).isAir():
		snowStart = getBottomOfSegment(snowStart - 1)
	)
	int snowEnd = lowerInt(`bigglobe:islands/snow_height`)
	if (snowEnd >= snowStart:
		setBlockStates(snowStart, snowEnd, 'minecraft:snow[layers=8]')
		int remaining = floorInt(`bigglobe:islands/snow_height` % 1.0I * 8.0I)
		if (snowStart == snowEnd: remaining = max(remaining, 1))
		if (remaining != 0:
			setBlockState(snowEnd, BlockState('minecraft:snow', layers: remaining))
		)
	)
	else (
		setBlockState(snowStart, 'minecraft:snow[layers=1]')
	)
)