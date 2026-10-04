boolean red      = random.nextBoolean()
int splitCount   = random.nextBetween[1, 3]
int firstHeight  = random.nextBetween[16, 32)
int deltaHeight  = random.nextBetween[3, 6]
double capRadius = random.nextDouble(3.5L, 4.5L)
double baseAngle = random.nextDouble(tau)
for (int split in range[0, splitCount):
	double angle = baseAngle + split * tau / splitCount
	int height = firstHeight + split * deltaHeight
	double*(cosAngle = cos(angle), sinAngle = sin(angle))
	(
		double outward = capRadius
		int centerX = floorInt(cosAngle * outward + 0.5L)
		int centerZ = floorInt(sinAngle * outward + 0.5L)
		boolean inside(int*(x, y, z):
			int r2 = (x - centerX) ^ 2 + (z - centerZ) ^ 2
			if (red:
				return(y <= roundInt(height - r2 * 1.5L / capRadius))
			)
			else (
				return(y <= roundInt(height - r2 * 2.0L / capRadius ^ 2))
			)
		)
		for (
			int z in range[ceilInt(centerZ - capRadius), floorInt(centerZ + capRadius)],
			int x in range[ceilInt(centerX - capRadius), floorInt(centerX + capRadius)],
			int y in range[red ? ceilInt(height - capRadius * 1.5L) : height - 1, height]
		:
			if (inside(x, y, z):
				boolean*(
					up    = !inside(x, y + 1, z)
					north = !inside(x, y, z - 1)
					south = !inside(x, y, z + 1)
					east  = !inside(x + 1, y, z)
					west  = !inside(x - 1, y, z)
				)
				if (up | north | south | east | west:
					setBlockStateReplaceable(x, y, z, BlockState(
						red
						? 'minecraft:red_mushroom_block'
						: 'minecraft:brown_mushroom_block',
						up: up,
						down: false,
						north: north,
						south: south,
						east: east,
						west: west
					))
				)
			)
		)
	)
	for (int y in range[0, height):
		double outward = y * capRadius / height
		double centerX = cosAngle * outward
		double centerZ = sinAngle * outward
		double radius = sqrt(2.0L)
		for (
			int z in range[ceilInt(centerZ - radius), floorInt(centerZ + radius)],
			int x in range[ceilInt(centerX - radius), floorInt(centerX + radius)]
		:
			if ((x - centerX) ^ 2 + (z - centerZ) ^ 2 <= 2.0L:
				setBlockStateReplaceable(x, y, z, 'minecraft:mushroom_stem[north=true,south=true,east=true,west=true,up=true,down=true]')
			)
		)
	)
)
return(true)