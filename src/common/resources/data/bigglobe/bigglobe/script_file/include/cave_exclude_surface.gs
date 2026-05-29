void excludeSurface(double amount:
	if (amount > 0.0L:
		double surfaceY = world_traits.`bigglobe:exact_surface_y`
		for (int y in range(floorInt(surfaceY - 16.0L), ceilInt(surfaceY)):
			double amountForY = unmixLinear(surfaceY - 16.0L, surfaceY, y)
			world_traits.`bigglobe:cave_noise`(y) += float(amountForY ^ 2 * amount)
		)
	)
)