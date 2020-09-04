package org.etg.espresso.codegen.viewPicking;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import org.etg.ETGProperties;
import org.etg.mate.models.Widget;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ImproverWithParentInfo extends ViewPickingStatementImprover {

    protected final String IS_DESCENDANT_OF = "isDescendantOfA";

    public ImproverWithParentInfo(ETGProperties etgProperties) {
        super(etgProperties);
    }

    /**
     * Search for parents
     * -
     **/
    public void improveStatementWithParentsOf(Widget widget, Statement statement) {
        Widget parent = widget.getParent();
        if (parent != null) {
            Expression isDescendanExpr = getIsDescendantOfExpression(parent);
            //Puede pasar mirando para arriba que ningun parent tenda id o content description
            //motibo por el cual no se puede hacer expresion con los parents
            if (isDescendanExpr != null) {
                addAllOfToFirstMethodCallIfAbsent(statement);
                findRootAllOfExpression(statement).addArgument(isDescendanExpr);
            }
        }

    }

    /**
     * Generates isDescendantOfA(@param widget) expression.
     **/
    public @Nullable
    Expression getIsDescendantOfExpression(Widget widget) {
        List<Expression> arguments = new ArrayList<>();

        addWithIdExpressionIfPossible(widget, arguments);

        if (arguments.size() == 0) {
            // only when there is no other option, use content-description if possible
            addWithContentDescriptionExpressionIfPossible(widget, arguments);
        }

        if (widget.getParent() != null) {
            Expression parentExpr = getIsDescendantOfExpression(widget.getParent());
            if (parentExpr != null) {
                if (arguments.size() == 0) {
                    // There are no more arguments for this level, and parentExpr is already a isDescendantOf expression.
                    // Return that expression to avoid unnecessary layers of isDescendantOf calls.
                    return parentExpr;
                } else {
                    arguments.add(parentExpr);
                }
            }
        }

        Expression isDescendantOfArgument;
        if (arguments.size() == 0)//no hay argumentos, no hay matcher para este widget
            return null;
        else if (arguments.size() == 1)//un solo argumento, no agrego ALL_OF
            isDescendantOfArgument = arguments.get(0);
        else//agrego ALL_OF
            isDescendantOfArgument = new MethodCallExpr(null, ALL_OF, new NodeList<>(arguments));


        return new MethodCallExpr(IS_DESCENDANT_OF, isDescendantOfArgument);
    }
}
