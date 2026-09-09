#version 330 compatibility

/*
 * Devouring Storms v5 — sky pass.
 *
 * This is the round-5 Story Look sky from the mod's own core shader
 * (position.fsh), ported to Iris uniforms so the SAME look is painted when a
 * shader pack is active:
 *
 *   calm  - EnderCon gate / Sky City pastels by day, warm violet dawn/dusk,
 *           deep indigo night; layered cloud decks (layer -> void gap ->
 *           layer) with pale-blue shadowed fringes.
 *   storm - the phase skies sampled from the Minecraft Story Mode frames:
 *           5.5-5.9 pinkish-violet (violet zenith, magenta mid, SALMON-PINK
 *           horizon - the purple body comes from the storm blob, not the
 *           sky), green-teal frames, sunset-orange frames, deep-purple
 *           frames; blue silhouette rim around the horizon, gigantic purple
 *           line across the upper vault, darker roof tone, and the storm
 *           sky SHRUNK to the sides - overhead the dome collapses into a
 *           dark calm violet while the mod's halo ring quad carries the
 *           coloured glow around the storm's flanks.
 *
 * The mod cannot feed its per-phase ColorModulator tint through Iris, so the
 * storm gate reads fogColor instead: the Wither Storm pulls the world fog
 * purple/magenta, and (min(r,b) - g) isolates exactly that hue - dusk fog is
 * orange (b < g) and night fog is blue (r < g), so neither false-positives.
 * The same gate already drives the storm lightning in final.fsh.
 *
 * Aurora borealis rides on top after dark, and biome hues tint the calm sky.
 */

in vec4 starData;      // rgb = star colour, a > 0.5 marks a star vertex
in vec3 viewPos;

uniform int worldTime;
uniform float frameTimeCounter;
uniform float rainStrength;
uniform vec3 fogColor;
uniform vec3 skyColor;
uniform mat4 gbufferModelViewInverse;

/* user options ------------------------------------------------------------ */
#define SKY_STORY_MODE  1     // [0 1]
#define STORM_SKY       1     // [0 1]
#define SKY_SATURATION  1.15  // [0.50 0.75 1.00 1.15 1.30 1.60 2.00]
#define HORIZON_GLOW    1     // [0 1]
#define AURORA          1     // [0 1]
#define AURORA_STRENGTH 1.00  // [0.00 0.25 0.50 0.75 1.00 1.50 2.00]
#define AURORA_SPEED    1.00  // [0.25 0.50 1.00 1.50 2.00]
#define AURORA_HEIGHT   0.30  // [0.10 0.20 0.30 0.45 0.60]
#define AURORA_R        0.35  // [0.00 0.35 0.60 1.00]
#define AURORA_G        1.00  // [0.00 0.35 0.60 1.00]
#define AURORA_B        0.80  // [0.00 0.20 0.80 1.00]
#define AURORA_COVERAGE 0.55  // [0.20 0.35 0.55 0.75 0.95]
#define AURORA_TIP_R    0.62  // [0.00 0.30 0.62 1.00]
#define AURORA_TIP_G    0.28  // [0.00 0.28 0.60 1.00]
#define AURORA_TIP_B    0.95  // [0.00 0.35 0.60 0.95]

/* biome skies */
#define BIOME_SKIES      1     // [0 1]
#define BIOME_STRENGTH   0.65  // [0.00 0.25 0.45 0.65 0.85 1.00]
#define BIOME_DESERT_R   0.98  // [0.00 0.50 0.75 0.98]
#define BIOME_DESERT_G   0.82  // [0.00 0.50 0.75 0.82]
#define BIOME_DESERT_B   0.58  // [0.00 0.35 0.58 0.90]
#define BIOME_SNOW_R     0.72  // [0.00 0.50 0.72 1.00]
#define BIOME_SNOW_G     0.84  // [0.00 0.50 0.84 1.00]
#define BIOME_SNOW_B     1.00  // [0.00 0.50 0.80 1.00]
#define BIOME_SWAMP_R    0.42  // [0.00 0.25 0.42 0.70]
#define BIOME_SWAMP_G    0.55  // [0.00 0.30 0.55 0.90]
#define BIOME_SWAMP_B    0.40  // [0.00 0.20 0.40 0.70]
#define BIOME_NETHER_R   0.61  // [0.00 0.30 0.61 1.00]
#define BIOME_NETHER_G   0.07  // [0.00 0.07 0.30 0.60]
#define BIOME_NETHER_B   0.48  // [0.00 0.20 0.48 0.90]

