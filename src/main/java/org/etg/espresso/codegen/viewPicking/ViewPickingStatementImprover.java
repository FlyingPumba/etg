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

public class ViewPickingStatementImprover {

    public static final String ALL_OF = "allOf";
    public static final String HAS_DESCENDANT = "hasDescendant";
    public static final String IS_DESCENDANT_OF = "isDescendantOfA";
    public static final String WITH_TEXT = "withText";
    public static final String WITH_CONTENT_DESCRIPTION = "withContentDescription";
    public static final String WITH_ID = "withId";
    public static final String EQUAL_TO_IGNORING_CASE = "equalToIgnoringCase";

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

    public static MethodCallExpr findRootAllOfExpression(Statement statement) {
        //Finds first "allOf" method call expression on statemente. Walks ast in pre-order
        return statement.findFirst(
                MethodCallExpr.class,
                methodCallExpr -> methodCallExpr.getName().toString().equals(ALL_OF)
        ).orElse(null);
    }

    public static void addWithTextExpressionIfPossible(Widget widget, List<Expression> arguments) {
        String text = widget.getText();
        if (text != null && !text.isEmpty()) {
            MethodCallExpr equalToIgnoringCaseMethod = new MethodCallExpr(EQUAL_TO_IGNORING_CASE, new StringLiteralExpr(text));
            arguments.add(new MethodCallExpr(WITH_TEXT, equalToIgnoringCaseMethod));
        }
    }

    public static void addWithContentDescriptionExpressionIfPossible(Widget widget, List<Expression> arguments) {
        String contentDesc = widget.getContentDesc();
        if (contentDesc != null && !contentDesc.isEmpty()) {
            MethodCallExpr equalToIgnoringCaseMethod = new MethodCallExpr(EQUAL_TO_IGNORING_CASE, new StringLiteralExpr(contentDesc));
            arguments.add(new MethodCallExpr(WITH_CONTENT_DESCRIPTION, equalToIgnoringCaseMethod));
        }
    }

    public static boolean addWithIdExpressionIfPossible(Widget widget, List<Expression> arguments) {
        String id = widget.getResourceID();
        if (id != null && !id.startsWith("android:id")) {//if widget is from android and not from app view we don't want it
            String strLiteral = convertIdToTestCodeFormat(id);
            if (strLiteral.startsWith("R.id.")) {
                arguments.add(new MethodCallExpr(WITH_ID, StaticJavaParser.parseExpression(strLiteral)));
                return true;
            }
        }
        return false;
    }

    /**
     * adds allOf to first method call of statement if there is no "allOf" call on the statement
     **/
    public static void addAllOfToFirstMethodCallIfAbsent(Statement statement) {
        if (!firstMethodCallIsOnViewWithArgumentAllOf(statement)) {//statement does not have onView(allOf(..))
            statement.findFirst(MethodCallExpr.class).ifPresent(onViewMethodCallExpr -> {
                NodeList<Expression> onViewArgument = onViewMethodCallExpr.getArguments();
                MethodCallExpr allOfExpr = new MethodCallExpr(null, ALL_OF, onViewArgument);
                onViewMethodCallExpr.setArguments(NodeList.nodeList(allOfExpr));
            });
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

    /**
     * Generates isDescendantOfA(@param widget) expression.
     **/
    public static @Nullable
    Expression getIsDescendantOfExpression(Widget widget) {
        List<Expression> arguments = new ArrayList<>();

        addWithIdExpressionIfPossible(widget, arguments);
        addWithContentDescriptionExpressionIfPossible(widget, arguments);
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

    /**
     * Answers if the first method call is "onView" with argumente "allOf"}
     * ie: true if first method call looks like onView(allOf(...))
     **/
    public static boolean firstMethodCallIsOnViewWithArgumentAllOf(Statement statement) {
        Optional<MethodCallExpr> firstMethodCall = statement.findFirst(MethodCallExpr.class);
        return firstMethodCall.isPresent() &&
                firstMethodCall.get().getName().toString().equals("onView") &&
                firstMethodCall.get().getArguments().isNonEmpty() &&
                firstMethodCall.get().getArguments().get(0).isMethodCallExpr() &&
                firstMethodCall.get().getArguments().get(0).asMethodCallExpr().getName().toString().equals(ALL_OF);
    }
}
