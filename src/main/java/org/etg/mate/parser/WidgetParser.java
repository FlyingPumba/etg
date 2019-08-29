package org.etg.mate.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etg.mate.models.Widget;

import java.util.Iterator;

public class WidgetParser {
    public static Widget parse(ObjectMapper mapper, JsonNode node) {
        Widget widget = new Widget(mapper.convertValue(node.get("id"), String.class),
                mapper.convertValue(node.get("clazz"), String.class),
                mapper.convertValue(node.get("idByActivity"), String.class));

        widget.setParent(null);

        // Boolean
        widget.setCheckable(mapper.convertValue(node.get("checkable"), Boolean.class));
        widget.setChecked(mapper.convertValue(node.get("checked"), Boolean.class));
        widget.setEnabled(mapper.convertValue(node.get("enabled"), Boolean.class));
        widget.setFocusable(mapper.convertValue(node.get("focusable"), Boolean.class));
        widget.setScrollable(mapper.convertValue(node.get("scrollable"), Boolean.class));
        widget.setClickable(mapper.convertValue(node.get("clickable"), Boolean.class));
        widget.setLongClickable(mapper.convertValue(node.get("longClickable"), Boolean.class));
        widget.setPassword(mapper.convertValue(node.get("password"), Boolean.class));
        widget.setSelected(mapper.convertValue(node.get("selected"), Boolean.class));

        // Integer
        widget.setIndex(mapper.convertValue(node.get("index"), Integer.class));
        widget.setX(mapper.convertValue(node.get("x"), Integer.class));
        widget.setY(mapper.convertValue(node.get("y"), Integer.class));
        widget.setX1(mapper.convertValue(node.get("x1"), Integer.class));
        widget.setX2(mapper.convertValue(node.get("x2"), Integer.class));
        widget.setY1(mapper.convertValue(node.get("y1"), Integer.class));
        widget.setY2(mapper.convertValue(node.get("y2"), Integer.class));
        widget.setMaxLength(mapper.convertValue(node.get("maxLength"), Integer.class));

        // String
        widget.setText(mapper.convertValue(node.get("text"), String.class));
        widget.setResourceID(mapper.convertValue(node.get("resourceID"), String.class));
        widget.setPackageName(mapper.convertValue(node.get("packageName"), String.class));
        widget.setContentDesc(mapper.convertValue(node.get("contentDesc"), String.class));
        widget.setLabeledBy(mapper.convertValue(node.get("labeledBy"), String.class));
        widget.setBounds(mapper.convertValue(node.get("bounds"), String.class));

        // children
        JsonNode childrenNode = node.get("children");

        for (Iterator<JsonNode> it = childrenNode.elements(); it.hasNext(); ) {
            JsonNode element = it.next();
            widget.setHasChildren(true);

            Widget child = WidgetParser.parse(mapper, element);
            child.setParent(widget);

            widget.addChild(child);
        }

        return widget;
    }
}
