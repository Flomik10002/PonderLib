# Attribution

PonderLib is licensed under LGPL-3.0-or-later (Copyright (c) 2026 Flomik). It is a derivative work of
`net.createmod.ponder` and `net.createmod.catnip` (MIT), whose portions keep their own license; the
required copyright and permission notice for those is in `THIRD_PARTY_LICENSES.md`.

This file exists to credit what was taken and from where — nothing else. Anything PonderLib wrote
itself is not attribution and is deliberately absent.

## Ported closely

Behaviour, formulas and/or structure follow upstream; the Java is rewritten against PonderLib's own
types (no Catnip dependency), but these are ports, not independent designs.

| PonderLib | Upstream |
|---|---|
| `foundation.PonderScene`, `PonderSceneBuilder`, `foundation.instruction.*` | `PonderScene`/`PonderSceneBuilder` — blocking/non-blocking instruction scheduler, storyboard/builder split |
| `PonderScene.seekToTime`/`getTotalTime`/`getSceneProgress`/`addToSceneTime`/`stopCounting`/`markKeyframe`/`getKeyframeTime` | same methods; `totalTime` accumulated via `onScheduled` by blocking instructions only |
| `foundation.instruction.KeyframeInstruction` | same class, `IMMEDIATE`/`DELAYED` with offsets 0/6 |
| `foundation.instruction.FadeInOutInstruction` | same class — `fadeTime = 5`, `fade * fade` ramps, `duration + 2 * fadeTime` |
| `foundation.instruction.RevealSectionInstruction` | `FadeIntoSceneInstruction` — light 5→15, half-block directional slide, `1 - remaining²` ease-out |
| `foundation.element.TextWindowElement` | `TextWindowElement` — scene-position anchoring, fade-scaled leader line, `width * lerp(yDiff², 6/8, 5/8)` placement, `min(targetX, anchor.x + 50)` clamp, `min(width - targetX, 180)` wrap, palette mixed halfway to `0xffffdd`, alpha floor of 5 |
| `api.scene.TextElementBuilder` | `TextElementBuilder` (minus its `sharedText` overloads) |
| `api.PonderPalette` | `PonderPalette` — value for value |
| `api.scene.SceneBuildingUtil`/`SelectionUtil`/`VectorUtil`/`PositionUtil` | same three-group split; `layer`/`layersFrom`/`layers`/`column`/`fromTo`/`cuboid`/`everywhere` all built from one `cuboid` primitive exactly as upstream derives them, including `column`'s literal (not "corrected") height formula; `centerOf`/`topOf`/`blockSurface`/`of`; `at`/`zero`. Not ported: `Selection.makeOutline`/`Predicate<BlockPos>` (no `Outliner` slot registry exists here to dispatch to - see `render.SceneOutline`'s entry). `SelectionUtil.positions(BlockPos...)` is PonderLib's own addition (upstream has no single-call way to select an arbitrary, non-cuboid set of positions) |
| `api.scene.Selection.add`/`subtract`/`copy` | `Selection.add`/`substract`/`copy` — deviation: spelled correctly here, not upstream's longstanding typo; implemented as pure functions returning a new `Selection` rather than upstream's in-place mutation (every real usage found composes them as a single chained expression, never relies on mutating a stored reference) |
| `foundation.registration.SchematicLoader.LoadedSchematic` | carries a schematic's real `size` alongside its blocks - what `SelectionUtil`'s scene-bounds-aware helpers are built on |
| `api.Pointing` | Catnip `Pointing` — the four directions, minus its block-face rotation helpers |
| `render.SceneOutline`, `foundation.element.OutlineElement`, `instruction.OutlineInstruction` | `OutlinerElement` + Catnip's `AABBOutline` — box outlines drawn as edge boxes. Not ported: the `Outliner` slot registry (needed upstream because Create outlines from arbitrary game code), and the CLUSTER silhouette a multi-position selection gets there — this draws one box per position |
| `api.scene.OverlayInstructions.showOutline`/`showOutlineWithText` | same methods; ours takes the text as a parameter because upstream's `TextElementBuilder` owns text registration and ours does not |
| `foundation.ui.PonderUI.renderNextUp`/`tickNextUp` | `renderNextUp` — 50-tick warmup, box above the right arrow rising 5px as it eases in, right arrow flashing once the scene is finished. Not ported: `isNextUpEnabled()` (no API to set it here) |
| `foundation.element.InputWindowElement`, `api.scene.InputElementBuilder`, `render.PonderSpeechBox` | `InputWindowElement`/`InputElementBuilder` and Catnip's speech box — anchored input hints with a directional tail. Icons are NOT ported (Create's `ICON_LMB`/`ICON_RMB`/`ICON_SCROLL` are All-Rights-Reserved assets); `showing(ScreenElement)` is absent for the same reason |
| `render.PonderBoxElement` | Catnip `BoxElement` — inflated background, four outer bars, gradient inner border |
| `foundation.ui.PonderUI.sceneToScreen` | `PonderScene.SceneTransform#sceneToScreen` |
| `foundation.ui.PonderUI.renderTimeline`/`renderKeyframeMarks`/`drawKeyframeMark`/`hoveredKeyframeIndex` | `PonderProgressBar` — 220px centred placement, `BoxElement` frame, `(-2,-2)` draw frame, `BAR_COLORS` two-band fill, `chase(target, .5f, EXP)` easing, marks hanging below the bar at 4/8px, hovered mark's second bar and `<`/`>` glyph, hitbox reaching 20px below, and its own `width + 2` vs `width + 4` denominator mismatch |
| `foundation.ui.PonderUI.tickFinishingFlash`/`renderBasePlateShadowAndFlash`/`forEachPerimeterSide` | `PonderUI.renderScene`'s "kool shadow fx" — perimeter loop, second `scaling(1,-1,1)` flip, shadow rect at local y `0..4`, glow at `-1..0` scaled by `0.5 + flash*0.75`, `∓1/1024` split, and the `raw*0.9` square/remap/square/invert curve |
| `foundation.ui.PonderUI.identifyMode`/`keyPressed` | real identify mode — `options.keyDrop` shortcut, `if (!identifyMode) activeScene.tick()` pause |
| `foundation.ui.PonderButton` | `PonderButton` (over Catnip's `BoxWidget`) — hover fade over 5 ticks, `flash()`/`dim()` toggle state, callback, and the shortcut key name drawn under the icon. Icons are NOT ported: upstream's come from Create's `widgets` texture atlas, whose `assets/` are All Rights Reserved |
| `foundation.ui.PonderUI.init`'s button row | upstream's own layout — `bY = height - 20 - 31`, spacing 8, steps of `20 + spacing` and `50 + spacing`, slow mode pinned at `width - 20 - 31` |
| `foundation.ui.PonderUI.scroll`/`mouseScrolled`, the `List<PonderScene>` + `index` model | `PonderUI.scroll(boolean)` — clamp, `begin()` the scene moved to, clear identify mode; paging on the scroll wheel as well as the buttons |
| Slow mode (`Config.COMFY_READING`, `extendedTickLength`/`extendedTickTimer`) | `isComfyReadingEnabled()` + upstream's tick gate: scene ticks every 3rd tick, and only while a text window is visible |
| `foundation.PonderTooltipHandler` | `PonderTooltipHandler` — tick/tooltip update model, `min(1, v + max(.25,v)*.25)` / `max(0, v-.05)` hold curve, `*8/7` display-only remap, subject-item green hint |
| `foundation.PonderClientLevelWrapper`, `PonderSceneCamera`, `PonderParticleProviders`, `PonderSceneParticles` | `catnip.levelWrappers.WrappedClientLevel`, `PonderScene.SceneCamera`, `PonderLevel.makeParticle`, `PonderWorldParticles.renderParticles` |
| `foundation.PonderLevel` | `catnip.levelWrappers.SchematicLevel` — delegate generic queries to a borrowed real level, implement only what scenes need; flat `1f` shade; `createBackup`/`restore` |
| `foundation.PonderLevel.addFreshEntity`/`tickEntities`/`renderEntities`/`getBrightness` | `PonderLevel.addFreshEntity` (via Catnip `SchematicLevel`, including its item-frame/armor-stand component sanitizing), `PonderLevel.tick()`'s entity loop (field names, `y <= -0.5` discard, remove-if-not-alive), `renderEntities` through the real `EntityRenderDispatcher` — light comes from the same `getPackedLightCoords` call upstream makes, resolving through `getBrightness` (flat full-bright, matching upstream's own override minus its unused `pushFakeLight`/`overrideLight` toggle) rather than a hardcoded value at the render call site. Deviation: `clearEntities()` (a plain wipe) stands in for `restore()`'s snapshot/reload round trip — PonderLib's schematics carry no entities of their own, so a scene's entity list is always empty at the equivalent snapshot point in practice, making the two operations equivalent here |
| `foundation.PonderLevel`'s component sanitizing (`withUnsafeComponentsDiscarded`/`isUnsafeItemComponent`) | Catnip `ComponentProcessors` — same allowlist (`ENCHANTMENTS`/`POTION_CONTENTS`/`DAMAGE`/`CUSTOM_NAME`), same empty-patch short circuit; own small copy instead of a Catnip dependency |
| `api.element.EntityElement`, `foundation.element.EntityElementImpl` | `TrackedElement<Entity>`/`EntityElementImpl`/`TrackedElementBase` — a weak reference to the entity, `ifPresent(Consumer)`, no render/tick role of its own |
| `api.scene.WorldInstructions.createEntity`/`createItemEntity`/`modifyEntity`/`modifyEntities`/`modifyEntitiesInside`, `PonderScene.forEachWorldEntity` | same methods and signatures |
| `render.SceneRenderBuffer`, `render.VirtualBlockView` | `SuperByteBuffer` — bake once, redraw by transform |
| `api.registration.*`, `foundation.PonderIndex` | static explicit plugin registry, `PonderPlugin` |
| `foundation.registration.PonderLocalization` | per-scene lang-key scheme `<modid>.ponder.<sceneId>.header` / `.text_N` |
| `api.scene.SceneBuilder.configureBasePlate`/`showBasePlate` | same names and semantics |

