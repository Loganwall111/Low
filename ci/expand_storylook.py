#!/usr/bin/env python3
"""Inline #moj_import includes so offline glslang can compile Story Look shaders.
Mirrors glslcheck/shimcheck.py: vanilla includes are SHIM stubs; mcsm_visuals is real."""
import os
import re
import sys

SHIM = {'chunksection.glsl': '\n'
                      'uniform vec3 ChunkSectionPos;\n'
                      'uniform ivec3 ChunkPosition;\n'
                      'uniform int UseRgss;\n'
                      'uniform vec2 TextureSize;\n'
                      'uniform float ChunkVisibility;\n',
 'dynamictransforms.glsl': '\n'
                           'uniform vec4 ColorModulator;\n'
                           '#ifndef MCSM_SHIM_MODELVIEWMAT\n'
                           '#define MCSM_SHIM_MODELVIEWMAT\n'
                           'uniform mat4 ModelViewMat;\n'
                           '#endif\n'
                           'uniform vec3 ModelOffset;\n'
                           'uniform mat4 TextureMat;\n',
 'fog.glsl': '\n'
             'uniform vec4 FogColor;\n'
             'uniform float FogEnvironmentalStart;\n'
             'uniform float FogEnvironmentalEnd;\n'
             'uniform float FogRenderDistanceStart;\n'
             'uniform float FogRenderDistanceEnd;\n'
             'uniform float FogSkyStart;\n'
             'uniform float FogSkyEnd;\n'
             'uniform float FogCloudsStart;\n'
             'uniform float FogCloudsEnd;\n'
             'uniform float FogShape;\n'
             'float fog_spherical_distance(vec3 pos) { return length(pos); }\n'
             'float fog_cylindrical_distance(vec3 pos) { return '
             'length(pos.xz); }\n'
             'float linear_fog_value(float s, float start, float end) { return '
             'clamp((s - start) / max(end - start, 0.001), 0.0, 1.0); }\n'
             'float total_fog_value(float s, float c, float envS, float envE, '
             'float defS, float defE) {\n'
             '    return clamp((s - envS) / max(envE - envS, 0.001), 0.0, '
             '1.0)\n'
             '         + clamp((s - defS) / max(defE - defS, 0.001), 0.0, '
             '1.0);\n'
             '}\n'
             'vec4 apply_fog(vec4 c, float s, float cy, float envS, float '
             'envE, float defS, float defE, vec4 fog) {\n'
             '    float v = clamp(total_fog_value(s, cy, envS, envE, defS, '
             'defE), 0.0, 1.0);\n'
             '    return mix(c, fog, v * fog.a);\n'
             '}\n',
 'globals.glsl': '\n'
                 'uniform float GameTime;\n'
                 'uniform vec2 ScreenSize;\n'
                 'uniform ivec3 CameraBlockPos;\n'
                 'uniform vec3 CameraOffset;\n',
 'light.glsl': '\n'
               'uniform vec3 Light0_Direction;\n'
               'uniform vec3 Light1_Direction;\n'
               'vec2 minecraft_compute_light(vec3 l0, vec3 l1, vec3 n) { '
               'return vec2(0.5 + 0.5 * dot(n, normalize(l0)), 0.5 + 0.5 * '
               'dot(n, normalize(l1))); }\n'
               'vec4 minecraft_mix_light_separate(vec2 light, vec4 c) { return '
               'vec4(c.rgb * (light.x + light.y) * 0.5, c.a); }\n'
               'vec4 minecraft_mix_light(vec3 l0, vec3 l1, vec3 n, vec4 c) { '
               'return '
               'minecraft_mix_light_separate(minecraft_compute_light(l0, l1, '
               'n), c); }\n',
 'projection.glsl': '\n'
                    'uniform mat4 ProjMat;\n'
                    '// 26.2 projection.glsl also brings ModelViewMat '
                    '(terrain.vsh uses it with no\n'
                    '// dynamictransforms import). Guarded so double-import '
                    'never redefines.\n'
                    '#ifndef MCSM_SHIM_MODELVIEWMAT\n'
                    '#define MCSM_SHIM_MODELVIEWMAT\n'
                    'uniform mat4 ModelViewMat;\n'
                    '#endif\n',
 'sample_lightmap.glsl': '\n'
                         'vec4 sample_lightmap(sampler2D s, ivec2 uv) { return '
                         'texelFetch(s, uv, 0); }\n'}

def load_inc(name):
    # Always prefer SHIM for vanilla 26.2 includes (offline symbols match shimcheck).
    if name in SHIM:
        return SHIM[name]
    for cand in (
        "mcsm-core-shaders/include/" + name,
        "storylook/assets/minecraft/shaders/include/" + name,
        "glslcheck/inc262/" + name,
    ):
        if os.path.isfile(cand):
            body = open(cand).read()
            lines = [ln for ln in body.splitlines() if not ln.strip().startswith("#version")]
            return "\n".join(lines) + "\n"
    raise FileNotFoundError("include not found: " + name)

src = open(sys.argv[1]).read()
lines = src.splitlines()
body = []
seen = set()
prelude = ["#version 330"]
for ln in lines:
    if ln.strip().startswith("#version"):
        continue
    m = re.match(r"^\s*#moj_import\s*<minecraft:([A-Za-z0-9_./-]+)>\s*$", ln)
    if m:
        name = m.group(1)
        if name in seen:
            continue
        seen.add(name)
        prelude.append(load_inc(name))
        continue
    body.append(ln)
open(sys.argv[2], "w").write("\n".join(prelude) + "\n" + "\n".join(body) + "\n")
