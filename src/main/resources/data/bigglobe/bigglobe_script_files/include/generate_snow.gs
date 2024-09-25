void generateSnow(int snowBottom, double snowY:
	int snowTop = int(snowY)
	while (snowBottom > minY && getBlockState(snowBottom - 1).?isAir() ?: true:
		snowBottom = getBottomOfSegment(snowBottom - 1)
	)
	if (snowTop >= snowBottom:
		setBlockStates(snowBottom, snowTop, 'minecraft:snow[layers=8]')
		int remaining = int(snowY % 1.0L * 8.0L)
		if (snowBottom == snowTop && remaining == 0: remaining = 1)
		if (remaining != 0:
			setBlockState(snowTop, BlockState('minecraft:snow', layers: remaining))
		)
	)
	else (
		setBlockState(snowBottom, 'minecraft:snow[layers=1]')
	)
)