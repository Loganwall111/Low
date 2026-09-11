#version 330

// ============================================================================
//  MCSM visuals - sky.fsh  (v6)
//
//  Story path (no storm): six-stop columns sampled from the reference PNGs
//  (day + midnight "the regular two" kept exact; no aurora - ribbons gone).
//
//  Storm path v6 follows the CORRECTED storyboard (I misread v5 and deleted
//  the teal; it is restored and sequenced exactly as specified):
//    5.00 turquoise sky
//    5.10 pink-purple
//    5.20 pink, pinker
//    5.30 dark purple
//    5.50-5.95 pink sky with dark purple overhead
//    6.00 grey sky with a bit of purple (the sampled Phase-6 reference)
//    7.00 pink sky
//    8.00 dark red blood sky
//  Transitions are hard-ish (0.08-0.14 phase windows), matching how the
//  Story Mode cutscene snaps colours between beats.
//
//  1.9.215.1 (port): day/night vaults retuned to the 2026-09-11 references
//  (vivid mid-blue day with lilac horizon; PURPLE night, not blue). The
//  storm glare is now the INFINITE SKYBOX BLOB (see mcsm_visuals.glsl)
//  running through 5.00-6.95, and phase 6 uses the four-color sunset split.
//
//  Bodies (sun/moon) fade OUT as the storm matures - "the sun shining
//  through the storm dome" was the wrong look; Story Mode kills it at 5.
// ============================================================================

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:mcsm_visuals.glsl>

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec3 mcsmCamRay;

out vec4 fragColor;

// ---- story gradients: index 0 = zenith, 5 = horizon (sampled) -------------
// 1.9.215.1 (port) -- day revamped: clear vivid mid-blue zenith falling to
// the pale-lilac horizon of the reference stills (was too lavender-heavy at
// the top). Night is PURPLE now (user 2026-09-11: "the night time sky is
// purple not blue"): indigo-violet vault, soft lavender-purple horizon glow.
const vec3 SKY_DAY[6] = vec3[](
    vec3(0.300, 0.470, 0.940), vec3(0.380, 0.530, 0.965), vec3(0.470, 0.590, 0.985),
    vec3(0.560, 0.645, 0.995), vec3(0.650, 0.700, 1.000), vec3(0.760, 0.760, 1.000));
const vec3 SKY_NIGHT[6] = vec3[](
    vec3(0.045, 0.028, 0.105), vec3(0.060, 0.040, 0.150), vec3(0.085, 0.060, 0.220),
    vec3(0.120, 0.090, 0.310), vec3(0.170, 0.135, 0.420), vec3(0.240, 0.200, 0.560));
const vec3 SKY_DUSK[6] = vec3[](
    vec3(0.388, 0.122, 0.196), vec3(0.520, 0.150, 0.220), vec3(0.660, 0.200, 0.250),
    vec3(0.820, 0.290, 0.220), vec3(0.933, 0.400, 0.180), vec3(0.980, 0.560, 0.280));

vec3 mcsm_sky6(const vec3 c0, const vec3 c1, const vec3 c2, const vec3 c3,
               const vec3 c4, const vec3 c5, float up) {
    float t = clamp(up, 0.0, 1.0) * 5.0;
    int i = int(floor(t));
    float f = t - floor(t);
    vec3 a = c0, b = c1;
    if (i == 0)      { a = c0; b = c1; }
    else if (i == 1) { a = c1; b = c2; }
    else if (i == 2) { a = c2; b = c3; }
    else if (i == 3) { a = c3; b = c4; }
    else if (i == 4) { a = c4; b = c5; }
    else             { a = c5; b = c5; }
    return mix(a, b, f);
}

// three-stop storm column (zenith/mid/horizon)
vec3 mcsm_col(float up, vec3 z, vec3 m, vec3 h) {
    return up > 0.5 ? mix(m, z, (up - 0.5) * 2.0) : mix(h, m, up * 2.0);
}

