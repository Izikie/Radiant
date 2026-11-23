layout(std140, set = 0, binding = 0) uniform Environment {
    mat4 viewProjection;
    mat4 inverseViewProjection;
    vec4 fogAndStars;
    vec4 skyColor;
    vec4 sunrise;
    vec4 celestial;
    vec4 lowerSky;
    vec4 skyFog;
    vec4 cloudFog;
    float skyMode;
    float customSky;
} env;
layout(set = 0, binding = 1) uniform sampler2D clouds;
layout(set = 0, binding = 2) uniform sampler2D sun;
layout(set = 0, binding = 3) uniform sampler2D moon;
layout(set = 0, binding = 4) uniform sampler2D endSky;
#if CLOUD_PASS
layout(location = 0) in vec2 uv;
layout(location = 1) in vec4 cloudTint;
#elif END_SKY_PASS
layout(location = 0) in vec2 uv;
#elif LOWER_SKY_PASS
layout(location = 0) in float fogDepth;
#elif SUNRISE_PASS
layout(location = 0) in float alpha;
#elif !STAR_PASS
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
    vec4 texel = texture(endSky, uv);
    color = vec4(outputColor(texel.rgb * env.skyColor.rgb), texel.a);
}
#else
float fogVisibility(vec4 fog, float depth) {
    if (fog.x == 1.0) return clamp((fog.z - depth) / max(fog.z - fog.y, 1e-4), 0.0, 1.0);
    if (fog.x == 2.0) return exp(-depth * fog.w);
    if (fog.x == 3.0) {
        float density = depth * fog.w;
        return exp(-density * density);
    }
    return 1.0;
}

#if LOWER_SKY_PASS
void main() {
    float visibility = fogVisibility(env.skyFog, fogDepth);
    vec3 rgb = mix(env.fogAndStars.rgb, env.lowerSky.rgb, visibility);
    color = vec4(outputColor(rgb), 1.0);
}
#elif SUNRISE_PASS
void main() {
    vec3 tint = floor(env.sunrise.rgb * 255.0) / 255.0;
    color = vec4(outputColor(tint), alpha);
}
#elif STAR_PASS
void main() {
    float brightness = env.fogAndStars.a;
    color = vec4(outputColor(vec3(brightness)), brightness);
}
#elif CLOUD_PASS
void main() {
    vec4 texel = texture(clouds, uv) * cloudTint;
    if (texel.a <= 0.1) discard;
    vec3 rgb = mix(env.fogAndStars.rgb, texel.rgb, fogVisibility(env.cloudFog, 1.0 / gl_FragCoord.w));
    color = vec4(outputColor(rgb), texel.a);
}
#else
vec3 unproject(float depth) {
    vec4 relative = env.inverseViewProjection * vec4(ndc.x, -ndc.y, depth, 1.0);
    return relative.xyz / relative.w;
}

vec4 celestialDisc(sampler2D image, vec2 uv, vec2 tile, vec2 sheet) {
    if (any(lessThan(uv, vec2(0.0))) || any(greaterThanEqual(uv, vec2(1.0)))) {
        return vec4(0.0);
    }
    return textureLod(image, (tile + uv) / sheet, 0.0);
}

float upperSkyVisibility(vec3 origin, vec3 ray) {
    float extent = env.skyColor.a;
    if (extent <= 0.0 || ray.y < 1e-6) return 0.0;
    float t = (16.0 - origin.y) / ray.y;
    vec3 point = origin + ray * t;
    vec4 clip = env.viewProjection * vec4(point, 1.0);
    if (t < 0.0 || clip.z > clip.w || any(lessThan(point.xz, vec2(-extent)))
            || any(greaterThan(point.xz, vec2(extent + 64.0)))) return 0.0;

    return fogVisibility(env.skyFog, clip.w);
}

vec4 celestialSample(vec3 origin, vec3 ray) {
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
            return vec4(disc.rgb, disc.a * env.celestial.z);
        }
    }
    return vec4(0.0);
}

void main() {
    if (env.skyMode == 2.0) {
        color = vec4(outputColor(env.fogAndStars.rgb), 1.0);
        return;
    }
    vec3 origin = unproject(-1.0);
    vec3 ray = normalize(unproject(1.0) - origin);
    #if CELESTIAL_PASS
    vec4 disc = celestialSample(origin, ray);
    if (disc.a == 0.0) discard;
    #if SRGB_TARGET
    color = vec4(outputColor(disc.rgb * disc.a), 1.0);
    #else
    color = disc;
    #endif
    #else
    vec3 rgb = mix(env.fogAndStars.rgb, env.skyColor.rgb, upperSkyVisibility(origin, ray));
    #if SRGB_TARGET
    if (env.customSky == 0.0 && env.sunrise.a <= 0.0) {
        vec4 disc = celestialSample(origin, ray);
        rgb += disc.rgb * disc.a;
    }
    #endif
    color = vec4(outputColor(rgb), 1.0);
    #endif
}
#endif
#endif
