package net.optifine.entity.model.anim;

import net.optifine.expr.IExpression;

public class RenderResolverEntity implements IRenderResolver {
	@Override
    public IExpression getParameter(String name) {
		RenderEntityParameterBool renderentityparameterbool = RenderEntityParameterBool.parse(name);

		if (renderentityparameterbool != null) {
			return renderentityparameterbool;
		} else {
            return RenderEntityParameterFloat.parse(name);
		}
	}
}