/* ---- noise (vec2 for aurora, vec3 for the cloud decks) ------------------ */

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }

float wsNoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x),
               mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 4; i++) { v += a * wsNoise(p); p *= 2.03; a *= 0.5; }
    return v;
}

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float vnoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash13(i);
    float b = hash13(i + vec3(1.0, 0.0, 0.0));
    float c = hash13(i + vec3(0.0, 1.0, 0.0));
    float d = hash13(i + vec3(1.0, 1.0, 0.0));
    float e = hash13(i + vec3(0.0, 0.0, 1.0));
    float g = hash13(i + vec3(1.0, 0.0, 1.0));
    float h = hash13(i + vec3(0.0, 1.0, 1.0));
    float k = hash13(i + vec3(1.0, 1.0, 1.0));
    return mix(mix(mix(a, b, f.x), mix(c, d, f.x), f.y),
               mix(mix(e, g, f.x), mix(h, k, f.x), f.y), f.z);
}

float fbm3(vec3 p) {
    float s = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        s += a * vnoise(p);
        p *= 2.03;
        a *= 0.5;
    }
    return s;
}

/* ---- shared cloud decks: layer -> void gap -> layer ---------------------- */

vec3 paintDecks(vec3 dirS, vec3 col, float acc0, vec3 litCol, vec3 shadeCol,
                float dayness, float warm, float sideFade, float mirror) {
    // mirror = 1 paints the SAME decks mirrored into the lower hemisphere:
    // the sky dome's bottom half only shows where terrain does not, so on
    // the ground this reads as a far cloud sea past the edge, and from Sky
    // City altitude it is the layers you fall through (user order: fall
    // through 5-15 cloud layers). No camera-height uniform needed.
    float dy = (mirror > 0.5) ? max(-dirS.y, 0.02) : dirS.y;
    if (dy <= 0.02) {
        return col;
    }
    vec2 pxz = dirS.xz / dy;
    float H[9];
    H[0] = 96.0;  H[1] = 146.0; H[2] = 152.0; H[3] = 420.0; H[4] = 430.0;
    H[5] = 1200.0; H[6] = 3500.0; H[7] = 9000.0; H[8] = 16000.0;
    float acc = acc0;
    for (int i = 0; i < 9; i++) {
        int grp = (i < 3) ? 0 : ((i < 6) ? 1 : 2);
        vec2 uv = pxz * (120.0 / pow(H[i] / 96.0, 0.55)) + vec2(float(i) * 7.3);
        float pres = (grp == 0) ? 1.0
                : smoothstep(0.30, 0.44, fbm3(vec3(pxz * 0.010 + vec2(float(grp) * 31.7), float(grp) * 13.0)));
        float cov = fbm3(vec3(uv * 0.9, float(i) * 3.1));
        float gapmask = smoothstep(0.40, 0.54, fbm3(vec3(uv * 0.33, float(i) * 9.0)));
        float nest = fbm3(vec3(uv * 3.4 + 17.0, float(i) * 5.7));
        float th = (i < 3) ? 0.62 : ((i < 7) ? 0.50 : 0.44);
        float ceilBonus = (i == 8) ? 0.25 : 0.0;
        float a = smoothstep(th, th + 0.08, cov) * gapmask * pres
                * (0.70 + 0.30 * smoothstep(0.35, 0.75, nest))
                + ceilBonus * smoothstep(0.35, 0.6, cov) * pres;
        // soften the deck edge into the horizon: kills the roof/side seam
        a *= smoothstep(0.02, 0.12, dy);
        // storm skies keep their decks on the sides, not overhead (upward
        // pass only - the mirrored sea below wants full coverage straight
        // down); mirrored decks sit a touch thinner overall
        if (mirror < 0.5) {
            a *= mix(1.0, sideFade, smoothstep(0.30, 0.70, dy));
        } else {
            a *= 0.85;
        }
        a = min(a, 0.92) * (1.0 - acc);
        float core = smoothstep(th - 0.12, th + 0.34, cov);
        vec3 dc = mix(shadeCol, litCol, min(mix(0.55, 0.82, mirror) + 0.45 * core, 1.0));
        dc *= (0.97 + 0.05 * float(i));
        dc = mix(dc, dc * vec3(1.06, 0.98, 0.88), (1.0 - clamp(dy, 0.0, 1.0)) * warm);
        col = mix(col, dc, a);
        acc += a * 0.85;
        if (acc > 0.97) {
            break;
        }
    }
    return col;
}

