package org.etg.espresso.codegen.viewPicking;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import org.etg.ETGProperties;
import org.etg.espresso.codegen.actions.ActionCodeMapper;
import org.etg.espresso.codegen.codeMapper.StandardTestCodeMapper;
import org.etg.mate.models.Action;
import org.etg.mate.models.Widget;

import java.util.*;

import static org.etg.espresso.codegen.viewPicking.ViewPickingStatementGenerator.convertIdToTestCodeFormat;
import static org.etg.espresso.util.StringHelper.escapeStringCharacters;

/**
 * Given a target widget, this View Picking Statement Generator produces a matcher that is guaranteed to not produce an
 * AmbiguousMatchingException when used. In order to do so, it takes into account all the other widgets in the UI tree.
 *
 * The process to generate such matcher consists on iteratively building the matchers for all widgets in the screen.
 * On each iteration, the generator adds a piece of information that might help differentiate the matcher for the target
 * widget from the others.
 * The process stops when the matcher for the target widget is unique.
 */
public class ViewPickingStatementGenerator2 extends ActionCodeMapper {

    protected final String ON_VIEW = "onView";
    protected final String ALL_OF = "allOf";
    protected final String IS = "`is`";
    protected final String WITH_TEXT_OR_HINT = "withTextOrHint";
    protected final String WITH_CONTENT_DESCRIPTION = "withContentDescription";
    protected final String WITH_ID = "withId";
    protected final String EQUAL_TO_IGNORING_CASE = "equalToTrimmingAndIgnoringCase";
    protected final String CLASS_OR_SUPER_CLASSES_NAME = "classOrSuperClassesName";
    protected final String IS_IMMEDIATE_DESCENDANT_OF = "isImmediateDescendantOfA";
    protected final String HAS_DESCENDANT = "hasDescendant";

    public static HashMap<Widget, MatcherCombination> reducedMatcherCombinationsForWidget = new HashMap<>();

    public enum MatcherType {
        ResourceId,
        Clazz,
        ContentDescription,
        Text,
    }

    public class MatcherForRelativeWidget {
        private final String relativePath;
        private final MatcherType type;

        public MatcherForRelativeWidget(String relativePath, MatcherType type) {
            this.relativePath = relativePath;
            this.type = type;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public MatcherType getType() {
            return type;
        }
    }

    public class MatcherCombination {
        private final TreeSet<MatcherForRelativeWidget> matchers = new TreeSet<>((m, n) -> {
            // 1st, try to order by matcher type
            int result = m.getType().compareTo(n.getType());

            if (result == 0) {
                // 2nd, if tie, order by closeness of matcher to target widget
                int commasInM = m.getRelativePath().length() -
                        m.getRelativePath().replace(",", "").length();
                int commasInN = n.getRelativePath().length() -
                        n.getRelativePath().replace(",", "").length();
                result = commasInM - commasInN;
            }

            if (result == 0) {
                // 3rd, if tie, order alphabetically using relative path
                result = m.getRelativePath().compareTo(n.getRelativePath());
            }

            return result;
        });
        private Boolean unique = null;

        public MatcherCombination() {}

        public MatcherCombination(HashSet<MatcherForRelativeWidget> matchers) {
            this.matchers.addAll(matchers);
        }

        /**
         * Relative path is formed by a chain of comma-separated integers.
         * An empty string indicates that the relative path points to the current widget.
         * An integer with value greater or equal than zero indicates the index of a children to which we should move.
         * An integer with value -1 indicates that we should move to the parent.
         *
         * @param relativePath
         * @param type
         */
        public void addMatcher(String relativePath, MatcherType type) {
            MatcherForRelativeWidget matcher = new MatcherForRelativeWidget(relativePath, type);

            if (!matchers.contains(matcher)) {
                matchers.add(matcher);
                unique = null;
            }
        }

