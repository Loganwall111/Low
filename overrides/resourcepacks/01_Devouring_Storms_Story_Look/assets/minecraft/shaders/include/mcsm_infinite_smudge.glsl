// ============================================================================
//  MCSM visuals - mcsm_infinite_smudge.glsl   (MCSM 1.9.200)
//
//  INFINITE SKYBOX ANGULAR SMUDGE — the final Wither Storm atmosphere layer.
//
//  This is NOT a 3D shape and NOT a screen-space overlay. The whole field is
//  a pure function of the fragment's world-space view direction. There is no
//  geometry, no box, no sphere, no distance-to-surface term: the cloud wall
//  lives at infinity, so no matter how far the player flies in any direction
//  they can never get physically closer to it (RULE 1).
//
//  RULE 1 -- INFINITE SPATIAL WRAPPING
//    All coordinates below are direction-only (unit vectors and angles in
//    the player's world-space view/look frame). Nothing depends on the
//    camera's position, so the smudge is infinitely distant and can never be
//    caught up to or flown through.
//
//  RULE 2 -- VERTICAL AND HORIZONTAL SMEARING
//    The angular centre of the field is the dot product between the player's
//    view vector and the direction toward the Wither Storm. The horizontal
//    look-vector axis is multiplied by 2.5x in the mapping metric (the full
//    360-degree azimuth band is folded into 2.5x less mapping space), which
//    crushes the mapping space and stretches the fbm field out into a massive,
//    uneven horizontal ink smudge instead of a clean geometric circle.
//
//  RULE 3 -- THE NEVER-ENDING REVOLUTION
//    u_StormProximity (0.0 .. 1.0, distance to the storm) expands the field:
//    far away it is a horizon ink-smear around the storm; as proximity rises
//    the field lerps across the whole 360-degree viewport; at 1.0 the vanilla
//    sky is completely eclipsed by the seamless, infinite phase void.
//
//  RULE 4 -- HEX COLOR INTEGRATION (HARDCODED vec3 PROFILES)
//    The exact extracted colour profiles are compiled into the render path,
//    selected by u_StormPhase:
//      5  = image 1        (green/teal energy)
//      55 = images 2 & 3   (deep purple void, phase 5.5 - 5.9)
//      6  = endgame sunset split
//    They are used verbatim -- no rescale, no grade here (the pipeline's
//    shared story grade still applies to the final dome in sky.fsh).
//
//  The edge gradients are warped by a 2D Fractal Brownian Motion noise loop
//  (5 octaves + domain warp) so the boundary reads torn, organic and
//  perfectly diffuse, like the purple mass bleeding across the reference sky.
//
//  Import AFTER fog.glsl, globals.glsl and mcsm_visuals.glsl
//  (mcsm-core-shaders/core/sky.fsh does exactly this).
// ============================================================================

uniform float witherstorm_Proximity;   // 0.0 .. 1.0 when bound, else 0

// RULE 2: the 2.5x horizontal crush factor.
const float MCSM_SMEAR_HORZ = 2.5;
// u_StormProximity carrier band on FogRenderDistanceStart (Java:
// McsmBlobCarrierPatch). mcsm_rd_start() in mcsm_visuals.glsl already maps
// the whole 9001..9299 band back to a sane fog distance, so fog math never
// sees these values. Vanilla near-fog sits around 1..20, so the band can
// never collide with a real distance.
const float MCSM_PROX_BAND_LO  = 9100.0;
const float MCSM_PROX_BAND_SPAN = 199.0;

// ---------------------------------------------------------------------------
// u_StormProximity decode (0.0 .. 1.0).
//   1) witherstorm_Proximity uniform when the custom pipeline binds it
//   2) FogRenderDistanceStart carrier band 9100.0 .. 9299.0
//   3) 0.0 otherwise (an unpatched jar degrades to the local smudge only)
// ---------------------------------------------------------------------------
float mcsm_storm_proximity() {
    if (witherstorm_Proximity > 0.0 && witherstorm_Proximity <= 1.0)
        return clamp(witherstorm_Proximity, 0.0, 1.0);
    float v = FogRenderDistanceStart;
    if (v >= MCSM_PROX_BAND_LO - 0.5 && v <= MCSM_PROX_BAND_LO + MCSM_PROX_BAND_SPAN + 0.5)
        return clamp((v - MCSM_PROX_BAND_LO) / MCSM_PROX_BAND_SPAN, 0.0, 1.0);
    return 0.0;
}