// the corrected storyboard
vec3 mcsm_storm_dome(float up, float p) {
    // MCSM 1.9.71: every stop rescaled x0.46. Measured against the Story Mode
    // reference frames: build read ~(0.60,0.51,0.79) at zenith where the refs
    // read ~(0.15,0.10,0.18). Hue was already right; brightness was ~2.2x high.
    // 1.9.215.1 (port) -- 5.0 re-keyed to the 2026-09-11 turquoise deck
    // (#1A2223 core / #2E4544 mid / #7C9885 edge -> pale mint horizon), the
    // same stops the phase-5 infinite blob smudge is painted from. Luminance
    // per stop lands within 4% of the previous fitted column, so the
    // brightness calibration survives while the hue tracks the hexes exactly.
    vec3 d = mcsm_col(up, vec3(0.102, 0.133, 0.137), vec3(0.180, 0.271, 0.267), vec3(0.540, 0.620, 0.560)); // 5.0 turquoise
    d = mix(d, mcsm_col(up, vec3(0.138, 0.037, 0.193), vec3(0.239, 0.083, 0.239), vec3(0.331, 0.138, 0.285)),
            mcsm_ramp(p, 5.04, 5.12));                                                                // 5.1 pink-purple
    d = mix(d, mcsm_col(up, vec3(0.184, 0.046, 0.202), vec3(0.304, 0.110, 0.276), vec3(0.423, 0.193, 0.359)),
            mcsm_ramp(p, 5.15, 5.23));                                                                // 5.2 pinker
    d = mix(d, mcsm_col(up, vec3(0.028, 0.005, 0.064), vec3(0.074, 0.018, 0.110), vec3(0.138, 0.037, 0.175)),
            mcsm_ramp(p, 5.26, 5.34));                                                                // 5.3 dark purple
    // MCSM 1.9.99 -- 5.5 stop RETUNED BY FIT against the reference frame
    // (uploads/Screenshot 2026-09-04 182220). Measured per-cell on an 8x6 grid
    // of the upper sky, our dome read too bright AND too red vs the reference:
    // mean |dLum| 0.032, mean |dHue| 0.45. Grid search on (brightness, blue)
    // put the optimum at 0.80x mid brightness / 1.50-1.80x blue -> dHue 0.34.
    //   was: zenith (0.150,0.055,0.175) mid (0.330,0.118,0.282)
    //        horizon (0.505,0.235,0.392)   -- lum 0.084 / 0.174 / 0.303
    //   now: zenith (0.067,0.022,0.134) mid (0.099,0.032,0.150)
    //        horizon (0.505,0.205,0.580)   -- lum 0.040 / 0.054 / 0.298
    // The MID stop is the one that mattered: it alone drives elevations
    // 10-45 deg, and it was 3.2x brighter than the reference there. The
    // horizon stop keeps its brightness (its blue/red only goes 0.78 -> 1.15,
    // so the low band stays pink-dominant) and the zenith is pushed darker
    // still so looking straight up reads black. Fit score over 32 sky cells:
    // mean |dLum| 0.0324 -> 0.0279, mean |dHue| 0.4529 -> 0.396.
    // REVERT by restoring the "was" line if the pinker 1.9.96 sky is preferred.
    d = mix(d, mcsm_col(up, vec3(0.067, 0.022, 0.134), vec3(0.099, 0.032, 0.150), vec3(0.505, 0.205, 0.580)),
            mcsm_ramp(p, 5.42, 5.52));                                                                // 5.5 violet-pink, near-black overhead (1.9.99 fit to reference)
    // 5.7-5.9 keeps the user's "dark pink end" but takes a milder 1.3x blue so
    // the sky does not snap back to pink the moment phase crosses 5.7.
    d = mix(d, mcsm_col(up, vec3(0.108, 0.032, 0.151), vec3(0.238, 0.076, 0.270), vec3(0.428, 0.152, 0.452)),
            mcsm_ramp(p, 5.70, 5.90));                                                                // 1.9.99 5.7-5.9: dark violet-pink end
    // 1.9.215.1 (port) -- phase 6 is the FOUR-COLOR APOCALYPTIC SUNSET SPLIT
    // (#171021 zenith / #44284D upper-mid / #A36B73 lower-mid / #D69776
    // bottom), not the old grey wash. The phase-6 blob paints the same split.
    d = mix(d, mcsm_col(up, vec3(0.090, 0.063, 0.129), vec3(0.471, 0.301, 0.375), vec3(0.839, 0.592, 0.463)),
            mcsm_ramp(p, 5.96, 6.10));                                                                // 6.0 four-color sunset split
    // MCSM 1.9.81: retargeted from a REAL rendered frame (Screenshot
    // 2026-09-03 131242) measured against reference 144855. The 1.9.71 values
    // were right in average brightness but wrong in two ways:
    //   B/R was 1.72 at zenith where the reference is 0.92  -> far too BLUE
    //   horizon/zenith luminance was only 1.34x vs 2.89x    -> far too FLAT
    // These stops are the reference profile directly: zenith (0.130,0.076,0.120),
    // mid (0.184,0.116,0.184), horizon (0.373,0.215,0.398).
    d = mix(d, mcsm_col(up, vec3(0.130, 0.076, 0.120), vec3(0.184, 0.116, 0.184), vec3(0.373, 0.215, 0.398)),
            mcsm_ramp(p, 6.85, 7.05));                                                                // 7.0 pink sky
    d = mix(d, mcsm_col(up, vec3(0.018, 0.002, 0.009), vec3(0.092, 0.009, 0.023), vec3(0.212, 0.023, 0.032)),
            mcsm_ramp(p, 7.80, 8.00));                                                                // 8.0 blood sky
    return d;
}

