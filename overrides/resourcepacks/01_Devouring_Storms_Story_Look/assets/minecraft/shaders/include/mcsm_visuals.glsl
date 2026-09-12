#version 330

// ============================================================================
//  MCSM visuals - mcsm_visuals.glsl   (shared state-machine library, v3)
//  Retuned to the user's storyboarded phase timeline (screenshot refs, 
//  2026-09-02) and the palettes sampled inside StormSkyDome.java:
//    TURQ #182F2E  PURP #382553  MAGE #761A67  PINK #A32E92  RED  #661326
//
//  TIMELINE
//    4.45-4.95  green fog only (sky untouched)
//    5.00-5.15  turquoise sky + green glare blob ("brighter" at 5.1)
//    5.20       sky goes dark purple (snap; teal hard-deactivated)
//    5.20-5.40  morphs through purple
//    5.40-5.55  morphs to pink with dark purple overhead (img 3)
//    5.55-5.90  holds pink/purple, glare deepens magenta->pink
//    6.00       dark grey sky (img 4)
//    6.05-6.25  Command Block Overload tapestry: pink/crimson/magenta + orange
//               rim + void-purple top (img 5-6)
//    7.00-8.05  dark red sky, orange low band, near-black top (img 7)
//
//  1.9.215.1 (port onto the user-designated 1.9.215 base): the glare is
//  rebuilt as the INFINITE SKYBOX BLOB (user spec 2026-09-11): a separate
//  infinite skybox layer tethered to the storm, painted with the exact hex
//  decks for phase 5 / 5.5-5.9 / 6 below.
//
//  CARRIERS (Java writes these as plain fields on FogRenderer's FogData —
//  see McsmFogCarrierMixin; no buffer surgery needed):
//    FogSkyEnd   = 1000 + phase*100                    (4.45 -> 1445 .. 8.05 -> 1805)
//    FogCloudsEnd = 1200 + (yaw+180)*2 + (pitch+90)*0.5  (glare blob aim, [1200,1925])
//    ... or bind witherstorm_Phase / witherstorm_BossPos on a custom pipeline.
//  Vanilla never reaches either window (cloudEnd sits at ~96, skyEnd <= ~576
//  at max render distance), every decode is range-guarded, and the cloud pass
//  restores its own fade through mcsm_clouds_end(), so the pack is
//  byte-identical vanilla whenever the storm is not out.
// ============================================================================

uniform float witherstorm_Phase;     // 4.45 .. 8.05 when bound, else 0
uniform float witherstorm_GameTime;  // seconds when bound, else 0
uniform vec3  witherstorm_BossPos;   // absolute world position (optional)

const float MCSM_TAU = 6.2831853;
const float MCSM_PI  = 3.14159265;

// forward decl: mcsm_ramp is defined further down, used by the v8 block below
float mcsm_ramp(float v, float lo, float hi);

// ============================================================================
//  MCSM v8 ADDITIONS - always-on atmosphere (no storm required)
//
//  Everything below runs in vanilla play as well as during the storm. These
//  implement the requested features:
//    * sun glow that intensifies through the late phases (5.5+)
//    * sun/moon directional shadows on the ground at ALL times
//    * cloud shadows cast down onto the terrain
//    * Story Mode tone: vivid saturation + gentle contrast lift
// ============================================================================

// Vivid Story Mode grade. Applied to terrain and sky so the whole frame
// matches the reference stills instead of only the storm.
// MCSM 1.9.96: user wants the world "a lot more contrast and vivid, just like
// the [MCSM] images". Bumped from the conservative 1.06/1.04 pair. Kept well
// below 1.34 (phase 4: that clipped channels and destroyed gradients) -- 1.14
// is measured-safe: mcsm_story_grade() clips to >= 0 only at the dark end, and
// the storm dome rows top out near 0.50 so 1.14x saturation cannot clip them.
const float MCSM_SATURATION = 1.28;   // 1.9.71: 1.34 clipped channels to 1.0 and flattened the gradient
const float MCSM_CONTRAST   = 1.15;   // 1.9.71: reduced with saturation
const float MCSM_LIFT       = 0.010;  // keeps blacks from crushing

vec3 mcsm_story_grade(vec3 c) {
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c = mix(vec3(l), c, MCSM_SATURATION);           // saturate
    c = (c - 0.5) * MCSM_CONTRAST + 0.5 + MCSM_LIFT; // contrast about mid grey
    return max(c, vec3(0.0));
}

// Day fraction -> true sun direction, INCLUDING night (sun below horizon).
// mcsm_sun_from_day() mirrors the sun up at night for a soft key light; this
// one is the honest vector, needed for real shadow direction and for knowing
// when the sun is actually up.
vec3 mcsm_sun_true(float t01) {
    // MCSM 1.9.71 FIX: was (t01*2.0-0.5)*PI which put the sun on the horizon
    // at noon and killed every sun-gated effect (cloud shadows, key light).
    float a = fract(t01) * 2.0 * MCSM_PI;
    return normalize(vec3(cos(a), sin(a) * 0.9 + 0.02, 0.25));
}

// How bright/large the sun disc burns. Ramps hard from phase 5.5 upward, so
// the late storm days get the blown-out low sun of the reference frames.
// With no storm (p == 0) this returns the calm vanilla-ish baseline of 1.0.
float mcsm_sun_intensity(float p) {
    if (p < 4.42) return 1.0;                       // no storm: normal sun
    if (p < 5.50) return mix(1.0, 1.18, mcsm_ramp(p, 4.95, 5.50));
    if (p < 6.10) return mix(1.18, 1.85, mcsm_ramp(p, 5.50, 6.10));
    if (p < 6.90) return mix(1.85, 2.40, mcsm_ramp(p, 6.10, 6.90));
    return mix(2.40, 2.95, mcsm_ramp(p, 6.90, 8.05));
}

// Warm-to-blood colour of the glow as the phases advance.
vec3 mcsm_sun_glow_color(float p) {
    if (p < 4.42) return vec3(1.00, 0.96, 0.86);
    if (p < 5.50) return mix(vec3(1.00, 0.96, 0.86), vec3(1.00, 0.88, 0.74), mcsm_ramp(p, 4.95, 5.50));
    if (p < 6.10) return mix(vec3(1.00, 0.88, 0.74), vec3(1.00, 0.72, 0.48), mcsm_ramp(p, 5.50, 6.10));
    if (p < 6.90) return mix(vec3(1.00, 0.72, 0.48), vec3(1.00, 0.54, 0.30), mcsm_ramp(p, 6.10, 6.90));
    return mix(vec3(1.00, 0.54, 0.30), vec3(1.00, 0.36, 0.18), mcsm_ramp(p, 6.90, 8.05));
}

