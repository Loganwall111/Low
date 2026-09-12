#!/usr/bin/env python3
"""shimcheck.py — offline syntax validation for the MCSM core shaders.

Replaces the (LFS-locked) valcore/validate harnesses for this session: strips
#moj_import lines, inlines our mcsm_visuals.glsl, and supplies minimal
COMPILE-ONLY shims for the vanilla includes (uniforms + function signatures).
These shims never ship — the game pulls the real vanilla includes at runtime.
The point is to catch GLSL syntax/typing errors without a Minecraft client jar.

Usage: python3 shimcheck.py <core_shaders_dir> [extra_glsl files...]
Exit 0 if every generated translation unit passes glslang, 1 otherwise.
"""
import os
import re
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
GLSLANG = os.path.join(HERE, 'bin', 'glslang')

SHIM = {
    'fog.glsl': r"""
uniform vec4 FogColor;
uniform float FogEnvironmentalStart;
uniform float FogEnvironmentalEnd;
uniform float FogRenderDistanceStart;
uniform float FogRenderDistanceEnd;
uniform float FogSkyStart;
uniform float FogSkyEnd;
uniform float FogCloudsStart;
uniform float FogCloudsEnd;
uniform float FogShape;
float fog_spherical_distance(vec3 pos) { return length(pos); }
float fog_cylindrical_distance(vec3 pos) { return length(pos.xz); }
float linear_fog_value(float s, float start, float end) { return clamp((s - start) / max(end - start, 0.001), 0.0, 1.0); }
float total_fog_value(float s, float c, float envS, float envE, float defS, float defE) {
    return clamp((s - envS) / max(envE - envS, 0.001), 0.0, 1.0)
         + clamp((s - defS) / max(defE - defS, 0.001), 0.0, 1.0);
}
vec4 apply_fog(vec4 c, float s, float cy, float envS, float envE, float defS, float defE, vec4 fog) {
    float v = clamp(total_fog_value(s, cy, envS, envE, defS, defE), 0.0, 1.0);
    return mix(c, fog, v * fog.a);
}
""",
    'dynamictransforms.glsl': r"""
uniform vec4 ColorModulator;
#ifndef MCSM_SHIM_MODELVIEWMAT
#define MCSM_SHIM_MODELVIEWMAT
uniform mat4 ModelViewMat;
#endif
uniform vec3 ModelOffset;
uniform mat4 TextureMat;
""",
    'globals.glsl': r"""
uniform float GameTime;
uniform vec2 ScreenSize;
uniform ivec3 CameraBlockPos;
uniform vec3 CameraOffset;
""",
    'projection.glsl': r"""
uniform mat4 ProjMat;
// 26.2 projection.glsl also brings ModelViewMat (terrain.vsh uses it with no
// dynamictransforms import). Guarded so double-import never redefines.
#ifndef MCSM_SHIM_MODELVIEWMAT
#define MCSM_SHIM_MODELVIEWMAT
uniform mat4 ModelViewMat;
#endif
""",
    'light.glsl': r"""
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;
vec2 minecraft_compute_light(vec3 l0, vec3 l1, vec3 n) { return vec2(0.5 + 0.5 * dot(n, normalize(l0)), 0.5 + 0.5 * dot(n, normalize(l1))); }
vec4 minecraft_mix_light_separate(vec2 light, vec4 c) { return vec4(c.rgb * (light.x + light.y) * 0.5, c.a); }
vec4 minecraft_mix_light(vec3 l0, vec3 l1, vec3 n, vec4 c) { return minecraft_mix_light_separate(minecraft_compute_light(l0, l1, n), c); }
""",
    'sample_lightmap.glsl': r"""
vec4 sample_lightmap(sampler2D s, ivec2 uv) { return texelFetch(s, uv, 0); }
""",
    'chunksection.glsl': r"""
uniform vec3 ChunkSectionPos;
uniform ivec3 ChunkPosition;
uniform int UseRgss;
uniform vec2 TextureSize;
uniform float ChunkVisibility;
""",
}

