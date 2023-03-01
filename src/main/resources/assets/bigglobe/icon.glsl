//this code can be run on https://www.shadertoy.com/new.

float square(float x) { return x * x; }

float smoothify(float x) { return x * x * (x * -2.0 + 3.0); }
vec2  smoothify(vec2  x) { return x * x * (x * -2.0 + 3.0); }

float unmix(float low, float high, float frac) { return (frac - low) / (high - low); }

float hash12(vec2 p) {
	vec3 p3 = fract(p.xyx * 0.1031);
	p3 += dot(p3, p3.yzx + 33.33);
	return fract((p3.x + p3.y) * p3.z);
}

float hash13(vec3 p3) {
	p3 = fract(p3 * 0.1031);
	p3 += dot(p3, p3.zyx + 31.32);
	return fract((p3.x + p3.y) * p3.z);
}

float noiseWithSeed(float coord, float seed) {
	float  fractCoord = fract(coord);
	float  floorCoord = coord - fractCoord;
	float   ceilCoord = floorCoord + 1.0;
	float smoothCoord = smoothify(fractCoord);
	return mix(
		hash12(vec2(floorCoord, seed)),
		hash12(vec2( ceilCoord, seed)),
		smoothCoord
	);
}

float mountainNoise(float coord, float seed) {
	float sum = 0.0;
	sum += noiseWithSeed(coord       , seed) * 0.5;
	sum += noiseWithSeed(coord *  2.0, seed) * 0.25;
	sum += noiseWithSeed(coord *  4.0, seed) * 0.125;
	sum += noiseWithSeed(coord *  8.0, seed) * 0.0625;
	sum /= 1.0 - 0.0625;
	sum = sum * 2.0 - 1.0;
	sum = abs(sum);
	sum *= 2.0 - sum;
	sum = sum * -2.0 + 1.0;
	return sum;
}

float caveNoise(vec2 coord, float seed) {
	vec2  fractCoord = fract(coord);
	vec2  floorCoord = coord - fractCoord;
	vec2   ceilCoord = floorCoord + vec2(1.0);
	vec2 smoothCoord = smoothify(fractCoord);

	return mix(
		mix(
			hash13(vec3(floorCoord.x, floorCoord.y, seed)),
			hash13(vec3(floorCoord.x,  ceilCoord.y, seed)),
			smoothCoord.y
		),
		mix(
			hash13(vec3( ceilCoord.x, floorCoord.y, seed)),
			hash13(vec3( ceilCoord.x,  ceilCoord.y, seed)),
			smoothCoord.y
		),
		smoothCoord.x
	);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
	const float pixellation = 2.0;
	vec2 coord = floor(fragCoord / pixellation) * pixellation;
	//sky.
	vec3 color = mix(
		vec3(0.875, 0.9375, 1.0),
		vec3(0.25, 0.5, 1.0),
		clamp(unmix(128.0, 256.0, coord.y), 0.0, 1.0)
	);
	vec3 skyColor = color; //will be used later by fog.

	//mountains.
	for (int i = 1; i <= 7; i++) {
		float height = float(i) * 8.0 + 96.0;
		height += mountainNoise(coord.x / 128.0, float(i) + 1.234) * 64.0;
		height -= mountainNoise(coord.x /  64.0, float(i) + 2.345) * 32.0;
		if (coord.y < height) {
			if (coord.y >= height - pixellation) color = vec3(0.375, 0.75, 0.25); //grass.
			else if (coord.y >= height - pixellation * 4.0) color = vec3(0.541, 0.388, 0.275); //dirt.
			else color = vec3(mix(0.25, 0.5, coord.y / height)); //stone; gets darker when deeper.

			//caves.
			float caves = 0.0;
			caves += caveNoise(coord / 32.0, float(i)) * 0.5;
			caves += caveNoise(coord / 16.0, float(i)) * 0.25;
			caves /= 1.0 - 0.25;
			caves = caves * 2.0 - 1.0;
			caves *= caves;
			caves += square(max(1.0 - coord.y * 0.125, 0.0) * 0.1875); //fade out at the bottom.
			float caveWidthSquared = square(mix(0.1875, 0.0625, coord.y / height));
			if (caves < caveWidthSquared) {
				color *= 0.5;
				//lava.
				if (coord.y < 16.0) {
					color = vec3(1.0, 0.5, 0.0);
				}
			}

			//fog.
			color = mix(color, skyColor, float(i) / 8.0);
			break;
		}
	}

	fragColor = vec4(color, 1.0);
}