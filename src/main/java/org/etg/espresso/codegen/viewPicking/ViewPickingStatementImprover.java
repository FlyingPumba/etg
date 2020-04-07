package org.etg.espresso.codegen.viewPicking;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.Statement;
import org.etg.mate.models.Widget;

import java.util.List;
import java.util.Optional;

import static org.etg.espresso.codegen.viewPicking.ViewPickingStatementGenerator.convertIdToTestCodeFormat;
import static org.etg.espresso.util.StringHelper.escapeStringCharacters;

public class ViewPickingStatementImprover {

    public static final String ALL_OF = "allOf";
    public static final String WITH_TEXT_OR_HINT = "withTextOrHint";
    public static final String WITH_CONTENT_DESCRIPTION = "withContentDescription";
    public static final String WITH_ID = "withId";
    public static final String EQUAL_TO_IGNORING_CASE = "equalToIgnoringCase";


    public static MethodCallExpr findRootAllOfExpression(Statement statement) {
        //Finds first "allOf" method call expression on statemente. Walks ast in pre-order
        return statement.findFirst(
                MethodCallExpr.class,
                methodCallExpr -> methodCallExpr.getName().toString().equals(ALL_OF)
        ).orElse(null);
    }

    public static void addWithTextOrHintExpressionIfPossible(Widget widget, List<Expression> arguments) {
        if ("android.widget.Switch".equals(widget.getClazz())) {
            return;
        }

        String text = widget.getText();
        if (text != null && !text.isEmpty()) {
            MethodCallExpr equalToIgnoringCaseMethod = new MethodCallExpr(EQUAL_TO_IGNORING_CASE, new StringLiteralExpr(escapeStringCharacters(text)));
            arguments.add(new MethodCallExpr(WITH_TEXT_OR_HINT, equalToIgnoringCaseMethod));
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
