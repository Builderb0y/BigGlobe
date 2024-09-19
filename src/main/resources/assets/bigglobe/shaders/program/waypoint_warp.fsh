#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform mat4 ActualProjMat;
uniform mat4 ModelViewMat;
uniform float time;

uniform int bigglobe_waypoint_count;
//satin does not expose array-type uniforms, so I need 16 different uniforms instead.
uniform vec4
	bigglobe_waypoint_0,
	bigglobe_waypoint_1,
	bigglobe_waypoint_2,
	bigglobe_waypoint_3,
	bigglobe_waypoint_4,
	bigglobe_waypoint_5,
	bigglobe_waypoint_6,
	bigglobe_waypoint_7,
	bigglobe_waypoint_8,
	bigglobe_waypoint_9,
	bigglobe_waypoint_10,
	bigglobe_waypoint_11,
	bigglobe_waypoint_12,
	bigglobe_waypoint_13,
	bigglobe_waypoint_14,
	bigglobe_waypoint_15;

in vec2 texCoord;

out vec4 fragColor;

const float PI = 3.14159265359;
const float TAU = PI * 2.0;

float square(float f) {
	return f * f;
}

vec2 square(vec2 v) {
	return v * v;
}

vec3 square(vec3 v) {
	return v * v;
}

vec3 smoothify(vec3 v) {
	return v * v * (v * -2.0 + 3.0);
}

vec2 hash21(float p) {
	vec3 p3 = fract(vec3(p) * vec3(0.1031, 0.1030, 0.0973));
	p3 += dot(p3, p3.yzx + 33.33);
	return fract((p3.xx + p3.yz) * p3.zy);
}

vec2 hash23(vec3 p3) {
	p3 = fract(p3 * vec3(0.1031, 0.1030, 0.0973));
	p3 += dot(p3, p3.yzx + 33.33);
	return fract((p3.xx + p3.yz) * p3.zy);
}

vec4 hash42(vec2 p) {
	vec4 p4 = fract(p.xyxy * vec4(0.1031, 0.1030, 0.0973, 0.1099));
	p4 += dot(p4, p4.wzxy + 33.33);
	return fract((p4.xxyz + p4.yzzw) * p4.zywx);
}

vec3 unitVec(float seed) {
	vec2 surface = hash21(seed) * vec2(2.0, TAU) - vec2(1.0, 0.0);
	float r = sqrt(1.0 - surface.x * surface.x);
	return vec3(vec2(cos(surface.y), sin(surface.y)) * r, surface.x);
}

vec2 noise23(vec3 coord) {
	vec3  fractCoord = fract(coord);
	vec3  floorCoord = coord - fractCoord;
	vec3   ceilCoord = floorCoord + vec3(1.0);
	vec3 smoothCoord = smoothify(fractCoord);

	return mix(
		mix(
			mix(
				hash23(vec3(floorCoord.x, floorCoord.y, floorCoord.z)),
				hash23(vec3(floorCoord.x, floorCoord.y,  ceilCoord.z)),
				smoothCoord.z
			),
			mix(
				hash23(vec3(floorCoord.x,  ceilCoord.y, floorCoord.z)),
				hash23(vec3(floorCoord.x,  ceilCoord.y,  ceilCoord.z)),
				smoothCoord.z
			),
			smoothCoord.y
		),
		mix(
			mix(
				hash23(vec3( ceilCoord.x, floorCoord.y, floorCoord.z)),
				hash23(vec3( ceilCoord.x, floorCoord.y,  ceilCoord.z)),
				smoothCoord.z
			),
			mix(
				hash23(vec3( ceilCoord.x,  ceilCoord.y, floorCoord.z)),
				hash23(vec3( ceilCoord.x,  ceilCoord.y,  ceilCoord.z)),
				smoothCoord.z
			),
			smoothCoord.y
		),
		smoothCoord.x
	);
}

vec2 backgroundNoise(vec3 norm) {
	vec2 noise = noise23(norm);
	return vec2(abs(noise.x - 0.5), noise.y);
}

vec3 smoothHue(float h) {
	return sqrt(normalize(square(cos(h * TAU - vec3(0.0, 1.0, 2.0) * (TAU / 3.0)) * 0.5 + 0.5)));
}

void main() {
	vec4 waypoints[16] = vec4[](
		bigglobe_waypoint_0,
		bigglobe_waypoint_1,
		bigglobe_waypoint_2,
		bigglobe_waypoint_3,
		bigglobe_waypoint_4,
		bigglobe_waypoint_5,
		bigglobe_waypoint_6,
		bigglobe_waypoint_7,
		bigglobe_waypoint_8,
		bigglobe_waypoint_9,
		bigglobe_waypoint_10,
		bigglobe_waypoint_11,
		bigglobe_waypoint_12,
		bigglobe_waypoint_13,
		bigglobe_waypoint_14,
		bigglobe_waypoint_15
	);
	vec3 uv = vec3(texCoord, texture(DiffuseDepthSampler, texCoord).r);
	vec4 tmp = inverse(ActualProjMat) * vec4(uv * 2.0 - 1.0, 1.0);
	vec3 norm = normalize(tmp.xyz);
	float maxIntensity = 0.0;
	for (int index = 0; index < bigglobe_waypoint_count; index++) {
		vec4 waypoint = waypoints[index];
		float distanceFromOriginToClosest = max(dot(waypoint.xyz, norm), 0.0);
		vec3 closest = norm * distanceFromOriginToClosest;
		float distanceFromClosestToWaypoint = distance(closest, waypoint.xyz);
		if (distanceFromClosestToWaypoint < 1.0) {
			vec4 screenSpaceWaypoint = ActualProjMat * vec4(waypoint.xyz, 1.0);
			screenSpaceWaypoint.xyz = screenSpaceWaypoint.xyz / screenSpaceWaypoint.w * 0.5 + 0.5;
			if (screenSpaceWaypoint.z < uv.z) {
				float intensity = square(1.0 - distanceFromClosestToWaypoint) * 4.0 * waypoint.w;
				maxIntensity += intensity;
				uv.xy = mix(uv.xy, screenSpaceWaypoint.xy, intensity);
			}
		}
	}
	if (maxIntensity > 1.0) {
		norm *= mat3(ModelViewMat);
		vec2 noise = vec2(0.0);
		noise += backgroundNoise(norm *  2.0) * 0.5;
		noise += backgroundNoise(norm *  4.0) * 0.25;
		noise += backgroundNoise(norm *  8.0) * 0.125;
		noise += backgroundNoise(norm * 16.0) * 0.0625;
		noise += backgroundNoise(norm * 32.0) * 0.03125;
		noise += backgroundNoise(norm * 64.0) * 0.015625;

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
			vec4 starData = hash42(floorPos);
			vec2  starPos = mix(starData.xy, vec2(0.5), starData.z);
			float starIntensity = max(1.0 - 2.0 * distance(fractPos, starPos) / starData.z, 0.0);
			starIntensity = square(square(square(starIntensity)));
			starIntensity *= sin(time + starData.w * TAU) * 0.5 + 0.5;
			vec3 starColor = exp2((starData.z * 8.0 - 7.0) * (vec3(1.0, 2.0, 4.0) / 4.0));
			starSum += starColor * starIntensity * planeIntensity;
		}
		starSum *= noise.x;

		fragColor = vec4(color + starSum * 4.0, 1.0);
	}
	else {
		fragColor = texture(DiffuseSampler, uv.xy);
	}
	fragColor.rgb *= sqrt(abs(maxIntensity - 1.0));
}