// ---------------------------------------------------------------------------
// 2D noise + Fractal Brownian Motion loop.
// Hashed value noise on the integer grid, 5 octaves (lacunarity ~2.03,
// gain 0.5) -- the "2D fbm loop" that tears the edge gradients.
// ---------------------------------------------------------------------------
float mcsm_smudge_hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float mcsm_smudge_vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = mcsm_smudge_hash(i);
    float b = mcsm_smudge_hash(i + vec2(1.0, 0.0));
    float c = mcsm_smudge_hash(i + vec2(0.0, 1.0));
    float d = mcsm_smudge_hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float mcsm_smudge_fbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.5;
    float tot = 0.0;
    for (int o = 0; o < 5; o++) {
        sum += mcsm_smudge_vnoise(p) * amp;
        tot += amp;
        p = p * 2.03 + vec2(17.13, 9.71);
        amp *= 0.5;
    }
    return sum / tot;
}

// ---------------------------------------------------------------------------
// RULE 4 -- the exact extracted colour profiles, hard-coded.
// u_StormPhase semantics: 5 = phase-5.x teal energy, 55 = phase 5.5-5.9
// purple void, 6 = phase 6+ endgame sunset split.
// ---------------------------------------------------------------------------
int mcsm_smudge_phase_id(float p) {
    if (p < 5.45) return 5;
    if (p < 6.00) return 55;
    return 6;
}

void mcsm_smudge_palette(float p, out vec3 cCore, out vec3 cInner,
                         out vec3 cMid, out vec3 cOuter) {
    int u_StormPhase = mcsm_smudge_phase_id(p);
    if (u_StormPhase == 5) { // Image 1 (Green/Teal Energy)
        cCore   = vec3(0.086, 0.102, 0.114); // #161A1D
        cMid    = vec3(0.176, 0.259, 0.247); // #2D423F
        cOuter  = vec3(0.416, 0.604, 0.471); // #6A9A78
        cInner  = mix(cCore, cMid, 0.5);
    } else if (u_StormPhase == 55) { // Image 2 & 3 (Deep Purple Void)
        cCore   = vec3(0.043, 0.016, 0.063); // #0B0410
        cInner  = vec3(0.176, 0.078, 0.259); // #2D1442
        cMid    = vec3(0.345, 0.110, 0.431); // #581C6E
        cOuter  = vec3(0.529, 0.322, 0.612); // #87529C
    } else { // u_StormPhase == 6 -- Endgame Sunset Split
        // vertical split profile: zenith (top) -> base (bottom)
        cCore   = vec3(0.102, 0.071, 0.149); // #1A1226  (c_Zenith)
        cInner  = vec3(0.275, 0.165, 0.322); // #462A52  (c_MidHi)
        cMid    = vec3(0.588, 0.380, 0.451); // #966173  (c_MidLo)
        cOuter  = vec3(0.847, 0.596, 0.455); // #D89874  (c_Base)
    }
}

