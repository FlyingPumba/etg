package org.etg.espresso.codegen.viewPicking;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import org.etg.mate.models.Widget;

import java.util.ArrayList;
import java.util.List;

public class ImproverWithChildrenInfo extends ViewPickingStatementImprover {

    public static final String HAS_DESCENDANT = "hasDescendant";

    /**
     * Search for a childs with any of the following
     * - id
     * - text
     * - content description
     * <p>
     * For every child found, add it to picking statement
     **/
    public static void improveStatementWithChildrensOf(Widget widget, Statement statement) {
        for (Widget child : widget.getChildren()) {
            Expression hasDescendantExpr = getHasDescendantExpression(child);

            addAllOfToFirstMethodCallIfAbsent(statement);
            findRootAllOfExpression(statement).addArgument(hasDescendantExpr);
        }
    }

    /**
     * Generates hasDescendant expression with "arguments" as arguments
     *
     * @param widget*/
    public static Expression getHasDescendantExpression(Widget widget) {
        List<Expression> arguments = new ArrayList<>();

        addWithIdExpressionIfPossible(widget, arguments);
        addWithTextExpressionIfPossible(widget, arguments);

        if (arguments.size() == 0) {
            // only when there are no other option, use content-description if possible
            addWithContentDescriptionExpressionIfPossible(widget, arguments);
        }

        for (Widget child : widget.getChildren()) {
            Expression hasDescendantExpr = getHasDescendantExpression(child);
            arguments.add(hasDescendantExpr);
        }

        Expression hasDescendantArgument;
        if (arguments.size() == 1)
            hasDescendantArgument = arguments.get(0);
        else
            hasDescendantArgument = new MethodCallExpr(null, ALL_OF, new NodeList<Expression>(arguments));

        return new MethodCallExpr(HAS_DESCENDANT, hasDescendantArgument);
    }
}
