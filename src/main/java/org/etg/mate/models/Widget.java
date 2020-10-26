package org.etg.mate.models;

import java.lang.reflect.Array;
import java.util.*;

public class Widget {

    private Widget parent;
    private String id;
    private String idByActivity;
    private String clazz;
    private String text;
    private String resourceID;
    private int index;
    private String packageName;
    private String contentDesc;
    private String labeledBy;
    private boolean checkable;
    private boolean checked;
    private boolean enabled;
    private boolean focusable;
    private boolean scrollable;
    private boolean clickable;
    private boolean longClickable;
    private boolean password;
    private boolean selected;
    private boolean displayed;
    private boolean androidView;
    private String bounds;
    private String originalBounds;
    private int X;
    private int Y;
    private int x1;
    private int x2;
    private int y1;
    private int y2;
    private int maxLength;

    private int inputType;
    private boolean hasChildren;

    private Vector<Widget> children;

    private boolean usedAsStateDiff;

    private String hint;
    private Vector<Integer> widgetPath = new Vector<>();

    public Widget(String id, String clazz, String idByActivity) {
        setId(id);
        setClazz(clazz);
        originalBounds = "";
        setBounds("[0,0][0,0]");
        setContentDesc("");
        setText("");
        children = new Vector<Widget>();
        maxLength = -1;
        this.idByActivity = idByActivity;
        usedAsStateDiff = false;
        hint = "";

    }

    public boolean isHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public Widget getParent() {
        return parent;
    }

