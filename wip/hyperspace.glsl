vec2 backgroundNoise(vec3 norm) {
	vec2 noise = noise23(norm);
	return vec2(abs(noise.x - 0.5), noise.y);
}

vec3 unitVec(float seed) {
	vec2 surface = hash21(seed) * vec2(2.0, TAU) - vec2(1.0, 0.0);
	float r = sqrt(1.0 - surface.x * surface.x);
	return vec3(vec2(cos(surface.y), sin(surface.y)) * r, surface.x);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
	vec2 uv = fragCoord / iResolution.xy;
	uv = uv * 2.0 - 1.0;
	uv.x *= iResolution.x / iResolution.y;

	vec3 norm = normalize(vec3(uv, 1.0));
	vec2 sc = sinCos(iTime * 0.25);
	norm.xz *= mat2(sc.y, -sc.x, sc.x, sc.y);

	//*
	vec3 warp = vec3(0.0);
	warp += noise33(norm * -1.0) * 0.5;
	warp += noise33(norm * -2.0) * 0.25;
	warp += noise33(norm * -4.0) * 0.125;
	warp /= 1.0 - 0.125;
	norm += warp - 0.5;
	norm = normalize(norm);
	//*/

	//*
	vec2 noise = vec2(0.0);
	noise += backgroundNoise(norm *  2.0) * 0.5;
	noise += backgroundNoise(norm *  4.0) * 0.25;
	noise += backgroundNoise(norm *  8.0) * 0.125;
	noise += backgroundNoise(norm * 16.0) * 0.0625;
	noise += backgroundNoise(norm * 32.0) * 0.03125;
	noise += backgroundNoise(norm * 64.0) * 0.015625;
	//*/

	vec3 color = mix(smoothHue(noise.y * 0.5 + 0.375), vec3(1.0), noise.x) * noise.x;
	vec3 starSum = vec3(0.0);

	for (int planeIndex = 1; planeIndex <= 16; planeIndex++) {
		vec3 axis1 = unitVec( float(planeIndex));
		vec3 axis2 = unitVec(-float(planeIndex));
		axis2 = normalize(axis2 - axis1 * dot(axis1, axis2));
		vec2 planePos = vec2(dot(norm, axis1), dot(norm, axis2));
		float planeIntensity = 1.0 - dot(planePos, planePos);
		vec2 scaledPlanePos = planePos * 16.0;
		vec2 fractPos = fract(scaledPlanePos);
		vec2 floorPos = scaledPlanePos - fractPos;
		vec3 starData = hash32(floorPos);
		vec2  starPos = mix(starData.xy, vec2(0.5), starData.z);
		float starIntensity = max(1.0 - 2.0 * distance(fractPos, starPos) / starData.z, 0.0);
		starIntensity = square(square(square(starIntensity)));
		vec3 starColor = exp2((starData.z * 8.0 - 7.0) * (vec3(1.0, 2.0, 4.0) / 4.0));
		starSum += starColor * starIntensity * planeIntensity;
	}
	starSum *= noise.x;

	fragColor = vec4(color + starSum * 4.0, 1.0);
}