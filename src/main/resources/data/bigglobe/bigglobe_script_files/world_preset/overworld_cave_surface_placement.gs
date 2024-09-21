CaveSurfaceData cave = world_traits.`bigglobe:cave_surface_data`
boolean canGenerateFloor = cave.depth > 0.0L && cave.floor_states != null
boolean canGenerateCeiling = cave.depth > 0.0L && cave.ceiling_states != null
if (canGenerateFloor || canGenerateCeiling:
	int minIteration = world_traits.`bigglobe:cave_min_y`
	int maxIteration = world_traits.`bigglobe:cave_max_y`
	if (canGenerateFloor:
		for (int y = maxIteration + 1, (y := getBottomOfSegment(y - 1)) >= minIteration, noop:
			if (getBlockState(y) ==. 'minecraft:air':
				int limit = max(getBottomOfSegment(y - 1), y - floorInt(columnSeed(16x1CAD9C3F950EB0EFUL).newSeed(y).nextFloat(cave.depth)) - 1)
				setBlockStates(limit, y, cave.floor_states.under)
				setBlockState(y - 1, cave.floor_states.top)
			)
		)
	)
	if (canGenerateCeiling:
		for (int y = minIteration, (y := getTopOfSegment(y)) <= maxIteration, noop:
			if (getBlockState(y - 1) ==. 'minecraft:air':
				int limit = min(getTopOfSegment(y), y + higherInt(columnSeed(16xD34EFA2F169E4C50UL).newSeed(y).nextFloat(cave.depth)))
				setBlockStates(y, limit, cave.ceiling_states.above)
				setBlockState(y, cave.ceiling_states.bottom)
			)
		)
	)
)