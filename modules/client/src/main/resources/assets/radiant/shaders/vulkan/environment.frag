layout(std140, set = 0, binding = 0) uniform Environment {
    mat4 viewProjection;
    mat4 inverseViewProjection;
    vec4 fogAndStars;
    vec4 skyColor;
    vec4 sunrise;
    vec4 celestial;
    vec4 cloudColor;
    vec4 cloudOffset;
    vec4 lowerSky;
    vec4 skyFog;
} env;
layout(set = 0, binding = 1) uniform sampler2D clouds;
layout(set = 0, binding = 2) uniform sampler2D sun;
layout(set = 0, binding = 3) uniform sampler2D moon;
layout(set = 0, binding = 4) uniform sampler2D endSky;
layout(location = 0) in vec2 ndc;
layout(location = 0) out vec4 color;

vec3 unproject(float depth) {
    vec4 relative = env.inverseViewProjection * vec4(ndc.x, -ndc.y, depth, 1.0);
    return relative.xyz / relative.w;
}

vec3 outputColor(vec3 rgb) {
    rgb = clamp(rgb, 0.0, 1.0);
#if SRGB_TARGET
    return mix(rgb / 12.92, pow((rgb + 0.055) / 1.055, vec3(2.4)), greaterThan(rgb, vec3(0.04045)));
#else
    return rgb;
#endif
}

#if CLOUD_PASS
vec4 traceCloud(vec3 origin, vec3 ray, float range, out float distance, out float shade) {
    float base = env.cloudOffset.z;
    shade = 1.0;
    distance = range;
    if (env.cloudColor.a < 2.0) {
        distance = (base - origin.y) / ray.y;
        if (distance <= 0.0 || distance >= range) {
            return vec4(0.0);
        }
        vec2 uv = (origin.xz + ray.xz * distance + env.cloudOffset.xy) / 2048.0;
        return textureLod(clouds, fract(uv), 0.0);
    }

    // Fancy clouds are a four-block slab with one solid column for each covered texture cell.
    float bottom = (base - origin.y) / ray.y;
    float top = (base + 4.0 - origin.y) / ray.y;
    float entry = max(0.0, min(bottom, top));
    float exitDistance = min(range, max(bottom, top));
    if (entry >= exitDistance) {
        return vec4(0.0);
    }
    shade = ray.y > 0.0 ? 0.7 : 1.0;
    vec2 size = vec2(textureSize(clouds, 0));
    vec2 gridScale = size / 3072.0;
    vec2 speed = ray.xz * gridScale;
    vec2 stepDirection = sign(speed);
    vec2 grid = (origin.xz + ray.xz * (entry + 0.001) + env.cloudOffset.xy) * gridScale;
    vec2 cell = floor(grid);
    vec2 next = vec2(1e8);
    vec2 stepDistance = vec2(1e8);
    for (int axis = 0; axis < 2; axis++) {
        if (abs(speed[axis]) > 0.000001) {
            float boundary = cell[axis] + (speed[axis] > 0.0 ? 1.0 : 0.0);
            next[axis] = entry + 0.001 + (boundary - grid[axis]) / speed[axis];
            stepDistance[axis] = abs(1.0 / speed[axis]);
        }
    }
    distance = entry;
    for (int i = 0; i < 128 && distance < exitDistance; i++) {
        vec4 texel = textureLod(clouds, fract((cell + 0.5) / size), 0.0);
        if (texel.a > 0.1) {
            return texel;
        }
        int axis = next.x < next.y ? 0 : 1;
        distance = next[axis];
        next[axis] += stepDistance[axis];
        cell[axis] += stepDirection[axis];
        shade = axis == 0 ? 0.9 : 0.8;
    }
    return vec4(0.0);
}

void main() {
    vec3 origin = unproject(-1.0);
    vec3 span = unproject(1.0) - origin;
    vec3 ray = normalize(span);
    if (abs(ray.y) < 0.00001) {
        discard;
    }
    float range = min(length(span), env.cloudColor.a < 2.0 ? 256.0 : 384.0);
    float distance;
    float shade;
    vec4 texel = traceCloud(origin, ray, range, distance, shade);
    float alpha = texel.a * 0.8 * (1.0 - smoothstep(range * 0.72, range, distance));
    if (alpha <= 0.1) {
        discard;
    }
    vec4 clip = env.viewProjection * vec4(origin + ray * distance, 1.0);
    gl_FragDepth = clamp(clip.z / clip.w * 0.5 + 0.5, 0.0, 1.0);
    color = vec4(outputColor(env.cloudColor.rgb * texel.rgb * shade), alpha);
}
#else
float hash(vec2 cell) {
    return fract(sin(dot(cell, vec2(127.1, 311.7))) * 43758.5453);
}

vec4 celestialDisc(sampler2D image, vec2 uv, vec2 tile, vec2 sheet) {
    if (any(lessThan(uv, vec2(0.0))) || any(greaterThanEqual(uv, vec2(1.0)))) {
        return vec4(0.0);
    }
    return textureLod(image, (tile + uv) / sheet, 0.0);
}

