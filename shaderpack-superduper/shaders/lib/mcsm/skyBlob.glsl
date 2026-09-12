/*
================================ MCSM INFINITE SKYBOX BLOB 1.9.218 ================================

    The glare as Telltale actually built it: an INFINITE skybox blob.

    The blob is NOT a 3D object and NOT distance fog.  It is a function of
    VIEW ANGLE around the storm direction -- exactly the "infinite hallway"
    trick.  Consequences that match the freecam observations:

      * flying up into the greenness / blackness NEVER ends: every ray that
        looks within the angular radius is inside it, no matter how far you
        travel, because the test is angle, not distance;
      * turning 180 degrees away fades back to the normal sky: rays outside
        the angular radius are untouched, so the vanilla sky "shines from
        the outer edges";
      * the blob travels with uStormPos, always framing the boss's back;
      * the center is a SOLID OPAQUE dark-matter core mask that replaces
        the game sky, and the border bleeds through a wide smooth gradient
        flare with zero blocky edges (all smoothsteps, per-fragment noise
        smudge, linear-filtered).

    Colours are the exact hex gradients below, dynamically lerped by
    uStormPhase.  No raymarching, no textures: a flat angular projection.
=============================================================================================
*/

#ifndef MCSM_SHARED_UNIFORMS
#define MCSM_SHARED_UNIFORMS
uniform mat4 gbufferModelViewInverse;
uniform mat4 gbufferProjectionInverse;
uniform vec3 cameraPosition;
uniform vec3 uStormPos;
uniform float uStormPhase;
#endif
uniform float uGlareSize;
// Java writes this carrier every frame; it is intentionally normalized 0..1.
uniform float u_StormProximity;


vec3 mcsmHex(float r, float g, float b){ return vec3(r, g, b) / 255.0; }

/* ---- compact 3D noise (smudge wobble only) ---- */
vec3 msMod289(vec3 x){ return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 msMod289(vec4 x){ return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 msPermute(vec4 x){ return msMod289(((x * 34.0) + 1.0) * x); }
vec4 msTInvSqrt(vec4 r){ return 1.79284291400159 - 0.85373472095314 * r; }
float msNoise(vec3 v){
    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);
    vec3 i  = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy;
    vec3 x3 = x0 - D.yyy;
    i = msMod289(i);
    vec4 p = msPermute(msPermute(msPermute(
        i.z + vec4(0.0, i1.z, i2.z, 1.0))
        + i.y + vec4(0.0, i1.y, i2.y, 1.0))
        + i.x + vec4(0.0, i1.x, i2.x, 1.0));
    float n_ = 0.142857142857;
    vec3 ns = n_ * D.wyz - D.xzx;
    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_);
    vec4 x = x_ * ns.x + ns.yyyy;
    vec4 y = y_ * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);
    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);
    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));
    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;
    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);
    vec4 norm = msTInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x; p1 *= norm.y; p2 *= norm.z; p3 *= norm.w;
    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}

