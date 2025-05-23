package net.minecraft.client.renderer.block.model;

import net.minecraft.util.Direction;
import org.joml.Vector3f;

public class BlockPartRotation {
    public final Vector3f origin;
    public final Direction.Axis axis;
    public final float angle;
    public final boolean rescale;

    public BlockPartRotation(Vector3f originIn, Direction.Axis axisIn, float angleIn, boolean rescaleIn) {
        this.origin = originIn;
        this.axis = axisIn;
        this.angle = angleIn;
        this.rescale = rescaleIn;
    }
}
