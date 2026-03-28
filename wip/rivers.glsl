float genRivers(vec2 uv) {
	//uv.x -= noise11(uv.y * 8.0) * 0.125 - 0.0625;
	float riverWide = smoothify(clamp(abs(fract(uv.x) - 0.5) * 64.0, 0.0, 1.0));
	float riverLong = smoothify(fract(uv.y));
	float value = mix(riverWide, 1.0, riverLong);
	float prevRiverLong = riverLong, prevSign = -1.0;
	uv.y *= 8.0;
	for (int i = 0; true;) {
		//uv.y -= noise11(uv.x * 16.0) * 0.5 - 0.25;
		uv.y -= square(max(1.0 - abs(fract(uv.x) - 0.5) * 4.0, 0.0)) * 0.5 * prevSign;
		riverWide = smoothify(clamp(abs(fract(uv.y) - 0.5) * 8.0, 0.0, 1.0));
		riverLong = smoothify(abs(fract(uv.x) * 2.0 - 1.0));
		value = mix(prevRiverLong, value, mix(riverWide, 1.0, riverLong));
		if (++i >= 4 || abs(fract(uv.x) - 0.5) < 0.03125) break;
		prevRiverLong = mix(prevRiverLong, 1.0, riverLong);
		prevSign = sign(fract(uv.x) - 0.5);
		uv *= mat2(0.0, 1.0, -15.0, 0.0);
	}
	return value;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
	vec2 uv = fragCoord / iResolution.xy;
	uv.x *= iResolution.x / iResolution.y;
	uv *= 3.0;

	vec3 color = vec3(genRivers(
		uv + (
			+ noise22(uv       ) * 0.4
			+ noise22(uv *  2.0) * 0.16
			+ noise22(uv *  4.0) * 0.064
			+ noise22(uv *  8.0) * 0.0256
			+ noise22(uv * 16.0) * 0.01024
		) / (0.4 + 0.16 + 0.064 + 0.0256 + 0.01024) - 0.5
	));
	if (color.r < smootherify(sin(iTime) * 0.5 + 0.5)) color = mix(color, vec3(0.25, 0.5, 1.0), 0.5);

	/*
	float height = lengthSquared(uv);
	float angle  = atan2(uv);
	float radius = sqrt(height);
	float angle2 = fract(angle * 8.0 / TAU + 4.0);
	float index  = floor(angle * 8.0 / TAU + 4.0);
	float riverCenter = noise12(vec2(log2(radius) * 4.0, index)) * 0.5 + 0.25;
	float river  = smoothify(clamp(abs(angle2 - riverCenter) * radius * 16.0, 0.0, 1.0));
	height *= river;

	vec3 color = vec3(height);
	if (height < 0.0625) color = mix(color, vec3(0.25, 0.5, 1.0), 0.5);
	*/

	fragColor = vec4(color, 1.0);
}