    public void setParent(Widget parent) {
        this.parent = parent;
        parent.addChild(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getContentDesc() {
        return contentDesc;
    }

    public void setContentDesc(String contentDesc) {
        this.contentDesc = contentDesc;
    }

    public boolean isCheckable() {
        return checkable;
    }

    public void setCheckable(boolean checkable) {
        this.checkable = checkable;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFocusable() {
        return focusable;
    }

    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    public boolean isScrollable() {
        return scrollable;
    }

    public void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
    }

    public boolean isClickable() {
        return clickable;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    public boolean isLongClickable() {
        return longClickable;
    }

    public void setLongClickable(boolean longClickable) {
        this.longClickable = longClickable;
    }

    public boolean isPassword() {
        return password;
    }

    public void setPassword(boolean password) {
        this.password = password;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getBounds() {
        return bounds;
    }

    public int getX() {
        return X;
    }

    public void setX(int x) {
        X = x;
    }

    public int getY() {
        return Y;
    }

    public void setY(int y) {
        Y = y;
    }

    public boolean isEditable() {

        if (clazz.contains("android.widget.EditText"))
            return true;
        if (clazz.contains("AppCompatEditText"))
            return true;
        if (clazz.contains("AutoCompleteTextView"))
            return true;
        if (clazz.contains("ExtractEditText"))
            return true;
        if (clazz.contains("GuidedActionEditText"))
            return true;
        if (clazz.contains("SearchEditText"))
            return true;
        if (clazz.contains("AppCompatAutoCompleteTextView"))
            return true;
        if (clazz.contains("AppCompatMultiAutoCompleteTextView"))
            return true;
        if (clazz.contains("MultiAutoCompleteTextView"))
            return true;
        if (clazz.contains("TextInputEditText"))
            return true;
        return false;
    }

    public boolean isExecutable() {
        return clickable || longClickable || scrollable || isEditable();
    }

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getX2() {
        return x2;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }

    public int getY1() {
        return y1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    public int getY2() {
        return y2;
    }

    public void setY2(int y2) {
        this.y2 = y2;
    }

    public void setBounds(String bounds) {
        this.bounds = bounds;

        if (originalBounds != null && originalBounds.equals(""))
            originalBounds = bounds;

        String value = bounds;
        value = value.replace("][", "|");
        value = value.replace("[", "");
        value = value.replace("]", "");
        String[] twoPos = value.split("\\|");
        String[] first = twoPos[0].split(",");
        String[] second = twoPos[1].split(",");
        x1 = Integer.valueOf(first[0]);
        y1 = Integer.valueOf(first[1]);

        x2 = Integer.valueOf(second[0]);
        y2 = Integer.valueOf(second[1]);

        setX((x1 + x2) / 2);
        setY((y1 + y2) / 2);
    }

    public boolean directSonOf(String type) {
        Widget wparent = this.parent;
        if (wparent != null)
            if (wparent.getClazz().contains(type))
                return true;
        return false;
    }

    public boolean isSonOf(String type) {
        Widget wparent = this.parent;
        while (wparent != null) {
            if (wparent.getClazz().contains(type))
                return true;
            else
                wparent = wparent.getParent();
        }
        return false;
    }

    public Vector<Widget> getNextChildWithText() {
        Vector<Widget> ws = new Vector<Widget>();
        for (Widget child : children) {
            //System.out.println("has children: " + child.getText());
            if (!child.getText().equals("")) {
                ws.add(child);
            }
            ws.addAll(child.getNextChildWithText());
        }

        return ws;
    }

    public Vector<Widget> getNextChildWithDescContentText() {
        Vector<Widget> ws = new Vector<Widget>();

        for (Widget child : children) {
            //System.out.println("has children: " + child.getText());
            if (!child.getContentDesc().equals("")) {
                ws.add(child);

            }
            ws.addAll(child.getNextChildWithDescContentText());
        }

        return ws;
    }

    public void addChild(Widget widget) {
        if (!children.contains(widget)) {
            children.add(widget);
            widget.setParent(this);
        }
    }

    public String getNextChildsText() {
        String childText = "";
        for (Widget wg : getNextChildWithText())
            childText += wg.getText() + " ";
        return childText;
    }

    public String getIdByActivity() {
        return idByActivity;
    }

    public void setIdByActivity(String idByActivity) {
        this.idByActivity = idByActivity;
    }

    public String getResourceID() {
        return resourceID;
    }

    public void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }

    public boolean isSonOfLongClickable() {
        Widget wparent = this.parent;
        while (wparent != null) {
            if (wparent.isLongClickable())
                return true;
            else
                wparent = wparent.getParent();
        }
        return false;
    }

    public boolean isSonOfScrollable() {
        Widget parent = this.parent;
        while (parent != null) {
            if ("android.widget.ScrollView".equals(parent.clazz) ||
                    "android.widget.HorizontalScrollView".equals(parent.clazz) ||
                    "android.widget.ListView".equals(parent.clazz))
                return true;
            else
                parent = parent.getParent();
        }
        return false;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public int getInputType() {
        return inputType;
    }

    public void setInputType(int inputType) {
        this.inputType = inputType;
    }

    public boolean isUsedAsStateDiff() {
        return usedAsStateDiff;
    }

    public void setUsedAsStateDiff(boolean usedAsStateDiff) {
        this.usedAsStateDiff = usedAsStateDiff;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getHint() {
        return hint;
    }

    public boolean isEmpty() {
        if (hint.equals(text))
            return true;
        if (text.equals(""))
            return true;
        return false;
    }

    public void setLabeledBy(String labeledBy) {
        this.labeledBy = labeledBy;
    }

    public String getLabeledBy() {
        return this.labeledBy;
    }

    public boolean needsContrastChecked() {
        Set<String> excludedClasses = new HashSet<String>();
        excludedClasses.add("Layout");
        excludedClasses.add("ViewGroup");
        excludedClasses.add("ScrollView");
        excludedClasses.add("Spinner");
        excludedClasses.add("TableRow");
        excludedClasses.add("ListView");
        excludedClasses.add("GridView");

        if (this.bounds.equals("[0,0][0,0]"))
            return false;

        if (this.isEditable() && this.text.equals(""))
            return false;

        if (this.getClazz().contains("Text") && this.getText().equals(""))
            return false;

        if (this.getClazz().contains("Image") && !this.isExecutable())
            return false;

        for (String excluded : excludedClasses) {
            if (this.clazz.contains(excluded))
                return false;
        }
        if (!this.isExecutable() && !this.getClazz().contains("Text"))
            return false;


        return true;
    }

    public Vector<Widget> getChildren() {
        return children;
    }

    public int getRecyclerViewChildPosition() {
        if (parent == null || !parent.getClazz().equals("android.support.v7.widget.RecyclerView")) {
            return -1;
        }

        return parent.getChildren().indexOf(this);
    }

    public int getAdapterViewChildPosition() {
        if (getRecyclerViewChildPosition() != -1 || parent == null || !parent.getClazz().contains("Adapter")) {
            return -1;
        }

        return parent.getChildren().indexOf(this);
    }

    public int getGroupViewChildPosition() {
        if (getAdapterViewChildPosition() != -1 || parent == null || !parent.getClazz().contains("ViewGroup")) {
            return -1;
        }

        // the following does not work reliably, so I'm turning it off in the meantime.
        // return parent.getChildren().indexOf(this);
        return -1;
    }

    public Widget getChildrenWithRId() {
        Vector<Widget> children = getChildren();
        for (Widget child : children) {
            // first, try to find a leaf element that has R.id in the hierarchy of my child
            Widget childrenWithRId = child.getChildrenWithRId();
            if (childrenWithRId != null) {
                return childrenWithRId;
            }

            // if unsuccessful, check whether the child is itself a leaf that has R.id
            if (child.getChildren().isEmpty() && child.getId().contains("R.id")) {
                return child;
            }
        }
        // otherwise, return null
        return null;
    }

    public Widget getChildrenWithContentDescriptionOrText() {
        Vector<Widget> children = getChildren();
        for (Widget child : children) {
            // first, try to find a leaf element that has content description or text in the hierarchy of my child
            Widget childrenWithSomeText = child.getChildrenWithContentDescriptionOrText();
            if (childrenWithSomeText != null) {
                return childrenWithSomeText;
            }

            // if unsuccessful, check whether the child is itself a leaf that has content description or text
            if (child.getChildren().isEmpty() &&
                    (!child.getContentDesc().isEmpty() || !child.getText().isEmpty())) {
                return child;
            }
        }
        // otherwise, return null
        return null;
    }


    public Widget getReceiverOfClickInCoordinates(int x, int y){
        for(Widget child: children){
            Widget receiver = child.getReceiverOfClickInCoordinates(x, y);
            if (receiver != null) return receiver;
        }

        if (receivesClickOnCoordinates(x, y)) return this;
        else return null;
    }


    private boolean receivesClickOnCoordinates(int x, int y){
        return isInRange(x, x1, x2) && isInRange(y, y1, y2);
    }


    private boolean isInRange(int c, int c1, int c2){
        return (c1 <= c && c <= c2) || (c2 <= c && c <= c1);
    }

    public Vector<Integer> getWidgetPath() {
        return widgetPath;
    }

    public void setWidgetPath(Vector<Integer> widgetPath) {
        this.widgetPath = widgetPath;
    }

    public boolean isDisplayed() {
        return displayed;
    }

    public void setIsDisplayed(boolean displayed) {
        this.displayed = displayed;
    }

    public boolean isAndroidView() {
        return androidView;
    }

    public void setIsAndroidView(boolean androidView) {
        this.androidView = androidView;
    }

    public Widget getWidgetByRelativePath(String relativePath) {
        if (relativePath.isEmpty()) {
            return this;
        }

        String[] indexes = relativePath.split(",");
        Widget current = this;
        for (String strIndex : indexes) {
            int index = Integer.parseInt(strIndex);
            if (index < 0) {
                // we are going up one level
                if (current.getParent() != null) {
                    current = current.getParent();
                } else {
                    // we reached a dead end using this relative path from this widget
                    return null;
                }
            } else {
                // we are going down one level
                if (current.getChildren().size() > index) {
                    current = current.getChildren().elementAt(index);
                } else {
                    // we reached a dead end using this relative path from this widget
                    return null;
                }
            }
        }
        return current;
    }

    /**
     * This method computes the relative path between this widget and another one.
     * It does so by looking at the absolute path of both and checking if there are matching prefixes.
     * @param otherWidget
     * @return
     */
    public String getRelativePathToWidget(Widget otherWidget) {
        Vector<Integer> ourPath = new Vector<>(this.widgetPath);
        Vector<Integer> theirPath = new Vector<>(otherWidget.getWidgetPath());

        // find common prefix length
        int commonPrefixLength = 0;
        for (int i = 0; i < Math.min(ourPath.size(), theirPath.size()); i++) {
            Integer ourIndex = ourPath.get(i);
            Integer theirIndex = theirPath.get(i);

            if (ourIndex.equals(theirIndex)) {
                commonPrefixLength += 1;
            } else {
                break;
            }
        }

        // Let's build relative path
        Vector<String> relativePath = new Vector<>();
        // first, we go up on our path until we reach the beginning of the common prefix
        for (int i = ourPath.size() - 1; i >= commonPrefixLength; i--) {
            relativePath.add("-1");
        }
        // then we go down on the path of the other widget
        for (int i = commonPrefixLength; i < theirPath.size(); i++) {
            relativePath.add(theirPath.get(i).toString());
        }

        return String.join(",", relativePath);
    }
}
