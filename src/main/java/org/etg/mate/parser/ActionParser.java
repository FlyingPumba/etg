package org.etg.mate.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etg.mate.models.Action;
import org.etg.mate.models.ActionType;
import org.etg.mate.models.Swipe;
import org.etg.mate.models.Widget;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//todo: Se pueden remover todos los parsers y usar gson o jackson para deserializar el json
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
        ActionType actionType = mapper.convertValue(node.get("actionType"), ActionType.class);
        Action action = new Action(widget, actionType);
        action.setExtraInfo(node.get("extraInfo").asText());

        if (!node.get("swipe").isNull()){
            Point initial = new Point(node.get("swipe").get("initialPosition").get("x").asInt(), node.get("swipe").get("initialPosition").get("y").asInt());
            Point finalP = new Point(node.get("swipe").get("finalPosition").get("x").asInt(), node.get("swipe").get("finalPosition").get("y").asInt());
            action.setSwipe(new Swipe(initial, finalP, 15));
        }

        return action;
    }
}
