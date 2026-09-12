/*
================================ ATMOSPHERIC W'S CLOUD 1.9.218 =================================

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

// Three-octave FBM used by Atmospheric W's Cloud. The last octave is
// deliberately fine enough to shred soot and dust into the sky.
float msFbm3(vec3 p){
    float sum = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    for (int i = 0; i < 3; i++) {
        sum += msNoise(p) * amp;
        norm += amp;
        p = p * 2.03 + vec3(17.13, 9.71, 4.37);
        amp *= 0.5;
    }
    return sum / norm;
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

/* ---- Atmospheric W's Cloud ----
   returns vec4(rgb, densityAlpha): the dark smoke is capped at 0.80 and
   falls smoothly to zero at the shredded horizon edge. */
vec4 mcsmSkyBlob(vec2 texCoord){
    float phase = uStormPhase;
    float actv = smoothstep(3.8, 4.3, phase)
               * clamp(u_StormProximity, 0.0, 1.0);
    if (actv <= 0.002) return vec4(0.0);

    // World ray of this pixel. This is the infinite spatial wrap: the cloud
    // is evaluated from look direction and storm direction only, never from a
    // finite world-space box or a sphere that the player could approach.
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec4 farP = gbufferProjectionInverse * vec4(ndc, 1.0, 1.0);
    vec4 nearP = gbufferProjectionInverse * vec4(ndc, -1.0, 1.0);
    farP /= max(abs(farP.w), 1.0E-6);
    nearP /= max(abs(nearP.w), 1.0E-6);
    vec3 rayDir = normalize(mat3(gbufferModelViewInverse) * (farP.xyz - nearP.xyz));
    vec3 stormDir = normalize(uStormPos - cameraPosition + vec3(1.0E-5));

    // Local horizon axes around the storm's view direction.
    vec3 right = normalize(cross(vec3(0.0, 1.0, 0.0), stormDir) + vec3(1.0E-5));
    vec3 up = cross(stormDir, right);
    float dotR = dot(rayDir, right);
    float dotU = dot(rayDir, up);
    float dotF = dot(rayDir, stormDir);
    float angX = atan(dotR, dotF);
    float angY = atan(dotU, dotF);

    // Wide Horizon Smog: multiply the horizontal plane by 4 and compress Y
    // to 0.5. max(abs()) is an intentionally flat weather-wall boundary;
    // there is no length(uv), circular radius, ellipse, or doughnut here.
    const float HORIZONTAL_STRETCH = 4.0;
    const float VERTICAL_COMPRESSION = 0.5;
    const float TOP_LIFT = 0.22; // cloud crown sits above the storm bearing
    const float HORIZONTAL_MULTIPLIER = 2.5;
    float baseY = 0.72 / max(uGlareSize, 0.35);
    float baseX = baseY * HORIZONTAL_MULTIPLIER;
    vec2 smog = vec2(
        angX / (baseX * HORIZONTAL_STRETCH),
        angY / (baseY * VERTICAL_COMPRESSION) - TOP_LIFT);
    vec2 absSmog = abs(smog);
    float boxEdge = max(absSmog.x, absSmog.y);

    // Broad, asymmetric ink shoulders plus a 3-octave fine tear pattern.
    float broad = msFbm3(vec3(smog * 1.35 + vec2(2.7, 8.4), phase * 0.11));
    float fine = msFbm3(vec3(smog * 5.75 + vec2(-5.1, 3.2), phase * 0.19));
    float shred = msFbm3(vec3(smog * 12.0 + vec2(13.0, -7.0), phase * 0.27));
    float shoulder = 0.20 * smoothstep(-1.0, 0.15, -smog.x)
                   * (1.0 - smoothstep(-0.20, 0.80, smog.y));
    float trailing = 0.16 * smoothstep(-0.15, 0.95, smog.x)
                   * (1.0 - smoothstep(-0.30, 0.65, smog.y));
    float notch = 0.13 * smoothstep(0.05, 0.80, smog.y)
                * smoothstep(-0.15, 0.85, smog.x);
    float boundary = 0.86 + shoulder + trailing - notch
                   + (broad - 0.5) * 0.30
                   + (fine - 0.5) * 0.16
                   + (shred - 0.5) * 0.10;
    boundary = max(boundary, 0.48);
    float edgeCoord = boxEdge / boundary;

    vec3 coreC, midC, outerC, beamC, zenC, botC;
    mcsmBlobProfile(phase, coreC, midC, outerC, beamC, zenC, botC);
    float upness = clamp(smog.y * 0.5 + 0.5, 0.0, 1.0);

    // Semi-transparent smoke density. The centre is capped at 80%, and the
    // squared noise curve makes the outer soot dissolve into the phase sky.
    float body = 1.0 - smoothstep(0.46,
        1.08 + (fine - 0.5) * 0.28, edgeCoord);
    body *= 1.0 - smoothstep(0.72, 1.14,
        edgeCoord + (shred - 0.5) * 0.16);
    float smokeNoise = clamp(0.50 + 0.50 * msFbm3(vec3(
        smog * 9.0 + vec2(3.0, 1.0), phase * 0.31)), 0.0, 1.0);
    float densityAlpha = min(0.80,
        0.80 * pow(clamp(body * smokeNoise, 0.0, 1.0), 2.0)) * actv;
    if (densityAlpha <= 0.001) return vec4(0.0);

    // Preserve the sunset/phase colors through the dark cloud center.
    vec3 phaseColor = mix(botC, zenC, upness);
    vec3 smokeColor = mix(phaseColor, vec3(0.02), densityAlpha);
    smokeColor = mix(smokeColor, mix(midC, outerC, clamp(upness * 0.72 + broad * 0.28, 0.0, 1.0)),
                     densityAlpha * (1.0 - body) * 0.12);
    return vec4(smokeColor, densityAlpha);
}
