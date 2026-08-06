package dev.flomik.ponderlib.foundation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

// PonderParticleProviders.create() itself needs a live, world-connected Minecraft client (it
// fetches Minecraft.getInstance().particleEngine) - can't run in this environment (same
// constraint as PonderTooltipHandlerTest's client-dependent branches). What CAN be verified
// without one: that the private-field reflection this whole mechanism depends on
// (ParticleEngine's "providers" field, no public accessor - see the class javadoc for why plain
// reflection instead of a Mixin accessor) still resolves against the real ParticleEngine class
// shipped with this Minecraft version. If a future update renames/removes that field, this test
// fails loudly here instead of real particles silently going missing in a running game.
class PonderParticleProvidersTest {

    @Test
    void theProvidersFieldReflectionResolvesAgainstTheRealParticleEngineClass() {
        assertTrue(PonderParticleProviders.isAvailable(),
            "ParticleEngine's \"providers\" field could not be reflected - did a Minecraft update rename it?");
    }
}
