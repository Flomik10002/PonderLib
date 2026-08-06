# PonderLib

An in-game documentation/tutorial system for NeoForge 1.21.1 mods, packaged as a standalone public
API with no dependency on Create, Flywheel or Catnip.

Any mod can register scenes for its own blocks and items: static geometry comes from a vanilla
structure `.nbt` schematic, behaviour from a storyboard written in code.

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
