package net.optifine.shaders.uniform;

import net.optifine.expr.*;

public enum UniformType {
	BOOL,
	INT,
	FLOAT,
	VEC2,
	VEC3,
	VEC4;

	public static UniformType parse(String type) {
		UniformType[] uniformTypes = values();

		for (UniformType uniform : uniformTypes) {
			if (uniform.name().toLowerCase().equals(type))
				return uniform;
		}

		return null;
	}

	public ShaderUniformBase makeShaderUniform(String name) {
		return switch (this) {
			case BOOL -> new ShaderUniform1i(name);
			case INT -> new ShaderUniform1i(name);
			case FLOAT -> new ShaderUniform1f(name);
			case VEC2 -> new ShaderUniform2f(name);
			case VEC3 -> new ShaderUniform3f(name);
			case VEC4 -> new ShaderUniform4f(name);
		};
	}

	public void updateUniform(IExpression expression, ShaderUniformBase uniform) {
		switch (this) {
			case BOOL -> this.updateUniformBool((IExpressionBool) expression, (ShaderUniform1i) uniform);
			case INT -> this.updateUniformInt((IExpressionFloat) expression, (ShaderUniform1i) uniform);
			case FLOAT -> this.updateUniformFloat((IExpressionFloat) expression, (ShaderUniform1f) uniform);
			case VEC2 -> this.updateUniformFloat2((IExpressionFloatArray) expression, (ShaderUniform2f) uniform);
			case VEC3 -> this.updateUniformFloat3((IExpressionFloatArray) expression, (ShaderUniform3f) uniform);
			case VEC4 -> this.updateUniformFloat4((IExpressionFloatArray) expression, (ShaderUniform4f) uniform);
			default -> throw new RuntimeException("Unknown uniform type: " + this);
		}
	}

	private void updateUniformBool(IExpressionBool expression, ShaderUniform1i uniform) {
		boolean flag = expression.eval();
		int i = flag ? 1 : 0;
		uniform.setValue(i);
	}

	private void updateUniformInt(IExpressionFloat expression, ShaderUniform1i uniform) {
		int i = (int) expression.eval();
		uniform.setValue(i);
	}

	private void updateUniformFloat(IExpressionFloat expression, ShaderUniform1f uniform) {
		float f = expression.eval();
		uniform.setValue(f);
	}

	private void updateUniformFloat2(IExpressionFloatArray expression, ShaderUniform2f uniform) {
		float[] afloat = expression.eval();

		if (afloat.length != 2) {
			throw new RuntimeException("Value length is not 2, length: " + afloat.length);
		} else {
			uniform.setValue(afloat[0], afloat[1]);
		}
	}

	private void updateUniformFloat3(IExpressionFloatArray expression, ShaderUniform3f uniform) {
		float[] afloat = expression.eval();

		if (afloat.length != 3) {
			throw new RuntimeException("Value length is not 3, length: " + afloat.length);
		} else {
			uniform.setValue(afloat[0], afloat[1], afloat[2]);
		}
	}

	private void updateUniformFloat4(IExpressionFloatArray expression, ShaderUniform4f uniform) {
		float[] afloat = expression.eval();

		if (afloat.length != 4) {
			throw new RuntimeException("Value length is not 4, length: " + afloat.length);
		} else {
			uniform.setValue(afloat[0], afloat[1], afloat[2], afloat[3]);
		}
	}

	public boolean matchesExpressionType(ExpressionType expressionType) {
		return switch (this) {
			case BOOL -> expressionType == ExpressionType.BOOL;
			case INT -> expressionType == ExpressionType.FLOAT;
			case FLOAT -> expressionType == ExpressionType.FLOAT;
			case VEC2, VEC3, VEC4 -> expressionType == ExpressionType.FLOAT_ARRAY;
		};
	}
}
