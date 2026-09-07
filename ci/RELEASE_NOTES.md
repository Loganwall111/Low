# Devouring Storms 1.9.144 — mega-phase 8: the phase sky, and the halo without heads

## The purple sky is a PHASE, not a night
The big glowing purple vault was being painted by the shader pack's night
and aurora path, so it showed up on ordinary nights and never lined up
with the creature. It has been taken off the clock entirely:

- **night and midnight go back to a true deep blue** in both sky paths
  (the embedded Iris pack and the core-shader fallback), and the aurora
  ribbons are off by default — nothing paints the night purple any more;
- the purple vault and its fog are now drawn by the mod itself, on a sky
  dome it only paints while the storm sits in **phase 5.4 through 5.9**;
- **past 5.9 the purple leaves and the vault turns light pink**, exactly
  where the reference frames end;
- the vault is lit *from* the storm's bearing rather than washed flat, and
  the phase fog is thickest at the horizon so the zenith stays readable;
- the storm sky gate now needs a strong purple fog, so phases 4–5.3 no
  longer swap the sky out from under the player.

## Daytime and noon
The day gradient was carrying a warm beige horizon. It now runs the
contrasted bluish ramp from the reference sheet: deep blue zenith
(0.106, 0.286, 0.694) → azure mid → pale blue-white horizon.

## The halo, rebuilt with no heads
The old halo hung far off the storm and carried three heads and a symbol
behind the creature. **That disc is deleted.** In its place:

- a **pure colour palette halo** — three concentric soft-gradient discs,
  no face anywhere — drawn in sky space at the storm's own bearing, so it
  rides with the sky *and* with the storm, faces the camera (it still
  reads when you walk around behind it), and stays small enough that the
  ordinary sky shows around its rim;
- palette per phase, sampled off the frames: desaturated teal-grey at 5.2,
  lavender/periwinkle at 5.4, deep purple-magenta 5.5–5.9, rose mauve 6+.

## The silhouette stack, 5.5 → 5.9
Layered outward from the body, all sky-glued:
1. the mod's **dark-blue over-storm light**, replicated;
2. a **dark purple layer** laid on top of it and wrapped round the back;
3. the second pair — **purple over, dark moon-blue** beneath;
4. a **black glow capping the storm** that takes the top of the
   *atmosphere* out of the picture completely (alpha-blended, not
   additive, so it truly erases rather than brightens).

On the body itself the removed face overlay is replaced by a plain
palette fringe: dark blue, then dark purple over it.

## Sky City altitude (1.9.143's fix, now actually shipping)
1.9.143 failed to compile — the shipped Mixin jar predates
`@ModifyReturnValue`. The altitude raise now hooks `enqueue` instead:
floating sites are the only jobs in the 200–1000 band, so they are
re-enqueued 3,904 blocks higher and the original call is cancelled.
Sky City lands at y≈4,200, above the 96/146/152/420/430/1200 decks — a
jump off the edge falls through the story cloud layers on the way down.
Structures still place whole, and the static queue is still cleared on
every world change.
