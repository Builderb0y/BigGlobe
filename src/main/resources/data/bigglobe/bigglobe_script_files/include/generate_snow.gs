void generateSnow(int snowBottom, double snowY:
	int snowTop = int(snowY)
	if (getBlockState(snowBottom - 1).?isAir() ?: false:
		snowBottom = getBottomOfSegment(snowBottom - 1)
		if (world_traits.`bigglobe:snow_chance` < 1.0I:
			setBlockState(snowBottom, 'minecraft:snow[layers=1]')
			return()
		)
	)
	if (snowTop >= snowBottom:
		setBlockStates(snowBottom, snowTop, 'minecraft:snow_block')
		int remaining = int((snowY - floor(snowY)) * 8.0L)
		if (snowBottom == snowTop && remaining == 0: remaining = 1)
		if (remaining != 0:
			setBlockState(snowTop, BlockState('minecraft:snow', layers: remaining))
		)
	)
	else (
		setBlockState(snowBottom, 'minecraft:snow[layers=1]')
	)
)