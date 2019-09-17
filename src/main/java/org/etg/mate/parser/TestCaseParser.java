package org.etg.mate.parser;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etg.mate.models.Action;
import org.etg.mate.models.WidgetTestCase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TestCaseParser {
    public static List<WidgetTestCase> parseList(ObjectMapper mapper, JsonNode node) {
        List<WidgetTestCase> widgetTestCases = new ArrayList<>();
        for (Iterator<JsonNode> it = node.elements(); it.hasNext(); ) {
            JsonNode element = it.next();
            WidgetTestCase widgetTestCase = TestCaseParser.parse(mapper, element);
            widgetTestCases.add(widgetTestCase);
        }
        return widgetTestCases;
    }

    public static WidgetTestCase parse(ObjectMapper mapper, JsonNode node) {
        JavaType listOfTestCasesType = mapper.getTypeFactory().constructCollectionType(List.class, WidgetTestCase.class);
        // List<WidgetTestCase> testCases =  mapper.convertValue(jsonNode, listOfTestCasesType);
        WidgetTestCase widgetTestCase = new WidgetTestCase(mapper.convertValue(node.get("id"), String.class));

        if (mapper.convertValue(node.get("crashDetected"), Boolean.class)) {
            widgetTestCase.setCrashDetected();
        }

        widgetTestCase.setSparseness(mapper.convertValue(node.get("sparseness"), Double.class));
        widgetTestCase.setNovelty(mapper.convertValue(node.get("novelty"), Float.class));

        JavaType listOfStringType = mapper.getTypeFactory().constructCollectionType(List.class, String.class);
        List<String> visitedActivities = mapper.convertValue(node.get("visitedActivities"), listOfStringType);

        for (String activity : visitedActivities) {
            widgetTestCase.updateVisitedActivities(activity);
        }

        List<Action> eventSequence = ActionParser.parseList(mapper, node.get("eventSequence"));

        for (Action event : eventSequence) {
            widgetTestCase.addEvent(event);
        }

        return widgetTestCase;
    }
}
