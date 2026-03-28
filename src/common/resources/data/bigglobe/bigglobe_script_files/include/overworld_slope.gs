double computeOverworldSlope(double*(hilliness1, erosion1, amp1, dHilliness1, dErosion1, dAmp1):
	double*(
		common1 = erosion1 ^ 2 * 8.0L + 1.0L
		common2 = (amp1 * hilliness1) ^ 2 + 1.0L
	)
	(
		(amp1 * dHilliness1 + hilliness1 * dAmp1) * common1
		- 16.0L * amp1 * erosion1 * hilliness1 * dErosion1 * common2
	)
	/
	(
		common1 ^ 2 * common2 * sqrt(common2)
	)
)