# MCSM Visuals r1 — Instructions

Everything you need to install, verify, build, and publish the
**MCSM Visuals r1** release (`v1.9.61-26.2`).

---

## 1. Requirements

| Item | Version |
| :--- | :--- |
| Minecraft | **26.2** (Java Edition) |
| Mod loader | **Fabric Loader 0.19.3+** |
| Mods | **Fabric API** (any build for 26.2), **Mod Menu** (recommended, for config) |
| Shader loader | **Iris** or **OptiFine** (only needed for the shader pack) |

---

## 2. Installation

### 2.1 The three files

| File | Where it goes |
| :--- | :--- |
| `dabywitherstormmod-1.9.61-26.2-beta-r1.jar` | `.minecraft/mods/` |
| `MCSM_ResourcePack.zip` | `.minecraft/resourcepacks/` |
| `MCSM_ShaderPack.zip` | `.minecraft/shaderpacks/` |

`.minecraft` is `%APPDATA%/.minecraft` on Windows, `~/Library/Application Support/minecraft` on macOS, `~/.minecraft` on Linux.

**Do not** unzip any of them. Drop the files in as-is.

### 2.2 In-game

1. Launch Minecraft 26.2 with the Fabric loader.
2. **Options → Resource Packs** → enable **MCSM Resource Pack** (move it above any other packs).
3. **Options → Video Settings → Shader Packs** (Iris/OptiFine) → select **MCSM_ShaderPack**.
4. **Options → Mods → Wither Storm Mod → Config** (Mod Menu) → confirm:
   - `stormAtmosphere` = ON (full-screen storm post pass)
   - `stormBlobFX` = ON (phase-5 blob, halo, rear fog, flash, vortex)
   - `atmospherePulse` = ON (breathing glare)
   - `haloRings` = ON (crisp cubic halo-ring timeline, phases 4–9)
   - `vortexDebris` = ON (block-pellet abduction vortex from phase 4)
   - `shieldDome` = OFF (no dome over the sky — the MCSM look)
   - preset **MCSM** (default) — carries the Story-Mode beam/eye/teeth colours.

### 2.3 Recommended settings

- Video Settings → Quality → **Custom Sky: ON**, **Sky / Sun & Moon: ON**
- Shader Pack → Shader Options → **Cloud Rendering: fast** (procedural GLSL),
  **Custom Skies: ON**
- Story Mode colored lighting, the turquoise teeth glow and the matte
  charcoal skin grade are compiled into the pack — always ON, no toggles.

---

## 3. What you should see (phase-by-phase verification)

| Phase | Expected look |
| :--- | :--- |
| Any | Lavender zenith → orange horizon skybox; clock keeps running |
| 4.0+ | Thin white **cubic under-halo**; block pellets rip off the ground and spiral up the **abduction vortex**; teeth burn **cyan-white** |
| 4.5 | Sky/fog turns **green** |
| 5.0 | Sky/fog turns **turquoise** |
| 5.1–5.9 | Giant colour-shifting centre blob (dark purple → magenta → pink/blue/black), heavy magenta/purple/black fog layer on the storm's **back**, moving with it |
| 6.0+ | Storm grows into the bigger devourer; thin cube rings spiral **clockwise** (diagonal + vertical); backdrop swaps to the **ORANGE** layout; bright **flash above the storm every 2 minutes** |
| 7.0+ | **More rings**, wide outer ones counter-rotating; **maximized purple flares** pulsing over the storm + heads |
| 8.0–9 | Cosmetic halos **die out**; three gigantic **counter-rotating** ring systems (ten nested rings each) engulf the sky; sky/fog burn **red/dark-orange inferno** |
| Day/night | Cloud colour shifts (white/coral → periwinkle); **sun-cast shadows sweep the ground and water** |

The storm body is **100% shader/GLSL** — no 3D shells, no PNG cloud sheets.
The cloud core + dark backdrop also ship **inside the mod JAR**
(`assets/dabywitherstormmod/shaders/core/rendertype_clouds.{vsh,fsh}`,
`textures/environment/sky/`), so the mod renders the Story Mode sky standalone
even without the shader pack enabled.

---

## 4. Building from source

Requires **JDK 25** and **Gradle 9.5.1** (the wrapper is included; CI uses the same versions).

```bash
gradle build --no-daemon --stacktrace
```

Output: `build/libs/dabywitherstormmod-1.9.61-26.2-beta.jar`

Rebuild the two packs from the committed directories (flat zips, PNG-cloud + skybox-set checks):

```bash
python3 tools/build_mcsm_packs.py
# writes MCSM_ResourcePack.zip and MCSM_ShaderPack.zip at the repo root
```

Validate everything:

```bash
python3 tools/validate_release_artifacts.py \
  --rp MCSM_ResourcePack.zip --sp MCSM_ShaderPack.zip \
  --jar build/libs/dabywitherstormmod-1.9.61-26.2-beta.jar \
  --expect-version 1.9.61-26.2-beta
```