// Halo around the sun disc in the sky pass. dirDotSun is dot(viewDir, sunDir).
vec3 mcsm_sun_halo(vec3 sky, float dirDotSun, float p, float sunUp) {
    float inten = mcsm_sun_intensity(p);
    float core  = pow(max(dirDotSun, 0.0), mix(220.0, 90.0, clamp((inten - 1.0) / 1.95, 0.0, 1.0)));
    float bloom = pow(max(dirDotSun, 0.0), mix(14.0, 5.0, clamp((inten - 1.0) / 1.95, 0.0, 1.0)));
    vec3  gc    = mcsm_sun_glow_color(p);
    float amt   = sunUp * (inten - 0.45);
    // MCSM 1.9.83: scaled x0.46 to match the phase-4/25 dome rescale.
    // Measured from frame 131150: the halo added (0.562,0.384,0.401) to a dome
    // whose zenith luminance is 0.091 -- a 4.7x blow-out that CLIPPED 90% of the
    // sky (R pinned at 0.993). Clipping also destroyed the hue, which made a
    // too-blue dome measure as "correct magenta" and nearly sent the phase-25
    // retarget the wrong way. Fourth instance of the same trap: an additive term
    // calibrated against the old 2.2x-brighter dome (cf. phase 15 blob core,
    // phase 16 lightning).
    // 1.9.106: phase 6 screenshots showed this swelling into a huge magenta
    // ceiling light. Keep the sun punch, but tighten and dim the wide halo hard
    // as the storm reaches 6.0 so the sky stays ominous instead of washed out.
    float late = mcsm_ramp(p, 5.88, 6.06);
    float coreScale  = mix(0.58, 0.36, late);
    float bloomScale = mix(0.10, 0.025, late);
    return sky + gc * (core * coreScale + bloom * bloomScale) * amt * 0.34;
}

// ------------------------------------------------------------ cloud shadow
// Procedural cloud deck sampled on the world XZ plane, projected along the
// sun direction so the pattern slides across the ground correctly and drifts
// with time, matching the drifting Story Mode cloud deck overhead.
float mcsm_cloud_noise(vec2 uv) {
    vec2 i = floor(uv), f = fract(uv);
    f = f * f * (3.0 - 2.0 * f);
    // cheap value-noise hash, 4 corners
    vec4 h = fract(sin(vec4(
        dot(i + vec2(0.0, 0.0), vec2(127.1, 311.7)),
        dot(i + vec2(1.0, 0.0), vec2(127.1, 311.7)),
        dot(i + vec2(0.0, 1.0), vec2(127.1, 311.7)),
        dot(i + vec2(1.0, 1.0), vec2(127.1, 311.7))
    )) * 43758.5453);
    return mix(mix(h.x, h.y, f.x), mix(h.z, h.w, f.x), f.y);
}

// Returns a multiplier <= 1.0 to darken ground beneath cloud cover.
// worldPos: fragment world position. sunDir: true sun vector. clock: seconds.
float mcsm_cloud_shadow(vec3 worldPos, vec3 sunDir, float clock, float upFace) {
    if (sunDir.y <= 0.02) return 1.0;                   // sun down: no cloud shadow
    float deckY = 192.0;
    float dy = deckY - worldPos.y;
    if (dy <= 1.0) return 1.0;                          // above the deck: no shadow
    float t = dy / max(sunDir.y, 0.05);
    vec2 hit = worldPos.xz + sunDir.xz * t;

    // MCSM 1.9.107 -- visible MOVING shadows even with no storm. The previous
    // cell-accurate shadow matched vanilla cloud speed but moved too slowly to
    // read in play. This keeps the correct sun projection and adds large soft
    // Story-Mode cloud/tree-shadow bands drifting over terrain.
    vec2 uv = hit * 0.030 + vec2(clock * 0.050, -clock * 0.020);
    float n1 = mcsm_cloud_noise(uv);
    float n2 = mcsm_cloud_noise(uv * 2.15 + vec2(17.4, clock * 0.030));
    float soft = smoothstep(0.48, 0.78, n1 * 0.70 + n2 * 0.30);
    float streak = smoothstep(0.62, 0.86, sin((hit.x + hit.y * 0.55) * 0.020 + clock * 0.18) * 0.5 + 0.5);
    float cov = clamp(soft * 0.85 + streak * 0.22, 0.0, 1.0);
    float strength = 0.58 * clamp(sunDir.y * 1.7, 0.0, 1.0) * upFace;
    return 1.0 - cov * strength;
}
// ---------------------------------------------------------------- decode
bool mcsm_fog_active(float p) { return p >= 4.42 && p <= 8.06; }
bool mcsm_sky_active(float p) { return p >= 4.95 && p <= 8.06; }
bool mcsm_active(float p)     { return mcsm_sky_active(p); }

float mcsm_phase(float fogSkyEnd, vec4 fogColor, float fogRenderDistanceEnd) {
    if (witherstorm_Phase > 4.4 && witherstorm_Phase < 8.1) {
        return clamp(witherstorm_Phase, 4.45, 8.05);
    }
    if (fogSkyEnd > 1395.0 && fogSkyEnd < 1855.0) {
        return clamp((fogSkyEnd - 1000.0) * 0.01, 4.45, 8.05);
    }
    float mn = min(fogColor.r, min(fogColor.g, fogColor.b));
    float mx = max(fogColor.r, max(fogColor.g, fogColor.b));
    if (mx - mn < 0.25 || mx < 0.30) return 0.0;
    if (fogColor.g > 1.45 * fogColor.r && fogColor.b > 1.45 * fogColor.r) {
        if (fogRenderDistanceEnd > 130.0) return 0.0;   // not a storm, not Warped Forest
        return fogRenderDistanceEnd < 96.0 ? 5.10 : 5.00;
    }
    if (fogColor.r > 0.55 && fogColor.b > 0.30
        && fogColor.g < 0.50 * min(fogColor.r, fogColor.b)) {
        if (fogColor.g > 0.15 * fogColor.r && fogColor.g < 0.65 * fogColor.r
            && fogColor.b < 0.65 * fogColor.g) {
            return 6.10;
        }
        return fogColor.b > 0.55 * fogColor.r ? 5.30 : 5.50;
    }
    return 0.0;
}

// Safe cloud-fade end: restores sane vanilla cloud distance-fade while the
// aim carrier occupies FogCloudsEnd (only this pack's cloud pass reads it).
float mcsm_clouds_end() {
    float v = FogCloudsEnd;
    // 1.9.71 carrier band 3000..68340, plus the legacy 1100..2150 band.
    // MCSM 1.9.98: the WIDE carrier (aim + glare size) sits at 47000..1093455
    // -- the cloud pass must not read any of these as a real distance.
    if ((v > 1100.0 && v < 2150.0) || (v >= 2999.0 && v <= 68341.0)
        || (v >= 47000.0 && v <= 1093455.0))
        return clamp(FogRenderDistanceEnd * 0.75, 32.0, 192.0);
    return v;
}

// Defensive twin for the render-distance slot (unused by the v3 carrier, but
// harmless and future-proof if the aim ever moves back there).
// MCSM 1.9.96: also guard the 9001..9299 band -- it will carry the user
// glare-size preference (size*10 offset from 9000) once the Java carrier
// ships; fog math must never see those values as a real distance.
float mcsm_rd_start() {
    float v = FogRenderDistanceStart;
    if ((v > 1100.0 && v < 2150.0) || (v >= 2999.0 && v <= 68341.0)
        || (v >= 9001.0 && v <= 9299.0))
        return FogRenderDistanceEnd * 0.72;
    return v;
}

