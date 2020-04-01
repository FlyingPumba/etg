package org.etg.espresso.codegen.actions;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.TestCodeMapper;
import org.etg.mate.models.Action;

import java.util.List;

public class BackActionCodeMapper extends ActionCodeMapper {

    public BackActionCodeMapper(ETGProperties etgProperties, Action action) {
        super(etgProperties, action);
    }

    @Override
    public String addTestCodeLines(List<String> testCodeLines, TestCodeMapper testCodeMapper) {
        String lastStatement = null;
        if (testCodeLines.size() > 0) {
            lastStatement = testCodeLines.get(testCodeLines.size() - 1);
        }

        // the following statement is identically to
        // onView(isRoot()).perform(ViewActions.pressBackUnconditionally());
        // choosing one or the other is just a matter of taste
        String statement = String.format("Espresso.%s()%s", getPressBackCmd(), testCodeMapper.getStatementTerminator());

        if (lastStatement != null && lastStatement.contains("pressMenuKey")) {
            // add hoc heuristic:
            // In the cases where pressMenuKey was just fired, it seems to work better if we don't specifiy the root view
            // as the target of the pressBackUnconditionally action.
            statement = String.format("ViewActions.%s()%s", getPressBackCmd(), testCodeMapper.getStatementTerminator());
        }

        statement += "\n";

        testCodeLines.add(statement);

        return null;
    }

    private String getPressBackCmd() {
        try {
            if (etgProperties.getEspressoVersion().startsWith("3")) {
                return "pressBackUnconditionally";
            } else {
                return "pressBack";
            }
        } catch (Exception e) {
            return "pressBack";
        }
    }
}
