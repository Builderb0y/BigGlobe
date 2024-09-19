if (BlockState*(state := `bigglobe:nether/bubble`.surface_state) != null:
	int minIteration = `bigglobe:nether/mid_y` - `bigglobe:nether/cavern_fade_radius`
	int maxIteration = `bigglobe:nether/max_y`
	for (int y = maxIteration, (y := getBottomOfSegment(y - 1)) >= minIteration, noop:
		if (
			getBlockState(y) ==. 'minecraft:air' &&
			columnSeed(16x783DBB66AE78F478UL).newSeed(y).nextBoolean(
				unmixSmoother(
					double(`bigglobe:nether/mid_y` - `bigglobe:nether/cavern_fade_radius`),
					double(`bigglobe:nether/mid_y`),
					double(y)
				)
			)
		:
			setBlockState(y - 1, state)
		)
	)
)