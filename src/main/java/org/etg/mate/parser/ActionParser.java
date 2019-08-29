package org.etg.mate.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etg.mate.models.Action;
import org.etg.mate.models.Widget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ActionParser {
    public static List<Action> parseList(ObjectMapper mapper, JsonNode node) {
        List<Action> eventSequence = new ArrayList<>();
        for (Iterator<JsonNode> it = node.elements(); it.hasNext(); ) {
            JsonNode element = it.next();
            Action event = parse(mapper, element);
            eventSequence.add(event);
        }
        return eventSequence;
    }

    public static Action parse(ObjectMapper mapper, JsonNode node) {
        Widget widget = WidgetParser.parse(mapper, node.get("widget"));
        int actionType = mapper.convertValue(node.get("actionType"), Integer.class);
        return new Action(widget, actionType);
    }
}
