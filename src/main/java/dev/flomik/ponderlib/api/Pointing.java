package dev.flomik.ponderlib.api;

/**
 * Which side of its anchor point a speech box's tail points out of — used by {@link
 * dev.flomik.ponderlib.api.scene.OverlayInstructions#showControls} and the "next scene" teaser.
 */
public enum Pointing {
    UP,
    DOWN,
    LEFT,
    RIGHT
}
