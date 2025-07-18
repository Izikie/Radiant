package net.optifine.entity.model.anim;

import net.minecraft.client.model.ModelRenderer;
import net.optifine.Log;

public enum ModelVariableType {
	POS_X("tx"),
	POS_Y("ty"),
	POS_Z("tz"),
	ANGLE_X("rx"),
	ANGLE_Y("ry"),
	ANGLE_Z("rz"),
	OFFSET_X("ox"),
	OFFSET_Y("oy"),
	OFFSET_Z("oz"),
	SCALE_X("sx"),
	SCALE_Y("sy"),
	SCALE_Z("sz");

	public static final ModelVariableType[] VALUES = values();
	private final String name;

	ModelVariableType(String name) {
		this.name = name;
	}

	public static ModelVariableType parse(String str) {
		for (ModelVariableType modelvariabletype : VALUES) {
			if (modelvariabletype.getName().equals(str)) {
				return modelvariabletype;
			}
		}

		return null;
	}

	public String getName() {
		return this.name;
	}

	public float getFloat(ModelRenderer mr) {
		return switch (this) {
			case POS_X -> mr.rotationPointX;
			case POS_Y -> mr.rotationPointY;
			case POS_Z -> mr.rotationPointZ;
			case ANGLE_X -> mr.rotateAngleX;
			case ANGLE_Y -> mr.rotateAngleY;
			case ANGLE_Z -> mr.rotateAngleZ;
			case OFFSET_X -> mr.offsetX;
			case OFFSET_Y -> mr.offsetY;
			case OFFSET_Z -> mr.offsetZ;
			case SCALE_X -> mr.scaleX;
			case SCALE_Y -> mr.scaleY;
			case SCALE_Z -> mr.scaleZ;
			default -> {
				Log.error("GetFloat not supported for: " + this);
				yield 0.0F;
			}
		};
	}

	public void setFloat(ModelRenderer mr, float val) {
		switch (this) {
			case POS_X:
				mr.rotationPointX = val;
				return;

			case POS_Y:
				mr.rotationPointY = val;
				return;

			case POS_Z:
				mr.rotationPointZ = val;
				return;

			case ANGLE_X:
				mr.rotateAngleX = val;
				return;

			case ANGLE_Y:
				mr.rotateAngleY = val;
				return;

			case ANGLE_Z:
				mr.rotateAngleZ = val;
				return;

			case OFFSET_X:
				mr.offsetX = val;
				return;

			case OFFSET_Y:
				mr.offsetY = val;
				return;

			case OFFSET_Z:
				mr.offsetZ = val;
				return;

			case SCALE_X:
				mr.scaleX = val;
				return;

			case SCALE_Y:
				mr.scaleY = val;
				return;

			case SCALE_Z:
				mr.scaleZ = val;
				return;

			default:
				Log.error("SetFloat not supported for: " + this);
		}
	}
}
