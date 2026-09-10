# Skybox audit for 1.9.199

Audit command run in the workspace:

```bash
find . -type d \( -name '*skybox*' -o -name '*fabricskyboxes*' -o -name 'sky' \) | sort
```

Result: no FabricSkyBoxes/OptiFine skybox override folders are present in the shipped packs. The only sky-related folders are the intended Devouring Storms atmosphere texture sources:

- `src/main/resources/assets/dabywitherstormmod/textures/mcsm_atmosphere/sky`
- `src/main/resources/assets/dabywitherstormmod/textures/sky`
- `jar-overrides/assets/dabywitherstormmod/textures/mcsm_atmosphere/sky`
- `jar-overrides/assets/dabywitherstormmod/textures/sky`

So the thin purple phase look is not coming from a hidden Fabric skybox folder in this checkout. 1.9.199 refreshes these intended folders with the phase/day/night/sunset gradient assets and keeps purple/pink limited to storm phases/distance influence in code.