// Raw read of the render-distance slot, for carrier bands that need the
// unmodified value (glare size). Fog consumers must use mcsm_rd_start().
float mcsm_rd_raw() {
    return FogRenderDistanceStart;
}

float mcsm_clock(float gameTime01) {
    // MCSM 1.9.77: returns SECONDS, not ticks.
    // Every consumer of this value was written in seconds -- the comments say
    // "every ~4.3 s" (lightning) and "roar pulse" (a slow throb) -- but the old
    // body returned gameTime01*24000, i.e. ticks, which advance at 20 per real
    // second. That made the lightning fire 4.65 times PER SECOND and turned the
    // blob pulse into a 9.5 Hz strobe. One MC day = 24000 ticks = 1200 real
    // seconds, so ticks/20 = seconds.
    if (witherstorm_GameTime > 0.0) return witherstorm_GameTime;
    if (gameTime01 >= 0.0 && gameTime01 <= 1.5) return gameTime01 * 1200.0;
    return 0.0;
}

vec3 mcsm_sun_from_day(float t01) {
    float a = (fract(t01) * 2.0 - 0.5) * MCSM_PI;   // horizon -> zenith -> horizon
    return normalize(vec3(cos(a), abs(sin(a)) * 0.9 + 0.1, 0.25));
}

// Glare-blob aim direction: true world vector when BossPos is bound, else the
// yaw/pitch carrier smuggled through FogCloudsEnd. Returns (dir, present).
vec4 mcsm_boss_dir(vec3 camWorld) {
    if (dot(witherstorm_BossPos, witherstorm_BossPos) > 1.0) {
        vec3 d = witherstorm_BossPos - camWorld;
        if (dot(d, d) > 4.0) return vec4(normalize(d), 1.0);
    }
    float v = FogCloudsEnd;
    float yaw, pitch;
    // MCSM 1.9.71: invertible carrier written by McsmBlobCarrierPatch.
    //   cloudEnd = 3000 + yawIdx*181 + pitchIdx
    // The old 1200-band packing aliased 45:1 and decoded pitch to -90 almost
    // always, which drew the glare blob below the world -> "blob is missing".
    //
    // MCSM 1.9.98: WIDE carrier. Same integer payload, multiplied by 16, with
    // the glare-size index packed into the low nibble:
    //   cloudEnd = (3000 + yawIdx*181 + pitchIdx) * 16 + sizeIdx
    // Max = 68340*16+15 = 1093455, exact in float32 (< 2^24). Band split is
    // clean: old tops out at 68340, wide starts at 48000.
    if (v >= 47000.0 && v <= 1093455.0) {
        float e16 = floor(v / 16.0);
        float e   = e16 - 3000.0;
        float yi  = floor(e / 181.0);
        float pi  = e - yi * 181.0;
        yaw   = clamp(yi, 0.0, 360.0) - 180.0;
        pitch = clamp(pi, 0.0, 180.0) - 90.0;
    } else if (v >= 2999.0 && v <= 68341.0) {
        float e  = v - 3000.0;
        float yi = floor(e / 181.0);
        float pi = e - yi * 181.0;
        yaw   = clamp(yi, 0.0, 360.0) - 180.0;
        pitch = clamp(pi, 0.0, 180.0) - 90.0;
    } else {
        return vec4(0.0, 0.0, 1.0, 0.0);
    }
    float yr = yaw * (MCSM_PI / 180.0);
    float pr = pitch * (MCSM_PI / 180.0);
    // MCSM 1.9.96 -- ANTIPODE FIX. Frames 194701 / 195146 show the glare mass
    // parked low-right while the storm sits high-left -- mirrored in BOTH axes,
    // i.e. the carrier's yaw()/pitch() describe boss->camera, not
    // camera->boss. The dome blob therefore rendered at the exact opposite
    // point of the sky (and its rim poked out "through the other side of the
    // storm", the user's clip-through report). Negating the decoded vector
    // re-centres the mass on the storm, which also delivers "the storm must be
    // in the centre". The witherstorm_BossPos branch above is already
    // camera->boss (BossPos - camWorld), so it is left untouched.
    // If a future frame ever shows the blob opposite the storm again, this one
    // negation is the suspect -- flip it back first.
    vec3 d = vec3(cos(pr) * cos(yr), sin(pr), cos(pr) * sin(yr));
    return vec4(-d, 1.0);
}

// MCSM 1.9.98 -- user-facing glare size, rides the wide carrier's low nibble.
//   sizeIdx 0..15 -> size 0.35 + 0.18*idx  (0.35 .. 3.05x the old mass)
// Absent band (unpatched jar-side writer) -> 1.18, i.e. "a tiny bit bigger
// than 1.9.95", the user's final ruling; the in-game slider (MCSM Extras tab)
// writes the real value each frame once the Java half ships (phase 30 builds
// from CI). 1.9.96/97's FogRenderDistanceStart band was retired before use;
// its guard in mcsm_rd_start() stays as harmless future-proofing.
float mcsm_glare_size() {
    float v = FogCloudsEnd;
    if (v >= 47000.0 && v <= 1093455.0) {
        float sizeIdx = v - floor(v / 16.0) * 16.0;
        return clamp(0.35 + 0.18 * sizeIdx, 0.25, 3.05);
    }
    return 0.58;
}

// ------------------------------------------------------------- helpers
vec3 mcsm_kill_teal(vec3 c, float p) {
    float teal = max(0.0, min(c.g, c.b) - c.r);
    float gate = smoothstep(5.15, 5.20, p);       // teal dies at the purple snap
    return c - vec3(0.0, teal, teal * 0.55) * gate;
}

float mcsm_ramp(float v, float lo, float hi) {
    if (hi <= lo) return v >= hi ? 1.0 : 0.0;
    float t = clamp((v - lo) / (hi - lo), 0.0, 1.0);
    return t * t * (3.0 - 2.0 * t);
}

