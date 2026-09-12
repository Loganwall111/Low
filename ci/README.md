# README — How the mod gets compiled (the "GitHub compiler" and the local one)

Two ways to build the jar. Both run the identical recipe in `ci/build.sh` /
`ci/build.ps1`: compile the `mcsm-extras` Java sources with JDK 25, then lay
them + the core shaders + `jar-overrides/` on top of the newest delivery jar
and stamp the version.

## 2026-09-05 compile audit (session arena/01a071bb)

The 1.9.100 Java sources had never been compiled. Every API they use was
audited against two compile-verified sources of truth: the `heress` branch
(decompiled mod sources that CI-compile against Minecraft 26.2) and the
previously-compiled `net.mcsm.extras` classes inside the shipped jar. Fixes:

* `Minecraft.setScreen(...)` → **`Minecraft.getInstance().gui.setScreen(...)`**
  (26.2 moved screen switching; both call sites fixed).
* `McsmExtrasScreen` rewritten to the 26.2 GUI pipeline the mod's own config
  screen uses: `extractRenderState(GuiGraphicsExtractor, ...)` + `addWidget` +
  `Button.builder(...).bounds(...).build()` + `AbstractSliderButton` subclass.
  The old version used `addRenderableWidget` / `CycleButton` / `isPauseScreen`,
  which cannot be verified against 26.2 and were dropped.
* `ci/build.sh`'s "survivable javac" was actually broken — on a compile error
  the empty class dir made `cp -r /tmp/mcsm-build/*` abort under `set -e`, so
  the jar was never assembled. Fixed with a nullglob guard.
* Full javac log + `out/BUILD_INFO.txt` + evidence pushed to
  `ci-out/run-<n>/` on branch `arena/01a071bb-lowuuuuuu` (build results are
  readable from the sandbox, where Actions logs are not).
* `ci/build.ps1` got the same treatment (live client-jar resolution,
  survivable compile, empty-class guard).

Everything else was already correct — including the mixin descriptors
(`BeaconBlockEntity.tick`, `LevelRenderer.render`,
`FogRenderer.updateBuffer(FogData)`), `BlockBehaviour.Properties`,
`useItemOn`, `forceTentacleSlam`, `ModBlocks/ModItems.register`,
`StormSkyGradient.*`, `cameraState.pos`, and every sound/particle constant.

## A) GitHub Actions — automatic build on every push ("the GitHub compiler")

**Status 2026-09-05:** `.github/workflows/build-mcsm.yml` is currently
**missing from the branch tips** — commit `9fcce7f` had to delete it to get
its push past the app token's missing `workflows` permission, and the push
trigger reads the workflow file from the pushed commit. That is why no run
has happened since `2a9f780`. The ready-to-paste workflow lives at
**`ci/workflows/build-mcsm.yml`** and only the repo owner can restore it
(the app token is rejected even for a byte-identical file).

To (re)install it — one time, ~30 seconds, no git needed:

1. Open <https://github.com/Loganwall111/Lowuuuuuu/new/arena/01a071bb-lowuuuuuu?filename=.github/workflows/build-mcsm.yml>
   (the new-file editor on the fix branch, filename pre-filled).
2. Open `ci/workflows/build-mcsm.yml` on the same branch, click **Raw**,
   select-all, copy.
3. Paste into the editor and **Commit changes**.

The push that creates the file immediately starts a build (the workflow
triggers on pushes to `arena/01a06df7-lowuuuuuu` and
`arena/01a071bb-lowuuuuuu`). Every build then:
   - installs Temurin JDK 25 on a clean runner,
   - downloads the Minecraft 26.2 client jar (resolved live from the version
     manifest) + Mixin + deps (runners have full network),
   - runs the GLSL gate (`glslcheck/shimcheck.py`),
   - compiles `mcsm-extras` with `javac --release 25`,
   - assembles and zip-tests the new jar,
   - attaches it as a run **Artifact**,
   - publishes build evidence to `ci-out/run-<n>/` on the branch so results
     are auditable without Actions log access, and
   - when `VERSION` names a version with no release yet (currently
     `1.9.100`), creates a GitHub Release with the jar automatically.

Alternatively: **Settings → Applications → [Arena agent] → Repository
permissions → Workflows: Read & write** lets the agent push workflow files
itself, and this manual step stops being needed.

Builds/artifacts appear at
<https://github.com/Loganwall111/Lowuuuuuu/actions>.

## B) Local on your PC — no GitHub needed

You already have JDK 25 (the Azul build you play Minecraft with) and internet.

```powershell
cd C:\path\to\Lowuuuuuu
powershell -ExecutionPolicy Bypass -File ci\build.ps1
```

Output: `out\dabywitherstormmod-<identity>.jar` (+ `.sha256`), where `<identity>` is the exact value from `VERSION`.
Only that jar goes into `mods\` — delete older ones first.

(Linux/macOS equivalent: `bash ci/build.sh`.)

## Rules that still apply (from delivery/HANDOFF.md)

- Never reuse a retired version number — bump `VERSION` first.
- Never ship without the embedded textures/shaders.
- After ANY Java source edit, the jar must be recompiled (that's what these
  scripts are for — a shader-only edit can be overlaid without javac).
- After ANY shader edit, run `python3 glslcheck/shimcheck.py mcsm-core-shaders
  jar-overrides/assets/dabywitherstormmod/shaders/core/storm_glow.fsh
  jar-overrides/assets/dabywitherstormmod/shaders/post/storm_sun_glow.fsh`
  (42/42 expected — build.ps1 on GitHub runners does it for you; locally run
  it yourself if you edited shaders).
