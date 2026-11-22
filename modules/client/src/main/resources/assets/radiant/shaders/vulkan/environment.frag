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
    vec4 cloudFog;
    float cloudClipDistance;
} env;
layout(set = 0, binding = 1) uniform sampler2D clouds;
layout(set = 0, binding = 2) uniform sampler2D sun;
layout(set = 0, binding = 3) uniform sampler2D moon;
layout(set = 0, binding = 4) uniform sampler2D endSky;
#if END_SKY_PASS
layout(location = 0) in vec2 uv;
#elif SUNRISE_PASS
layout(location = 0) in vec4 clipPosition;
layout(location = 1) in float alpha;
#elif STAR_PASS
layout(location = 0) noperspective in vec2 ndc;
#else
layout(location = 0) in vec2 ndc;
#endif
layout(location = 0) out vec4 color;

vec3 outputColor(vec3 rgb) {
    rgb = clamp(rgb, 0.0, 1.0);
#if SRGB_TARGET
    return mix(rgb / 12.92, pow((rgb + 0.055) / 1.055, vec3(2.4)), greaterThan(rgb, vec3(0.04045)));
#else
    return rgb;
#endif
}

#if END_SKY_PASS
void main() {
    vec3 texel = texture(endSky, uv).rgb;
    color = vec4(outputColor(texel * env.skyColor.rgb), 1.0);
}
#else
vec3 unproject(float depth) {
    #if SUNRISE_PASS
    vec2 screen = clipPosition.xy / clipPosition.w;
    #else
    vec2 screen = ndc;
    #endif
    vec4 relative = env.inverseViewProjection * vec4(screen.x, -screen.y, depth, 1.0);
    return relative.xyz / relative.w;
}

float fogVisibility(vec4 fog, float depth) {
    if (fog.x == 1.0) return clamp((fog.z - depth) / max(fog.z - fog.y, 1e-4), 0.0, 1.0);
    if (fog.x == 2.0) return exp(-depth * fog.w);
    if (fog.x == 3.0) {
        float density = depth * fog.w;
        return exp(-density * density);
    }
    return 1.0;
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
        vec2 point = origin.xz + ray.xz * distance;
        if (any(greaterThan(abs(point), vec2(256.0)))) {
            return vec4(0.0);
        }
        vec2 uv = (point + env.cloudOffset.xy) / 2048.0;
        return textureLod(clouds, fract(uv), 0.0);
    }

    // Fancy clouds are a four-block slab with one solid column for each covered texture cell.
    float bottom = (base - origin.y) / ray.y;
    float top = (base + 4.0 - origin.y) / ray.y;
    float entry = max(0.0, min(bottom, top));
    float exitDistance = min(range, max(bottom, top));
    // The fancy cloud mesh spans eight tiles per axis, starting three tiles behind its origin.
    vec2 tileOffset = mod(env.cloudOffset.xy, 12.0);
    vec2 lower = vec2(-288.0) - tileOffset;
    vec2 upper = vec2(480.0) - tileOffset;
    for (int axis = 0; axis < 2; axis++) {
        float direction = ray.xz[axis];
        float start = origin.xz[axis];
        if (abs(direction) < 1e-6) {
            if (start < lower[axis] || start > upper[axis]) return vec4(0.0);
        } else {
            float a = (lower[axis] - start) / direction;
            float b = (upper[axis] - start) / direction;
            entry = max(entry, min(a, b));
            exitDistance = min(exitDistance, max(a, b));
        }
    }
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
        if (texel.a * 0.8 > 0.1) {
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
    const float nearPlane = 0.05;
    vec3 depthAxis = vec3(env.viewProjection[0].w, env.viewProjection[1].w, env.viewProjection[2].w);
    float range = (env.cloudClipDistance - nearPlane) / dot(depthAxis, ray);
    float distance;
    float shade;
    vec4 texel = traceCloud(origin, ray, range, distance, shade);
    float alpha = texel.a * 0.8;
    if (alpha <= 0.1) {
        discard;
    }
    vec4 clip = env.viewProjection * vec4(origin + ray * distance, 1.0);
    // Clouds use the same view and field of view with a farther clipping plane than terrain.
    gl_FragDepth = clamp((1.0 - nearPlane / clip.w) * env.cloudClipDistance
            / (env.cloudClipDistance - nearPlane), 0.0, 1.0);
    // The legacy cloud mesh stores its shaded tint in unsigned bytes before texturing and fog.
    vec3 tint = floor(clamp(env.cloudColor.rgb * shade, 0.0, 1.0) * 255.0) / 255.0;
    vec3 rgb = mix(env.fogAndStars.rgb, tint * texel.rgb, fogVisibility(env.cloudFog, clip.w));
    color = vec4(outputColor(rgb), alpha);
}
#else
vec4 celestialDisc(sampler2D image, vec2 uv, vec2 tile, vec2 sheet) {
    if (any(lessThan(uv, vec2(0.0))) || any(greaterThanEqual(uv, vec2(1.0)))) {
        return vec4(0.0);
    }
    return textureLod(image, (tile + uv) / sheet, 0.0);
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

    return fogVisibility(env.skyFog, clip.w);
}

