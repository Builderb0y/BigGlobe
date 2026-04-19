int*(
	keepSnow = 0
	setSnow  = 1
	addSnow  = 2
)

void setSurfaceY(double*(y, fraction), int snowMode:
	int iY = ceilInt(y)
	double*(
		oldY = world_traits.`bigglobe:automatic_exact_surface_y`(iY)
		newY = mixLinear(y, oldY, fraction)
	)
	world_traits.`bigglobe:automatic_exact_surface_y`(iY) = newY
	switch (snowMode:
		case (0: noop)
		case (1: world_traits.`bigglobe:automatic_snow_y`(iY) = mixLinear(y - 1.0L, world_traits.`bigglobe:automatic_snow_y`(iY), fraction))
		case (2: world_traits.`bigglobe:automatic_snow_y`(iY) += newY - oldY)
	)
	world_traits.`bigglobe:river_water_y` += newY - oldY
)

void setSnowY(double*(y, fraction):
	int iY = ceilInt(y)
	double oldSnowY = world_traits.`bigglobe:automatic_snow_y`(iY)
	double newSnowY = mixLinear(y, oldSnowY, fraction)
	world_traits.`bigglobe:automatic_snow_y`(iY) = newSnowY
)

void setSurfaceFoliage(int y, double fraction:
	world_traits.`bigglobe:automatic_surface_foliage`(y) -= float(1.0L - fraction)
)