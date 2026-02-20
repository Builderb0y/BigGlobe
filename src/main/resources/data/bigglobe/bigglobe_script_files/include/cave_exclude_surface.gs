void excludeSurface(double amount:
	if (amount > 0.0L:
		double basicY     = world_traits.`bigglobe:slope_surface_y`
		double processedY = world_traits.`bigglobe:exact_surface_y`
		for (int y in range(floorInt(basicY - 16.0L), ceilInt(processedY)):
			double amountForY = unmixLinear(basicY - 16.0L, basicY, y)
			world_traits.`bigglobe:cave_noise`(y) += float(amountForY ^ 2 * amount)
		)
	)
)