/* ---- exact phase profiles (core / mid / outer / beam / zenith / bottom) ---- */
void mcsmBlobProfile(float phase, out vec3 coreC, out vec3 midC, out vec3 outerC,
                     out vec3 beamC, out vec3 zenC, out vec3 botC){
    // phase 4 (measured teal deck)
    vec3 c4 = mcsmHex(8.0, 20.0, 34.0);
    vec3 m4 = mcsmHex(28.0, 72.0, 74.0);
    vec3 o4 = mcsmHex(94.0, 160.0, 166.0);
    vec3 b4 = mcsmHex(200.0, 240.0, 255.0);
    vec3 z4 = c4; vec3 t4 = o4;
    // PHASE 5 -- #161A1D / #2D423F / #6A9A78 / cool energy accent
    vec3 c5 = mcsmHex(0x16, 0x1A, 0x1D);
    vec3 m5 = mcsmHex(0x2D, 0x42, 0x3F);
    vec3 o5 = mcsmHex(0x6A, 0x9A, 0x78);
    vec3 b5 = mcsmHex(0x84, 0xD8, 0xFF);
    vec3 z5 = c5; vec3 t5 = o5;
    // PHASE 5.5-5.9 -- #0B0410 / #2D1442 / #581C6E / #87529C
    vec3 c55 = mcsmHex(0x0B, 0x04, 0x10);
    vec3 m55 = mcsmHex(0x2D, 0x14, 0x42);
    vec3 h55 = mcsmHex(0x58, 0x1C, 0x6E);
    vec3 o55 = mcsmHex(0x87, 0x52, 0x9C);
    vec3 b55 = mcsmHex(0xB9, 0x76, 0xFF);
    vec3 z55 = c55; vec3 t55 = o55;
    // PHASE 6 -- #1A1226 / #462A52 / #966173 / #D89874
    vec3 c6 = mcsmHex(0x1A, 0x12, 0x26);
    vec3 m6 = mcsmHex(0x46, 0x2A, 0x52);
    vec3 l6 = mcsmHex(0x96, 0x61, 0x73);
    vec3 b6 = mcsmHex(0xD8, 0x98, 0x74);
    vec3 z6 = mcsmHex(0x1A, 0x12, 0x26);
    vec3 t6 = mcsmHex(0xD8, 0x98, 0x74);
    vec3 b6b = mcsmHex(0xF0, 0xB3, 0x8A);
    // phase 7 (green storm)
    vec3 c7 = mcsmHex(10.0, 26.0, 18.0);
    vec3 m7 = mcsmHex(30.0, 74.0, 46.0);
    vec3 o7 = mcsmHex(104.0, 168.0, 120.0);
    vec3 b7 = mcsmHex(180.0, 255.0, 210.0);
    vec3 z7 = c7; vec3 t7 = o7;
    // phase 8-9 (ember sky)
    vec3 c8 = mcsmHex(30.0, 10.0, 4.0);
    vec3 m8 = mcsmHex(96.0, 38.0, 14.0);
    vec3 o8 = mcsmHex(186.0, 118.0, 60.0);
    vec3 b8 = mcsmHex(255.0, 140.0, 60.0);
    vec3 z8 = c8; vec3 t8 = o8;

    coreC  = mix(c4, c5,  smoothstep(4.2, 5.0, phase));
    coreC  = mix(coreC, c55, smoothstep(5.0, 5.5, phase));
    coreC  = mix(coreC, c6,  smoothstep(5.5, 6.0, phase));
    coreC  = mix(coreC, c7,  smoothstep(6.0, 7.0, phase));
    coreC  = mix(coreC, c8,  smoothstep(7.0, 8.0, phase));
    midC   = mix(m4, m5,  smoothstep(4.2, 5.0, phase));
    midC   = mix(midC, m55, smoothstep(5.0, 5.5, phase));
    midC   = mix(midC, m6,  smoothstep(5.5, 6.0, phase));
    midC   = mix(midC, m7,  smoothstep(6.0, 7.0, phase));
    midC   = mix(midC, m8,  smoothstep(7.0, 8.0, phase));
    outerC = mix(o4, o5,  smoothstep(4.2, 5.0, phase));
    outerC = mix(outerC, mix(h55, o55, 0.5), smoothstep(5.0, 5.5, phase));
    outerC = mix(outerC, l6,  smoothstep(5.5, 6.0, phase));
    outerC = mix(outerC, o7,  smoothstep(6.0, 7.0, phase));
    outerC = mix(outerC, o8,  smoothstep(7.0, 8.0, phase));
    beamC  = mix(b4, b5,  smoothstep(4.2, 5.0, phase));
    beamC  = mix(beamC, b55, smoothstep(5.0, 5.5, phase));
    beamC  = mix(beamC, b6b, smoothstep(5.5, 6.0, phase));
    beamC  = mix(beamC, b7,  smoothstep(6.0, 7.0, phase));
    beamC  = mix(beamC, b8,  smoothstep(7.0, 8.0, phase));
    zenC   = mix(z4, z5,  smoothstep(4.2, 5.0, phase));
    zenC   = mix(zenC, z55, smoothstep(5.0, 5.5, phase));
    zenC   = mix(zenC, z6,  smoothstep(5.5, 6.0, phase));
    zenC   = mix(zenC, z7,  smoothstep(6.0, 7.0, phase));
    zenC   = mix(zenC, z8,  smoothstep(7.0, 8.0, phase));
    botC   = mix(t4, t5,  smoothstep(4.2, 5.0, phase));
    botC   = mix(botC, t55, smoothstep(5.0, 5.5, phase));
    botC   = mix(botC, t6,  smoothstep(5.5, 6.0, phase));
    botC   = mix(botC, t7,  smoothstep(6.0, 7.0, phase));
    botC   = mix(botC, t8,  smoothstep(7.0, 8.0, phase));
}

/* ---- the infinite sky blob ----
   returns vec4(rgb, coverage): coverage = 1 inside the dark-matter core
   (fully masks the game sky), falling smoothly to 0 at the flare edge. */