#if SUNRISE_PASS
void main() {
    vec3 origin = unproject(-1.0);
    vec3 ray = normalize(unproject(1.0) - origin);
    if (ray.y < 0.0 && skyPlaneVisibility(origin, ray, env.lowerSky.a) >= 0.0) discard;
    vec3 tint = floor(env.sunrise.rgb * 255.0) / 255.0;
    color = vec4(outputColor(tint), alpha);
}
#elif STAR_PASS
void main() {
    // The lower sky is drawn after the stars in OpenGL and covers them where the plane is visible.
    vec3 origin = unproject(-1.0);
    vec3 ray = normalize(unproject(1.0) - origin);
    if (ray.y < 0.0 && skyPlaneVisibility(origin, ray, env.lowerSky.a) >= 0.0) discard;
    float brightness = env.fogAndStars.a;
    color = vec4(outputColor(vec3(brightness)), brightness);
}
#else
vec3 celestialColor(vec3 origin, vec3 ray) {
    float sine = env.celestial.x;
    float cosine = env.celestial.y;

    // Undo the celestial rotation used by the world to locate the sun and current moon phase.
    vec3 local = vec3(ray.z, cosine * ray.y - sine * ray.x, -sine * ray.y - cosine * ray.x);
    if (env.celestial.z > 0.0 && abs(local.y) > 1e-6) {
        vec3 localOrigin = vec3(origin.z, cosine * origin.y - sine * origin.x, -sine * origin.y - cosine * origin.x);
        bool sunward = local.y > 0.0;
        float distance = ((sunward ? 100.0 : -100.0) - localOrigin.y) / local.y;
        vec4 clip = env.viewProjection * vec4(origin + ray * distance, 1.0);
        if (distance >= 0.0 && clip.z <= clip.w) {
            vec2 hit = localOrigin.xz + local.xz * distance;
            vec4 disc;
            if (sunward) {
                disc = celestialDisc(sun, vec2(0.5) + hit / 60.0, vec2(0.0), vec2(1.0));
            } else {
                vec2 uv = vec2(0.5) + vec2(-hit.x, hit.y) / 40.0;
                vec2 phase = vec2(mod(env.celestial.w, 4.0), floor(env.celestial.w / 4.0));
                disc = celestialDisc(moon, uv, phase, vec2(4.0, 2.0));
            }
            return disc.rgb * disc.a * env.celestial.z;
        }
    }
    return vec3(0.0);
}

void main() {
    if (env.cloudOffset.w == 2.0) {
        color = vec4(outputColor(env.fogAndStars.rgb), 1.0);
        return;
    }
    vec3 origin = unproject(-1.0);
    vec3 ray = normalize(unproject(1.0) - origin);
    float lower = ray.y < 0.0 ? skyPlaneVisibility(origin, ray, env.lowerSky.a) : -1.0;
    #if CELESTIAL_PASS
    if (lower >= 0.0) discard;
    color = vec4(outputColor(celestialColor(origin, ray)), 1.0);
    #else
    float upper = ray.y > 0.0 ? skyPlaneVisibility(origin, ray, 16.0) : -1.0;
    vec3 rgb = mix(env.fogAndStars.rgb, env.skyColor.rgb, max(upper, 0.0));
    if (env.sunrise.a <= 0.0) rgb += celestialColor(origin, ray);
    if (lower >= 0.0) rgb = mix(env.fogAndStars.rgb, env.lowerSky.rgb, lower);
    color = vec4(outputColor(rgb), 1.0);
    #endif
}
#endif
#endif
#endif
