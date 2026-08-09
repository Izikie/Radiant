package net.minecraft.client.renderer.block.model;

import net.minecraft.util.Direction;
import org.joml.Vector3f;

public record BlockPartRotation(Vector3f origin, Direction.Axis axis, float angle, boolean rescale) {
}
