int surfaceY = ceilInt(`bigglobe:overworld/processed_surface_y`)
;skip the bulk of the work if the surface is outside the world height,
;which could happen with cubic chunks.
if (surfaceY.isBetween[minY, maxY]:
	long seed = columnSeed(16x89EA3521C6A72ABCUL)
	double slopeSquared = (
		+ dx(`bigglobe:overworld/basic_surface_y`) ^ 2
		+ dz(`bigglobe:overworld/basic_surface_y`) ^ 2
	)
	if ((seed := seed.newSeed()).nextDouble() < unmixSmooth(8.0L, 4.0L, `bigglobe:overworld/processed_surface_y`):
		int depth = (seed := seed.newSeed()).nextInt(3, 7)
		setBlockStates(surfaceY - depth, surfaceY, 'minecraft:gravel')
	)
	if (`bigglobe:overworld/lake_surface_states` != null:
		setBlockStates(
			surfaceY - (seed := seed.newSeed()).nextInt(3, 7),
			surfaceY,
			`bigglobe:overworld/lake_surface_states`.under
		)
	)
	int depth = floorInt(
		+ (seed := seed.newSeed()).nextDouble(3.0L, 7.0L) ;base randomness
		- (slopeSquared * 3.0L) ;less depth when slope is high
		+ (`bigglobe:overworld/height_adjusted_foliage`(surfaceY) * 2.0L)
	)
	if (depth > 0:
		if (`bigglobe:overworld/lake_surface_states` != null:
			setBlockStates(
				surfaceY - depth,
				surfaceY,
				`bigglobe:overworld/lake_surface_states`.top
			)
		)
		else (
			SurfaceStates states = `bigglobe:overworld/surface_states`
			boolean hadBlock = false
			for (int y in -range[surfaceY - depth, surfaceY):
				hadBlock = (getBlockState(y) !=. 'minecraft:air').if (
					setBlockState(y, hadBlock ? states.subsurfaceState : states.surfaceState)
				)
			)
		)
	)
	if (`bigglobe:overworld/processed_surface_y` > `bigglobe:overworld/sea_level` && (seed := seed.newSeed()).nextFloat() < world_traits.`bigglobe:snow_chance`:
		int snowStart = surfaceY
		while (snowStart > minY && getBlockState(snowStart - 1).?isAir(): --snowStart)
		int snowEnd = lowerInt(`bigglobe:overworld/snow_y`)
		if (snowEnd >= snowStart:
			setBlockStates(snowStart, snowEnd, 'minecraft:snow[layers=8]')
			int remaining = floorInt((`bigglobe:overworld/snow_y` % 1.0I) * 8.0I)
			if (snowStart == snowEnd: remaining = max(remaining, 1))
			if (remaining != 0:
				setBlockState(snowEnd, BlockState('minecraft:snow', layers: remaining))
			)
		)
		else (
			setBlockState(snowStart, 'minecraft:snow[layers=1]')
		)
	)
)