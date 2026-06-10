HashMap positions = new(1024)
void generateTree(double*(centerX, centerZ, bottomR), int*(minY, maxY), WoodPalette woodPalette:
	double*(
		goldenSin   = sin(goldenAngle)
		goldenCos   = cos(goldenAngle)

		branchAngle = random.nextDouble(tau)
		branchSin   = fastSin(branchAngle)
		branchCos   = fastCos(branchAngle)

		leafAngle   = random.nextDouble(tau)
		leafSin     = fastSin(leafAngle)
		leafCos     = fastCos(leafAngle)
	)
	for (int y in -range[minY - 4, maxY]:
		boolean placedAny = y == maxY
		double*(
			fraction = unmixLinear(double(minY), double(maxY), double(y))
			logR = mixLinear(bottomR, sqrt(0.5), fraction)
			leafR = mixLinear(bottomR * 2.0 + 2.0, sqrt(0.5), fraction)
			leafOffset = 1.0 - fraction
			leafX = centerX + leafCos * leafOffset
			leafZ = centerZ + leafSin * leafOffset
			branchR = leafR - leafOffset - sqrt(2.0)
		)
		if (branchR > 0.0 && fraction > 0.25L + 1.0L / (maxY - minY):
			repeat (random.roundInt(branchR):
				double*(
					x = centerX
					z = centerZ
					rcpMagnitude = 1.0L / max(abs(branchSin), abs(branchCos))
					addX = branchCos * rcpMagnitude
					addZ = branchSin * rcpMagnitude
				)
				String axis = abs(addX) > abs(addZ) ? 'x' : 'z'
				for (double dist = 0.0L, dist <= branchR, x += addX,, z += addZ,, dist += rcpMagnitude:
					int*(fx = floorInt(x), fz = floorInt(z))
					if (BlockState existing = getBlockState(fx, y, fz),, existing.isReplaceable() || existing.isIn('#bigglobe:tree_branch_replaceables'):
						positions.(BlockPos.new(fx, y, fz)) = palette.woodState(axis: axis)
					)
					else (
						abort()
					)
				)
				double*(
					;(
						[ +goldenCos -goldenSin ]
						[ +goldenSin +goldenCos ]
						* +branchCos +branchSin
						= branchCos * (goldenCos, goldenSin) + branchSin * (-goldenSin, goldenCos)
						= (
							branchCos * goldenCos - branchSin * goldenSin,
							branchCos * goldenSin + branchSin * goldenCos
						)
					)
					nextBranchCos = branchCos * goldenCos - branchSin * goldenSin
					nextBranchSin = branchCos * goldenSin + branchSin * goldenCos
				)
				branchCos = nextBranchCos
				branchSin = nextBranchSin
			)
		)
		double iterR = max(logR, leafR + leafOffset)
		for (
			int z in range[floorInt(centerZ - iterR), ceilInt(centerZ + iterR)],
			int x in range[floorInt(centerX - iterR), ceilInt(centerX + iterR)]
		:
			if (y < maxY && (x + 0.5L - centerX) ^ 2 + (z + 0.5L - centerZ) ^ 2 < logR ^ 2:
				if (BlockState existing = getBlockState(x, y, z),, existing.isReplaceable() || existing.isIn('#bigglobe:tree_trunk_replaceables'):
					positions.(BlockPos.new(x, y, z)) = woodPalette.logState(random, axis: 'y')
					placedAny = true
				)
				else if (y >= minY + 4:
					abort()
				)
			)
			else if (fraction >= 0.25 && (x + 0.5L - leafX) ^ 2 + (z + 0.5L - leafZ) ^ 2 < leafR ^ 2:
				if (BlockState existing = getBlockState(x, y, z),, existing.isReplaceable() || existing.isIn('#bigglobe:tree_leaf_replaceables'):
					positions.putIfAbsent(BlockPos.new(x, y, z), woodPalette.leavesState(random, distance: 1, persistent: false, waterlogged: false))
				)
			)
		)
		unless (placedAny: return())
		double*(
			nextLeafCos = leafCos * goldenCos - leafSin * goldenSin
			nextLeafSin = leafCos * goldenSin + leafSin * goldenCos
		)
		leafCos = nextLeafCos
		leafSin = nextLeafSin
	)
	abort()
)

void placeTree(:
	for (BlockPos pos, BlockState state in positions:
		setBlockState(pos.x, pos.y, pos.z, state)
		if (!state.isAir() && random.nextBoolean(world_traits.`bigglobe:snow_chance_at`(pos.x, pos.y + 1, pos.z)):
			if (getBlockState(pos.x, pos.y + 1, pos.z).isAir():
				setBlockState(pos.x, pos.y + 1, pos.z, 'minecraft:snow[layers=1]')
			)
		)
	)
)

void placeDecorations(:
	for (BlockPos pos, BlockState state in positions:
		ConfiguredFeature decoration = decorations.(state.getBlock())
		if (decoration != null:
			placeFeature(pos.x, pos.y, pos.z, decoration)
		)
	)
)

void generateAndPlaceArtificialTree(double*(centerX, centerZ, baseRadius), WoodPalette woodPalette:
	generateTree(centerX, centerZ, baseRadius, originY, originY + random.roundInt(baseRadius * 12.0L), woodPalette)
	placeTree()
)