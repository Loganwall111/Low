/*
================================ MCSM STORM VOLUME (OPTIONAL LAYER) 1.9.219 ================================

    Pure code-driven volumetric cloud deck for the Wither Storm, per the
    original formula:

      * NO billboard particles, NO texture sheets -- a math-based 3D
        raymarched volume;
      * a massive bounding cloud box permanently centered on the camera
        (always enveloped);
      * uStormPos warps the 3D simplex turbulence: density scales up
        exponentially as the ray approaches the storm;
      * phase-driven dynamic colour lerp with the exact hex profiles below.

    IMPORTANT: this is an OPTIONAL layer and is disabled by default. The
    live Halo is owned by the tethered native renderer; this file must never
    create a second sky attachment. Enable this deck only by defining
    MCSM_STORM_VOLUME_EXTRA in main/composite6.glsl.
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
uniform float frameTime;
uniform sampler2D depthtex0;


vec3 msVolHex(float r, float g, float b){ return vec3(r, g, b) / 255.0; }

/* ---- Ashima 3D simplex noise (code-driven turbulence) ---- */
vec3 msVolMod289(vec3 x){ return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 msVolMod289(vec4 x){ return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 msVolPermute(vec4 x){ return msVolMod289(((x * 34.0) + 1.0) * x); }
vec4 msVolTaylorInvSqrt(vec4 r){ return 1.79284291400159 - 0.85373472095314 * r; }

float msVolSnoise(vec3 v){
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
    i = msVolMod289(i);
    vec4 p = msVolPermute(msVolPermute(msVolPermute(
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
    vec4 norm = msVolTaylorInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x; p1 *= norm.y; p2 *= norm.z; p3 *= norm.w;
    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}

float msVolFbm(vec3 p){
    return msVolSnoise(p) * 0.55
         + msVolSnoise(p * 2.03 + vec3(11.5, 7.3, 3.1)) * 0.27
         + msVolSnoise(p * 4.07 + vec3(23.0, 13.7, 9.2)) * 0.18;
}

/* ---- phase colour profiles: the exact hex ramps, lerped by phase ---- */
void msVolProfile(float phase, out vec3 skyC, out vec3 horC, out vec3 auraC, out vec3 beamC){
    // calm / unset: neutral navy haze (never purple at night)
    vec3 s0 = vec3(0.012, 0.028, 0.075);
    vec3 h0 = vec3(0.055, 0.085, 0.165);
    vec3 a0 = vec3(0.0);
    vec3 b0 = vec3(0.0);
    // phase 4: measured teal-blue deck
    vec3 s4 = msVolHex(14.0, 42.0, 74.0);
    vec3 h4 = msVolHex(66.0, 150.0, 180.0);
    vec3 a4 = msVolHex(45.0, 200.0, 210.0);
    vec3 b4 = msVolHex(200.0, 240.0, 255.0);
    // PHASE 5 -- #1A2E30 / #3D6266 / #2DE0D7 / #D2FCFA
    vec3 s5 = msVolHex(0x1A, 0x2E, 0x30);
    vec3 h5 = msVolHex(0x3D, 0x62, 0x66);
    vec3 a5 = msVolHex(0x2D, 0xE0, 0xD7);
    vec3 b5 = msVolHex(0xD2, 0xFC, 0xFA);
    // PHASE 5.5-5.9 -- #2A153D / #52297A / #8E44AD / #B976FF
    vec3 s55 = msVolHex(0x2A, 0x15, 0x3D);
    vec3 h55 = msVolHex(0x52, 0x29, 0x7A);
    vec3 a55 = msVolHex(0x8E, 0x44, 0xAD);
    vec3 b55 = msVolHex(0xB9, 0x76, 0xFF);
    // PHASE 6 -- #120D1A / burning horizon #D98353 / #4B2766 / #F0B38A
    vec3 s6 = msVolHex(0x12, 0x0D, 0x1A);
    vec3 h6 = msVolHex(0xD9, 0x83, 0x53);
    vec3 a6 = msVolHex(0x4B, 0x27, 0x66);
    vec3 b6 = msVolHex(0xF0, 0xB3, 0x8A);
    // phase 7: green-torn storm
    vec3 s7 = vec3(0.020, 0.055, 0.045);
    vec3 h7 = vec3(0.165, 0.330, 0.255);
    vec3 a7 = msVolHex(115.0, 255.0, 158.0);
    vec3 b7 = msVolHex(180.0, 255.0, 210.0);
    // phase 8-9: ember
    vec3 s8 = msVolHex(30.0, 4.0, 2.0);
    vec3 h8 = msVolHex(186.0, 135.0, 86.0);
    vec3 a8 = msVolHex(217.0, 89.0, 31.0);
    vec3 b8 = msVolHex(255.0, 128.0, 51.0);

    skyC  = mix(mix(s0, s4, smoothstep(0.0, 4.2, phase)), s5, smoothstep(4.2, 5.0, phase));
    skyC  = mix(skyC, s55, smoothstep(5.0, 5.5, phase));
    skyC  = mix(skyC, s6,  smoothstep(5.5, 6.0, phase));
    skyC  = mix(skyC, s7,  smoothstep(6.0, 7.0, phase));
    skyC  = mix(skyC, s8,  smoothstep(7.0, 8.0, phase));
    horC  = mix(mix(h0, h4, smoothstep(0.0, 4.2, phase)), h5, smoothstep(4.2, 5.0, phase));
    horC  = mix(horC, h55, smoothstep(5.0, 5.5, phase));
    horC  = mix(horC, h6,  smoothstep(5.5, 6.0, phase));
    horC  = mix(horC, h7,  smoothstep(6.0, 7.0, phase));
    horC  = mix(horC, h8,  smoothstep(7.0, 8.0, phase));
    auraC = mix(a4, a5, smoothstep(4.2, 5.0, phase));
    auraC = mix(auraC, a55, smoothstep(5.0, 5.5, phase));
    auraC = mix(auraC, a6,  smoothstep(5.5, 6.0, phase));
    auraC = mix(auraC, a7,  smoothstep(6.0, 7.0, phase));
    auraC = mix(auraC, a8,  smoothstep(7.0, 8.0, phase));
    beamC = mix(b4, b5, smoothstep(4.2, 5.0, phase));
    beamC = mix(beamC, b55, smoothstep(5.0, 5.5, phase));
    beamC = mix(beamC, b6,  smoothstep(5.5, 6.0, phase));
    beamC = mix(beamC, b7,  smoothstep(6.0, 7.0, phase));
    beamC = mix(beamC, b8,  smoothstep(7.0, 8.0, phase));
}

/* ---- standard ray-box intersection to isolate the sky volume ---- */
vec2 msVolRayBox(vec3 ro, vec3 rd, vec3 bmin, vec3 bmax){
    vec3 inv = 1.0 / max(abs(rd), vec3(1.0E-6));
    vec3 t0 = (bmin - ro) * inv;
    vec3 t1 = (bmax - ro) * inv;
    vec3 tmin = min(t0, t1), tmax = max(t0, t1);
    float a = max(max(tmin.x, tmin.y), tmin.z);
    float b = min(min(tmax.x, tmax.y), tmax.z);
    return vec2(max(a, 0.0), b);
}

/* ---- the raymarched storm volume ----
   returns vec4(averageColour, averageDensity) */
vec4 mcsmStormVolume(vec2 texCoord){
    float phase = uStormPhase;
    vec3 skyC, horC, auraC, beamC;
    msVolProfile(phase, skyC, horC, auraC, beamC);
    float stormActive = clamp(phase - 3.5, 0.0, 1.0);
    vec3 stormPos = uStormPos;
    float stormR = 14.0 + 12.0 * clamp(phase - 4.0, 0.0, 4.0);

    // world-space ray through this pixel
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec4 farP = gbufferProjectionInverse * vec4(ndc, 1.0, 1.0);
    vec4 nearP = gbufferProjectionInverse * vec4(ndc, -1.0, 1.0);
    farP /= max(abs(farP.w), 1.0E-6);
    nearP /= max(abs(nearP.w), 1.0E-6);
    vec3 ro = cameraPosition;
    vec3 rd = normalize(mat3(gbufferModelViewInverse) * (farP.xyz - nearP.xyz));

    // the cloud deck: a massive box permanently centered on the camera
    vec3 bmin = ro + vec3(-360.0, -160.0, -360.0);
    vec3 bmax = ro + vec3( 360.0,  620.0,  360.0);
    vec2 tb = msVolRayBox(ro, rd, bmin, bmax);
    if (tb.y <= tb.x) return vec4(0.0);

    vec3 acc = vec3(0.0);
    float accD = 0.0;
    const int STEPS = 24;
    float t = tb.x + 4.0;
    float stride = max(6.0, (tb.y - tb.x) / float(STEPS));

    for (int i = 0; i < STEPS; i++){
        vec3 p = ro + rd * t;
        float hgt = p.y - ro.y;
        // deck band: horizon -> ~200 above, thinning above 420
        float band = smoothstep(-40.0, 12.0, hgt) * (1.0 - smoothstep(150.0, 520.0, hgt));
        if (band > 0.002){
            vec3 sp = p * 0.006 + vec3(0.0, frameTime * 0.012, 0.0);
            float dens = max(0.0, msVolFbm(sp) * 0.55 + 0.45) * band;
            if (stormActive > 0.01){
                float sd = distance(p, stormPos);
                // exponential density ramp toward the storm core
                float warp = stormActive * (1.0 + 7.5 * exp(-sd * 0.028));
                dens *= warp;
                // swirl the sample around the storm axis (the churn)
                float swirl = (1.0 - clamp(sd / 150.0, 0.0, 1.0)) * 1.4;
                float ang = swirl * (0.55 + 0.45 * sin(frameTime * 0.16));
                vec2 csp = p.xz - stormPos.xz;
                float cs = cos(ang), sn = sin(ang);
                csp = mat2(cs, -sn, sn, cs) * csp;
                vec3 swp = vec3(csp.x + stormPos.x, p.y, csp.y + stormPos.z) * 0.006
                         + vec3(0.0, frameTime * 0.02, 0.0);
                dens += max(0.0, msVolFbm(swp)) * 0.30 * warp;
            }
            dens = clamp(dens, 0.0, 1.8);
            if (dens > 0.004){
                float g = smoothstep(8.0, 230.0, hgt);
                vec3 col = mix(horC, skyC, g) * dens;
                // aura glow around the storm core
                float aura = clamp(1.0 - distance(p, stormPos) / (stormR * 3.4), 0.0, 1.0);
                col += auraC * dens * aura * aura * 1.5 * stormActive;
                // beam pierce: bright shaft inside the storm column
                if (phase >= 5.0){
                    float hd = length((p - stormPos).xz);
                    float shaft = exp(-hd * hd / (stormR * stormR * 1.25))
                        * smoothstep(stormPos.y - 90.0, stormPos.y, p.y)
                        * (1.0 - smoothstep(stormPos.y + stormR * 1.8, stormPos.y + stormR * 5.5, p.y));
                    col += beamC * shaft * 0.85 * stormActive;
                }
                acc += col;
                accD += dens;
            }
        }
        t += stride;
        if (t > tb.y) break;
    }
    accD = clamp(accD / float(STEPS), 0.0, 1.0);
    // calm (no storm uniform yet): a weak neutral haze only -- the storm
    // deck takes over the sky as the phase climbs
    accD *= 0.30 + 0.70 * stormActive;
    vec3 avg = acc / max(accD * float(STEPS), 1.0E-4);
    return vec4(avg, accD);
}
