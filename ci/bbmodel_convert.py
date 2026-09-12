#!/usr/bin/env python3
"""1.9.215: convert real Telltale bbmodel meshes -> OptiFine/EMF JEM models.

The asset pack (repo Loganwall111/gggggrff) stores the 1:1 storm meshes as
free-format triangle meshes (vertices + per-vertex UV faces).  Minecraft
models are cube-based, so this converter VOXELIZES the mesh:

  * every vertex, edge-midpoint and face-centroid marks its voxel cell,
  * the shell is flood-filled solid along all three axes,
  * each voxel becomes one JEM box with a textureOffset derived from the
    per-vertex UVs scaled into the 160x160 JEM atlas,
  * outliner groups become JEM models (submodels under a single part),
    carrying the Telltale transforms (translate + rotate, invertAxis xy).

Output is written straight into the ogs-cem CEM pack so the build ships it.
"""
import json, math, os, sys

JEM_HEAD = """{
\t"credit": "Telltale 1:1 meshes (extracted by wheatlycrab5892, voxelised by Devouring Storms)",
\t"textureSize": [160, 160],
\t"models": [
"""
JEM_TAIL = """\t]
}
"""

def load(path):
    with open(path, encoding='utf-8', errors='replace') as f:
        return json.load(f)

def element_cloud(element):
    """-> (verts: id->(x,y,z), faces: [(ids, tex, uv)])"""
    verts = {}
    for vid, v in element.get('vertices', {}).items():
        if isinstance(v, list) and len(v) >= 3:
            try:
                verts[vid] = (float(v[0]), float(v[1]), float(v[2]))
            except (TypeError, ValueError):
                pass
    faces = []
    for f in element.get('faces', {}).values():
        ids = f.get('vertices', [])
        if len(ids) < 3:
            continue
        uv = f.get('uv', {})
        faces.append((ids, f.get('texture', 0), uv))
    return verts, faces

def bounds(pts):
    mn = [min(p[i] for p in pts) for i in range(3)]
    mx = [max(p[i] for p in pts) for i in range(3)]
    return mn, mx

class Voxelizer:
    def __init__(self, step):
        self.step = float(step)
        self.cells = {}          # (i,j,k) -> (u, v, tex, n)
        self.pts = []            # all verts for bounds

    def add_point(self, x, y, z, u, v, tex):
        self.pts.append((x, y, z))
        i = int(math.floor(x / self.step))
        j = int(math.floor(y / self.step))
        k = int(math.floor(z / self.step))
        key = (i, j, k)
        cur = self.cells.get(key)
        if cur is None:
            self.cells[key] = [u, v, tex, 1]
        else:
            cur[0] += u; cur[1] += v; cur[3] += 1
            if tex is not None:
                cur[2] = tex

    def feed(self, verts, faces):
        for fid, (ids, tex, uv) in enumerate(faces):
            pts = []
            us = []
            for vid in ids:
                p = verts.get(vid)
                if p is None:
                    continue
                pts.append(p)
                uvv = uv.get(vid, [0.0, 0.0])
                try:
                    us.append((float(uvv[0]), float(uvv[1])))
                except (TypeError, ValueError):
                    us.append((0.0, 0.0))
            if len(pts) < 3:
                continue
            samples = list(pts)
            n = len(pts)
            for a in range(n):
                b = (a + 1) % n
                mid = tuple((pts[a][d] + pts[b][d]) * 0.5 for d in range(3))
                samples.append(mid)
            samples.append(tuple(sum(pts[a][d] for a in range(n)) / n for d in range(3)))
            for s in samples:
                # nearest vertex uv for the sample
                self.add_point(s[0], s[1], s[2], us[0][0], us[0][1], tex)

    def flood_fill(self):
        # union of axis fills: any gap in the shell becomes solid
        filled = set(self.cells.keys())
        for axis in range(3):
            a, b, c = (axis + 1) % 3, (axis + 2) % 3, axis
            by_pair = {}
            for (i, j, k) in self.cells:
                key = (i, j, k)[::]  # copy
                p = (key[a], key[b])
                by_pair.setdefault(p, []).append(key[c])
            for p, cs in by_pair.items():
                lo, hi = min(cs), max(cs)
                for v in range(lo + 1, hi):
                    key = [0, 0, 0]
                    key[a] = p[0]; key[b] = p[1]; key[c] = v
                    filled.add(tuple(key))
        return filled

    def boxes(self, filled, mn, cx, cy_off, cz, scale, atlas=160):
        """-> list of jem box dicts (centered, y-down like bbmodel)"""
        out = []
        for (i, j, k) in filled:
            x = (mn[0] + (i + 0.5) * self.step - cx) * scale
            y = (mn[1] + (j + 0.5) * self.step - cy_off) * scale
            z = (mn[2] + (k + 0.5) * self.step - cz) * scale
            sz = self.step * scale
            info = self.cells.get((i, j, k))
            u = v = 0
            if info is not None and info[3] > 0:
                u = int(info[0] / info[3]) % atlas
                v = int(info[1] / info[3]) % atlas
            out.append({
                "coordinates": [round(x, 3), round(y, 3), round(z, 3),
                                round(sz, 3), round(sz, 3), round(sz, 3)],
                "textureOffset": [u, v],
            })
        return out

