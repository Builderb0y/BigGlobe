int surfaceY = world_traits.`bigglobe:y_level_on_surface`
;skip the bulk of the work if the surface is outside the world height,
;which could happen with cubic chunks.
if (surfaceY.isBetween[minY, maxY]:
	long seed = columnSeed(16x89EA3521C6A72ABCUL)
	double slopeSquared = (
		+ dx(world_traits.`bigglobe:slope_surface_y`) ^ 2
		+ dz(world_traits.`bigglobe:slope_surface_y`) ^ 2
	)
	if ((seed := seed.newSeed()).nextDouble() < unmixSmooth(8.0L, 4.0L, world_traits.`bigglobe:exact_surface_y`):
		int depth = (seed := seed.newSeed()).nextInt(3, 7)
		setBlockStates(surfaceY - depth, surfaceY, 'minecraft:gravel')
	)
	else if (`bigglobe:overworld/is_desert`:
		int lowerBound = ceilInt(world_traits.`bigglobe:slope_surface_y`) - 32
		int upperBound = world_traits.`bigglobe:y_level_in_surface`
		double terracottaOffset = `bigglobe:overworld/terracotta_offset`
		boolean mesa = `bigglobe:overworld/is_mesa`
		int step = hints.distanceBetweenColumns
		int mask = ~(step - 1)
		for (int blockY in range[lowerBound, upperBound]:
			int lodY = blockY & mask
			if (BlockState state = getBlockState(blockY),, state != null && !state.isAir():
				double dY = lodY + terracottaOffset
				int layerY = floorInt(dY) & mask
				double fracY = dY - layerY
				long thisSeed  = (worldSeed # 16xB5F4CE9A9B83A3EDUL).newSeed(lodY)
				long upperSeed = (worldSeed # 16xB5F4CE9A9B83A3EDUL).newSeed(lodY + step)
				if (fracY <   thisSeed.nextDouble(-0.125L, 0.125L): layerY -= step)
				if (fracY >= upperSeed.nextDouble(step - 0.125L, step + 0.125L): layerY += step)
				long terracottaSeed = (worldSeed # 16x41E4CF20890390BCUL).newSeed(layerY)
				setBlockState(blockY,
					mesa
					? terracottaSeed.switch (
						'minecraft:terracotta',
						'minecraft:yellow_terracotta',
						'minecraft:gray_terracotta',
						'minecraft:black_terracotta',
						'minecraft:red_terracotta',
						'minecraft:orange_terracotta',
						'minecraft:brown_terracotta',
						'minecraft:red_sandstone'
					)
					: terracottaSeed.switch (
						'minecraft:terracotta',
						'minecraft:yellow_terracotta',
						'minecraft:light_gray_terracotta',
						'minecraft:white_terracotta',
						'minecraft:cyan_terracotta',
						'minecraft:light_blue_terracotta',
						'minecraft:blue_terracotta',
						'minecraft:sandstone'
					)
				)
			)
		)
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
		+ (world_traits.`bigglobe:foliage_at`(surfaceY) * 2.0L)
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
	if (world_traits.`bigglobe:exact_surface_y` > `bigglobe:overworld/sea_level` && (seed := seed.newSeed()).nextBoolean(world_traits.`bigglobe:snow_chance`):
		generateSnow(surfaceY, `bigglobe:overworld/snow_y`)
	)
)