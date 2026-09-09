#version 330 compatibility
/* Devouring Storms v5: persistent Story Mode cloud plane.

   Iris/Oculus replaces Minecraft's vanilla/core cloud shader with this file.
   That is why the resource-pack Story Mode clouds used to disappear when a
   shader pack was enabled: the cloud pass stopped using the built-in core
   shader. This pass now carries its own Story Mode treatment permanently:
   white/lavender cloud colour, storm-phase tint, procedural softness, and an
   alpha floor so real cloud geometry cannot fade to nothing just because a
   shader pipeline changed the incoming colour alpha. */

/* DRAWBUFFERS:0 */

in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;

uniform sampler2D gtexture;
uniform int worldTime;
uniform float frameTimeCounter;
uniform float rainStrength;
uniform vec3 fogColor;

#define STORY_CLOUDS      1     // [0 1]
#define CLOUD_VISIBILITY  1.00  // [0.25 0.50 0.75 1.00 1.25 1.50]
#define CLOUD_LAVENDER    0.35  // [0.00 0.15 0.35 0.55 0.75]

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p *= 2.03;
        a *= 0.5;
    }
    return v;
}

void timeWeights(out float day, out float dusk, out float night) {
    float t = float(worldTime);
    dusk  = smoothstep(11000.0, 12800.0, t) * (1.0 - smoothstep(13600.0, 14400.0, t));
    dusk += smoothstep(22000.0, 23200.0, t) * (1.0 - smoothstep(23600.0, 24000.0, t));
    night = smoothstep(13200.0, 14200.0, t) * (1.0 - smoothstep(22200.0, 23400.0, t));
    day   = clamp(1.0 - dusk - night, 0.0, 1.0);
}

vec3 storyCloudColour(vec3 base) {
    float day, dusk, night;
    timeWeights(day, dusk, night);

    vec3 white = vec3(1.00, 1.00, 1.00) * day
               + vec3(1.00, 0.86, 0.94) * dusk
               + vec3(0.40, 0.45, 0.68) * night;
    vec3 shade = vec3(0.70, 0.68, 0.86) * day
               + vec3(0.76, 0.58, 0.76) * dusk
               + vec3(0.08, 0.10, 0.25) * night;

    float stormK = clamp((min(fogColor.r, fogColor.b) - fogColor.g) * 3.0, 0.0, 1.0);
    vec3 stormTint = mix(vec3(0.42, 0.36, 0.60), vec3(0.22, 0.18, 0.34), rainStrength);
    white = mix(white, stormTint, stormK * 0.75);
    shade = mix(shade, stormTint * 0.55, stormK * 0.80);

    float luma = dot(base, vec3(0.299, 0.587, 0.114));
    vec3 shaped = mix(shade, white, smoothstep(0.18, 0.92, luma));
    shaped = mix(shaped, shaped * vec3(1.04, 0.98, 1.12), CLOUD_LAVENDER);
    return mix(base, shaped, 0.88);
}

void main() {
    vec4 color = texture(gtexture, texcoord) * glcolor;

#if STORY_CLOUDS
    // Geometry exists only where Minecraft emitted cloud faces. If a shader
    // pipeline hands us nearly-zero texture alpha, keep a safe floor so the
    // cloud face still renders instead of disappearing completely.
    float geomAlpha = max(glcolor.a, 0.0);
    if (geomAlpha <= 0.001 && color.a <= 0.001) discard;

    float p = fbm(texcoord * 18.0 + vec2(frameTimeCounter * 0.010, -frameTimeCounter * 0.006));
    float soft = 0.78 + 0.22 * smoothstep(0.30, 0.82, p);
    color.a = max(color.a, geomAlpha * 0.34) * soft * CLOUD_VISIBILITY;
    color.rgb = storyCloudColour(color.rgb);
#else
    if (color.a <= 0.0) discard;
#endif

    if (color.a <= 0.01) discard;
    gl_FragData[0] = color;
}
