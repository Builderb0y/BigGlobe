double delayedPeek2(double*(depth, peek, limit):
	double scale = (4.0L * peak) / (2.0L * peak - limit)
	double rampUp = 1.0L - exp2(-(depth ^ 2))
	double taperOut = mixLinear(limit / peak, 2.0L, exp2(scale * (1.0L - depth)))
	return(rampUp * taperOut * peak)
)