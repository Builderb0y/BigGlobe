#version 150

uniform sampler2D colortex;
uniform sampler2D depthtex;
uniform isamplerBuffer waypoints;

//AAAAAAAAAAAAAAAAAAAAAaAAAAA
vec4 getWaypoint(int index) {
	return vec4(
		float(int(
			((uint(texelFetch(waypoints, index * 16     ).r) & 255u)      ) |
			((uint(texelFetch(waypoints, index * 16 +  1).r) & 255u) <<  8) |
			((uint(texelFetch(waypoints, index * 16 +  2).r) & 255u) << 16) |
			((uint(texelFetch(waypoints, index * 16 +  3).r) & 255u) << 24)
		)) / 65536.0,
		float(int(
			((uint(texelFetch(waypoints, index * 16 +  4).r) & 255u)      ) |
			((uint(texelFetch(waypoints, index * 16 +  5).r) & 255u) <<  8) |
			((uint(texelFetch(waypoints, index * 16 +  6).r) & 255u) << 16) |
			((uint(texelFetch(waypoints, index * 16 +  7).r) & 255u) << 24)
		)) / 65536.0,
		float(int(
			((uint(texelFetch(waypoints, index * 16 +  8).r) & 255u)      ) |
			((uint(texelFetch(waypoints, index * 16 +  9).r) & 255u) <<  8) |
			((uint(texelFetch(waypoints, index * 16 + 10).r) & 255u) << 16) |
			((uint(texelFetch(waypoints, index * 16 + 11).r) & 255u) << 24)
		)) / 65536.0,
		float(int(
			((uint(texelFetch(waypoints, index * 16 + 12).r) & 255u)      ) |
			((uint(texelFetch(waypoints, index * 16 + 13).r) & 255u) <<  8) |
			((uint(texelFetch(waypoints, index * 16 + 14).r) & 255u) << 16) |
			((uint(texelFetch(waypoints, index * 16 + 15).r) & 255u) << 24)
		)) / 65536.0
	);
}

layout(std140) uniform Uniforms {
	mat4 modelViewMatrix;
	mat4 inverseModelViewMatrix;
	mat4 projectionMatrix;
	mat4 inverseProjectionMatrix;
	float time;
	int waypointCount;
};

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
	vec3 uv = vec3(texCoord, texture(depthtex, texCoord).r);
	vec4 tmp = inverseModelViewMatrix * (inverseProjectionMatrix * vec4(uv * 2.0 - 1.0, 1.0));
	vec3 playerDir = normalize(tmp.xyz);
	float maxIntensity = 0.0;
	for (int index = 0; index < waypointCount; index++) {
		vec4 waypoint = getWaypoint(index);
		float distanceFromOriginToClosest = max(dot(waypoint.xyz, playerDir), 0.0);
		vec3 closest = playerDir * distanceFromOriginToClosest;
		float distanceFromClosestToWaypoint = distance(closest, waypoint.xyz);
		if (distanceFromClosestToWaypoint < 1.0) {
			vec4 screenSpaceWaypoint = projectionMatrix * (modelViewMatrix * vec4(waypoint.xyz, 1.0));
			screenSpaceWaypoint.xyz = screenSpaceWaypoint.xyz / screenSpaceWaypoint.w * 0.5 + 0.5;
			if (screenSpaceWaypoint.z < uv.z) {
				float intensity = square(1.0 - distanceFromClosestToWaypoint) * 4.0 * waypoint.w;
				maxIntensity += intensity;
				uv.xy = mix(uv.xy, screenSpaceWaypoint.xy, intensity);
			}
		}
	}
	if (maxIntensity > 1.0) {
		vec2 noise = vec2(0.0);
		noise += backgroundNoise(playerDir *  2.0) * 0.5;
		noise += backgroundNoise(playerDir *  4.0) * 0.25;
		noise += backgroundNoise(playerDir *  8.0) * 0.125;
		noise += backgroundNoise(playerDir * 16.0) * 0.0625;
		noise += backgroundNoise(playerDir * 32.0) * 0.03125;
		noise += backgroundNoise(playerDir * 64.0) * 0.015625;

		vec3 color = noise.x * mix(vec3(noise.x), vec3(2.0 - noise.x), smoothHue(noise.y * 0.5 + 0.375));
		vec3 starSum = vec3(0.0);

		for (int planeIndex = 1; planeIndex <= 16; planeIndex++) {
			vec3 axis1 = unitVec( float(planeIndex));
			vec3 axis2 = unitVec(-float(planeIndex));
			axis2 = normalize(axis2 - axis1 * dot(axis1, axis2));
			vec2 planePos = vec2(dot(playerDir, axis1), dot(playerDir, axis2));
			float rotation = time * 0.015625;
			vec2 cs = vec2(cos(rotation), sin(rotation));
			planePos *= mat2(cs.x, cs.y, -cs.y, cs.x);
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
		fragColor = texture(colortex, uv.xy);
	}
	fragColor.rgb *= sqrt(abs(maxIntensity - 1.0));
}