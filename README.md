# PonderLib

An in-game documentation/tutorial system for Forge 1.20.1 mods, distributed as a standalone
public API with no external mod dependencies.

Any mod can register scenes for its own blocks and items: static geometry comes from a vanilla
structure `.nbt` schematic, behaviour from a storyboard written in code.

Docs, API walkthrough, and setup: **[the wiki](https://github.com/Flomik10002/PonderLib/wiki)**.

## Input hints

Storyboards can demonstrate the player's configured drop-item key without hardcoding its default:

```java
scene.overlay().showControls(target, Pointing.DOWN, 40).drop();
```

The window renders `Drop [Q]` by default and follows the player's current key binding.

## Scripted entity movement

Entities can follow a deterministic eased path without a per-tick `modifyEntity` loop:

```java
scene.world().moveEntity(
    diamond,
    poolInside,
    24,
    Easing.QUAD_IN,
    CollisionMode.IGNORE
);
scene.idle(24);
```

`CollisionMode.IGNORE` drives the position directly, allowing an item to enter a container even
when ordinary entity collision would stop a throw. `CollisionMode.RESPECT` routes every step
through vanilla collision instead and therefore may stop short of the requested target. Movement
is non-blocking, like section animation; its duration is included in the scene timeline, while the
single `idle` above is only needed when following storyboard actions must wait for arrival.

## Navigation tags

```java
public void registerTags(PonderTagRegistrationHelper helper) {
    helper.registerTag("functional_flowers")
        .title("Functional Flowers")
        .description("Flowers that interact with the world using mana")
        .icon(ModBlocks.BELLOTHORN)
        .addToIndex()
        .register();
    helper.addToTag(helper.asLocation("functional_flowers"), ModBlocks.BELLOTHORN, ModBlocks.HOPPERHOCK);
}
```

The category appears beside each member's scene while every component keeps its own storyboards.

## Chapters, sound, custom elements, diagnostics

```java
scene.addKeyframe("Generating mana");
scene.effects().playSound(ModSounds.MANA_BURST, .8f, 1f);
ElementLink<MyElement> link = scene.addElement(MyElement.class, MyElement::new);
scene.modifyElement(link, element -> element.setAmount(10));
scene.removeElement(link);
```

Sounds are suppressed while seeking. Custom element factories are replay-safe.
`PonderDoctor.validateAll()` checks resources, tag metadata, keyframes, compilation, completion and
declared versus measured playback time.

## Licensing

Copyright (c) 2026 Flomik

PonderLib is free software: you can redistribute it and/or modify it under the terms of the **GNU
Lesser General Public License** as published by the Free Software Foundation, **either version 3 of
the License, or (at your option) any later version**.

- The LGPL text is in [`COPYING.LESSER`](COPYING.LESSER).
- LGPL-3.0 incorporates the terms of GPL-3.0 by reference, so its text is included in
  [`COPYING`](COPYING) as the license requires.

PonderLib is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the license for
details.

**What this means for mods that use PonderLib:** nothing. Depending on this library and calling its
API places no obligations on your own mod — your mod's license stays whatever you want it to be,
including all-rights-reserved. That freedom is the entire reason this is LGPL rather than GPL.

**What it means for forks:** if you distribute a modified PonderLib, those modifications have to stay
under LGPL-3.0 and their source has to be available.

### Third-party code

PonderLib is a derivative work of `net.createmod.ponder` and `net.createmod.catnip` (MIT). Those
portions remain under MIT with their original copyright notice, which is reproduced in
[`THIRD_PARTY_LICENSES.md`](THIRD_PARTY_LICENSES.md) as that license requires. A file-by-file
breakdown of what was ported, what was adapted, and what is PonderLib's own is in
[`ATTRIBUTION.md`](ATTRIBUTION.md).

No Create assets are used — this project ships no art of any kind.
