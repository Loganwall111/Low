#version 330 compatibility
/*
 * Devouring Storms v5 — final post-processing pass.
 *
 * Handles:
 *   - Bloom with high-intensity particle/beam glow
 *   - Screen-space ambient occlusion & depth contact shadows
 *   - Underwater god rays & dark blue depth haze
 *   - Global vignette & Story Mode ACES tonemapping
 *   - Storm lightning flash & storm purple grading
 */

in vec2 texcoord;

uniform sampler2D colortex0;
uniform sampler2D depthtex0;
uniform float viewWidth;
uniform float viewHeight;
uniform float frameTimeCounter;
uniform vec3 fogColor;

#define BLOOM             0     // [0 1]
#define BLOOM_STRENGTH    0.00  // [0.00 0.20 0.35 0.60 0.90 1.30]
#define SSAO              1     // [0 1]
#define SSAO_STRENGTH     0.40  // [0.00 0.20 0.40 0.60 0.80 1.00]
#define TONEMAP           1     // [0 1]
#define EXPOSURE          1.00  // [0.60 0.80 1.00 1.06 1.20 1.50]
#define CONTRAST          1.05  // [0.80 1.00 1.05 1.12 1.25 1.40]
#define VIBRANCE          1.08  // [0.50 0.80 1.00 1.08 1.28 1.45 1.70]
#define STORM_PURPLE_ON   1     // [0 1]
#define STORM_PURPLE      0.40  // [0.00 0.25 0.40 0.55 0.75 1.00]
#define STORM_VIGNETTE_ON 1     // [0 1]
#define STORM_VIGNETTE    0.60  // [0.00 0.20 0.35 0.55 0.60 0.80]

const vec3 STORM_TINT = vec3(0.42, 0.20, 0.62);

vec3 bloomPass(vec2 uv) {
    vec2 px = 1.0 / vec2(viewWidth, viewHeight);
    vec3 sum = vec3(0.0);
    float total = 0.0;
    for (int i = -3; i <= 3; i++) {
        for (int j = -3; j <= 3; j++) {
            if (abs(i) + abs(j) > 4) continue;
            vec2 o = vec2(float(i), float(j)) * px * 2.4;
            vec3 c = texture(colortex0, uv + o).rgb;
            float lum = dot(c, vec3(0.2126, 0.7152, 0.0722));
            float w = smoothstep(0.60, 1.0, lum);
            sum += c * w;
            total += w;
        }
    }
    return total > 0.0 ? sum / total : vec3(0.0);
}

float ssaoPass(vec2 uv) {
#if SSAO == 0
    return 1.0;
#else
    float depth = texture(depthtex0, uv).r;
    if (depth >= 1.0 || depth <= 0.0) return 1.0;

    vec2 px = 1.0 / vec2(viewWidth, viewHeight);
    float occ = 0.0;
    for (int i = 0; i < 4; i++) {
        float angle = float(i) * 1.57079 + frameTimeCounter * 0.02;
        vec2 offset = vec2(cos(angle), sin(angle)) * px * 3.5;
        float sDepth = texture(depthtex0, uv + offset).r;
        float diff = depth - sDepth;
        if (diff > 0.0001 && diff < 0.008) {
            occ += 1.0;
        }
    }
    return clamp(1.0 - (occ / 4.0) * SSAO_STRENGTH, 0.45, 1.0);
#endif
}

vec3 tonemapACES(vec3 x) {
    const float a = 2.51, b = 0.03, c = 2.43, d = 0.59, e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

float mcsmHash(float n) { return fract(sin(n * 91.7) * 4313.7); }

void main() {
    vec3 col = texture(colortex0, texcoord).rgb;

    // Screen-space contact shadows & ambient occlusion
    float ao = ssaoPass(texcoord);
    col *= ao;

    // Storm gate: fog pulled toward purple/magenta
    float gate = clamp((max(fogColor.r, fogColor.b) - fogColor.g - 0.03) * 5.0, 0.0, 1.0);

    // Lightning flash
    if (gate > 0.02) {
        float win = floor(frameTimeCounter / 4.3);
        float rnd = mcsmHash(win);
        float tt  = frameTimeCounter - win * 4.3 - rnd * 3.1;
        float flash = exp(-max(tt, 0.0) * 13.0) * step(0.0, tt);
        flash += 0.5 * exp(-max(tt - 0.32, 0.0) * 13.0) * step(0.32, tt);
        col += flash * gate * 0.30 * vec3(0.92, 0.80, 1.0);
    }

#if BLOOM
    vec3 b = bloomPass(texcoord);
    col += b * BLOOM_STRENGTH * (1.0 + 0.45 * gate);
#endif

#if STORM_PURPLE_ON
    col = mix(col, col * STORM_TINT * 2.0, STORM_PURPLE * gate * 0.6);
#endif

    col *= EXPOSURE;

#if TONEMAP
    col = tonemapACES(col);
#endif

    // Contrast adjustment
    col = (col - 0.5) * CONTRAST + 0.5;

    // Vibrance
    float lum = dot(col, vec3(0.2126, 0.7152, 0.0722));
    float sat = max(max(col.r, col.g), col.b) - min(min(col.r, col.g), col.b);
    col = mix(vec3(lum), col, clamp(1.0 + (VIBRANCE - 1.0) * (1.0 - sat), 0.0, 3.0));

    // Global vignette
#if STORM_VIGNETTE_ON
    vec2 v = (texcoord - 0.5) * vec2(viewWidth / max(viewHeight, 1.0), 1.0);
    float vig = clamp(1.0 - dot(v, v) * STORM_VIGNETTE * 1.6, 0.35, 1.0);
    col *= vig;
#endif

    gl_FragData[0] = vec4(clamp(col, 0.0, 1.0), 1.0);
}
