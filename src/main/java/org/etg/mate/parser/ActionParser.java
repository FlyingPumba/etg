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
import java.util.Vector;

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
        JsonNode rootWidgetNode = node.get("rootWidget");
        if (rootWidgetNode == null) {
            // this is an old JSON, use the widget field as root widget
            rootWidgetNode = node.get("widget");
        }

        Widget rootWidget = WidgetParser.parse(mapper, rootWidgetNode);
        ActionType actionType = mapper.convertValue(node.get("actionType"), ActionType.class);

        // target widget path
        final JsonNode targetWidgetPathArray = node.get("targetWidgetPath");
        Vector<Integer> targetWidgetPath = new Vector<>();
        if (targetWidgetPathArray != null){
            for (Iterator<JsonNode> it = targetWidgetPathArray.elements(); it.hasNext(); ) {
                JsonNode element = it.next();
                targetWidgetPath.add(element.asInt());
            }
        }

        Action action = new Action(rootWidget, targetWidgetPath, actionType);

        // swipe?
        if (!node.get("swipe").isNull()){
            Point initial = new Point(node.get("swipe").get("initialPosition").get("x").asInt(), node.get("swipe").get("initialPosition").get("y").asInt());
            Point finalP = new Point(node.get("swipe").get("finalPosition").get("x").asInt(), node.get("swipe").get("finalPosition").get("y").asInt());
            action.setSwipe(new Swipe(initial, finalP, 15));
        }

        // extra info?
        final JsonNode extraInfo = node.get("extraInfo");
        if (extraInfo != null){
            action.setExtraInfo(extraInfo.asText());
        }

        // networking info?
        final JsonNode networkingInfoArray = node.get("networkingInfo");
        List<String> networkingInfo = new ArrayList<>();
        if (networkingInfoArray != null){
            for (Iterator<JsonNode> it = networkingInfoArray.elements(); it.hasNext(); ) {
                JsonNode element = it.next();
                networkingInfo.add(element.asText());
            }
        }
        action.setNetworkingInfo(networkingInfo);


        return action;
    }
}
