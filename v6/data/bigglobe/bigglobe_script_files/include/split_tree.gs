class Pos(int*(x, y, z))
HashMap positions = new(1024)
void branch(double*(x1, y1, z1, r1, x2, y2, z2, r2), WoodPalette woodPalette, int recursionLevel:
	double*(
		dx = x2 - x1
		dy = y2 - y1
		dz = z2 - z1
		dr = 1.0L / (dx ^ 2 + dy ^ 2 + dz ^ 2)
	)
	int*(
		minX = higherInt(min(x1 - r1, x2 - r2))
		minY =  floorInt(min(y1 - r1, y2 - r2)) ;one block lower for ground replacements.
		minZ = higherInt(min(z1 - r1, z2 - r2))
		maxX = lowerInt(max(x1 + r1, x2 + r2))
		maxY = lowerInt(max(y1 + r1, y2 + r2))
		maxZ = lowerInt(max(z1 + r1, z2 + r2))
	)
	for (
		int z in range[minZ, maxZ],
		int x in range[minX, maxX]
	:
		boolean placedPreviously = false
		for (int y in -range[minY, maxY]:
			double*(
				relativeX = x - x1
				relativeY = y - y1
				relativeZ = z - z1
				dot = clamp(0.0L, 1.0L, (relativeX * dx + relativeY * dy + relativeZ * dz) * dr)
				closestX = dx * dot
				closestY = dy * dot
				closestZ = dz * dot
				projDist = (
					+ (relativeX - closestX) ^ 2
					+ (relativeY - closestY) ^ 2
					+ (relativeZ - closestZ) ^ 2
				)
				threshold = mixLinear(r1, r2, dot) ^ 2
			)
			BlockState state = getBlockState(x, y, z)
			if (state.isReplaceable() || state.isIn('#bigglobe:tree_trunk_replaceables'):
				placedPreviously = (projDist < threshold).if (
					positions.(Pos.new(x, y, z)) = woodPalette.woodState(random, axis: y)
				)
			)
			else (
				if (recursionLevel == 0:
					if (placedPreviously:
						BlockState replacement = groundReplacements.(state.getBlock())
						if (replacement != null:
							if (replacement != state:
								positions.(Pos.new(x, y, z)) = replacement
							)
						)
						else (
							if (requireValidGround:
								abort()
							)
						)
						placedPreviously = false
					)
				)
				else (
					abort()
				)
			)
		)
	)
)

void tree(
	double*(x1, y1, z1, r1, x2, y2, z2, r2),
	WoodPalette woodPalette,
	double leafRadius,
	int recursionLevel
:
	branch(x1, y1, z1, r1, x2, y2, z2, r2, woodPalette, recursionLevel)
	double*(
		dx = x2 - x1
		dy = y2 - y1
		dz = z2 - z1
		dr = sqrt(dx ^ 2 + dy ^ 2 + dz ^ 2)
	)
	if (dr >= 1.0L:
		dx /= dr
		dy /= dr
		dz /= dr
		double*(
			latitude  = random.nextDouble(-1.0L, +1.0L)
			longitude = random.nextDouble(tau)
			cylinder  = sqrt(1.0L - latitude ^ 2)
			offsetX = cos(longitude) * cylinder
			offsetY = latitude
			offsetZ = sin(longitude) * cylinder
			dot = dx * offsetX + dy * offsetY + dz * offsetZ
		)
		offsetX -= dot * dx
		offsetY -= dot * dy
		offsetZ -= dot * dz
		double offsetR = sqrt(offsetX ^ 2 + offsetY ^ 2 + offsetZ ^ 2)
		offsetX /= offsetR
		offsetY /= offsetR
		offsetZ /= offsetR
		;(
			ux uy uz
			dx dy dz
			ox oy oz
			=
			ux            uy            uz
			   dy dz   dx    dz   dx dy
			   oy oz   ox    oz   ox oy
		)
		double*(
			sideX = dy * offsetZ - dz * offsetY
			sideY = dz * offsetX - dx * offsetZ
			sideZ = dz * offsetY - dy * offsetX
		)
		int count = random.nextInt(1, 4)
		for (int branch in range[0, count):
			double*(
				sinBranch = sin(branch * tau / count)
				cosBranch = cos(branch * tau / count)
				;if I really wanted each branch to be half the length of the previous one,
				;I would divide by sqrt(2) too. but 0.5 on its own seems to produce decent results.
				x3 = x2 + (dx + offsetX * cosBranch + sideX * sinBranch) * dr * 0.5L
				y3 = y2 + (dy + offsetY * cosBranch + sideY * sinBranch) * dr * 0.5L
				z3 = z2 + (dz + offsetZ * cosBranch + sideZ * sinBranch) * dr * 0.5L
			)
			tree(
				x2, y2, z2, r2,
				x3, y3, z3, r2 * 0.75L + 0.25L,
				woodPalette,
				leafRadius,
				recursionLevel + 1
			)
		)
	)
	else (
		repeat (random.roundInt(leafRadius ^ 3 * 32.0L):
			int*(
				x = roundInt(x2 + random.nextGaussian() * leafRadius)
				y = roundInt(y2 + random.nextGaussian() * leafRadius)
				z = roundInt(z2 + random.nextGaussian() * leafRadius)
			)
			BlockState existing = getBlockState(x, y, z)
			if (existing.isReplaceable() || existing.isIn('#bigglobe:tree_leaf_replaceables'):
				positions.putIfAbsent(Pos.new(x, y, z), woodPalette.leavesState(random, distance: 1, persistent: false, waterlogged: false))
			)
		)
	)
)

void placeTree(:
	for (Pos pos, BlockState state in positions:
		setBlockState(pos.x, pos.y, pos.z, state)
		if (random.nextBoolean(world_traits.`bigglobe:snow_chance_at`(pos.x, pos.y + 1, pos.z)):
			if (getBlockState(pos.x, pos.y + 1, pos.z).isAir():
				setBlockState(pos.x, pos.y + 1, pos.z, 'minecraft:snow[layers=1]')
			)
		)
	)
)

void placeDecorations(:
	for (Pos pos, BlockState state in positions:
		ConfiguredFeature decoration = decorations.(state.getBlock())
		if (decoration != null:
			placeFeature(pos.x, pos.y, pos.z, decoration)
		)
	)
)