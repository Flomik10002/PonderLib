package dev.flomik.ponderlib.foundation.instruction;

import dev.flomik.ponderlib.foundation.PonderScene;

public class MarkAsFinishedInstruction extends PonderInstruction {

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public void tick(PonderScene scene) {
        scene.setFinished(true);
    }

    @Override
    public void onScheduled(PonderScene scene) {
        scene.stopCounting();
    }
}
