# Native sky renderer audit

The legacy texture-backed sky route is no longer part of the source or
assembly inputs. The active implementation is the Java `SkyRenderer` state
hook in `McsmStormSkyColorPatch` plus `McsmNativeSkyRenderer`; it supplies
interpolated RGB endpoints and matching fog state to Minecraft's native
spherical sky pass.

The build scripts also remove legacy sky directories from the pinned base jar
before assembly and fail the jar audit if any of those paths survive. The
retained `mcsm_atmosphere/glare` textures are unrelated to sky colour and are
kept for the existing glare system.
