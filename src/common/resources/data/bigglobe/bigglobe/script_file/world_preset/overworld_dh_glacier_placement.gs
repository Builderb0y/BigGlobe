if (hints.isLod && `bigglobe:overworld/glacier_cell`.hardDistance <= `bigglobe:overworld/glacier_crack_threshold`:
	int glacierMinY = max(
		world_traits.`bigglobe:y_level_on_surface`,
		floorInt(`bigglobe:overworld/glacier_min_y`)
	)
	int glacierMaxY = floorInt(`bigglobe:overworld/glacier_max_y`)
	setBlockStates(glacierMinY, glacierMaxY, 'minecraft:snow_block')
	int layers = truncInt(`bigglobe:overworld/glacier_max_y` % 1.0L * 8.0L)
	if (layers != 0 && glacierMinY <= glacierMaxY:
		setBlockState(glacierMaxY, BlockState('minecraft:snow', layers: layers))
	)
)