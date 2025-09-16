double computeOverworldSlope(double*(heightmap1, amp1, dHeightmap1, dAmp1):
	1.5L * (1.0 - heightmap1 ^ 2) * dAmp1 * (1.0L - amp1 ^ 2) + 4.0L * (1.5L - 0.5L * amp1 ^ 2) * amp1 * heightmap1 * dHeightmap1 * (heightmap1 ^ 2 - 1.0L)
)