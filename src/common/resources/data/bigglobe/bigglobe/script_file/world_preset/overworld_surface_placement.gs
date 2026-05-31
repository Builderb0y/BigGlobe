int surfaceY = world_traits.`bigglobe:y_level_on_surface`
;skip the bulk of the work if the surface is outside the world height,
;which could happen with cubic chunks.
if (surfaceY.isBetween[minY, maxY]:
	long seed = columnSeed(16x89EA3521C6A72ABCUL)
	if (`bigglobe:overworld/is_desert`:
		int lowerBound = hints.isLod ? world_traits.`bigglobe:y_level_on_surface` - 16 : world_traits.`bigglobe:core_max_y`
		int upperBound = world_traits.`bigglobe:y_level_in_surface`
		double terracottaOffset = `bigglobe:overworld/terracotta_offset` * `bigglobe:overworld/raw/mountainness`
		boolean mesa = `bigglobe:overworld/is_mesa`
		for (int blockY in range[lowerBound, upperBound]:
			double dY = (blockY + terracottaOffset) * 0.5L
			int layerY = floorInt(dY)
			double fracY = dY - layerY
			long thisSeed  = (worldSeed # 16xB5F4CE9A9B83A3EDUL).newSeed(layerY)
			long upperSeed = (worldSeed # 16xB5F4CE9A9B83A3EDUL).newSeed(layerY + 1)
			if (fracY <   thisSeed.nextDouble(-0.25L, 0.25L): layerY -= 1)
			if (fracY >= upperSeed.nextDouble(+0.75L, 1.25L): layerY += 1)
			long terracottaSeed = (worldSeed # 16x41E4CF20890390BCUL).newSeed(layerY)
			double terracottaChance = 1.0L - smooth(`bigglobe:approximate_distance_below_surface`(blockY) / 128.0L - 0.5L)
			BlockState replacement = if (mesa:
				terracottaSeed.switch (
					11.0L - 11.0L * terracottaChance: 'minecraft:stone',
					4.0L * terracottaChance: 'minecraft:red_sandstone',
					terracottaChance: 'minecraft:terracotta',
					terracottaChance: 'minecraft:yellow_terracotta',
					terracottaChance: 'minecraft:gray_terracotta',
					terracottaChance: 'minecraft:black_terracotta',
					terracottaChance: 'minecraft:red_terracotta',
					terracottaChance: 'minecraft:orange_terracotta',
					terracottaChance: 'minecraft:brown_terracotta',
					default: 'minecraft:stone'
				)
			)
			else (
				terracottaSeed.switch (
					8.0L - 8.0L * terracottaChance: 'minecraft:stone',
					4.0I * terracottaChance: 'minecraft:sandstone',
					terracottaChance: 'minecraft:terracotta',
					terracottaChance: 'minecraft:yellow_terracotta',
					terracottaChance: 'minecraft:light_gray_terracotta',
					terracottaChance: 'minecraft:white_terracotta',
					default: 'minecraft:stone'
				)
			)
			setBlockState(blockY, replacement)
		)
	)
	if ((seed := seed.newSeed()).nextDouble() < unmixSmooth(8.0L, 4.0L, world_traits.`bigglobe:exact_surface_y`):
		int depth = (seed := seed.newSeed()).nextInt(3, 7)
		setBlockStates(surfaceY - depth, surfaceY, 'minecraft:gravel')
	)
	if (`bigglobe:overworld/surface_states_override` != null:
		setBlockStates(
			surfaceY - (seed := seed.newSeed()).nextInt(3, 7),
			surfaceY,
			`bigglobe:overworld/surface_states_override`.under
		)
	)
	int depth = floorInt(
		+ (seed := seed.newSeed()).nextDouble(3.0L, 7.0L) ;base randomness
		+ (`bigglobe:overworld/raw/foliage_noise` * 2.0L) ;more depth when foliage is high
		- (`bigglobe:overworld/mountainness` * 8.0L) ;less depth when Y level is high
		- (`bigglobe:overworld/hilliness` * world_traits.`bigglobe:eroded_foliage` * 4.0L) ;more depth in valleys
	)
	if (depth > 0:
		if (`bigglobe:overworld/surface_states_override` != null:
			setBlockStates(
				surfaceY - depth,
				surfaceY,
				`bigglobe:overworld/surface_states_override`.top
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
)