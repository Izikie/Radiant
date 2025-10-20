package net.optifine.expr;

public class FunctionFloatArray implements IExpressionFloatArray {
	private final FunctionType type;
	private final IExpression[] arguments;

	public FunctionFloatArray(FunctionType type, IExpression[] arguments) {
		this.type = type;
		this.arguments = arguments;
	}

	@Override
    public float[] eval() {
		return this.type.evalFloatArray(this.arguments);
	}

	@Override
    public ExpressionType getExpressionType() {
		return ExpressionType.FLOAT_ARRAY;
	}

	public String toString() {
		return this.type + "()";
	}
}
