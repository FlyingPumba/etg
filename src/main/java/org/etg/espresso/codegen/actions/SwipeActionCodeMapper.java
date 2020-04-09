package org.etg.espresso.codegen.actions;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.TestCodeMapper;
import org.etg.espresso.codegen.viewPicking.ViewPickingStatementGenerator;
import org.etg.mate.models.Action;
import org.etg.mate.models.Swipe;

import java.util.List;

public class SwipeActionCodeMapper extends ActionCodeMapper {

    public SwipeActionCodeMapper(ETGProperties etgProperties, Action action) {
        super(etgProperties, action);
    }

    @Override
    public String addTestCodeLines(List<String> testCodeLines, TestCodeMapper testCodeMapper, int actionIndex, int actionsCount) {
        ViewPickingStatementGenerator pickingStatementGenerator = new ViewPickingStatementGenerator(etgProperties, action);
        String variableName = pickingStatementGenerator.addTestCodeLines(testCodeLines, testCodeMapper, actionIndex, actionsCount);

        Swipe swipe = action.getSwipe();

        String methdCall = getSwipeAction(swipe);

        testCodeLines.add(createActionStatement(variableName, methdCall, testCodeMapper));
        testCodeLines.add(getWaitToScrollEndStatement() + testCodeMapper.getStatementTerminator() + "\n");
        testCodeMapper.swipeActionAdded = true;

        return null;
    }

    private String getSwipeAction(Swipe swipe) {
        String methdCall = "getSwipeAction($fromX, $fromY, $toX, $toY)";
        methdCall =
                methdCall
                        .replace("$fromX", String.valueOf(swipe.getInitialPosition().x))
                        .replace("$fromY", String.valueOf(swipe.getInitialPosition().y))
                        .replace("$toX", String.valueOf(swipe.getFinalPosition().x))
                        .replace("$toY", String.valueOf(swipe.getFinalPosition().y));
        return methdCall;
    }

    private String getWaitToScrollEndStatement() {
        return "waitToScrollEnd()";
    }
}