vec3 mcsm_horizon_glow(float hy, float dayW, float duskW) {
    float g = exp(-hy * 6.0) * 0.18;
    return vec3(1.000, 0.850, 0.600) * g * dayW + vec3(1.000, 0.520, 0.250) * g * 1.4 * duskW;
}

vec3 mcsm_biome_tint(vec3 c) {
    vec3 f = clamp(FogColor.rgb, 0.0, 1.0);
    float mx = max(f.r, max(f.g, f.b));
    float mn = min(f.r, min(f.g, f.b));
    float w = 0.35 * smoothstep(0.02, 0.12, mx - mn);
    vec3 push = vec3(1.0);
    push = mix(push, vec3(1.05, 0.98, 0.94), clamp((f.r - f.b) * 2.0, 0.0, 1.0));
    push = mix(push, vec3(0.94, 1.04, 0.95), clamp((f.g - max(f.r, f.b)) * 2.0, 0.0, 1.0));
    push = mix(push, vec3(0.94, 0.99, 1.06), clamp((f.b - max(f.r, f.g)) * 2.0, 0.0, 1.0));
    return c * mix(vec3(1.0), push, w);
}

// Devouring Storms 1.9.175 -- high-atmosphere Story Mode cloud strata.
// This is the no-Iris/default-mod twin of the shaderpack stack: 1024 logical
// cloud layers from y=192 to y=1,000,000, clustered into short decks and long
// void gaps. It is altitude gated so the ground view keeps the normal nearby
// Story Mode clouds; the million-block stack appears only once the camera is
// actually high enough to fly/fall through it.
float mcsm_sky_strata_fbm(vec2 uv) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * mcsm_cloud_noise(uv);
        uv *= 2.03;
        a *= 0.5;
    }
    return v;
}

float mcsm_sky_strata_height(float idx) {
    float q = clamp(idx / 1023.0, 0.0, 1.0);
    return 192.0 * pow(1000000.0 / 192.0, q);
}