// ---------------------------------------------------------------- sky keys
// key: (bottom, mid, top, lift, sharp). Lift = how far the horizon colour
// climbs; sharp = ribbon crispness (0 feathered .. 1 hard edge).
vec3 mcsm_k_bot(int k) {
    if (k == 0) return vec3(0.000, 0.880, 0.800);   // 5.00 turquoise horizon
    if (k == 1) return vec3(0.060, 0.940, 0.850);   // 5.10 brighter green-teal
    if (k == 2) return vec3(0.150, 0.050, 0.250);   // 5.20 dark purple (img 3 base)
    if (k == 3) return vec3(0.240, 0.080, 0.330);   // 5.40 purple morph mid
    if (k == 4) return vec3(0.920, 0.360, 0.680);   // 5.55 light story-pink core
    if (k == 5) return vec3(0.700, 0.160, 0.430);   // 5.90 dark-pink end, not pure purple
    if (k == 6) return vec3(0.280, 0.245, 0.270);   // 6.00 dark grey (img 4)
    if (k == 7) return vec3(0.950, 0.420, 0.120);   // 6.10 orange horizon bleed
    if (k == 8) return vec3(0.850, 0.300, 0.060);   // 7.00 orange-red low band
    return vec3(0.550, 0.160, 0.030);                // 8.00 deeper ember
}
vec3 mcsm_k_mid(int k) {
    if (k == 0) return vec3(0.000, 0.340, 0.320);
    if (k == 1) return vec3(0.040, 0.560, 0.520);
    if (k == 2) return vec3(0.220, 0.145, 0.325);   // PURP #382553
    if (k == 3) return vec3(0.340, 0.120, 0.420);
    if (k == 4) return vec3(0.620, 0.170, 0.510);   // rose-purple body
    if (k == 5) return vec3(0.460, 0.100, 0.340);   // dark pink/magenta
    if (k == 6) return vec3(0.190, 0.170, 0.200);
    if (k == 7) return vec3(0.660, 0.300, 0.460);   // dark light pink
    if (k == 8) return vec3(0.380, 0.040, 0.060);   // dark red
    return vec3(0.220, 0.020, 0.040);
}
vec3 mcsm_k_top(int k) {
    if (k == 0) return vec3(0.012, 0.018, 0.022);   // charcoal/black vignette
    if (k == 1) return vec3(0.010, 0.020, 0.024);
    if (k == 2) return vec3(0.020, 0.005, 0.048);   // black-purple overhead
    if (k == 3) return vec3(0.040, 0.010, 0.090);
    if (k == 4) return vec3(0.090, 0.020, 0.120);   // dark purple around pink
    if (k == 5) return vec3(0.065, 0.010, 0.090);
    if (k == 6) return vec3(0.055, 0.048, 0.075);   // grey-on-grey
    if (k == 7) return vec3(0.035, 0.020, 0.090);   // dark blue-black top
    if (k == 8) return vec3(0.020, 0.008, 0.020);   // "somewhat black but dark"
    return vec3(0.010, 0.005, 0.015);
}
void mcsm_keys(float p, out vec3 bot, out vec3 mid, out vec3 top,
               out float lift, out float sharp) {
    // segment boundaries: 5.0 5.1 5.2 5.4 5.55 5.9 6.0 6.1 7.0 8.0
    float k0, t;
    if (p <= 5.10)      { k0 = 0.0; t = (p - 5.00) / 0.10; }
    else if (p <= 5.20) { k0 = 1.0; t = mcsm_ramp(p, 5.15, 5.20); }   // snap
    else if (p <= 5.40) { k0 = 2.0; t = (p - 5.20) / 0.20; }
    else if (p <= 5.55) { k0 = 3.0; t = (p - 5.40) / 0.15; }
    else if (p <= 5.90) { k0 = 4.0; t = (p - 5.55) / 0.35; }
    else if (p <= 6.00) { k0 = 5.0; t = (p - 5.90) / 0.10; }
    else if (p <= 6.10) { k0 = 6.0; t = (p - 6.00) / 0.10; }
    else if (p <= 7.00) { k0 = 7.0; t = (p - 6.10) / 0.90; }
    else                { k0 = 8.0; t = clamp(p - 7.00, 0.0, 1.0); }
    int ka = int(k0);
    int kb = ka + 1;
    if (kb > 9) kb = 9;
    float u = clamp(t, 0.0, 1.0);
    bot = mix(mcsm_k_bot(ka), mcsm_k_bot(kb), u);
    mid = mix(mcsm_k_mid(ka), mcsm_k_mid(kb), u);
    top = mix(mcsm_k_top(ka), mcsm_k_top(kb), u);
    // horizon colour climbs higher at 5.1 (denser teal) and flattens at 6.1 tapestry
    lift  = mix(p <= 5.15 ? 0.42 : 0.30, p >= 6.0 ? 0.90 : (p >= 5.55 ? 0.55 : 0.36), u);
    sharp = p < 5.2 ? 0.0 : (p < 5.4 ? 0.9 : (p < 6.0 ? 0.45 : 0.8));
}

// 6.05-6.25 tapestry (re-keyed Command Block Overload, matching img 5-6):
// flat bands orange / crimson / magenta / void purple.
vec3 mcsm_apocalypse_bands(float height, float clock) {
    // MCSM 1.9.71 FIX: this used hard if/else steps, so u=0.75 cut bright
    // magenta straight to near-black -> the purple rim at the zenith.
    // Now interpolated between the same four anchors.
    float u = clamp(height * 0.5 + 0.5, 0.0, 1.0);
    vec3 cA = vec3(1.00, 0.45, 0.05);   // low  orange
    vec3 cB = vec3(0.78, 0.05, 0.11);   // red
    vec3 cC = vec3(0.86, 0.10, 0.92);   // magenta
    vec3 cD = vec3(0.05, 0.00, 0.12);   // near-black overhead
    vec3 col = mix(cA, cB, smoothstep(0.08, 0.36, u));
    col = mix(col, cC, smoothstep(0.34, 0.60, u));
    col = mix(col, cD, smoothstep(0.58, 0.94, u));
    float idxf = floor(u * 4.0);
    float flick = 0.93 + 0.07 * step(0.55, fract(clock * 0.10 + idxf * 0.31));
    return col * flick;
}

vec3 mcsm_sky_color(float height, float p, float clock) {
    vec3 bot, mid, top; float lift, sharp;
    mcsm_keys(p, bot, mid, top, lift, sharp);
    float h = clamp(height, -1.0, 1.0);
    float u = clamp((h + 0.10) / max(lift, 0.05), 0.0, 1.0);
    float feather = mix(0.16, 0.015, sharp);
    vec3 c = mix(bot, mid, smoothstep(0.0, 0.5, u));
    c = mix(c, top, smoothstep(0.55 - feather, 0.55 + feather, u));
    c *= 1.0 - 0.55 * smoothstep(0.45, 1.0, h);
    c = mix(c, mcsm_apocalypse_bands(h, clock),
            mcsm_ramp(p, 6.02, 6.18) * (1.0 - mcsm_ramp(p, 6.90, 7.30)));
    c *= 0.97 + 0.03 * sin(clock * 0.55 + h * 2.5);
    return mcsm_kill_teal(c, p);
}

// ---------------------------------------------------------------- blob (glare)
// Retuned to the CORRECTED 2026-09-11 decks. This is the ground-rim tint
// under the storm column (terrain.fsh), so it tracks the same palettes the
// sky blob paints: 5 moss-green, 5.5-5.9 velvet violet, 6 dusty rose/amber,
// 7/8 ember.
vec3 mcsm_blob_color(float p, float clock) {
    vec3 teal = mix(vec3(45.0, 66.0, 63.0),  vec3(106.0, 154.0, 120.0), 0.55) / 255.0; // #2D423F -> #6A9A78
    vec3 purp = mix(vec3(45.0, 20.0, 66.0),  vec3(135.0, 82.0, 156.0), 0.45) / 255.0; // #2D1442 -> #87529C
    vec3 six  = mix(vec3(150.0, 97.0, 115.0), vec3(216.0, 152.0, 116.0), 0.35) / 255.0; // #966173 -> #D89874
    vec3 ember = vec3(0.720, 0.180, 0.100);
    vec3 c = teal;
    c = mix(c, purp,  mcsm_ramp(p, 5.42, 5.52));
    c = mix(c, six,   mcsm_ramp(p, 5.92, 6.08));
    c = mix(c, ember, mcsm_ramp(p, 6.90, 7.10));
    c *= 0.92 + 0.08 * sin(clock * 3.0);   // roar pulse
    return c;
}