IMPORT_RE = re.compile(r'^\s*#moj_import\s*<minecraft:([A-Za-z0-9_./-]+)>\s*$')


def build_tu(path, defines, include_dir):
    """Assembles a standalone TU: shims for vanilla imports, real include for ours."""
    with open(path) as f:
        lines = f.read().splitlines()
    prelude = ['#version 330']
    for n, v in defines.items():
        prelude.append('#define %s %s' % (n, v))
    body = []
    imported = set()
    for ln in lines:
        if ln.strip().startswith('#version'):
            continue
        m = IMPORT_RE.match(ln)
        if m:
            inc = m.group(1)
            if inc in imported:
                continue
            imported.add(inc)
            if inc == 'mcsm_visuals.glsl':
                with open(os.path.join(include_dir, 'include', 'mcsm_visuals.glsl')) as g:
                    inc_body = [l2 for l2 in g.read().splitlines() if not l2.strip().startswith('#version')]
                prelude.append('\n'.join(inc_body))
            elif inc in SHIM:
                prelude.append(SHIM[inc])
            elif inc.startswith('mcsm_') and os.path.isfile(os.path.join(include_dir, 'include', inc)):
                # Custom MCSM libraries ship beside mcsm_visuals.glsl. Inline
                # them exactly like the shared visuals include so the offline
                # gate validates new core imports too (Minecraft resolves these
                # from the assembled shader include directory at runtime).
                with open(os.path.join(include_dir, 'include', inc)) as g:
                    inc_body = [l2 for l2 in g.read().splitlines() if not l2.strip().startswith('#version')]
                prelude.append('\n'.join(inc_body))
            else:
                raise SystemExit('no shim for include %s in %s' % (inc, path))
            continue
        body.append(ln)
    return '\n'.join(prelude) + '\n' + '\n'.join(body) + '\n'


def check(path, defines, include_dir, tmp):
    src = build_tu(path, defines, include_dir)
    ext = '.frag' if path.endswith('.fsh') else '.vert'
    tag = os.path.basename(path) + '.' + ('all_on' if defines else 'plain')
    tu = os.path.join(tmp, tag + ext)
    with open(tu, 'w') as f:
        f.write(src)
    r = subprocess.run([GLSLANG, tu], capture_output=True, text=True)
    ok = r.returncode == 0
    return ok, (r.stdout + r.stderr).strip(), tu


def main():
    core = os.path.abspath(sys.argv[1])
    extras = sys.argv[2:]
    tmp = tempfile.mkdtemp(prefix='mcsm_glsl_')
    if os.access(GLSLANG, os.X_OK) is False:
        os.chmod(GLSLANG, 0o755)
    fails = 0
    total = 0

    cores = []
    for n in sorted(os.listdir(os.path.join(core, 'core'))):
        if n.endswith(('.fsh', '.vsh')):
            cores.append(os.path.join(core, 'core', n))

    combo_sets = [
        ('plain', {}),
        ('lit', {'PER_FACE_LIGHTING': '1', 'STORM_SHADING': '1', 'SUN_X': '0.30', 'SUN_Y': '0.80', 'SUN_Z': '0.50'}),
        ('misc', {'DISSOLVE': '1', 'ALPHA_CUTOUT': '0.5F', 'NO_OVERLAY': '1', 'EMISSIVE': '1', 'APPLY_TEXTURE_MATRIX': '1'}),
        ('rev', {'NO_CARDINAL_LIGHTING': '1', 'REVERSE_SHADING': '1'}),
    ]
    for path in cores:
        for tag, defines in combo_sets:
            total += 1
            ok, log, tu = check(path, defines, core, tmp)
            if not ok:
                fails += 1
                print('FAIL %s [%s]\n%s' % (os.path.relpath(path), tag, log[:4000]))

    for path in extras:
        total += 1
        ok, log, tu = check(path, {}, HERE, tmp)
        if not ok:
            fails += 1
            print('FAIL %s\n%s' % (path, log[:4000]))

    print('shimcheck: %d/%d pass' % (total - fails, total))
    return 1 if fails else 0


if __name__ == '__main__':
    sys.exit(main())
