package org.etg.espresso.codegen.viewPicking;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import org.etg.ETGProperties;
import org.etg.mate.models.Widget;

import java.util.ArrayList;
import java.util.List;

public class ImproverWithChildrenInfo extends ViewPickingStatementImprover {

    protected final String HAS_DESCENDANT = "hasDescendant";

    public ImproverWithChildrenInfo(ETGProperties etgProperties) {
        super(etgProperties);
    }

    /**
     * Search for a childs with any of the following
     * - id
     * - text
     * - content description
     * <p>
     * For every child found, add it to picking statement
     **/
    public void improveStatementWithChildrensOf(Widget widget, Statement statement) {
        for (Widget child : widget.getChildren()) {
            Expression hasDescendantExpr = getHasDescendantExpression(child);

            if (hasDescendantExpr != null) {
                addAllOfToFirstMethodCallIfAbsent(statement);
                findRootAllOfExpression(statement).addArgument(hasDescendantExpr);
            }
        }
    }

    /**
     * Generates hasDescendant expression with "arguments" as arguments
     *
     * @param widget*/
    public Expression getHasDescendantExpression(Widget widget) {
        List<Expression> arguments = new ArrayList<>();

        addWithIdExpressionIfPossible(widget, arguments);
        addWithTextOrHintExpressionIfPossible(widget, arguments);

        if (arguments.size() == 0) {
            // only when there are no other option, use content-description if possible
            addWithContentDescriptionExpressionIfPossible(widget, arguments);
        }

        for (Widget child : widget.getChildren()) {
            Expression hasDescendantExpr = getHasDescendantExpression(child);
            if (hasDescendantExpr != null) {
                if (arguments.size() == 0) {
                    // There are no more arguments for this level, and hasDescendantExpr is already a hasDescendant expression.
                    // Return that expression to avoid unnecessary layers of hasDescendant calls.
                    return hasDescendantExpr;
                } else {
                    arguments.add(hasDescendantExpr);
                }
            }
        }

        Expression hasDescendantArgument;
        if (arguments.size() == 0)
            return null;
        else if (arguments.size() == 1)
            hasDescendantArgument = arguments.get(0);
        else
            hasDescendantArgument = new MethodCallExpr(null, ALL_OF, new NodeList<Expression>(arguments));

        return new MethodCallExpr(HAS_DESCENDANT, hasDescendantArgument);
    }
}