        public boolean isUnique(Widget targetWidget, List<Widget> otherWidgets) {
            if (unique != null) {
                return unique;
            }

            String targetWidgetHash = getMatcherHash(targetWidget);

            for (Widget widget : otherWidgets) {
                String otherHash = getMatcherHash(widget);
                if (otherHash.equals(targetWidgetHash)) {
                    unique = false;
                    return false;
                }
            }

            unique = true;
            return true;
        }

        public String getMatcherHash(Widget widget) {
            StringBuilder hash = new StringBuilder();

            for (MatcherForRelativeWidget matcher : matchers) {
                Widget relativeWidget = widget.getWidgetByRelativePath(matcher.getRelativePath());
                if (relativeWidget == null) {
                    continue;
                }

                if (matcher.getType().equals(MatcherType.ResourceId)) {
                    hash.append(relativeWidget.getResourceID());
                }

                if (matcher.getType().equals(MatcherType.Clazz)) {
                    hash.append(relativeWidget.getClazz());
                }

                if (matcher.getType().equals(MatcherType.ContentDescription)) {
                    hash.append(relativeWidget.getContentDesc());
                }

                if (matcher.getType().equals(MatcherType.Text)) {
                    hash.append(relativeWidget.getText());
                }
            }

            return hash.toString();
        }

        public TreeSet<MatcherForRelativeWidget> getMatchers() {
            return matchers;
        }
    }

    public ViewPickingStatementGenerator2(ETGProperties etgProperties, Action action) {
        super(etgProperties, action);
    }

    @Override
    public String addTestCodeLines(List<String> testCodeLines,
                                   StandardTestCodeMapper testCodeMapper,
                                   int actionIndex,
                                   int actionsCount) {
        Widget targetWidget = action.getWidget();
        List<Widget> otherWidgets = action.getOtherWidgetsInScreen();

        MatcherCombination matcherCombination = generateMatcherCombination(targetWidget, otherWidgets);

        if (!matcherCombination.isUnique(targetWidget, otherWidgets)) {
            throw new Error("Unable to find unique matcher for target widget");
        }

        reducedMatcherCombinationsForWidget.put(targetWidget, matcherCombination);

        String viewPickingStatement = buildPickingStatementFromMatcherCombination(matcherCombination, targetWidget);
        String variableName = "view";

        if(etgProperties.useKotlinFormat()) {
            viewPickingStatement = String.format("val %s = %s", variableName, viewPickingStatement);
        } else {
            viewPickingStatement = String.format("ViewInteraction %s = %s;", variableName, viewPickingStatement);
        }

        testCodeLines.add(viewPickingStatement);

        return variableName;
    }

    private MatcherCombination generateMatcherCombination(Widget targetWidget, List<Widget> otherWidgets) {
        MatcherCombination matcherCombination = new MatcherCombination();

        // get all relative paths to other widgets in this screen
        List<String> relativePaths = new ArrayList<>();
        relativePaths.add("");

        for (Widget widget : otherWidgets) {
            relativePaths.add(targetWidget.getRelativePathToWidget(widget));
        }

        boolean uniqueMatcherFound = false;
        for (String relativePath : relativePaths) {
            for (MatcherType type : MatcherType.values()) {
                matcherCombination.addMatcher(relativePath, type);
                if (matcherCombination.isUnique(targetWidget, otherWidgets)) {
                    uniqueMatcherFound = true;
                    break;
                }
            }

            if (uniqueMatcherFound) {
                break;
            }
        }

        return reduceMatcherCombination(matcherCombination, targetWidget, otherWidgets);
    }