// ============================================================================
//  1.9.215.1 (port of the infinite-skybox work) -- THE INFINITE SKYBOX BLOB
//  (user spec, 2026-09-11)
//
//  Telltale's glare is NOT a 3D volume, NOT a billboard and NOT a cloud
//  layer. It is a separate INFINITE skybox projection tethered to the Wither
//  Storm -- the same trick as the infinite-hallway portals: a flat, smeared
//  oval gradient mass painted into the sky pass (gbuffers_sky / sky.fsh),
//  centred behind u_StormPos (witherstorm_BossPos / the aim carrier).
//
//    * looking at the storm: the gradient never ends. Every ray inside the
//      oval is blob colour -- fly up into it, through it, around it, and it
//      never terminates or reveals a back face;
//    * the vanilla skybox stays "in front" at the outer edges: the smudge
//      blends over it and the ordinary sky shines through the soft borders;
//    * looking away from the storm: the field falls off to zero and the
//      regular sky returns untouched;
//    * distance: the Java driver (McsmInfiniteSkyboxBlob) fades the aim
//      carrier out between 700 and 1600 blocks, so far away the sky slowly
//      changes back to vanilla.
//
//  No raymarching, no volumetric fog, no transparent horizon-fog variables:
//  pure angular dome-plane projection. The gradients are procedural
//  (infinitely smooth -- the GL_LINEAR-smudge look with zero pixelated
//  edges, so no texture sampling is needed at all).
// ============================================================================

// CORRECTED artist hexes (user 2026-09-11, re-measured from the reference
// screenshots). x/255 -> display space.
// PHASE 5 -- GREEN SKYBOX BLOB
const vec3 P5_CORE  = vec3(22.0, 26.0, 29.0) / 255.0;    // #161A1D near-black charcoal
const vec3 P5_MID   = vec3(45.0, 66.0, 63.0) / 255.0;    // #2D423F muted jade-teal green
const vec3 P5_EDGE  = vec3(106.0, 154.0, 120.0) / 255.0; // #6A9A78 cinematic pastel moss green
// PHASE 5.5-5.9 -- PURPLE & PINK VOID BLOB
const vec3 P55_CORE = vec3(11.0, 4.0, 16.0) / 255.0;     // #0B0410 deep void black-purple
const vec3 P55_MID  = vec3(45.0, 20.0, 66.0) / 255.0;    // #2D1442 thick velvet violet
const vec3 P55_HIGH = vec3(88.0, 28.0, 110.0) / 255.0;   // #581C6E vibrant royal magenta
const vec3 P55_EDGE = vec3(135.0, 82.0, 156.0) / 255.0;  // #87529C glowing soft pink-lavender
// PHASE 6 -- THE FOUR-COLOR SUNSET SPLIT BLOB (vertical)
const vec3 P6_TOP   = vec3(26.0, 18.0, 38.0) / 255.0;    // #1A1226 midnight void purple (zenith)
const vec3 P6_UMID  = vec3(70.0, 42.0, 82.0) / 255.0;    // #462A52 muted dark burgundy
const vec3 P6_LMID  = vec3(150.0, 97.0, 115.0) / 255.0;  // #966173 cinematic dusty rose-pink
const vec3 P6_BOT   = vec3(216.0, 152.0, 116.0) / 255.0; // #D89874 toxic opaque amber orange-peach

// The blob is intentionally compact around the storm bearing.  The old
// 1.9.305 ellipse used one analytic radius, which made the sky read as a
// clean circular colour grade.  These proportions are only the base frame;
// mcsm_inf_field() domain-warps the silhouette and adds asymmetric lobes.
const float MCSM_INF_Y    = 1.35;
const float MCSM_INF_X    = MCSM_INF_Y * 2.5; // required horizontal multiplier
const float MCSM_INF_TILT = 0.18;   // radians

// ---------------------------------------------------------------------------
// Angular paint/noise.  This is evaluated from direction only: no world
// position or finite radius enters the shape, so flying toward it can never
// reveal a back face or make the cloud wall get closer.
float mcsm_inf_hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float mcsm_inf_noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = mcsm_inf_hash(i);
    float b = mcsm_inf_hash(i + vec2(1.0, 0.0));
    float c = mcsm_inf_hash(i + vec2(0.0, 1.0));
    float d = mcsm_inf_hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float mcsm_inf_fbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    for (int i = 0; i < 5; i++) {
        sum += mcsm_inf_noise(p) * amp;
        norm += amp;
        p = p * 2.03 + vec2(17.13, 9.71);
        amp *= 0.5;
    }
    return sum / norm;
}

// Dome-plane projection plus a warped, asymmetric distance field.  `x` is
// the normalized paint radius, `y` is elevation inside the smear, and `z`
// marks a valid forward-facing storm ray.  The angular lobes and the 5-octave
// FBM are what stop the edge from becoming a perfect circle/oval.
vec3 mcsm_inf_field(vec3 wd, vec3 bd, float outerDeg) {
    float cd = dot(wd, bd);
    if (cd <= 0.02) return vec3(2.0, 0.5, 0.0);
    vec3 upRef = abs(bd.y) > 0.985 ? vec3(0.0, 0.0, 1.0) : vec3(0.0, 1.0, 0.0);
    vec3 ex = normalize(cross(upRef, bd));
    vec3 ey = cross(bd, ex);
    float ct = cos(MCSM_INF_TILT), st = sin(MCSM_INF_TILT);
    vec3 exT = ex * ct + ey * st;
    vec3 eyT = ey * ct - ex * st;
    vec2 s = vec2(dot(wd, exT), dot(wd, eyT))
           / (cd * tan(radians(outerDeg)));
    vec2 e = vec2(s.x / MCSM_INF_X, s.y / MCSM_INF_Y);
    float raw = length(e);
    float theta = atan(e.y, e.x);

    // Low-frequency torn contour + finer paint breakup.  The explicit lobes
    // are deliberately unequal so the mass has a heavy side and a ragged
    // trailing side like the reference frames, not radial symmetry.
    float n0 = mcsm_inf_fbm(e * 1.35 + vec2(2.7, 8.4));
    float n1 = mcsm_inf_fbm(e * 3.70 + vec2(-5.1, 3.2));
    float lobes = 0.070 * sin(theta * 3.0 + 0.7)
                + 0.045 * sin(theta * 7.0 - 1.4)
                + 0.028 * sin(theta * 11.0 + n1 * 5.0);
    float boundary = 1.0 + 0.23 * (n0 - 0.5) * 2.0 + lobes;
    // Push a low hanging shoulder and a narrower upper-right notch into the
    // contour.  These are angular features, not geometry in the world.
    boundary += 0.085 * smoothstep(-0.90, -0.05, e.y) * smoothstep(-1.0, -0.05, -e.x);
    boundary -= 0.070 * smoothstep(0.10, 0.85, e.y) * smoothstep(-0.20, 0.80, e.x);
    boundary = max(boundary, 0.58);

    float u = raw / boundary;
    float upness = clamp((s.y / MCSM_INF_Y) * 0.5 + 0.5, 0.0, 1.0);
    return vec3(u, upness, 1.0);
}

