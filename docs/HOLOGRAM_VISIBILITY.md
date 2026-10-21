# Hologram Visibility

Default border/fill alpha is 200/70. Both remain client-configurable. Existing saved config files retain their current values; set them manually or reset those entries to use the new defaults.

## Behavior

- All outlines keep their full configured strength. The previous exposed-block fading prototype and its CPU ray checks have been removed.
- Holograms remain visible through ordinary terrain. Exact fill still avoids directly visible ore faces.
- Directly visible, opaque full-block targets mask hologram fragments farther behind their visible surfaces. Their own coplanar front outlines remain visible.
- Masking is per screen pixel, not per block or vein. Parts of a buried block visible around an exposed block remain projected at full strength.
- The mask applies to fill, outlines and guide beams, across placed and train-mounted projectors. Beams remain visible through ordinary rock rather than switching the whole vein to normal depth testing.
- Buried ore cannot write into the mask: mask faces are tested against a copy of the rendered scene depth. Covering exposed ore immediately restores through-terrain projections at those pixels.
- Only opaque full blocks contribute to the mask. Chests, crystals, spawners, fluids and other non-solid/cutout shapes must not produce false solid-cube occluders.
- Mask geometry uses actual block borders, independent of the configurable hologram inset.
- The shared private framebuffer is reused and resized with the window, then released on leaving the world. It does not modify scene color, depth, stencil or alpha. Existing water/pause-menu compositing stays in place.
- No new config options, saved-data formats or translations are introduced. Ponder retains its illustrative renderer.

## Manual Checks

Perform on Forge 1.20.1 and NeoForge 1.21.1. Builds and test-source compilation are checked automatically; no game clients or GameTests are launched automatically. The custom GPU shader compiles when Minecraft loads/reloads resources, so an in-game check is required.

1. With alpha 200/70, locate a completely buried lead/copper vein. Strong outlines, underground fill and through-terrain guide beams should remain readable.
2. Uncover one front block while leaving ore behind and beside it. Its front outline should stay strong. Hologram fill, rear outlines and beam portions behind its visible face should disappear only within that face's screen footprint.
3. Move sideways: a partially covered buried block should reappear around the exposed block, rather than disappearing as a whole block or vein. A tiny visible corner should work without waiting for CPU sampling.
4. Put ordinary stone in front of the exposed ore. The through-terrain hologram should immediately return. Remove that stone and check that exposed-ore masking returns.
5. Fully expose the vein. Check that visible ore textures remain unobscured and rear outlines no longer show through front ore faces. Mine blocks one by one; remaining geometry and mask coverage must update.
6. Overlap multiple projectors/deposits in the view. Visible projected opaque ore should mask projection fragments behind it, including fragments from another projector. Nearest uncovered surfaces should win visually.
7. Check a moving-train projector: targets/masks stay fixed in world space while beam roots follow the train. Check multiple facings and assemble/disassemble.
8. Check chests, spawners, amethyst and water/lava. They must not create opaque cube-shaped holes in unrelated projections. Test zero outlines, disabled fill and disabled guides.
9. Check Fast/Fancy (and Fabulous where available), water compositing and the pause menu, particularly NeoForge Fast. No black holograms, invisible water or scene-alpha changes should appear.
10. Resize the window, toggle fullscreen and press F3+T. Check shader reload, mask alignment and resource recreation. Leave/rejoin a world and verify no old mask leaks into the next world.
11. Check large veins and multiple projectors for frame pacing. There are no CPU visibility rays; the new work is a shared depth copy and a GPU ore-mask pass.
