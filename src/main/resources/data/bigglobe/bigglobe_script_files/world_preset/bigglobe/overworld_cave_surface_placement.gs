OverworldCaveCell cave = `bigglobe:overworld/cave`
float rawDepth = cave.surface_depth
CaveFloorStates floorStates = cave.floor_states
CaveCeilingStates ceilingStates = cave.ceiling_states
boolean canGenerateFloor = rawDepth > 0.0L && floorStates != null
boolean canGenerateCeiling = rawDepth > 0.0L && ceilingStates != null
if (canGenerateFloor || canGenerateCeiling:
	double*(
		basicSurfaceY = ceilInt(`bigglobe:overworld/basic_surface_y`)
		processedSurfaceY = ceilInt(`bigglobe:overworld/processed_surface_y`)
		maxCaveDepth = ceilInt(`bigglobe:overworld/cave`.depth)
	)
	int minIteration = floorInt(basicSurfaceY - maxCaveDepth)
	int maxIteration = ceilInt(processedSurfaceY)
	if (canGenerateFloor:
		for (int y = maxIteration + 1, (y := getBottomOfSegment(y - 1)) >= minIteration, noop:
			if (getBlockState(y) ==. 'minecraft:air':
				int limit = max(getBottomOfSegment(y - 1), y - floorInt(columnSeed(16x1CAD9C3F950EB0EFUL).newSeed(y).nextFloat(rawDepth)) - 1)
				setBlockStates(limit, y, floorStates.under)
				setBlockState(y - 1, floorStates.top)
			)
		)
	)
	if (canGenerateCeiling:
		for (int y = minIteration, (y := getTopOfSegment(y)) <= maxIteration, noop:
			if (getBlockState(y - 1) ==. 'minecraft:air':
				int limit = min(getTopOfSegment(y), y + higherInt(columnSeed(16xD34EFA2F169E4C50UL).newSeed(y).nextFloat(rawDepth)))
				setBlockStates(y, limit, ceilingStates.above)
				setBlockState(y, ceilingStates.bottom)
			)
		)
	)
)