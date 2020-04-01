package org.etg.espresso.codegen.viewPicking;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.Statement;
import org.etg.mate.models.Widget;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.etg.espresso.codegen.viewPicking.ViewPickingStatementGenerator.convertIdToTestCodeFormat;

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
            List<Expression> arguments = new ArrayList<>();
            addWithIdExpressionIfPossible(child, arguments);
            addWithContentDescriptionExpressionIfPossible(child, arguments);
            addWithTextExpressionIfPossible(child, arguments);

            if (!arguments.isEmpty()) {
                Expression hasDescendantExpr = getHasDescendantExpression(arguments);
                addAllOfToFirstMethodCallIfAbsent(statement);
                findRootAllOfExpression(statement).addArgument(hasDescendantExpr);
            }
        }
    }

    /**
     * Generates hasDescendant expression with "arguments" as arguments
     **/
    public static Expression getHasDescendantExpression(List<Expression> arguments) {
        Expression hasDescendantArgument;
        if (arguments.size() == 1)
            hasDescendantArgument = arguments.get(0);
        else
            hasDescendantArgument = new MethodCallExpr(null, ALL_OF, new NodeList<Expression>(arguments));

        return new MethodCallExpr(HAS_DESCENDANT, hasDescendantArgument);
    }
}
