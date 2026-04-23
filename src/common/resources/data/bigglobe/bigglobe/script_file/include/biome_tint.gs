int computeBiomeTint(boolean foliage, float volcano:
	float*(
		t = world_traits.`bigglobe:temperature_at`
		h = world_traits.`bigglobe:humidity_at`
		m = world_traits.`bigglobe:magicalness`

		r = + t * 4.0I + h * 0.0I + m * 0.25I + 0.0I
		g = + t * 0.5I + h * 0.5I + m * 0.5I  + 0.5I
		b = - t * 0.5I + h * 0.0I + m * 1.0I  - 0.5I
	)

	if (foliage:
		r -= 0.25I
		g -= 0.25I
		b -= 0.25I
	)

	r = r / sqrt(r ^ 2 + 1.0I).as(float) * 0.5I + 0.5I
	g = g / sqrt(g ^ 2 + 1.0I).as(float) * 0.5I + 0.5I
	b = b / sqrt(b ^ 2 + 1.0I).as(float) * 0.5I + 0.5I

	if (volcano > 0.0I:
		r = mixLinear(r, 0.25I, volcano)
		g = mixLinear(g, 0.25I, volcano)
		b = mixLinear(b, 0.25I, volcano)
	)

	return(packF(r, g, b))
)