> The repo-staged fallback jar in `docs/releases/r1/` is produced by
> `tools/merge_release_jar.py` (original classes + merged resources, no
> `geo/` Blockbench sources, no `ffmpeg`). The **CI-built jar always
> supersedes it** — that's the one to distribute.

---

## 5. Publishing a release

Release-asset uploads are blocked from the dev sandbox (`uploads.github.com`
unreachable), so publishing happens on GitHub's runner via the workflow.
Current state: the release **`v1.9.61-26.2-mcsm-r1` already exists** (created,
zero assets) and **PR #18 is open and mergeable**.

### 5.1 Exact steps (click-by-click)

1. **Merge PR #18** (`arena/01a04a20-lowuuuuuu` → `main`)
   - GitHub → **Pull requests → #18** → **Merge pull request** → **Confirm merge**.
   - Wait for the merge commit. (Do NOT skip this — the workflow builds from
     `main`, so main must contain the r1 code first. Merging also runs the
     normal `Build` check on main automatically.)
2. **Open the Actions tab**
   - GitHub → **Actions** → left sidebar → **MCSM Integrated Release Build**.
3. **Run the workflow**
   - Click the **Run workflow** button (top right of the workflow list).
   - In the popup, in the **release_tag** field, type exactly:
     `v1.9.61-26.2-mcsm-r1`
   - Click the green **Run workflow** button.
4. **Wait** (~3–5 minutes). The run:
   - compiles the mod jar from `main` (Java 25 + Fabric Loom), renamed
     `dabywitherstormmod-1.9.61-26.2-beta-r<run-number>.jar`,
   - rebuilds both packs via `tools/build_mcsm_packs.py`,
   - validates all artifacts (procedural-only clouds, skyboxes, version),
   - force-uploads every `dist/*` file to the release tag you typed,
   - rewrites the release notes + SHA-256 digests.
5. **Verify the publish**
   - GitHub → **Releases** → **v1.9.61-26.2-mcsm-r1** should now list assets:
     - `dabywitherstormmod-1.9.61-26.2-beta-r<run>.jar`
     - `MCSM_ResourcePack.zip`
     - `MCSM_ShaderPack.zip`
     - `MCSM_ResourcePack_and_Mod.zip` (convenience bundle)
     - `MCSM_ShaderPack_and_Mod.zip` (convenience bundle)
   - The release page shows the regenerated notes and the SHA-256 digests.
   - Optionally click the run in Actions → **Published assets summary** step
     to see the exact uploaded `{name, size, digest}` list.

### 5.2 If something fails

| Failure | Cause / fix |
| :--- | :--- |
| “No workflow found” or the workflow isn't listed | You must be on the **default branch view** of Actions, and the workflow only appears after it has run once or when a dispatch is possible. After merging PR #18, refresh the page. |
| Validation step fails | Open the failed step log: it lists the exact problem (e.g. a missing skybox, a `cloudTex` binding). Fix on the branch, push, and re-merge before retrying. |
| “release not found” | The tag must match exactly: `v1.9.61-26.2-mcsm-r1` (it already exists, so this shouldn't happen). |
| Asset upload partially fails mid-run | Rerun the workflow with the same `release_tag` — the workflow deletes stale assets and force-uploads (`--clobber`), so a re-run is safe. |

Until the workflow finishes, the artifacts stay downloadable from the repo at
`docs/releases/r1/` (open each file → **Download raw file**), with checksums
in `docs/releases/r1/SHA256SUMS.txt`.

---

## 6. Troubleshooting

| Symptom | Fix |
| :--- | :--- |
| Storm atmosphere post pass never appears | It self-disables when a shader pack is active (the pack provides its own sky/fog). Turn the shader pack off to see the mod's post pass, or keep it on and rely on the pack's look. Check the log for `[dabywitherstormmod] storm atmosphere: ...`. |
| Clouds look flat / no pattern | Shader pack not enabled, or **Story Mode Clouds** off in Shader Options. |
| Shadows missing on water | Re-select the shader pack (program list refresh), enable **Dynamic Skybox**. |
| Mod not loading | Confirm Fabric API is installed, loader ≥ 0.19.3, Minecraft 26.2. |
| Old-looking storm | A stale jar may be cached — rename the jar (CI builds are already renamed per run) or clear `.minecraft/mods` of older `dabywitherstormmod-1.9.60*` jars. |
| Teeth look dim purple | Teeth Colour R/G/B drifted — reset to ≈ 0.45/1.0/1.0 (MCSM preset), and keep **Wither Storm Teeth Glow** compiled in (it has no toggle). |
| PNG cloud sheets reappear after a rebuild | `tools/build_mcsm_packs.py` hard-fails if any `cloudTex` binding or cloud PNG sneaks back — keep the cloud programs procedural. |
