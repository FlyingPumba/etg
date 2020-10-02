package org.etg.espresso.templates.java;

import org.etg.espresso.templates.VelocityTemplate;

public class ClickWithoutDisplayConstraintJavaTemplate implements VelocityTemplate {

    @Override
    public String getName() {
        return "ClickWithoutDisplayConstraint.java";
    }

    @Override
    public String getRelativePath() {
        return "";
    }

    public String getAsRawString() {
        return "#if (${PackageName} && ${PackageName} != \"\")\n" +
                "package ${PackageName};\n" +
                "\n" +
                "#end\n" +
                "import static ${PackageName}.VisibleViewMatcher.isVisible;\n" +
                "import static org.hamcrest.Matchers.allOf;\n" +
                "\n" +
                "import android.util.Log;\n" +
                "import android.view.View;\n" +
                "import android.view.ViewConfiguration;\n" +
                "import android.webkit.WebView;\n" +
                "import ${EspressoPackageName}.espresso.PerformException;\n" +
                "import ${EspressoPackageName}.espresso.UiController;\n" +
                "import ${EspressoPackageName}.espresso.ViewAction;\n" +
                "import ${EspressoPackageName}.espresso.action.CoordinatesProvider;\n" +
                "import ${EspressoPackageName}.espresso.action.PrecisionDescriber;\n" +
                "import ${EspressoPackageName}.espresso.action.Tap;\n" +
                "import ${EspressoPackageName}.espresso.action.Tapper;\n" +
                "import ${EspressoPackageName}.espresso.util.HumanReadables;\n" +
                "import java.util.Locale;\n" +
                "import java.util.Optional;\n" +
                "\n" +
                "import org.hamcrest.Matcher;\n" +
                "\n" +
                "\n" +
                "public final class ClickWithoutDisplayConstraint implements ViewAction {\n" +
                "    private static final String TAG = \"ClickWithoutDisplayConstraint\";\n" +
                "\n" +
                "    final CoordinatesProvider coordinatesProvider;\n" +
                "    final Tapper tapper;\n" +
                "    final PrecisionDescriber precisionDescriber;\n" +
                "    private final Optional<ViewAction> rollbackAction;\n" +
                "    private final int inputDevice;\n" +
                "    private final int buttonState;\n" +
                "\n" +
                "    \n" +
                "    @Deprecated\n" +
                "    public ClickWithoutDisplayConstraint(\n" +
                "            Tapper tapper,\n" +
                "            CoordinatesProvider coordinatesProvider,\n" +
                "            PrecisionDescriber precisionDescriber) {\n" +
                "        this(tapper, coordinatesProvider, precisionDescriber, 0, 0, null);\n" +
                "    }\n" +
                "\n" +
                "    public ClickWithoutDisplayConstraint(\n" +
                "            Tapper tapper,\n" +
                "            CoordinatesProvider coordinatesProvider,\n" +
                "            PrecisionDescriber precisionDescriber,\n" +
                "            int inputDevice,\n" +
                "            int buttonState) {\n" +
                "        this(tapper, coordinatesProvider, precisionDescriber, inputDevice, buttonState, null);\n" +
                "    }\n" +
                "    \n" +
                "    @Deprecated\n" +
                "    public ClickWithoutDisplayConstraint(\n" +
                "            Tapper tapper,\n" +
                "            CoordinatesProvider coordinatesProvider,\n" +
                "            PrecisionDescriber precisionDescriber,\n" +
                "            ViewAction rollbackAction) {\n" +
                "        this(tapper, coordinatesProvider, precisionDescriber, 0, 0, rollbackAction);\n" +
                "    }\n" +
                "\n" +
                "    public ClickWithoutDisplayConstraint(\n" +
                "            Tapper tapper,\n" +
                "            CoordinatesProvider coordinatesProvider,\n" +
                "            PrecisionDescriber precisionDescriber,\n" +
                "            int inputDevice,\n" +
                "            int buttonState,\n" +
                "            ViewAction rollbackAction) {\n" +
                "        this.coordinatesProvider = coordinatesProvider;\n" +
                "        this.tapper = tapper;\n" +
                "        this.precisionDescriber = precisionDescriber;\n" +
                "        this.inputDevice = inputDevice;\n" +
                "        this.buttonState = buttonState;\n" +
                "        this.rollbackAction = Optional.ofNullable(rollbackAction);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    @SuppressWarnings(\"unchecked\")\n" +
                "    public Matcher<View> getConstraints() {\n" +
                "        Matcher<View> standardConstraint = isVisible();\n" +
                "        if (rollbackAction.isPresent()) {\n" +
                "            return allOf(standardConstraint, rollbackAction.get().getConstraints());\n" +
                "        } else {\n" +
                "            return standardConstraint;\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void perform(UiController uiController, View view) {\n" +
                "        float[] coordinates = coordinatesProvider.calculateCoordinates(view);\n" +
                "        float[] precision = precisionDescriber.describePrecision();\n" +
                "\n" +
                "        Tapper.Status status = Tapper.Status.FAILURE;\n" +
                "        int loopCount = 0;\n" +
                "        // Native event injection is quite a tricky process. A tap is actually 2\n" +
                "        // seperate motion events which need to get injected into the system. Injection\n" +
                "        // makes an RPC call from our app under test to the Android system server, the\n" +
                "        // system server decides which window layer to deliver the event to, the system\n" +
                "        // server makes an RPC to that window layer, that window layer delivers the event\n" +
                "        // to the correct UI element, activity, or window object. Now we need to repeat\n" +
                "        // that 2x. for a simple down and up. Oh and the down event triggers timers to\n" +
                "        // detect whether or not the event is a long vs. short press. The timers are\n" +
                "        // removed the moment the up event is received (NOTE: the possibility of eventTime\n" +
                "        // being in the future is totally ignored by most motion event processors).\n" +
                "        //\n" +
                "        // Phew.\n" +
                "        //\n" +
                "        // The net result of this is sometimes we'll want to do a regular tap, and for\n" +
                "        // whatever reason the up event (last half) of the tap is delivered after long\n" +
                "        // press timeout (depending on system load) and the long press behaviour is\n" +
                "        // displayed (EG: show a context menu). There is no way to avoid or handle this more\n" +
                "        // gracefully. Also the longpress behavour is app/widget specific. So if you have\n" +
                "        // a seperate long press behaviour from your short press, you can pass in a\n" +
                "        // 'RollBack' ViewAction which when executed will undo the effects of long press.\n" +
                "\n" +
                "        while (status != Tapper.Status.SUCCESS && loopCount < 3) {\n" +
                "            try {\n" +
                "                status = tapper.sendTap(uiController, coordinates, precision, inputDevice, buttonState);\n" +
                "                if (Log.isLoggable(TAG, Log.DEBUG)) {\n" +
                "                    Log.d(\n" +
                "                            TAG,\n" +
                "                            \"perform: \"\n" +
                "                                    + String.format(\n" +
                "                                    Locale.ROOT,\n" +
                "                                    \"%s - At Coordinates: %d, %d and precision: %d, %d\",\n" +
                "                                    this.getDescription(),\n" +
                "                                    (int) coordinates[0],\n" +
                "                                    (int) coordinates[1],\n" +
                "                                    (int) precision[0],\n" +
                "                                    (int) precision[1]));\n" +
                "                }\n" +
                "            } catch (RuntimeException re) {\n" +
                "                throw new PerformException.Builder()\n" +
                "                        .withActionDescription(\n" +
                "                                String.format(\n" +
                "                                        Locale.ROOT,\n" +
                "                                        \"%s - At Coordinates: %d, %d and precision: %d, %d\",\n" +
                "                                        this.getDescription(),\n" +
                "                                        (int) coordinates[0],\n" +
                "                                        (int) coordinates[1],\n" +
                "                                        (int) precision[0],\n" +
                "                                        (int) precision[1]))\n" +
                "                        .withViewDescription(HumanReadables.describe(view))\n" +
                "                        .withCause(re)\n" +
                "                        .build();\n" +
                "            }\n" +
                "\n" +
                "            int duration = ViewConfiguration.getPressedStateDuration();\n" +
                "            // ensures that all work enqueued to process the tap has been run.\n" +
                "            if (duration > 0) {\n" +
                "                uiController.loopMainThreadForAtLeast(duration);\n" +
                "            }\n" +
                "            if (status == Tapper.Status.WARNING) {\n" +
                "                if (rollbackAction.isPresent()) {\n" +
                "                    rollbackAction.get().perform(uiController, view);\n" +
                "                } else {\n" +
                "                    break;\n" +
                "                }\n" +
                "            }\n" +
                "            loopCount++;\n" +
                "        }\n" +
                "        if (status == Tapper.Status.FAILURE) {\n" +
                "            throw new PerformException.Builder()\n" +
                "                    .withActionDescription(this.getDescription())\n" +
                "                    .withViewDescription(HumanReadables.describe(view))\n" +
                "                    .withCause(\n" +
                "                            new RuntimeException(\n" +
                "                                    String.format(\n" +
                "                                            Locale.ROOT,\n" +
                "                                            \"Couldn't click at: %s,%s precision: %s, %s . Tapper: %s coordinate\"\n" +
                "                                                    + \" provider: %s precision describer: %s. Tried %s times. With Rollback?\"\n" +
                "                                                    + \" %s\",\n" +
                "                                            coordinates[0],\n" +
                "                                            coordinates[1],\n" +
                "                                            precision[0],\n" +
                "                                            precision[1],\n" +
                "                                            tapper,\n" +
                "                                            coordinatesProvider,\n" +
                "                                            precisionDescriber,\n" +
                "                                            loopCount,\n" +
                "                                            rollbackAction.isPresent())))\n" +
                "                    .build();\n" +
                "        }\n" +
                "\n" +
                "        if (tapper == Tap.SINGLE && view instanceof WebView) {\n" +
                "            // WebViews will not process click events until double tap\n" +
                "            // timeout. Not the best place for this - but good for now.\n" +
                "            uiController.loopMainThreadForAtLeast(ViewConfiguration.getDoubleTapTimeout());\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public String getDescription() {\n" +
                "        return tapper.toString().toLowerCase() + \" click\";\n" +
                "    }\n" +
                "}";
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof ClickWithoutDisplayConstraintJavaTemplate;
    }

    @Override
    public int hashCode() {
        return getAsRawString().hashCode();
    }


}