// Normalized warped field distance: 0 centre .. 1 broken silhouette edge ..
// 1.85 diffuse bleed.  It is still angular, so the layer is infinite.
float mcsm_inf_u(vec3 wd, vec3 bd, float outer, float ang) {
    vec3 fld = mcsm_inf_field(wd, bd, outer);
    if (fld.z > 0.5) return fld.x;
    return 2.0;
}

// Per-phase radial palette (core / mid / edge / high).
void mcsm_inf_palette(float p, out vec3 core, out vec3 mid, out vec3 edge,
                      out vec3 high) {
    core = P5_CORE; mid = P5_MID; edge = P5_EDGE; high = P5_MID;
    float w55 = mcsm_ramp(p, 5.42, 5.52);
    core = mix(core, P55_CORE, w55);
    mid  = mix(mid,  P55_MID,  w55);
    edge = mix(edge, P55_EDGE, w55);
    high = mix(high, P55_HIGH, w55);
    float w6 = mcsm_ramp(p, 5.92, 6.08);
    core = mix(core, P6_TOP, w6);
    mid  = mix(mid,  P6_UMID, w6);
    edge = mix(edge, mix(P6_LMID, P6_BOT, 0.5), w6);
    high = mix(high, P6_UMID, w6);
}

// Phase 6: the four-colour split is keyed to ray elevation, not the smear
// contour, so the bottom can stay dusty amber while the zenith stays void.
vec3 mcsm_inf_p6_split(float height) {
    float u = clamp(height * 0.5 + 0.5, 0.0, 1.0);
    vec3 c = mix(P6_BOT, P6_LMID, smoothstep(0.02, 0.30, u));
    c = mix(c, P6_UMID, smoothstep(0.26, 0.55, u));
    c = mix(c, P6_TOP,  smoothstep(0.52, 0.95, u));
    return c;
}

// Cloud-pass companion: use the same broken angular contour for cloud
// occlusion, without the time-varying colour streaks used by mcsm_blob().
float mcsm_mass_cover(vec3 wd, vec3 bd, float p) {
    float s = mcsm_glare_size();
    float outer = mix(58.0, 88.0, mcsm_ramp(p, 5.0, 6.0))
                * mix(0.78, 1.16, clamp((s - 0.25) / 2.80, 0.0, 1.0));
    float ang = degrees(acos(clamp(dot(normalize(wd), normalize(bd)), -1.0, 1.0)));
    if (ang >= outer * 1.85) return 0.0;
    float uu = mcsm_inf_u(normalize(wd), normalize(bd), outer, ang);
    float coreW = 1.0 - smoothstep(0.07, 0.38, uu);
    float midW  = smoothstep(0.18, 0.48, uu) * (1.0 - smoothstep(0.58, 0.97, uu));
    float edgeW = smoothstep(0.48, 0.86, uu) * (1.0 - smoothstep(0.92, 1.30, uu));
    return clamp(0.98 * coreW + 0.62 * midW + 0.16 * edgeW, 0.0, 0.97);
}

// Alpha-feathered core + multi-band, torn colour smear. sky.fsh composites
// this as dome * (1 - occ) + emission: only the irregular storm field replaces
// the vanilla sky, while its broken edge lets the ordinary sky show through.
vec4 mcsm_blob(vec3 worldDir, vec3 bossDir, float p, float clock, vec3 dome) {
    vec3 wd = normalize(worldDir);
    vec3 bd = normalize(bossDir);
    float s = mcsm_glare_size();
    float outer = mix(58.0, 88.0, mcsm_ramp(p, 5.0, 6.0))
                * mix(0.78, 1.16, clamp((s - 0.25) / 2.80, 0.0, 1.0));
    float ang = degrees(acos(clamp(dot(wd, bd), -1.0, 1.0)));
    if (ang >= outer * 1.85) return vec4(0.0);

    vec3 fld = mcsm_inf_field(wd, bd, outer);
    float uu = mcsm_inf_u(wd, bd, outer, ang);
    float upness = fld.y;
    vec3 core, mid, edge, high;
    mcsm_inf_palette(p, core, mid, edge, high);

    // Noise-jitter the colour boundaries independently from the silhouette.
    // That produces broad painted colour tongues rather than a single grade.
    float bandNoise = mcsm_inf_fbm(vec2(uu * 3.1 + upness * 1.7,
                                        upness * 2.4 - uu * 1.1 + clock * 0.004));
    float rr = uu + (bandNoise - 0.5) * 0.16;
    float coreW = 1.0 - smoothstep(0.07, 0.38, rr);
    float midW  = smoothstep(0.18, 0.48, rr) * (1.0 - smoothstep(0.58, 0.97, rr));
    float edgeW = smoothstep(0.48, 0.86, rr) * (1.0 - smoothstep(0.92, 1.30, rr));
    float bleedW = smoothstep(0.82, 1.26, rr) * (1.0 - smoothstep(1.24, 1.85, rr));

    vec3 c = core * coreW + mid * midW + edge * (edgeW + bleedW * 0.48);
    float streak = smoothstep(0.52, 0.82,
                      mcsm_inf_fbm(vec2(upness * 2.6 + 4.0, uu * 4.5 - clock * 0.008)));
    c = mix(c, mix(mid, edge, 0.68), streak * edgeW * 0.42);

    // Low-frequency zenith-to-horizon wash from the supplied phase sky
    // references. It is deliberately mixed into, rather than substituted for,
    // the torn radial paint so tongues and the dark storm heart survive.
    vec3 deckTop;
    vec3 deckBottom;
    if (p < 5.42) {
        deckTop = vec3(0.000, 0.180, 0.192);
        deckBottom = vec3(0.663, 0.847, 0.714);
    } else if (p < 5.92) {
        deckTop = vec3(0.216, 0.118, 0.275);
        deckBottom = vec3(0.804, 0.529, 0.549);
    } else {
        deckTop = vec3(0.353, 0.275, 0.373);
        deckBottom = vec3(0.765, 0.569, 0.627);
    }
    vec3 deck = mix(deckBottom, deckTop, clamp(upness, 0.0, 1.0));
    c = mix(c, deck, 0.30 + 0.38 * (1.0 - coreW));

    // 5.5-5.9 gets a richer royal-magenta upper bleed.
    c += mix(mid, high, 0.62) * midW * upness * mcsm_ramp(p, 5.42, 5.52) * 0.62;

    float w6 = mcsm_ramp(p, 5.92, 6.08);
    c = mix(c, mcsm_inf_p6_split(wd.y), w6);
    c *= mix(1.0, 0.62 + 0.38 * coreW, w6);

    float occ = clamp(0.98 * coreW + 0.62 * midW + 0.16 * edgeW + 0.06 * bleedW,
                      0.0, 0.97);
    c *= 0.96 + 0.04 * sin(clock * 1.3 + upness * 2.0);
    return vec4(c, occ);
}