vec2 endSkyUv(vec3 origin, vec3 ray) {
    // Intersect the 200-block cube in camera-relative world space, including the eye offset.
    vec3 exitDistance = (vec3(100.0) - sign(ray) * origin) / max(abs(ray), vec3(1e-8));
    vec3 hit = origin + ray * min(exitDistance.x, min(exitDistance.y, exitDistance.z));
    vec3 axis = abs(hit);
    vec2 face;
    // Apply the inverse of the legacy cube-face rotations to the intersection point.
    if (axis.y >= max(axis.x, axis.z)) {
        face = vec2(hit.x, -sign(hit.y) * hit.z);
    } else if (axis.z >= axis.x) {
        face = vec2(hit.x, sign(hit.z) * hit.y);
    } else {
        face = vec2(sign(hit.x) * hit.y, hit.z);
    }
    return fract((face / 200.0 + 0.5) * 16.0);
}

// Negative visibility indicates that this ray misses the finite legacy sky plane.
float skyPlaneVisibility(vec3 origin, vec3 ray, float height) {
    float extent = env.skyColor.a;
    if (extent <= 0.0 || abs(ray.y) < 1e-6) return -1.0;
    float t = (height - origin.y) / ray.y;
    vec3 point = origin + ray * t;
    vec4 clip = env.viewProjection * vec4(point, 1.0);
    if (t < 0.0 || clip.z > clip.w || any(lessThan(point.xz, vec2(-extent)))
            || any(greaterThan(point.xz, vec2(extent + 64.0)))) return -1.0;

    float depth = clip.w;
    if (env.skyFog.x == 1.0) {
        return clamp((env.skyFog.z - depth) / max(env.skyFog.z - env.skyFog.y, 1e-4), 0.0, 1.0);
    }
    if (env.skyFog.x == 2.0) return exp(-depth * env.skyFog.w);
    if (env.skyFog.x == 3.0) {
        float density = depth * env.skyFog.w;
        return exp(-density * density);
    }
    return 1.0;
}

void main() {
    if (env.cloudOffset.w == 2.0) {
        color = vec4(outputColor(env.fogAndStars.rgb), 1.0);
        return;
    }
    vec3 origin = unproject(-1.0);
    vec3 ray = normalize(unproject(1.0) - origin);
    if (env.cloudOffset.w == 1.0) {
        vec3 texel = textureLod(endSky, endSkyUv(origin, ray), 0.0).rgb;
        color = vec4(outputColor(texel * env.skyColor.rgb), 1.0);
        return;
    }
    float upper = ray.y > 0.0 ? skyPlaneVisibility(origin, ray, 16.0) : -1.0;
    vec3 rgb = mix(env.fogAndStars.rgb, env.skyColor.rgb, max(upper, 0.0));
    float sine = env.celestial.x;
    float cosine = env.celestial.y;
    float horizonGlow = pow(max(0.0, -sign(sine) * ray.x), 6.0) * smoothstep(-0.25, 0.0, ray.y);
    rgb = mix(rgb, env.sunrise.rgb, horizonGlow * env.sunrise.a);

    // Undo the celestial rotation used by the world to locate the sun and current moon phase.
    vec3 local = vec3(ray.z, cosine * ray.y - sine * ray.x, -sine * ray.y - cosine * ray.x);
    if (ray.y > -0.04) {
        if (env.fogAndStars.a > 0.0) {
            vec2 grid = vec2(atan(local.z, local.x) / 6.2831853, asin(clamp(local.y, -1.0, 1.0)) / 3.1415927)
                    * vec2(480.0, 240.0);
            float chance = hash(floor(grid));
            float star = 1.0 - smoothstep(0.09, 0.21, length(fract(grid) - 0.5));
            rgb += vec3(star * step(0.979, chance) * env.fogAndStars.a);
        }
        if (env.celestial.z > 0.0) {
            vec4 disc;
            if (local.y > 0.0) {
                disc = celestialDisc(sun, vec2(0.5) + local.xz / (local.y * 0.6), vec2(0.0), vec2(1.0));
            } else {
                vec2 uv = vec2(0.5) + vec2(-local.x, local.z) / (max(-local.y, 0.00001) * 0.4);
                vec2 phase = vec2(mod(env.celestial.w, 4.0), floor(env.celestial.w / 4.0));
                disc = celestialDisc(moon, uv, phase, vec2(4.0, 2.0));
            }
            rgb += disc.rgb * disc.a * env.celestial.z;
        }
    }
    float lower = ray.y < 0.0 ? skyPlaneVisibility(origin, ray, env.lowerSky.a) : -1.0;
    if (lower >= 0.0) rgb = mix(env.fogAndStars.rgb, env.lowerSky.rgb, lower);
    color = vec4(outputColor(rgb), 1.0);
}
#endif
