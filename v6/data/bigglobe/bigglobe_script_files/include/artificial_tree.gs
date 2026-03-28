unless (palette.saplingBlocks.contains(getBlockState(originX, originY, originZ).getBlock()): return(false))
ArrayDeque toCheck = new(8)
Pos origin = Pos.new(originX, originY, originZ)
toCheck.add(origin)
positions.put(origin, BlockState('minecraft:air'))
double*(centerX = originX, centerZ = originZ)
while outer (Pos*(pos := toCheck.pollFirst()) != null:
	for (int shift in range[0, 4):
		int offsetX = 2x01110000 << (30 - shift << 1) >> 30
		int offsetZ = 2x00000111 << (30 - shift << 1) >> 30
		Pos offsetPos = Pos.new(pos.x + offsetX, pos.y, pos.z + offsetZ)
		if (!positions.containsKey(offsetPos) && palette.saplingBlocks.contains(getBlockState(offsetPos.x, offsetPos.y, offsetPos.z).getBlock()):
			positions.put(offsetPos, BlockState('minecraft:air'))
			centerX += offsetPos.x
			centerZ += offsetPos.z
			if (positions.size() >= maxSaplings: break(outer))
			toCheck.addLast(offsetPos)
		)
	)
)
double saplingCount = positions.size()
centerX = centerX / saplingCount + 0.5L
centerZ = centerZ / saplingCount + 0.5L
centerX += random.nextDouble(-0.5L, +0.5L)
centerZ += random.nextDouble(-0.5L, +0.5L)
;enforce minimum radius of 1.0 for 1 sapling.
double baseRadius = sqrt((saplingCount + pi - 1.0L) / pi)