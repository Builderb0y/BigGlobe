if (hints.isLod && `bigglobe:overworld/glacier_cell`.?hard_distance ?: 1.0L <= `bigglobe:overworld/glacier_crack_threshold`:
	setBlockStates(
		max(
			world_traits.`bigglobe:y_level_on_surface`,
			floorInt(`bigglobe:overworld/glacier_min_y`)
		),
		floorInt(`bigglobe:overworld/glacier_max_y`),
		'minecraft:snow_block'
	)
	int layers = truncInt(`bigglobe:overworld/glacier_max_y` % 1.0L * 8.0L)
	if (layers != 0: setBlockState(floorInt(`bigglobe:overworld/glacier_max_y`), BlockState('minecraft:snow', layers: layers)))
)