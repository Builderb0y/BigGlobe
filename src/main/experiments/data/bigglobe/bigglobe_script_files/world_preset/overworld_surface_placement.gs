int surfaceY = world_traits.`bigglobe:y_level_on_surface`
;skip the bulk of the work if the surface is outside the world height,
;which could happen with cubic chunks.
if (surfaceY.isBetween[minY, maxY]:
	long seed = columnSeed(16x89EA3521C6A72ABCUL)
	double slopeSquared = (
		+ dx(world_traits.`bigglobe:slope_surface_y`) ^ 2
		+ dz(world_traits.`bigglobe:slope_surface_y`) ^ 2
	)
	(
		int lowerBound = hints.isLod ? ceilInt(world_traits.`bigglobe:slope_surface_y`) - 16 : world_traits.`bigglobe:core_max_y`
		int upperBound = world_traits.`bigglobe:y_level_in_surface`
		double terracottaOffset = `bigglobe:overworld/terracotta_offset`
		boolean desert = `bigglobe:overworld/is_desert`
		boolean mesa = `bigglobe:overworld/is_mesa`
		for (int blockY in range[lowerBound, upperBound]:
			if (BlockState state = getBlockState(blockY),, state != null && !state.isAir():
				double dY = (blockY + terracottaOffset) * 0.5L
				int layerY = floorInt(dY)
				double fracY = dY - layerY
				long thisSeed  = (worldSeed # 16xB5F4CE9A9B83A3EDUL).newSeed(layerY)
				long upperSeed = (worldSeed # 16xB5F4CE9A9B83A3EDUL).newSeed(layerY + 1)
				if (fracY <   thisSeed.nextDouble(-0.25L, 0.25L): layerY -= 1)
				if (fracY >= upperSeed.nextDouble(0.75L, 1.25L): layerY += 1)
				long terracottaSeed = (worldSeed # 16x41E4CF20890390BCUL).newSeed(layerY)
				double stoneOnly = unmixSmooth(
					world_traits.`bigglobe:deep_dark_max_y`,
					world_traits.`bigglobe:approximate_surface_y`,
					double(blockY)
				)
				double deepslate = (
					unmixSmooth(256.0L, 512.0L, `bigglobe:approximate_distance_below_surface`(blockY))
					* unmixSmooth(
						double(world_traits.`bigglobe:core_max_y`),
						double(world_traits.`bigglobe:deep_dark_min_y`),
						double(blockY)
					)
				)
				double oceanDripstone = world_traits.`bigglobe:approximate_surface_y` - world_traits.`bigglobe:sea_level`
				oceanDripstone /= sqrt((oceanDripstone * 0.015625L) ^ 2 + 1.0L)
				oceanDripstone = oceanDripstone * -0.015625L + 1.0L
				double riverDripstone = 2.0L / (256.0L * `bigglobe:overworld/raw/river/macro` ^ 2 + 1.0L)
				BlockState replacement = terracottaSeed.switch (
					4.0L: 'minecraft:stone',
					16.0I * deepslate: 'minecraft:deepslate[axis=y]',
					stoneOnly * (oceanDripstone + riverDripstone): 'minecraft:dripstone_block',
					stoneOnly * `bigglobe:overworld/raw/mountainness` * 4.0L: 'minecraft:granite',
					stoneOnly * exp2(world_traits.`bigglobe:foliage_at`(blockY) *  2.0L): 'minecraft:diorite',
					stoneOnly * exp2(world_traits.`bigglobe:foliage_at`(blockY) * -2.0L): 'minecraft:tuff',
					stoneOnly * exp2(world_traits.`bigglobe:temperature_at_surface`): 'minecraft:cyan_terracotta',
					stoneOnly * exp2(world_traits.`bigglobe:temperature_at_surface`): 'minecraft:light_gray_terracotta',
					stoneOnly * 2.0L: 'minecraft:andesite',
					stoneOnly * 1.0L: 'minecraft:calcite',
					stoneOnly * 0.25L: 'minecraft:smooth_basalt',
					default: 'minecraft:stone'
				)
				if (desert:
					terracottaSeed = terracottaSeed.newSeed()
					double terracottaChance = 1.0L - smooth(`bigglobe:approximate_distance_below_surface`(blockY) / 128.0L - 0.5L)
					replacement = if (mesa:
						terracottaSeed.switch (
							11.0L - 11.0L * terracottaChance: replacement,
							4.0L * terracottaChance: 'minecraft:red_sandstone',
							terracottaChance: 'minecraft:terracotta',
							terracottaChance: 'minecraft:yellow_terracotta',
							terracottaChance: 'minecraft:gray_terracotta',
							terracottaChance: 'minecraft:black_terracotta',
							terracottaChance: 'minecraft:red_terracotta',
							terracottaChance: 'minecraft:orange_terracotta',
							terracottaChance: 'minecraft:brown_terracotta',
							default: replacement
						)
					)
					else (
						terracottaSeed.switch (
							8.0L - 8.0L * terracottaChance: replacement,
							4.0I * terracottaChance: 'minecraft:sandstone',
							terracottaChance: 'minecraft:terracotta',
							terracottaChance: 'minecraft:yellow_terracotta',
							terracottaChance: 'minecraft:light_gray_terracotta',
							terracottaChance: 'minecraft:white_terracotta',
							default: replacement
						)
					)
				)
				setBlockState(blockY, replacement)
			)
		)
	)
	if ((seed := seed.newSeed()).nextDouble() < unmixSmooth(8.0L, 4.0L, world_traits.`bigglobe:exact_surface_y`):
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
	if (world_traits.`bigglobe:exact_surface_y` > world_traits.`bigglobe:sea_level` && (seed := seed.newSeed()).nextBoolean(world_traits.`bigglobe:snow_chance`):
		generateSnow(surfaceY, world_traits.`bigglobe:snow_y`)
	)
)