/* smooth day -> dusk -> night -> dawn weights from worldTime */
void timeWeights(out float day, out float dusk, out float night) {
    float t = float(worldTime);
    day   = smoothstep(23000.0, 500.0, t) + smoothstep(1000.0, 2000.0, t) * step(t, 11000.0);
    day   = clamp(day, 0.0, 1.0);
    dusk  = smoothstep(11000.0, 12800.0, t) * (1.0 - smoothstep(13600.0, 14400.0, t));
    dusk += smoothstep(22000.0, 23200.0, t) * (1.0 - smoothstep(23600.0, 24000.0, t));
    night = smoothstep(13200.0, 14200.0, t) * (1.0 - smoothstep(22200.0, 23400.0, t));
    day   = clamp(1.0 - dusk - night, 0.0, 1.0);
}

/* ---- calm Story Mode sky (EnderCon / Sky City pastels) -------------------- */

vec3 storyCalmSky(vec3 dirS) {
    float day, dusk, night;
    timeWeights(day, dusk, night);

    // vanilla's dawn and dusk both read orange, so the dawn palette carries
    // both ends of the day here - exactly like the core-shader round-5 sky.
    vec3 zen = day   * vec3(0.216, 0.394, 0.716)
             + dusk  * vec3(0.620, 0.560, 0.810)
             + night * vec3(0.010, 0.014, 0.070);
    vec3 mid = day   * vec3(0.394, 0.578, 0.806)
             + dusk  * vec3(0.620, 0.560, 0.810)
             + night * vec3(0.010, 0.014, 0.070);
    vec3 hor = day   * vec3(0.870, 0.745, 0.690)
             + dusk  * vec3(0.890, 0.680, 0.730)
             + night * vec3(0.019, 0.031, 0.130);

    // per-biome variants (vanilla hands the biome hue through fogColor)
    float gk = clamp((fogColor.g - max(fogColor.r, fogColor.b)) * 3.0, 0.0, 0.6) * day;
    float wk = clamp((fogColor.r - fogColor.b) * 1.2, 0.0, 0.6) * day * (1.0 - dusk);
    zen = mix(zen, vec3(0.150, 0.420, 0.470), gk);
    mid = mix(mid, vec3(0.320, 0.580, 0.530), gk);
    hor = mix(hor, vec3(0.620, 0.800, 0.700), gk);
    zen = mix(zen, vec3(0.350, 0.450, 0.700), wk);
    mid = mix(mid, vec3(0.560, 0.620, 0.760), wk);
    hor = mix(hor, vec3(0.880, 0.760, 0.640), wk);

    float ty = clamp(dirS.y, -1.0, 1.0);
    float t = pow(1.0 - clamp(ty, 0.0, 1.0), 1.35);

    vec3 col = mix(zen, mid, smoothstep(0.10, 0.60, t));
    col = mix(col, hor, smoothstep(0.75, 0.98, t));
#if HORIZON_GLOW
    col += hor * 0.14 * exp(-abs(ty) * 7.0);
#endif

    // white story clouds with pale-blue shadowed fringes
    vec3 litC = mix(vec3(0.960, 0.975, 1.000), hor, 0.10);
    vec3 shadeC = mix(zen, hor, 0.35) * 0.85;
    col = paintDecks(dirS, col, 0.0, litC, shadeC, day, 0.5, 1.0, 0.0);
    col = paintDecks(dirS, col, 0.0, litC, shadeC, day, 0.5, 1.0, 1.0);

    col = mix(col, hor * 0.5, smoothstep(0.0, -0.3, ty));
    float lum = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(vec3(lum), col, SKY_SATURATION);
    col = mix(col, col * col * (3.0 - 2.0 * col), 0.22);
    return col;
}

/* ---- storm-phase sky (round-5 port) --------------------------------------- */

