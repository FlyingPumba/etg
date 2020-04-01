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

public class ImproverWithParentInfo extends ViewPickingStatementImprover {

    public static final String IS_DESCENDANT_OF = "isDescendantOfA";

    /**
     * Search for parents
     * -
     **/
    public static void improveStatementWithParentsOf(Widget widget, Statement statement) {
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
    public static @Nullable
    Expression getIsDescendantOfExpression(Widget widget) {
        List<Expression> arguments = new ArrayList<>();

        addWithIdExpressionIfPossible(widget, arguments);
        // addWithContentDescriptionExpressionIfPossible(widget, arguments);
        if (widget.getParent() != null) {
            Expression parentExpr = getIsDescendantOfExpression(widget.getParent());
            if (parentExpr != null) arguments.add(parentExpr);
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