// ---------------------------------------------------------------- fog / tints
vec3 mcsm_fog_color(float p, vec3 vanilla) {
    // 4.5 green haze first, then the sky's own bottom colour drives the fog.
    vec3 green = vec3(0.100, 0.420, 0.300);
    float seg;
    vec3 bot, mid, top; float lift, sharp;
    mcsm_keys(p, bot, mid, top, lift, sharp);
    vec3 c = bot * 0.75 + mid * 0.25;
    seg = mcsm_ramp(p, 4.42, 4.95);
    c = mix(green, c, seg);
    return mcsm_kill_teal(mix(vanilla, c, 0.55 + 0.30 * seg), p);
}

float mcsm_fog_density(float p) {
    if (!mcsm_fog_active(p)) return 1.0;
    float peak = mcsm_ramp(p, 4.95, 5.06) * (1.0 - mcsm_ramp(p, 5.16, 5.40));
    return 1.0 + 0.40 * peak + 0.18 * mcsm_ramp(p, 5.4, 6.0) * (1.0 - mcsm_ramp(p, 7.0, 8.06));
}

vec3 mcsm_cloud_tint(float p) {
    if (p < 5.05) return vec3(0.98, 1.00, 0.98);
    if (p < 5.19) return mix(vec3(0.98, 1.00, 0.98), vec3(0.58, 1.00, 0.94), mcsm_ramp(p, 4.95, 5.10));
    if (p < 5.40) return mix(vec3(0.58, 1.00, 0.94), vec3(0.78, 0.42, 0.92), mcsm_ramp(p, 5.19, 5.30));
    if (p < 5.95) return vec3(0.92, 0.52, 0.80);
    if (p < 6.06) return mix(vec3(0.92, 0.52, 0.80), vec3(0.80, 0.78, 0.82), mcsm_ramp(p, 5.95, 6.04));
    if (p < 6.90) return vec3(0.98, 0.72, 0.60);                       // orange-lit at 6.1
    if (p < 8.06) return mix(vec3(0.98, 0.58, 0.42), vec3(0.72, 0.30, 0.20), mcsm_ramp(p, 6.9, 8.0));
    return vec3(1.0);
}

vec3 mcsm_star_tint(float p) {
    if (!mcsm_fog_active(p)) return vec3(1.0);
    if (p < 5.19) return vec3(0.10, 0.24, 0.22);
    if (p < 5.95) return vec3(0.30, 0.14, 0.30);
    if (p < 6.90) return vec3(0.22, 0.18, 0.24);
    return vec3(0.28, 0.10, 0.08);
}

vec3 mcsm_sky_body_tint(float p, vec3 body) {
    if (p < 5.19) return body * 0.22;
    if (p < 6.00) return body * vec3(0.95, 0.45, 0.80) * 0.5;
    if (p < 6.90) return body * vec3(1.00, 0.55, 0.60);
    return body * vec3(1.25, 0.45, 0.25);
}

// ------------------------------------------------------------- attachments
vec3 mcsm_attachment_color(float p, float clock, vec3 localPos, vec2 uv,
                           float texAlpha, vec3 base) {
    float rings = 0.5 + 0.5 * sin((uv.y * 6.0 - uv.x * 2.0) * MCSM_TAU + clock * 2.0);
    float edge  = smoothstep(0.06, 0.20, texAlpha) * (1.0 - smoothstep(0.30, 0.55, texAlpha));
    vec3 core   = vec3(0.030, 0.000, 0.075) * (0.55 + 0.45 * rings);
    vec3 pink   = vec3(1.000, 0.120, 0.620);
    vec3 lattice = core + pink * (edge * 1.6 + pow(rings, 3.0) * 0.35);

    vec3 pal[4];
    pal[0] = vec3(1.00, 0.45, 0.05);
    pal[1] = vec3(0.80, 0.05, 0.10);
    pal[2] = vec3(0.85, 0.10, 0.90);
    pal[3] = vec3(0.06, 0.00, 0.13);
    float ang = atan(localPos.x, localPos.z) / MCSM_TAU;
    float spiral = fract(ang + localPos.y * 0.015 + clock * 0.08);
    vec3 acc = vec3(0.0);
    float wsum = 0.0;
    for (int i = 0; i < 4; i++) {
        float center = float(i) * 0.25 + 0.125;
        float w = max(0.0, 1.0 - abs(fract(spiral - center + 0.5) - 0.5) * 4.0);
        acc += pal[i] * w;
        wsum += w;
    }
    vec3 mass = wsum > 0.0001 ? acc / wsum : pal[3];
    mass *= 0.8 + 0.35 * rings;

    // lattice from 5.25; storm-mass from 5.95 all the way through phase 8
    vec3 out1 = mix(lattice, mass, smoothstep(5.90, 6.00, p));
    return out1 + base * 0.10;
}

// ============================================================================
//  MCSM 1.9.96 -- AURORA BOREALIS in the MOD side.
//  User ask (2026-09-04): "add Aurora Borealis to the sky in cold biomes, in
//  the mod as well" -- previously aurora lived only in the Iris pack.
//  This is a night-only, cold-biased curtain: soft green bases fading to
//  violet tips, slow drift, deliberately subtle (~0.05 luminance) so it never
//  fights the byte-matched day/night gradients it sits on top of. Storm path
//  never reaches it (storm branch returns earlier).
//  coolW is computed by the caller from the biome fog colour; the gate is
//  "bluish fog" = snowy/cold biomes strongly, temperate nights weakly.
// ============================================================================
vec3 mcsm_aurora(vec3 worldDir, float clock, float nightW, float coolW) {
    if (nightW <= 0.01 || coolW <= 0.01) return vec3(0.0);
    float h = clamp(worldDir.y, 0.0, 1.0);
    if (h < 0.05) return vec3(0.0);
    float az = atan(worldDir.x, worldDir.z);
    // slow curtain wave: three octaves of wobble drifting east, like a real rayed arc
    float x = az * 2.6 + clock * 0.010;
    float wave = sin(x) * 0.50 + sin(x * 1.73 + 1.30) * 0.30 + sin(x * 3.10 + 2.10) * 0.20;
    // the band hangs above the horizon; wave lifts/drops its lower edge
    float band = smoothstep(0.16, 0.42, h - 0.26 * wave) * (1.0 - smoothstep(0.52, 0.92, h));
    // ray structure: fine vertical striations inside the band
    float rays = 0.5 + 0.5 * sin(az * 22.0 + wave * 4.0 + clock * 0.040);
    rays = 0.65 + 0.35 * rays * rays;
    float curtains = 0.5 + 0.5 * sin(az * 9.0 + wave * 3.0 + clock * 0.045);
    curtains *= curtains;
    vec3 green  = vec3(0.10, 0.85, 0.45);   // classic aurora green base
    vec3 violet = vec3(0.45, 0.18, 0.80);   // violet-purple tips (MCSM palette)
    vec3 col = mix(green, violet, clamp(h * 1.6 - 0.20, 0.0, 1.0));
    return col * (band * curtains * rays) * 0.165 * nightW * coolW;
}