vec3 mcsm_sky_high_strata(vec3 dir, vec3 sky, float clock, float stormP) {
    const float LAYERS = 1024.0;
    float camY = clamp(float(CameraBlockPos.y) + CameraOffset.y, -256.0, 1000000.0);
    float highGate = smoothstep(850.0, 4200.0, camY);
    if (highGate <= 0.001) return sky;

    float dy = abs(dir.y);
    if (dy <= 0.035) return sky; // high stacks hidden from ordinary ground-horizon views

    float baseIdx = log(max(camY, 192.0) / 192.0) / log(1000000.0 / 192.0) * (LAYERS - 1.0);
    baseIdx = clamp(baseIdx, 0.0, LAYERS - 1.0);
    float acc = 0.0;
    float storm = mcsm_sky_active(stormP) ? 1.0 : 0.0;

    for (int i = 0; i < 41; i++) {
        float idx = clamp(floor(baseIdx + (float(i) - 20.0) * 3.0), 0.0, LAYERS - 1.0);
        float inCluster = mod(idx, 64.0);
        float stackGate = smoothstep(0.0, 3.0, inCluster) * (1.0 - smoothstep(12.0, 20.0, inCluster));
        if (stackGate <= 0.001) continue;

        float h = mcsm_sky_strata_height(idx);
        float rel = h - camY;
        // Draw whichever side of the stack the player is looking through.
        if (rel * dir.y <= 2.0) continue;

        float rayLen = abs(rel) / max(dy, 0.035);
        float localGate = (1.0 - smoothstep(36000.0, 140000.0, rayLen)) * smoothstep(0.060, 0.180, dy);
        if (localGate <= 0.001) continue;

        float q = idx / (LAYERS - 1.0);
        vec2 uv = dir.xz * rayLen * mix(0.010, 0.00042, q)
                + vec2(idx * 2.173 + clock * 0.006, idx * 0.731 - clock * 0.003);
        float cov = mcsm_sky_strata_fbm(uv);
        float holes = mcsm_sky_strata_fbm(uv * 0.23 + idx * 0.017);
        float a = smoothstep(0.49, 0.64, cov) * smoothstep(0.35, 0.58, holes);
        a *= stackGate * localGate * highGate * 0.28 * (1.0 - acc);

        vec3 lit = mix(vec3(0.93, 0.96, 1.00), vec3(0.70, 0.78, 1.00), q * 0.35);
        vec3 shade = mix(vec3(0.50, 0.48, 0.70), vec3(0.16, 0.16, 0.32), q * 0.60);
        vec3 stormTint = mix(vec3(0.30, 0.22, 0.42), vec3(0.46, 0.20, 0.50), mcsm_ramp(stormP, 5.0, 5.8));
        lit = mix(lit, stormTint, storm * 0.60);
        shade = mix(shade, stormTint * 0.55, storm * 0.75);
        vec3 cc = mix(shade, lit, smoothstep(0.45, 0.78, cov));
        sky = mix(sky, cc, a);
        acc += a * 0.72;
        if (acc > 0.86) break;
    }
    return sky;
}