def outliner_leaves(outliner):
    """-> list of (path_name, origin, rotation, [element uuids])"""
    leaves = []
    def walk(node, path):
        if not isinstance(node, dict):
            # leaf: element uuid string
            leaves.append((path, None, None, [node]))
            return
        name = node.get('name') or 'part'
        p2 = path + [name]
        kids = node.get('children', [])
        if not kids:
            leaves.append((p2, node.get('origin'), node.get('rotation'), []))
            return
        for kid in kids:
            if isinstance(kid, dict) and not kid.get('children'):
                # leaf group may hold an element uuid inside children
                el = kid.get('children')
                if isinstance(el, list) and el and isinstance(el[0], str):
                    leaves.append((p2 + [kid.get('name') or 'part'],
                                   kid.get('origin'), kid.get('rotation'), el))
                    continue
            walk(kid, p2)
    for n in outliner:
        walk(n, [])
    return leaves

def convert(src, dst, target_height, layers, part, max_boxes=9000,
            uv_divisor=15.0, atlas=160):
    d = load(src)
    elements = [e for e in d.get('elements', []) if e.get('vertices')]
    uuid_map = {}
    clouds = {}
    for e in elements:
        uuid_map[e.get('uuid')] = e
    leaves = outliner_leaves(d.get('outliner', []))
    groups = []
    for path, origin, rot, refs in leaves:
        els = [uuid_map.get(r) for r in refs if r in uuid_map]
        els = [e for e in els if e]
        if els:
            groups.append((path, origin, rot, els))
    if not groups:
        # flat outliner (bare element uuids): one body group with everything
        groups = [(['body'], None, None, elements)]

    # global cloud for scale/bounds
    allpts = []
    for e in elements:
        verts, _ = element_cloud(e)
        allpts.extend(verts.values())
    if not allpts:
        print('!! no vertices in', src)
        return None
    mn, mx = bounds(allpts)
    span = max(mx[0] - mn[0], mx[1] - mn[1], mx[2] - mn[2])
    scale = target_height / max(1.0, span)
    step = max(1.0, span / layers)
    cx = (mn[0] + mx[0]) * 0.5
    cz = (mn[2] + mx[2]) * 0.5
    cy_off = mx[1]  # feet (y-down: max y = bottom)

    vox = Voxelizer(step)
    for e in elements:
        verts, faces = element_cloud(e)
        vox.feed(verts, faces)
    filled = vox.flood_fill()
    if len(filled) > max_boxes:
        # coarser retry
        step = span / (layers * 0.7)
        vox = Voxelizer(step)
        for e in elements:
            verts, faces = element_cloud(e)
            vox.feed(verts, faces)
        filled = vox.flood_fill()

    # union fill across ALL elements closes the body where per-element shells
    # are open, so big Telltale meshes voxelise as a solid mass
    union = Voxelizer(step)
    for e in elements:
        verts, faces = element_cloud(e)
        union.feed(verts, faces)
    unionFilled = union.flood_fill()
    # every union cell that no element covered becomes a body box
    covered = set()
    for (i, j, k) in filled:
        covered.add((i, j, k))
    bodyExtra = [b for b in union.boxes(unionFilled, mn, cx, cy_off, cz, scale, atlas)
                 for b in [b] if (lambda bb: False)(b) or True]  # placeholder, built below
    bodyExtra = []
    for (i, j, k) in unionFilled:
        if (i, j, k) in covered:
            continue
        x = (mn[0] + (i + 0.5) * step - cx) * scale
        y = (mn[1] + (j + 0.5) * step - cy_off) * scale
        z = (mn[2] + (k + 0.5) * step - cz) * scale
        sz = step * scale
        bodyExtra.append({
            "coordinates": [round(x, 3), round(y, 3), round(z, 3),
                            round(sz, 3), round(sz, 3), round(sz, 3)],
            "textureOffset": [0, 0],
        })

    models = []
    seen = set()
    for path, origin, rot, els in groups:
        if not els:
            continue
        if not path:
            path = [els[0].get('name') or 'body']
        name = sanitize(path[-1])
        key = name
        n = 1
        while key in seen:
            key = '%s%d' % (name, n); n += 1
        seen.add(key)
        tr = origin or [0.0, 0.0, 0.0]
        translate = [round((tr[0] - cx) * scale, 3),
                     round((tr[1] - cy_off) * scale, 3),
                     round((tr[2] - cz) * scale, 3)]
        m = {
            "part": part,
            "id": key,
            "invertAxis": "xy",
            "translate": translate,
        }
        if rot and any(rot):
            m["rotate"] = [round(float(r), 3) for r in rot]
        boxes = []
        for e in els:
            verts, faces = element_cloud(e)
            sub = Voxelizer(step)
            sub.feed(verts, faces)
            subfill = sub.flood_fill()
            boxes.extend(sub.boxes(subfill, mn, cx, cy_off, cz, scale, atlas))
        if boxes:
            m["boxes"] = boxes
            models.append(m)
    if bodyExtra:
        models.append({
            "part": part,
            "id": "bodyFill",
            "invertAxis": "xy",
            "translate": [0, 0, 0],
            "boxes": bodyExtra,
        })

    with open(dst, 'w') as f:
        f.write(JEM_HEAD)
        for i, m in enumerate(models):
            f.write('\t\t' + json.dumps(m, separators=(',', ': '))
                    .replace('}, {', '},\n\t\t\t{').replace('"boxes": [{', '"boxes": [\n\t\t\t\t{')
                    .replace('}, {"coordinates"', '},\n\t\t\t\t{"coordinates"')
                    + (',' if i < len(models) - 1 else '') + '\n')
        f.write(JEM_TAIL)
    print('%-64s -> %-48s boxes=%d parts=%d scale=%.4f step=%.2f' %
          (os.path.basename(src), dst, sum(len(m.get('boxes', [])) for m in models),
           len(models), scale, step * scale))
    return dst

