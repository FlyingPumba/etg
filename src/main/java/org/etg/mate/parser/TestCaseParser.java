package org.etg.mate.parser;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etg.mate.models.Action;
import org.etg.mate.models.TestCase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TestCaseParser {
    public static List<TestCase> parseList(ObjectMapper mapper, JsonNode node) {
        List<TestCase> testCases = new ArrayList<>();
        for (Iterator<JsonNode> it = node.elements(); it.hasNext(); ) {
            JsonNode element = it.next();
            TestCase testCase = TestCaseParser.parse(mapper, element);
            testCases.add(testCase);
        }
        return testCases;
    }

    public static TestCase parse(ObjectMapper mapper, JsonNode node) {
        JavaType listOfTestCasesType = mapper.getTypeFactory().constructCollectionType(List.class, TestCase.class);
        // List<TestCase> testCases =  mapper.convertValue(jsonNode, listOfTestCasesType);
        TestCase testCase = new TestCase(mapper.convertValue(node.get("id"), String.class));

        if (mapper.convertValue(node.get("crashDetected"), Boolean.class)) {
            testCase.setCrashDetected();
        }

        testCase.setSparseness(mapper.convertValue(node.get("sparseness"), Double.class));
        testCase.setNovelty(mapper.convertValue(node.get("novelty"), Float.class));

        JavaType listOfStringType = mapper.getTypeFactory().constructCollectionType(List.class, String.class);
        List<String> visitedActivities = mapper.convertValue(node.get("visitedActivities"), listOfStringType);

        for (String activity : visitedActivities) {
            testCase.updateVisitedActivities(activity);
        }

        List<Action> eventSequence = ActionParser.parseList(mapper, node.get("eventSequence"));

        for (Action event : eventSequence) {
            testCase.addEvent(event);
        }

        return testCase;
    }
}