// ---------------------------------------------------------------------------
// MAIN ENTRY.
//   viewDir  -- the fragment's world-space view direction (unit vector)
//   stormDir -- world-space direction from the camera toward the Wither Storm
//   p        -- storm phase (the sky-active window 4.95 .. 8.06)
//   clock    -- seconds
// Returns (colour, coverage). coverage == 1.0 where the void owns the sky;
// at u_StormProximity -> 1.0 it is 1.0 everywhere: the vanilla sky is fully
// eclipsed by the seamless infinite phase void (RULE 3).
// ---------------------------------------------------------------------------
vec4 mcsm_infinite_smudge(vec3 viewDir, vec3 stormDir, float p, float clock) {
    vec3 wd = normalize(viewDir);
    vec3 bd = normalize(stormDir);

    // only breathes while the storm owns the sky
    float gate = mcsm_ramp(p, 4.95, 5.05);
    if (gate <= 0.001) return vec4(0.0);

    // ------------------------------------------------------------------ RULE 1
    // Infinite spatial wrapping: everything below is computed from the
    // player's world-space look direction at infinity -- view direction and
    // storm direction only. No position, no radius-to-surface, no geometry:
    // the cloud layer stays infinitely distant forever.
    vec3 upRef = abs(bd.y) > 0.985 ? vec3(0.0, 0.0, 1.0) : vec3(0.0, 1.0, 0.0);
    vec3 ex = normalize(cross(upRef, bd));   // horizontal look axis
    vec3 ey = cross(bd, ex);                 // vertical look axis

    // ------------------------------------------------- RULE 2 (centre): the
    // dot product between the player's view vector and the direction vector
    // pointing toward the Wither Storm -- the angular centre of the smudge.
    float cd = clamp(dot(wd, bd), -1.0, 1.0);
    float az = atan(dot(wd, ex), cd);                // -PI..PI  around the storm
    float el = asin(clamp(dot(wd, ey), -1.0, 1.0));  // -PI/2..PI/2

    // ------------------------------------------------- RULE 2 (crush): the
    // horizontal look-vector axis is multiplied by 2.5x in the mapping
    // metric -- equivalently, the full 360-degree azimuth band is folded
    // into 2.5x less mapping space. The smudge radius therefore reaches 2.5x
    // farther out along the horizontal axis, crushing the mapping space so
    // the field stretches into a massive, uneven HORIZONTAL ink smudge
    // instead of a clean geometric circle.
    float dAz = abs(az) * (180.0 / MCSM_PI);   // angular offset, degrees
    float dEl = abs(el) * (180.0 / MCSM_PI);   // angular offset, degrees
    float hAz = dAz / MCSM_SMEAR_HORZ;         // crushed horizontal axis
    float rMetric = sqrt(hAz * hAz + dEl * dEl);

    // ------------------------------------------- 2D fbm loop: torn, organic,
    // perfectly diffuse edge. Sampled in the crushed mapping space (azimuth
    // pre-divided by the 2.5x crush) so the lobes smear along the horizon.
    vec2 smUV = vec2(az, el) * vec2(0.16, 0.34);
    float drift = clock * 0.016;
    float w1 = mcsm_smudge_fbm(smUV * 2.5 + vec2(drift, -drift * 0.6));
    float w2 = mcsm_smudge_fbm(smUV * 5.3 + vec2(-drift * 1.4, drift) + 7.31);
    vec2 warpUV = smUV + (vec2(w1, w2) - 0.5) * 0.9;
    float w3 = mcsm_smudge_fbm(warpUV * 3.1 + vec2(drift * 2.0, 0.0));
    float torn = (w3 - 0.5) * 2.0;                       // -1..1
    float r = rMetric * (1.0 + 0.42 * torn) + 16.0 * torn;

    // ------------------------------------------- RULE 3: the never-ending
    // revolution. Proximity lerps the field from a horizon ink-smear
    // (rOut ~ 48 degrees around the storm) across the whole 360-degree
    // canvas. rIn/rOut both lerp so the wipe stays smooth at every step.
    float prox = mcsm_storm_proximity();
    float pe = prox * prox * (3.0 - 2.0 * prox);
    float rIn  = mix(6.0,  -40.0, pe);
    float rOut = mix(48.0, 320.0, pe);
    float cover = 1.0 - smoothstep(rIn, rOut, r);
    cover = smoothstep(0.0, 1.0, cover) * gate;
    // hard guarantee: fully inside the core -> the void is seamless and total
    if (pe >= 0.999) cover = 1.0;
    if (cover <= 0.001) return vec4(0.0);

    // ------------------------------------------------ RULE 4: hard-coded hex
    // profiles, used exactly as extracted.
    vec3 cCore, cInner, cMid, cOuter;
    mcsm_smudge_palette(p, cCore, cInner, cMid, cOuter);
    int prof = mcsm_smudge_phase_id(p);

    vec3 col;
    if (prof == 6) {
        // Endgame Sunset Split: colour by (fbm-warped) elevation instead of
        // radius -- c_Base #D89874 at the bottom -> c_MidLo -> c_MidHi ->
        // c_Zenith #1A1226 at the top, bands torn by the same fbm field.
        float t = clamp(el * 0.5 + 0.5, 0.0, 1.0);
        t = clamp(t + (w3 - 0.5) * 0.16, 0.0, 1.0);
        col  = mix(cOuter, cMid,  smoothstep(0.02, 0.46, t));
        col  = mix(col,    cInner, smoothstep(0.42, 0.72, t));
        col  = mix(col,    cCore,  smoothstep(0.68, 0.98, t));
    } else {
        // Radial ink: cCore at the storm centre -> cInner -> cMid ->
        // cOuter at the smudge rim. u is normalised against the current
        // (proximity-expanded) outer radius, so as the revolution proceeds
        // the colour field glides outward with the edge instead of snapping.
        float u = clamp(r / rOut, 0.0, 1.0);
        col  = mix(cMid,  cOuter, smoothstep(0.45, 1.00, u));
        col  = mix(cInner, col,   smoothstep(0.22, 0.55, u));
        col  = mix(cCore,  col,   smoothstep(0.00, 0.30, u));
    }

    // slow roar pulse on the torn field (deliberately subtle)
    col *= 0.97 + 0.03 * sin(clock * 2.6 + az * 3.0);

    return vec4(col, cover);
}