def sanitize(name):
    return ''.join(ch for ch in name if ch.isalnum() or ch in '_-') or 'part'

if __name__ == '__main__':
    root = sys.argv[1] if len(sys.argv) > 1 else \
        '/home/user/gggggrff/ALL wither storm assets from MCSM for blockbench you\'ll ever need V0.9 4.zip'
    outroot = sys.argv[2] if len(sys.argv) > 2 else \
        '/home/user/Lowuuuuuu/ogs-cem/assets/minecraft/optifine/cem/dabywitherstormmod'
    jobs = [
        # phase ladder: 1=p2.0-4.49, 2=p4.5-4.99, 3=p5, 4=p5.5, 5=p6, 6=p6.5,
        #               7=p7+, 8=torn, 9=dismantled (see wither_storm.properties)
        ('Stage_A/witherstormStageA.bbmodel', 'wither_storm1.jem', 22, 40, 'mass'),
        ('Stage_A/witherstormStageA_with_debris.bbmodel', 'wither_storm2.jem', 24, 42, 'mass'),
        ('Traced_shading_Textures/witherstormStageB (with traced shading textures).bbmodel',
         'wither_storm3.jem', 56, 46, 'mass'),
        ('Traced_shading_Textures/witherstormStageC_Small (with traced shading textures).bbmodel',
         'wither_storm4.jem', 66, 74, 'mass'),
        ('Traced_shading_Textures/witherstormStageC_Big (with traced shading textures).bbmodel',
         'wither_storm5.jem', 82, 74, 'mass'),
        ('Traced_shading_Textures/witherstormStageC_Massive (with traced shading textures).bbmodel',
         'wither_storm6.jem', 110, 76, 'mass'),
        ('Traced_shading_Textures/witherstormStageD_Center_Massive.bbmodel',
         'wither_storm7.jem', 96, 46, 'mass'),
        ('Wither_Storm_Deadass/witherstorm_Deadass.bbmodel', 'severed_wither_storm.jem',
         36, 36, 'head'),
        ('Wither_Storm_Deadass/witherstormStageD_Deadass_Left.bbmodel',
         'wither_storm_head.jem', 30, 30, 'head'),
    ]
    for rel, out, th, layers, part in jobs:
        convert(os.path.join(root, rel), os.path.join(outroot, out), th, layers, part)