vec3 storyStormSky(vec3 dirS) {
    vec3 C = fogColor;
    float clum = dot(C, vec3(0.2126, 0.7152, 0.0722));

    float greenK  = clamp((C.g - max(C.r, C.b)) * 2.5, 0.0, 1.0);
    float orangeK = clamp((C.r - C.g) * 2.2, 0.0, 1.0) * step(C.b, C.g) * (1.0 - greenK);
    float pinkK   = clamp(1.0 - abs(C.r - C.b) * 3.0, 0.0, 1.0)
                  * step(C.g * 1.05, min(C.r, C.b)) * (1.0 - greenK);
    float magK    = clamp((C.r - C.b) * 2.0, 0.0, 1.0) * (1.0 - orangeK) * (1.0 - greenK);
    float wsum = pinkK + greenK + orangeK + magK;
    if (wsum < 0.02) {
        magK = 1.0;
        wsum = 1.0;
    }
    // 5.5-5.9 pinkish-violet (violet zenith, salmon-pink horizon)
    vec3 z1 = vec3(0.055, 0.022, 0.130);
    vec3 m1 = vec3(0.200, 0.060, 0.230);
    vec3 h1 = vec3(0.640, 0.300, 0.310);
    // green-teal frames
    vec3 z2 = vec3(0.050, 0.110, 0.095);
    vec3 m2 = vec3(0.120, 0.220, 0.180);
    vec3 h2 = vec3(0.440, 0.560, 0.360);
    // sunset-orange frames
    vec3 z3 = vec3(0.120, 0.060, 0.080);
    vec3 m3 = vec3(0.350, 0.140, 0.110);
    vec3 h3 = vec3(0.780, 0.280, 0.100);
    // deep purple / magenta frames
    vec3 z4 = vec3(0.070, 0.022, 0.120);
    vec3 m4 = vec3(0.230, 0.055, 0.220);
    vec3 h4 = vec3(0.560, 0.220, 0.320);
    vec3 zen = (z1 * pinkK + z2 * greenK + z3 * orangeK + z4 * magK) / wsum;
    vec3 mid = (m1 * pinkK + m2 * greenK + m3 * orangeK + m4 * magK) / wsum;
    vec3 hor = (h1 * pinkK + h2 * greenK + h3 * orangeK + h4 * magK) / wsum;
    // keep the world's own tint in the mix so the blob colour still reads
    zen = mix(zen, C * 0.35, 0.30);
    mid = mix(mid, C * 0.80, 0.30);
    hor = mix(hor, C * 1.35, 0.22);

    float ty = clamp(dirS.y, -1.0, 1.0);
    float t = pow(1.0 - clamp(ty, 0.0, 1.0), 1.35);

    vec3 col = mix(zen, mid, smoothstep(0.04, 0.45, t));
    col = mix(col, hor, smoothstep(0.45, 0.95, t));
#if HORIZON_GLOW
    // soft horizon glow band, continuous - no seam between vault and rim
    col += hor * 0.22 * exp(-abs(ty) * 6.0);
#endif
    // blue silhouette rim hugging the horizon, all the way around
    float rim = exp(-abs(ty - 0.015) * 42.0);
    col = mix(col, vec3(0.16, 0.34, 0.95), rim * 0.50);
    // gigantic purple line across the upper vault
    float topLine = exp(-abs(ty - 0.72) * 26.0);
    col = mix(col, vec3(0.40, 0.15, 0.85), topLine * 0.30);
    // darker back tone so the roof reads heavier than the sides
    col *= 1.0 - 0.38 * smoothstep(0.50, 1.0, ty);
    // the storm sky SHRINKS to the sides: overhead the dome collapses into a
    // dark calm violet; the mod's halo ring quad carries the flank glow.
    float over = smoothstep(0.30, 0.70, ty);
    float olum = dot(col, vec3(0.299, 0.587, 0.114));
    vec3 ocol = mix(vec3(olum) * vec3(0.42, 0.30, 0.52), vec3(0.02, 0.012, 0.03), 0.55);
    col = mix(col, ocol, over * 0.85);

    // purple-lit storm decks, kept on the sides (sideFade 0.35)
    vec3 litC = mix(vec3(0.52, 0.42, 0.62), hor, 0.35);
    vec3 shadeC = mix(zen, hor, 0.30) * 0.60;
    col = paintDecks(dirS, col, 0.0, litC, shadeC, 0.35, 0.5, 0.35, 0.0);
    col = paintDecks(dirS, col, 0.0, litC, shadeC, 0.35, 0.5, 0.80, 1.0);

    col = mix(col, hor * 0.45, smoothstep(0.0, -0.35, ty));
    float lum = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(vec3(lum), col, 1.22);
    col = mix(col, col * col * (3.0 - 2.0 * col), 0.25);
    return col;
}