void main() {
    float mcsmP = mcsm_phase(FogSkyEnd, FogColor, FogRenderDistanceEnd);
    float clock = mcsm_clock(GameTime);
    vec3 worldDir = normalize(transpose(mat3(ModelViewMat)) * normalize(mcsmCamRay));
    float height = clamp(worldDir.y, -1.0, 1.0);

    float isBody = step(0.10, length(ColorModulator.rgb - FogColor.rgb));

    // ---------------------------------------------------------------- death
    // MCSM 1.9.98: the demise cinematic. Dormant until the phase-31 Java
    // driver stamps the 1906..2906 FogSkyEnd band; while dormant
    // mcsm_death() is -1 and this whole block is skipped.
    float mcsmDt = mcsm_death(FogSkyEnd);
    if (mcsmDt >= 0.0) {
        vec3 ddir = mcsm_death_dir(worldDir, mcsmDt, clock);
        float upd = clamp(ddir.y * 0.5 + 0.5, 0.0, 1.0);
        upd = smoothstep(0.0, 1.0, upd);
        // the dying sky holds the late dark blood dome and drains toward black
        vec3 ddome = mcsm_storm_dome(upd, 7.6);
        ddome *= 1.0 - 0.78 * mcsm_ramp(mcsmDt, 0.0, 0.55);
        // the dust cloud settles out of the air and hangs low (user: "the
        // cloud of dust in the air starts to fall to the ground")
        vec3 dustC = vec3(0.16, 0.13, 0.12);
        float dustW = mcsm_ramp(mcsmDt, 0.62, 0.78) * (1.0 - mcsm_ramp(mcsmDt, 0.86, 1.0));
        ddome = mix(ddome, dustC, dustW * 0.5 * clamp(1.0 - ddir.y, 0.0, 1.0));
        vec3 camWd = vec3(CameraBlockPos) + CameraOffset;
        vec4 aimD = mcsm_boss_dir(camWd);
        vec3 dadd = mcsm_death_cracks(ddir, mcsmDt, clock);
        if (aimD.w > 0.5) {
            dadd += mcsm_death_implosion(worldDir, aimD.xyz, mcsmDt, clock);
            dadd += mcsm_supernova(ddir, aimD.xyz, mcsmDt, clock);
        }
        vec3 dsky = ddome + dadd + vec3(1.0, 0.98, 0.97) * mcsm_death_flash(mcsmDt);
        // ease out at the very end: as the storm despawns the carrier stops
        // and the normal sky resumes -- this fade hides the handoff
        dsky *= 0.35 + 0.65 * (1.0 - mcsm_ramp(mcsmDt, 0.95, 1.0));
        // sun/moon never show through the supernova
        fragColor = vec4(mcsm_story_grade(dsky), isBody > 0.5 ? 0.0 : 1.0);
        return;
    }


    if (!mcsm_sky_active(mcsmP)) {
        if (isBody > 0.5) {
            fragColor = apply_fog(ColorModulator, sphericalVertexDistance,
                                  cylindricalVertexDistance, 0.0,
                                  FogSkyEnd, FogSkyEnd, FogSkyEnd, FogColor);
            return;
        }
        float t = fract(clock / 24000.0) * 24000.0;
        float dayW   = smoothstep(1000.0, 3000.0, t) * (1.0 - smoothstep(9500.0, 12000.0, t));
        float nightW = smoothstep(12500.0, 15000.0, t) * (1.0 - smoothstep(21000.0, 23500.0, t));
        float duskW  = clamp(1.0 - dayW - nightW, 0.0, 1.0);
        float up = clamp(height * 0.5 + 0.5, 0.0, 1.0);
        vec3 sky = mcsm_sky6(SKY_DAY[0], SKY_DAY[1], SKY_DAY[2], SKY_DAY[3], SKY_DAY[4], SKY_DAY[5], up) * dayW
                 + mcsm_sky6(SKY_NIGHT[0], SKY_NIGHT[1], SKY_NIGHT[2], SKY_NIGHT[3], SKY_NIGHT[4], SKY_NIGHT[5], up) * nightW
                 + mcsm_sky6(SKY_DUSK[0], SKY_DUSK[1], SKY_DUSK[2], SKY_DUSK[3], SKY_DUSK[4], SKY_DUSK[5], up) * duskW;
        sky += mcsm_horizon_glow(1.0 - up, dayW, duskW);
        sky = mcsm_biome_tint(sky);

        // MCSM 1.9.96: AURORA in the mod itself (user ask: "Aurora Borealis to
        // the sky in cold biomes, in the mod as well"). Night-only, gated by a
        // cold-biome bias read off the fog colour (snowy biomes carry a bluer
        // fog than warm ones; the gate is smooth so temperate nights get a
        // faint show and deserts none). Storm sky never reaches this branch.
        // The SKY_DAY / SKY_NIGHT / SKY_DUSK arrays stay byte-identical; this
        // is additive on top of the finished night sky, not an edit of them.
        float coolFog = smoothstep(0.015, 0.10,
                          (FogColor.b - FogColor.r) + 0.5 * (FogColor.g - FogColor.r));
        sky += mcsm_aurora(worldDir, clock, nightW, coolFog);

        // MCSM v8: sun halo in ordinary play. Blooms wider and hotter through
        // the late phases; mcsmP is 0 with no storm so this is the calm
        // baseline glow until things start going wrong.
        vec3  sunTs  = mcsm_sun_true(GameTime);
        float sunUps = clamp(sunTs.y * 3.0, 0.0, 1.0);
        sky = mcsm_sun_halo(sky, dot(worldDir, sunTs), mcsmP, sunUps);

        // Story Mode vivid tone, applied last so the whole sky matches the refs.
        fragColor = vec4(max(mcsm_story_grade(sky), vec3(0.0)), 1.0);
        return;
    }

    // ---- storm -------------------------------------------------------------
    // MCSM 1.9.84 -- FIX "weird layer on top of the sky".
    // The old mapping was up = height*0.5+0.5, so the whole LOWER hemisphere
    // (height < 0) squeezed into up < 0.5 and everything at/below the horizon
    // clamped to one flat colour. Measured in frame 155231: a -0.369 luminance
    // CLIFF at y=0.32 with 10 identical rows (0.194) beneath it -- a dead slab
    // with a hard seam, exactly the "layer" complaint.
    // Remapping so the visible band above the horizon uses the FULL gradient and
    // the below-horizon band keeps descending instead of flat-lining.
    float up = clamp(height * 0.5 + 0.5, 0.0, 1.0);
    up = smoothstep(0.0, 1.0, up);          // soften the horizon crossing
    vec3 dome = mcsm_storm_dome(up, mcsmP);
    // below the horizon, continue darkening rather than holding one colour
    dome *= mix(0.62, 1.0, clamp(height * 4.0 + 1.0, 0.0, 1.0));

    // MCSM-FLASH: storm lightning. One bright blink with a dim echo every
    // ~4.3 s once the storm is up (phase 5.04-8.1), brighter toward zenith.
    // Reference frames show the sky itself lighting up between strikes.
    {
        // MCSM 1.9.87: the user reports the purple flash firing far too often
        // and far too early -- it is only meant to appear AFTER phase 6.
        // Old gate opened at 5.04. Now it ramps in over 6.00-6.20, so phases
        // 5.x have no lightning at all.
        float mcflGate = mcsm_ramp(mcsmP, 6.00, 6.20) * (1.0 - mcsm_ramp(mcsmP, 8.06, 8.10));
        // Cadence 4.3 s -> 11 s: 'extremely too often' at one strike every
        // four seconds. 11 s reads as an occasional storm flash.
        float mcflWin  = floor(clock / 11.0);
        float mcflRnd  = fract(sin(mcflWin * 91.7) * 4313.7);
        float mcflT    = clock - mcflWin * 11.0 - mcflRnd * 7.5;
        float mcflA    = exp(-max(mcflT, 0.0) * 16.0) * step(0.0, mcflT);
        mcflA         += 0.55 * exp(-max(mcflT - 0.30, 0.0) * 16.0) * step(0.30, mcflT);
        // MCSM 1.9.77: scaled x0.46 to match the phase-4 dome rescale. The old
        // amplitude was tuned against a dome 2.2x brighter; against the new one a
        // full flash toward the zenith added 0.546 luminance to a 0.165 sky -- a
        // 4.3x white-out that buried the storm silhouette on every strike.
        // Same additive-constant trap as the phase-15 blob core bite.
        dome += mcflA * mcflGate * (0.26 + 0.5 * clamp(height, 0.0, 1.0))
              * vec3(0.82, 0.66, 1.0) * 0.46;
    }

    // 1.9.215.1 (port) -- THE INFINITE SKYBOX BLOB. The glare is a separate
    // skybox layer tethered to the storm (u_StormPos), not a 3D volume: the
    // dark-matter core masks the vanilla sky, the smudge bleeds over it, and
    // rays outside the oval fall back to the plain dome ("looking the
    // opposite direction fades back to normal"). Runs 5.00-6.95 (5 / 5.5-5.9
    // / 6, the user's phase windows). The Java driver (McsmInfiniteSkyboxBlob)
    // fades the aim carrier out with distance, so far-away skies return to
    // vanilla on their own.
    vec3 camWorld = vec3(CameraBlockPos) + CameraOffset;
    vec4 aim = mcsm_boss_dir(camWorld);
    if (aim.w > 0.5 && mcsmP >= 5.00 && mcsmP <= 6.95) {
        vec4 blob = mcsm_blob(worldDir, aim.xyz, mcsmP, clock, dome);
        // blob.w is the full occlusion factor (the dark core replaces the
        // sky); blob.rgb is the premultiplied smudge emission layered over.
        dome = dome * (1.0 - blob.w) + blob.rgb;
    }

    // 1.9.175 (position twin): the high-atmosphere Story Mode cloud
    // strata stack, altitude gated from ground play.
    dome = mcsm_sky_high_strata(worldDir, dome, clock, mcsmP);

    // Bodies: tinted briefly at the start, then fade to nothing - no sun or
    // moon may shine through the storm dome (user: "being above the
    // atmosphere is still showing").
    float hide = mcsm_ramp(mcsmP, 5.05, 5.20);
    vec3 body = mcsm_sky_body_tint(mcsmP, ColorModulator.rgb);
    float a = mix(ColorModulator.a, 0.0, hide);
    // MCSM v8: keep the storm dome on the same vivid Story Mode curve as the
    // clear sky, so switching into the storm does not change the grade.
    fragColor = vec4(mcsm_story_grade(mix(dome, body, isBody)), mix(1.0, a, isBody));
}
