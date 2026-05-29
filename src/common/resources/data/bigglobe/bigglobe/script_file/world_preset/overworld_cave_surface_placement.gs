void placeCaveSurfaces(CaveSurfaceStates cave:
	boolean canGenerateFloor = !cave.floor.layers.isEmpty()
	boolean canGenerateCeiling = !cave.ceiling.layers.isEmpty()
	if (canGenerateFloor || canGenerateCeiling:
		int minIteration = world_traits.`bigglobe:cave_min_y`
		int maxIteration = world_traits.`bigglobe:cave_max_y`
		int y = minIteration
		while (y <= maxIteration:
			int nextY = getTopOfSegment(y)
			if (getBlockState(y).?isAir():
				int ceilingY = nextY
				nextY = getTopOfSegment(nextY)
				if (canGenerateFloor:
					placeSurface(y, cave.floor)
				)
				if (canGenerateCeiling:
					placeSurfaceAbove(ceilingY, cave.ceiling)
				)
			)
			y = nextY
		)
	)
)