# Contributing to PonderLib

This isn't a contribution process (there's no PR template, no CLA) — it's the set of conventions
this codebase actually follows, written down so a change fits in instead of standing out. If you're
about to add a comment, a javadoc, a test, or a new public method, read this first.

## Comments explain WHY, never WHAT

A well-named method already says what it does. A comment that repeats that in prose is dead weight
that will drift out of sync with the code the first time someone edits one without the other.
Write a comment only when there's a non-obvious reason behind the code — a gotcha, an invariant, a
deliberate tradeoff, a bug that would reappear if "cleaned up" by someone who doesn't know why it's
there. If removing the comment wouldn't confuse a future reader, don't write it.

Bad (restates the code):
```java
// Set visible to true
element.setVisible(true);
```

Good (the code is obvious; the *constraint* isn't):
```java
// Snapshotted BEFORE the storyboard runs - a later instruction mutating this position must not
// retroactively change what this element's own capture() already baked.
element.setVisible(true);
```

## No narrative, no history, no upstream comparisons

Comments describe the code as it stands *today*, not how it got there:

- No session narrative ("found the hard way", "after some back and forth", "the user pointed out").
- No changelog-in-comment-form ("used to do X", "previously this was Y") — that belongs in the
  commit message, which `git log`/`git blame` already preserve. A comment describing removed code
  is a comment about nothing.
- No comparisons to what this project is derived from ("ported from upstream's X", "Catnip does
  this differently", "deviation from real Ponder's Y"). PonderLib is licensed as a derivative work
  (see [ATTRIBUTION.md](ATTRIBUTION.md)), and that attribution lives there — in the license
  metadata — not scattered through source comments implying this is a patch against something else.
  If a design choice differs from how some *other* project might do it, justify it on its own
  terms: what this codebase needs, not what another one happened to pick.

The test for both: could this sentence be true regardless of this project's history? If not, cut it.

## API ergonomics

The `api/**` package is what a consuming mod actually touches — everything else is free to be
awkward internally as long as the public surface isn't. A method's name and parameter types should
make its contract obvious without opening the javadoc; when they can't (a non-obvious unit, a
sentinel value, an order-dependency between calls), that's exactly what `@param`/`@return` are for
— not as decoration on every method regardless of whether the signature already says it.

Prefer a concrete decision over a hypothetical abstraction. Three call sites with the same shape
don't need a shared interface yet; a config flag for a feature nobody's asked for yet doesn't need
to exist. This codebase would rather have a slightly-too-simple API today than a wrong guess at a
"flexible" one that has to support usage that hasn't happened.

## Tests are documentation with a pulse

A comment claiming "X always holds" can be wrong the moment the code around it changes and nobody
notices. A test asserting it fails loudly instead. Where a private implementation detail is worth
verifying directly rather than only through its public effect, widen its visibility to
package-private with a one-line comment saying why (see e.g.
`PonderSceneBuilder#basePlateSelection`) — that's a deliberate, narrow exception to normal
encapsulation, not a sign the design leaked.

Every change gets verified against the *actual* test run, not against what the change was intended
to do:

- `./gradlew build` for the unit suite — read the real pass/fail counts out of
  `build/test-results/test/*.xml`, never just the filtered console log.
- `./gradlew runGameTestServer` for the GameTest suite, checking the exact "All N required tests
  passed" line.
- `./gradlew runData` after touching anything datagen-related, checking that stale generated files
  actually get removed (`HashCache: ... removed stale: N`).

## Multi-branch, multi-loader

PonderLib is maintained on separate branches, one per loader/Minecraft-version combination, named
`{loader}/{minecraft_version}` (e.g. `neoforge/1.21.1`, `forge/1.20.1`). A fix or feature that
applies to the shared engine (anything outside the handful of genuinely loader-specific files —
mod entry point, event bus wiring, `mods.toml`) gets ported to every maintained branch, not just
the one it was written against. "Ported" here means re-applied with judgement, not copied blind:
each branch has its own vanilla/loader API differences (a data-component system that doesn't exist
two Minecraft versions back, a datapack folder renamed between versions, a reobfuscation step one
loader has and the other doesn't) — re-derive the fix against *that* branch's actual APIs, then
verify it the same way (build + unit tests + GameTest) before assuming it's done.

## What "done" looks like

Before calling a change finished:

1. It compiles and the full test suite (unit + GameTest) passes, verified by reading the actual
   output, not by assuming.
2. Every comment it added or touched passes the tests above — WHY, not WHAT; no narrative; no
   upstream comparison.
3. If it touches public API, the method signature is as self-explanatory as it can be, and the
   [wiki](https://github.com/Flomik10002/PonderLib/wiki) is updated to match.
4. If it applies beyond one branch, it's been re-applied (not copied) to every branch it applies to.

Create is a good example of the bar this project holds itself to for the actual *in-game* Ponder
experience it reimplements — not because its source is a template to copy, but because it's proof
that "explain the mechanism, not just gesture at it" scales to a huge, actively-maintained mod
without turning into unmaintainable prose. Aim for that in code comments too: dense, specific,
useful to someone who wasn't there when it was written.