vec4 mcsmSkyBlob(vec2 texCoord){
    float phase = uStormPhase;
    float actv = smoothstep(3.8, 4.3, phase)
               * clamp(u_StormProximity, 0.0, 1.0);
    if (actv <= 0.002) return vec4(0.0);

    // world ray of this pixel (angular space -- the blob never "ends")
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec4 farP = gbufferProjectionInverse * vec4(ndc, 1.0, 1.0);
    vec4 nearP = gbufferProjectionInverse * vec4(ndc, -1.0, 1.0);
    farP /= max(abs(farP.w), 1.0E-6);
    nearP /= max(abs(nearP.w), 1.0E-6);
    vec3 rayDir = normalize(mat3(gbufferModelViewInverse) * (farP.xyz - nearP.xyz));

    vec3 stormDir = normalize(uStormPos - cameraPosition + vec3(1.0E-5));

    // local basis around the storm direction
    vec3 right = normalize(cross(vec3(0.0, 1.0, 0.0), stormDir) + vec3(1.0E-5));
    vec3 up    = cross(stormDir, right);
    float dotR = dot(rayDir, right);
    float dotU = dot(rayDir, up);
    float dotF = dot(rayDir, stormDir);
    float angX = atan(dotR, dotF);
    float angY = atan(dotU, dotF);

    // 1.9.306: compact, asymmetric angular paint mass. The old 0.62/0.42
    // ellipse made a direct circle and its single radial grade erased the
    // colour tongues visible in the reference frames.
    // 1.9.307: the alpha paint must sit behind the whole storm silhouette,
    // not appear as a distant small circle.
    const float HORIZONTAL_MULTIPLIER = 2.5;
    float ry = 0.72 / max(uGlareSize, 0.35);
    float rx = ry * HORIZONTAL_MULTIPLIER;
    vec2 q = vec2(angX / rx, angY / ry);
    float raw = length(q);
    float theta = atan(q.y, q.x);
    float n0 = msNoise(vec3(q * 1.35 + vec2(2.7, 8.4), phase * 0.11));
    float n1 = msNoise(vec3(q * 3.70 + vec2(-5.1, 3.2), phase * 0.19));
    float lobes = 0.075 * sin(theta * 3.0 + 0.7)
                + 0.048 * sin(theta * 7.0 - 1.4)
                + 0.028 * sin(theta * 11.0 + n1 * 5.0);
    float boundary = 1.0 + 0.23 * (n0 - 0.5) * 2.0 + lobes;
    boundary += 0.085 * smoothstep(-0.90, -0.05, q.y) * smoothstep(-1.0, -0.05, -q.x);
    boundary -= 0.070 * smoothstep(0.10, 0.85, q.y) * smoothstep(-0.20, 0.80, q.x);
    boundary = max(boundary, 0.58);
    float r = raw / boundary;

    vec3 coreC, midC, outerC, beamC, zenC, botC;
    mcsmBlobProfile(phase, coreC, midC, outerC, beamC, zenC, botC);

    // solid opaque dark-matter core (blocks the normal sky)
    float coreM = 1.0 - smoothstep(0.30, 0.46, r);
    // mid smudge bleed
    float midM  = smoothstep(0.30, 0.60, r) * (1.0 - smoothstep(0.58, 0.80, r));
    // outer gradient flare
    float outM  = smoothstep(0.60, 0.88, r) * (1.0 - smoothstep(0.86, 1.12, r));

    vec3 col = coreC;
    col = mix(col, midC, midM);
    col = mix(col, outerC, outM);
    // Broken paint tongues keep the phase colours visible as blended bands
    // instead of flattening the whole sky to one purple grade.
    float tongue = smoothstep(0.40, 0.72,
        msNoise(vec3(angX * 3.2 + 4.0, angY * 5.0 - 2.0, phase * 0.17)));
    col = mix(col, mix(midC, outerC, 0.68), tongue * (midM + outM) * 0.34);

    // phase-6 four-colour sunset split: outer band becomes a vertical
    // zenith -> burning-horizon ramp inside the blob
    float six = smoothstep(5.6, 6.0, phase) * (1.0 - smoothstep(6.0, 6.6, phase));
    if (six > 0.01){
        float vgrad = smoothstep(-0.85, 0.85, angY / ry);
        col = mix(col, mix(zenC, botC, vgrad), six * smoothstep(0.45, 0.9, r));
    }

    // energy beam accent pinned at the exact centre
    col += beamC * exp(-r * r * 52.0) * 0.85 * smoothstep(4.9, 5.0, phase);

    // coverage: fully opaque core, heavy mid, vanishing flare edge so the
    // vanilla sky shines through the outer borders
    float cov = clamp(coreM + midM * 0.88 + outM * 0.50, 0.0, 1.0) * actv;
    return vec4(col, cov);
}