    /**
     * This method uses Delta Debugging to reduce the matcher combination to its minimal parts.
     * In other words, produce a _unique_ matcher that stops being unique after removing any part.
     *
     * @param originalCombination
     * @param targetWidget
     * @param otherWidgets
     * @return
     */
    private MatcherCombination reduceMatcherCombination(MatcherCombination originalCombination,
                                                        Widget targetWidget,
                                                        List<Widget> otherWidgets) {
        if (originalCombination.matchers.size() == 1) {
            return originalCombination;
        }

        List<MatcherForRelativeWidget> matchers = new ArrayList<>(originalCombination.matchers);

        // N will be the partition size for the Delta Debugging. Initial value is 2.
        int n = 2;

        while (matchers.size() >= n) {
            // divide matchers into deltas and their complements
            List<List<MatcherForRelativeWidget>> deltas = new ArrayList<>();
            List<List<MatcherForRelativeWidget>> complements = new ArrayList<>();
            int partitionSize = (int) Math.floor((float) matchers.size() / (float) n);

            for (int start = 0; start < matchers.size(); start += partitionSize) {
                int end = Math.min(start + partitionSize, matchers.size());
                List<MatcherForRelativeWidget> delta = new ArrayList<>(matchers.subList(start, end));
                deltas.add(delta);

                List<MatcherForRelativeWidget> complement = new ArrayList<>(matchers.subList(0, start));
                complement.addAll(matchers.subList(end, matchers.size()));
                complements.add(complement);
            }

            // remove duplicate entry from complements list that is in deltas list
            complements.removeAll(deltas);

            // test matchers in deltas
            int uniqueSublistIndex = -1;
            for (int i = 0; i < deltas.size(); i++) {
                MatcherCombination m = new MatcherCombination(new HashSet<>(deltas.get(i)));
                if (m.isUnique(targetWidget, otherWidgets)) {
                    uniqueSublistIndex = i;
                    break;
                }
            }

            if (uniqueSublistIndex != -1) {
                // reduce to the failing delta in next iteration
                n = 2;
                matchers = deltas.get(uniqueSublistIndex);
                continue;
            }

            // test matchers in complements
            int uniqueComplementIndex = -1;
            for (int i = 0; i < complements.size(); i++) {
                MatcherCombination m = new MatcherCombination(new HashSet<>(complements.get(i)));
                if (m.isUnique(targetWidget, otherWidgets)) {
                    uniqueComplementIndex = i;
                    break;
                }
            }

            if (uniqueComplementIndex != -1) {
                // reduce to the failing complement in next iteration
                matchers = complements.get(uniqueComplementIndex);
                n = n - 1;
            } else {
                // increase granularity, search in a finer space
                n = 2 * n;
                if (n > matchers.size()) {
                    break;
                }
            }
        }


        return new MatcherCombination(new HashSet<>(matchers));
    }

    private String buildPickingStatementFromMatcherCombination(MatcherCombination matcherCombination,
                                                               Widget targetWidget) {
        MethodCallExpr onViewExpr = new MethodCallExpr(null, ON_VIEW);
        List<Expression> onViewMethodArguments = new ArrayList<>();

        for (MatcherForRelativeWidget matcher : matcherCombination.matchers) {
            MethodCallExpr relativeWidgetExpr = buildRelativeWidgetMatcher(matcher.getRelativePath(), onViewMethodArguments);

            List<Expression> relativeWidgetArguments = relativeWidgetExpr != null?
                    relativeWidgetExpr.getArguments() : onViewMethodArguments;
            Widget widgetToMatch = relativeWidgetExpr != null?
                    targetWidget.getWidgetByRelativePath(matcher.getRelativePath()) : targetWidget;

            if (matcher.getType().equals(MatcherType.ResourceId)) {
                addWithIdExpressionIfPossible(widgetToMatch, relativeWidgetArguments);
            } else if (matcher.getType().equals(MatcherType.Clazz)) {
                addWithClassExpressionIfPossible(widgetToMatch, relativeWidgetArguments);
            } else if (matcher.getType().equals(MatcherType.ContentDescription)) {
                addWithContentDescriptionExpressionIfPossible(widgetToMatch, relativeWidgetArguments);
            } else if (matcher.getType().equals(MatcherType.Text)) {
                addWithTextOrHintExpressionIfPossible(widgetToMatch, relativeWidgetArguments);
            }
        }

        if (onViewMethodArguments.size() == 0) {
            throw new Error("Unable to build picking statement for matcher combination");
        } else if (onViewMethodArguments.size() == 1) {
            onViewExpr.addArgument(onViewMethodArguments.get(0));
        } else {
            MethodCallExpr allOfExpr = new MethodCallExpr(null, ALL_OF);
            for (Expression argument : onViewMethodArguments) {
                allOfExpr.addArgument(argument);
            }
            onViewExpr.addArgument(allOfExpr);
        }


        return onViewExpr.toString();
    }