/* Aurora borealis ribbons, as requested from the reference frames. */
vec3 aurora(vec3 dir, float night) {
#if AURORA == 0
    return vec3(0.0);
#else
    if (dir.y < 0.02) return vec3(0.0);
    float t = frameTimeCounter * 0.06 * AURORA_SPEED;
    // project onto a plane well above the player so ribbons stretch to the horizon
    vec2 p = dir.xz / max(dir.y, 0.05);
    float acc = 0.0;
    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        vec2 q = p * (0.45 + fi * 0.22) + vec2(t * (1.0 + fi * 0.35), -t * 0.5);
        float ribbon = fbm(q + vec2(fbm(q * 0.5 + t), 0.0) * 1.6);
        // thin the field into curtains
        ribbon = pow(smoothstep(0.42, 0.86, ribbon), 2.4);
        acc += ribbon * (1.0 - fi * 0.22);
    }
    acc *= smoothstep(0.02, AURORA_HEIGHT + 0.25, dir.y);
    acc *= night;                       // only after dark
    acc *= (1.0 - rainStrength * 0.8);
    vec3 col = vec3(AURORA_R, AURORA_G, AURORA_B);
    // green core fading to violet at the tips, like the real thing
    col = mix(col, vec3(AURORA_TIP_R, AURORA_TIP_G, AURORA_TIP_B), clamp(acc * 0.55, 0.0, 1.0));
    return col * acc * AURORA_COVERAGE * AURORA_STRENGTH;
#endif
}

/* Biome sky, calm weather only (the storm owns the hue when it is near).
 * Iris hands us fogColor, which Minecraft already varies per biome; its hue
 * classifies the biome without needing a biome ID uniform. */
vec3 biomeTint(vec3 base) {
#if BIOME_SKIES == 0
    return base;
#else
    float r = fogColor.r, g = fogColor.g, b = fogColor.b;
    float mx = max(max(r, g), b);

    vec3 tint = base;
    float desert = smoothstep(0.05, 0.22, r - b) * step(0.45, mx);
    float snow   = smoothstep(0.03, 0.18, b - r) * step(0.55, mx);
    float swamp  = smoothstep(0.02, 0.14, g - max(r, b));
    float nether = smoothstep(0.06, 0.25, r - g) * (1.0 - step(0.45, mx));

    tint = mix(tint, vec3(BIOME_DESERT_R, BIOME_DESERT_G, BIOME_DESERT_B), desert * BIOME_STRENGTH);
    tint = mix(tint, vec3(BIOME_SNOW_R,   BIOME_SNOW_G,   BIOME_SNOW_B),   snow   * BIOME_STRENGTH);
    tint = mix(tint, vec3(BIOME_SWAMP_R,  BIOME_SWAMP_G,  BIOME_SWAMP_B),  swamp  * BIOME_STRENGTH);
    tint = mix(tint, vec3(BIOME_NETHER_R, BIOME_NETHER_G, BIOME_NETHER_B), nether * BIOME_STRENGTH);
    return tint;
#endif
}

void main() {
    vec3 dir = normalize(mat3(gbufferModelViewInverse) * viewPos);
    vec4 outc;

    if (starData.a > 0.5) {
        // star vertex: keep vanilla stars, just cool them slightly
        outc = vec4(starData.rgb * vec3(0.86, 0.90, 1.0), 1.0);
    } else {
        float day, dusk, night;
        timeWeights(day, dusk, night);

        // storm gate: the Wither Storm pulls the fog purple/magenta.
        // (min(r,b) - g) fires ONLY on that hue - dusk is orange (b<g),
        // night is blue (r<g) - so neither false-positives into a storm sky.
        float stormK = 0.0;
#if STORM_SKY
        stormK = clamp((min(fogColor.r, fogColor.b) - fogColor.g) * 3.0, 0.0, 1.0)
               * (1.0 - rainStrength * 0.6);
#endif

#if SKY_STORY_MODE
        vec3 c;
        if (stormK > 0.02) {
            vec3 calm = storyCalmSky(dir);
            vec3 storm = storyStormSky(dir);
            c = mix(calm, storm, stormK);
        } else {
            c = storyCalmSky(dir);
            c = biomeTint(c);
        }
#else
        vec3 c = skyColor;
#endif
        c += aurora(dir, night);
        outc = vec4(c, 1.0);
    }
    gl_FragData[0] = outc;
}