// ============================================================================
//  MCSM 1.9.98 -- THE DEATH SEQUENCE (user storyboard, 2026-09-04):
//    dt 0.00-0.30  world/ sky distort, white cracks web across the sky,
//                  tiny in-falling motes whip toward the storm
//    dt 0.30-0.55  the mass SHAKES (aim jitter) and shrinks in layers,
//                  whitening -- particles impulse inward
//    dt 0.55       the implosion FLASH (whole-sky white spike)
//    dt 0.55-1.00  SUPERNOVA: six translucent rings -- purple, pink, blue,
//                  orange, green, yellow -- expand from the storm across the
//                  horizon (the gameplay-side radius damage falls trees/blocks
//                  and is Java-owned; these rings are the sky visible part)
//    dt ~0.92+     everything eases off so the post-storm sky transition can
//                  drift back to normal as the carrier stops
//
//  CARRIER: FogSkyEnd band 1906..2906 => dt = (fogSkyEnd - 1900) * 0.01.
//  Nobody stamps it before the phase-31 Java driver, so this whole engine is
//  DORMANT in 1.9.98 (mcsm_death() returns -1) and the frame is untouched.
// ============================================================================
float mcsm_death(float fogSkyEnd) {
    if (fogSkyEnd >= 1906.0 && fogSkyEnd <= 2906.0)
        return clamp((fogSkyEnd - 1900.0) * 0.01, 0.0, 1.0);
    return -1.0;
}

// Act-I distortion: space itself wobbles. Applied to the SAMPLING direction,
// so the dome and everything keyed off it bends while staying continuous.
vec3 mcsm_death_dir(vec3 worldDir, float dt, float clock) {
    float warp = 0.010 * mcsm_ramp(dt, 0.00, 0.30);
    if (warp <= 0.0) return worldDir;
    vec3 d = normalize(worldDir);
    return normalize(d + vec3(
        sin(d.y * 41.0 + clock * 7.0),
        sin(d.z * 37.0 - clock * 8.0),
        sin(d.x * 43.0 + clock * 6.0)) * warp);
}

// White crack filaments crawling over the dome (dt 0..0.5, gone by the flash).
vec3 mcsm_death_cracks(vec3 worldDir, float dt, float clock) {
    float a = mcsm_ramp(dt, 0.02, 0.30) * (1.0 - mcsm_ramp(dt, 0.42, 0.55));
    if (a <= 0.001) return vec3(0.0);
    vec3 d = normalize(worldDir);
    float web = sin(d.x * 34.0 + clock * 0.35)
              * sin(d.y * 27.0 - clock * 0.22)
              * sin(d.z * 31.0 + clock * 0.28);
    web = pow(abs(web), 24.0);   // thin filaments, mostly dark sky between
    float flicker = 0.70 + 0.30 * sin(clock * 6.0 + d.y * 9.0);
    return vec3(1.0, 0.97, 0.95) * web * a * flicker * 0.65;
}

// Implosion body: the mass contracts to a white-hot point (dt 0.25..0.55),
// ringed by in-rushing pink/white motes ("particles impulsing inwards").
vec3 mcsm_death_implosion(vec3 worldDir, vec3 bossDir, float dt, float clock) {
    float a = mcsm_ramp(dt, 0.25, 0.34);
    if (a <= 0.001) return vec3(0.0);
    // the storm shakes in the air while it shrinks
    float shake = mcsm_ramp(dt, 0.30, 0.50) * 2.2;   // degrees of jitter
    vec3 jb = normalize(bossDir + vec3(
        sin(clock * 17.0), sin(clock * 21.0 + 1.0), sin(clock * 19.0 + 2.0))
        * (shake * 0.0174533));                      // deg -> rad
    float ang = degrees(acos(clamp(dot(normalize(worldDir), jb), -1.0, 1.0)));
    // layers peel inward: radius stair-steps down rather than sliding
    float layer = floor(mcsm_ramp(dt, 0.25, 0.55) * 4.0) * 0.25;  // 0,.25,.5,.75
    float outer = mix(28.0, 2.5, layer);
    if (ang >= outer + 14.0) return vec3(0.0);
    float shape = 1.0 - smoothstep(0.0, outer, ang);
    float hot   = pow(shape, 2.0);
    // white takes over as the mass whitens
    vec3 coreCol = mix(vec3(0.55, 0.12, 0.42), vec3(1.0, 0.98, 1.0),
                       mcsm_ramp(dt, 0.30, 0.53));
    // converging motes: bright spokes whose radius slides inward with dt
    float conv = ang - mix(40.0, 3.0, mcsm_ramp(dt, 0.25, 0.55));
    float motes = pow(0.5 + 0.5 * sin(conv * 1.6 - clock * 2.5), 6.0)
                * smoothstep(0.0, 6.0, ang) * (1.0 - smoothstep(outer, outer + 14.0, ang));
    vec3 moteCol = mix(vec3(1.0, 0.55, 0.85), vec3(1.0), 0.35);
    float burst = mcsm_ramp(dt, 0.45, 0.55);
    return (coreCol * (0.30 + hot * (0.5 + 1.6 * burst)) * a)
         + moteCol * motes * 0.22 * a;
}

// The supernova rings themselves.
vec3 mcsm_supernova(vec3 worldDir, vec3 bossDir, float dt, float clock) {
    float st = mcsm_ramp(dt, 0.55, 0.60);        // rings ignite at the flash
    if (st <= 0.0) return vec3(0.0);
    float ang = degrees(acos(clamp(dot(normalize(worldDir), normalize(bossDir)), -1.0, 1.0)));
    const vec3 RING_COL[6] = vec3[](
        vec3(0.62, 0.20, 0.95),   // purple
        vec3(0.98, 0.28, 0.72),   // pink
        vec3(0.25, 0.50, 1.00),   // blue
        vec3(1.00, 0.52, 0.12),   // orange
        vec3(0.25, 0.95, 0.45),   // green
        vec3(1.00, 0.86, 0.20));  // yellow
    vec3 acc = vec3(0.0);
    for (int i = 0; i < 6; i++) {
        float fi = float(i);
        float local = clamp((dt - 0.55 - fi * 0.035) / 0.45, 0.0, 1.0);
        if (local <= 0.0 || local >= 1.0) continue;
        float rDeg = local * (95.0 + fi * 14.0);        // each ring flies farther
        float w = 2.2 + fi * 0.35;                       // translucent band width
        float ring = exp(-pow((ang - rDeg) / w, 2.0));
        float fade = (1.0 - local) * (1.0 - local);      // dims as it crosses the sky
        acc += RING_COL[i] * ring * fade * 0.42;
    }
    return acc * st;
}

// The whole-sky white spike at dt = 0.55 ("completely implodes").
float mcsm_death_flash(float dt) {
    return exp(-pow((dt - 0.55) * 20.0, 2.0)) * 1.35;
}