Specific constants taken as-is: camera transform (`translate(w/2, h/2-20, 200)`, `rotate(-35°, 55°)`,
`scaling(1,-1,1)`, `scale(30)`) and `DIFFUSE_LIGHT_0`/`DIFFUSE_LIGHT_1` in `PonderUI`;
`BAR_COLORS` (`0x80aaaadd`/`0x50aaaadd`); `COLOR_IDLE`/`COLOR_HOVER` and the `0x70`/`0xe0` keyframe
alphas; `TextWindowElement`'s border colours; `FLASH_WARMUP_TICKS` (30); `DefaultVertexFormat.BLOCK`
byte offsets in `SceneRenderBuffer`.

## Concept only

Researched upstream first, then built PonderLib's own simplified version — the navigation shape or
idea is theirs, the implementation is not.

- `api.registration.StoryBoardEntry.getTags`/`getOrderBefore`/`getOrderAfter` and
  `PonderSceneRegistry`'s ordering — flat `Set<ResourceLocation>` instead of the hierarchical tag
  builder, own topological sort (Kahn's).
- `PonderTagIndexScreen`/`PonderIndexScreen`'s tag filter — index→tag→scene navigation graph and
  namespace grouping only; the whole visual layer (Catnip `BoxWidget`/`PonderButton` grid, 28×28 icon
  layout, pagination, screen transitions) is not ported.
- `api.scene.EffectInstructions` — the `effects()` slot and its purpose; `emitSparks(Vec3, int, int)`
  is PonderLib's own simpler signature, not upstream's `ParticleOptions`-taking `emitParticles`.