    private MethodCallExpr buildRelativeWidgetMatcher(String relativePath, List<Expression> arguments) {
        if (relativePath.isEmpty()) {
            return null;
        }

        MethodCallExpr firstExpr = null;
        MethodCallExpr currentExpr = null;
        String[] indexes = relativePath.split(",");
        for (String strIndex : indexes) {
            int index = Integer.parseInt(strIndex);
            if (index < 0) {
                // we are going up one level
                // TODO: check if we have already a matcher for this direction in the arguments?

                if (currentExpr == null) {
                    // this is the first matcher in the chain to get to the relative widget
                    currentExpr = new MethodCallExpr(null, IS_IMMEDIATE_DESCENDANT_OF);
                    firstExpr = currentExpr;
                } else {
                    // we already have a matcher in the direction of the relative widget,
                    // append the new expression to the existing matcher
                    MethodCallExpr expr = new MethodCallExpr(null, IS_IMMEDIATE_DESCENDANT_OF);
                    currentExpr.addArgument(expr);
                    currentExpr = expr;
                }
            } else {
                // we are going down one level
                // TODO: check if we have already a matcher for this direction in the arguments?

                if (currentExpr == null) {
                    // this is the first matcher in the chain to get to the relative widget
                    currentExpr = new MethodCallExpr(null, HAS_DESCENDANT);
                    firstExpr = currentExpr;
                } else {
                    // we already have a matcher in the direction of the relative widget,
                    // append the new expression to the existing matcher
                    MethodCallExpr expr = new MethodCallExpr(null, HAS_DESCENDANT);
                    currentExpr.addArgument(expr);
                    currentExpr = expr;
                }
            }
        }

        arguments.add(firstExpr);

        return currentExpr;
    }

    private void addWithClassExpressionIfPossible(Widget targetWidget, List<Expression> arguments) {
        MethodCallExpr isMethod = new MethodCallExpr(IS, new StringLiteralExpr(targetWidget.getClazz()));
        arguments.add(new MethodCallExpr(CLASS_OR_SUPER_CLASSES_NAME, isMethod));
    }

    public void addWithTextOrHintExpressionIfPossible(Widget widget, List<Expression> arguments) {
        if ("android.widget.Switch".equals(widget.getClazz())) {
            return;
        }

        String text = widget.getText();
        if (text != null && !text.isEmpty()) {
            MethodCallExpr equalToIgnoringCaseMethod = new MethodCallExpr(EQUAL_TO_IGNORING_CASE,
                    new StringLiteralExpr(escapeStringCharacters(text)));
            arguments.add(new MethodCallExpr(WITH_TEXT_OR_HINT, equalToIgnoringCaseMethod));
        }
    }

    public void addWithContentDescriptionExpressionIfPossible(Widget widget, List<Expression> arguments) {
        String contentDesc = widget.getContentDesc();
        if (contentDesc != null && !contentDesc.isEmpty()) {
            MethodCallExpr equalToIgnoringCaseMethod = new MethodCallExpr(EQUAL_TO_IGNORING_CASE,
                    new StringLiteralExpr(contentDesc));
            arguments.add(new MethodCallExpr(WITH_CONTENT_DESCRIPTION, equalToIgnoringCaseMethod));
        }
    }

    public void addWithIdExpressionIfPossible(Widget widget, List<Expression> arguments) {
        if (widget.isAndroidView()) {
            return;
        }

        String id = widget.getResourceID();

        //if widget is from android and not from app view we don't want it
        if (id != null && !id.startsWith("android:id")) {
            String strLiteral = convertIdToTestCodeFormat(id);
            if (strLiteral.startsWith("R.id.")) {
                arguments.add(new MethodCallExpr(WITH_ID, StaticJavaParser.parseExpression(strLiteral)));
            }
        }
    }
}
