package net.optifine.entity.model.anim;

import net.optifine.Log;
import net.optifine.expr.ExpressionParser;
import net.optifine.expr.IExpressionFloat;
import net.optifine.expr.ParseException;

public class ModelVariableUpdater {
    private final String modelVariableName;
    private final String expressionText;
    private ModelVariableFloat modelVariable;
    private IExpressionFloat expression;

    public ModelVariableUpdater(String modelVariableName, String expressionText) {
        this.modelVariableName = modelVariableName;
        this.expressionText = expressionText;
    }

    public boolean initialize(IModelResolver mr) {
        this.modelVariable = mr.getModelVariable(this.modelVariableName);

        if (this.modelVariable == null) {
            Log.error("Model variable not found: " + this.modelVariableName);
            return false;
        } else {
            try {
                ExpressionParser expressionparser = new ExpressionParser(mr);
                this.expression = expressionparser.parseFloat(this.expressionText);
                return true;
            } catch (ParseException exception) {
                Log.error("Error parsing expression: " + this.expressionText);
                Log.error(exception.getClass().getName() + ": " + exception.getMessage());
                return false;
            }
        }
    }

    public void update() {
        float f = this.expression.eval();
        this.modelVariable.setValue(f);
    }
}
