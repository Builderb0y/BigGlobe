const float PI = 3.14159265359;
const float TAU = PI * 2.0;

float square(float f) { return f * f; }

//a low discrepency sequence for the
//angles and offsets seems to work better.
float halton(int startIndex, int base) {
	float position = 0.0;
	float offset = 1.0;
	for (int index = startIndex; index > 0;) {
		offset /= float(base);
		position += float(offset) * float(index % base);
		index /= base;
	}
	return position;
}

float sinNoise(vec2 coord, int startIndex) {
	const int iterations = 16;
	float sum = 0.0;
	for (int iteration = 0; iteration < iterations; iteration++) {
		vec2 rng = vec2(
			halton(startIndex + iteration * 5, 2),
			halton(startIndex + iteration * 5, 3)
		)
		* TAU;
		vec2 unit = vec2(cos(rng.x), sin(rng.x));
		sum += sin(dot(coord, unit) + rng.y);
	}
	return sum / sqrt(float(iterations));
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
	vec2 coord = fragCoord / 64.0;
	vec2 warp = vec2(
		sinNoise(coord, 0x7FDD),
		sinNoise(coord, 0x3387)
	);
	float angle = atan(warp.y, warp.x);
	float radius = length(warp);
	float brightness = square(sin(angle * 8.0 - exp2(4.0) * log2(1.0 - exp2(-4.0 * radius)))) * (1.0 - exp(-radius));
	vec3 color = vec3(brightness);
	fragColor = vec4(color, 1.0);
}