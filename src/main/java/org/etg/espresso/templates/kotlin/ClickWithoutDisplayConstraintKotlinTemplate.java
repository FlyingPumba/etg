package org.etg.espresso.templates.kotlin;

import org.etg.espresso.templates.VelocityTemplate;

public class ClickWithoutDisplayConstraintKotlinTemplate implements VelocityTemplate {

    @Override
    public String getName() {
        return "ClickWithoutDisplayConstraint.kt";
    }
    
    @Override
    public String getRelativePath() {
        return "";
    }

    public String getAsRawString() {
        return "package ${PackageName}\n" +
                "\n" +
                "import org.hamcrest.Matchers.allOf\n" +
                "\n" +
                "import android.util.Log\n" +
                "import android.view.View\n" +
                "import android.view.ViewConfiguration\n" +
                "import android.webkit.WebView\n" +
                "import ${EspressoPackageName}.espresso.PerformException\n" +
                "import ${EspressoPackageName}.espresso.UiController\n" +
                "import ${EspressoPackageName}.espresso.ViewAction\n" +
                "import ${EspressoPackageName}.espresso.action.CoordinatesProvider\n" +
                "import ${EspressoPackageName}.espresso.action.PrecisionDescriber\n" +
                "import ${EspressoPackageName}.espresso.action.Tap\n" +
                "import ${EspressoPackageName}.espresso.action.Tapper\n" +
                "import ${EspressoPackageName}.espresso.util.HumanReadables\n" +
                "import ${PackageName}.VisibleViewMatcher.Companion.isVisible\n" +
                "import java.util.Locale\n" +
                "import java.util.Optional\n" +
                "\n" +
                "import org.hamcrest.Matcher\n" +
                "\n" +
                "private val TAG: String = \"ClickWithoutDisplayConstraint\"\n" +
                "\n" +
                "class ClickWithoutDisplayConstraint : ViewAction {\n" +
                "\n" +
                "    val coordinatesProvider: CoordinatesProvider\n" +
                "    val tapper: Tapper\n" +
                "    val precisionDescriber: PrecisionDescriber\n" +
                "    private val rollbackAction: Optional<ViewAction>\n" +
                "    private val inputDevice: Int\n" +
                "    private val buttonState: Int\n" +
                "\n" +
                "    constructor(tapper: Tapper,\n" +
                "                coordinatesProvider: CoordinatesProvider,\n" +
                "                precisionDescriber: PrecisionDescriber) :\n" +
                "            this(tapper, coordinatesProvider, precisionDescriber, 0, 0, null)\n" +
                "\n" +
                "    constructor(tapper: Tapper,\n" +
                "                coordinatesProvider: CoordinatesProvider,\n" +
                "                precisionDescriber: PrecisionDescriber,\n" +
                "                inputDevice: Int,\n" +
                "                buttonState: Int) :\n" +
                "            this(tapper, coordinatesProvider, precisionDescriber, inputDevice, buttonState, null)\n" +
                "\n" +
                "    constructor(tapper: Tapper,\n" +
                "                coordinatesProvider: CoordinatesProvider,\n" +
                "                precisionDescriber: PrecisionDescriber,\n" +
                "                rollbackAction: ViewAction) :\n" +
                "            this(tapper, coordinatesProvider, precisionDescriber, 0, 0, rollbackAction)\n" +
                "\n" +
                "    constructor(tapper: Tapper,\n" +
                "                coordinatesProvider: CoordinatesProvider,\n" +
                "                precisionDescriber: PrecisionDescriber,\n" +
                "                inputDevice: Int,\n" +
                "                buttonState: Int,\n" +
                "                rollbackAction: ViewAction?) {\n" +
                "        this.coordinatesProvider = coordinatesProvider\n" +
                "        this.tapper = tapper\n" +
                "        this.precisionDescriber = precisionDescriber\n" +
                "        this.inputDevice = inputDevice\n" +
                "        this.buttonState = buttonState\n" +
                "        this.rollbackAction = Optional.ofNullable(rollbackAction)\n" +
                "    }\n" +
                "\n" +
                "    override fun getConstraints(): Matcher<View> {\n" +
                "        val standardConstraint: Matcher<View> = isVisible()\n" +
                "        if (rollbackAction.isPresent()) {\n" +
                "            return allOf(standardConstraint, rollbackAction.get().getConstraints())\n" +
                "        } else {\n" +
                "            return standardConstraint\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    override fun getDescription(): String {\n" +
                "        return tapper.toString().toLowerCase() + \" click\"\n" +
                "    }\n" +
                "\n" +
                "    override fun perform(uiController: UiController, view: View) {\n" +
                "        val coordinates = coordinatesProvider.calculateCoordinates(view)\n" +
                "        val precision = precisionDescriber.describePrecision()\n" +
                "\n" +
                "        var status = Tapper.Status.FAILURE\n" +
                "        var loopCount = 0\n" +
                "\n" +
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
                "                status = tapper.sendTap(uiController, coordinates, precision, inputDevice, buttonState)\n" +
                "                if (Log.isLoggable(TAG, Log.DEBUG)) {\n" +
                "                    Log.d(\n" +
                "                            TAG,\n" +
                "                            \"perform: \"\n" +
                "                                    + String.format(\n" +
                "                                    Locale.ROOT,\n" +
                "                                    \"%s - At Coordinates: %d, %d and precision: %d, %d\",\n" +
                "                                    this.getDescription(),\n" +
                "                                    coordinates[0].toInt(),\n" +
                "                                    coordinates[1].toInt(),\n" +
                "                                    precision[0].toInt(),\n" +
                "                                    precision[1].toInt()))\n" +
                "                }\n" +
                "            } catch (re: RuntimeException) {\n" +
                "                throw PerformException.Builder()\n" +
                "                        .withActionDescription(\n" +
                "                                String.format(\n" +
                "                                        Locale.ROOT,\n" +
                "                                        \"%s - At Coordinates: %d, %d and precision: %d, %d\",\n" +
                "                                        this.getDescription(),\n" +
                "                                        coordinates[0].toInt(),\n" +
                "                                        coordinates[1].toInt(),\n" +
                "                                        precision[0].toInt(),\n" +
                "                                        precision[1].toInt()))\n" +
                "                        .withViewDescription(HumanReadables.describe(view))\n" +
                "                        .withCause(re)\n" +
                "                        .build()\n" +
                "            }\n" +
                "\n" +
                "            val duration = ViewConfiguration.getPressedStateDuration()\n" +
                "            // ensures that all work enqueued to process the tap has been run.\n" +
                "            if (duration > 0) {\n" +
                "                uiController.loopMainThreadForAtLeast(duration.toLong())\n" +
                "            }\n" +
                "            if (status == Tapper.Status.WARNING) {\n" +
                "                if (rollbackAction.isPresent()) {\n" +
                "                    rollbackAction.get().perform(uiController, view)\n" +
                "                } else {\n" +
                "                    break\n" +
                "                }\n" +
                "            }\n" +
                "            loopCount++\n" +
                "        }\n" +
                "        if (status == Tapper.Status.FAILURE) {\n" +
                "            throw PerformException.Builder()\n" +
                "                    .withActionDescription(this.getDescription())\n" +
                "                    .withViewDescription(HumanReadables.describe(view))\n" +
                "                    .withCause(\n" +
                "                            RuntimeException(\n" +
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
                "                    .build()\n" +
                "        }\n" +
                "\n" +
                "        if (tapper == Tap.SINGLE && view is WebView) {\n" +
                "            // WebViews will not process click events until double tap\n" +
                "            // timeout. Not the best place for this - but good for now.\n" +
                "            uiController.loopMainThreadForAtLeast(ViewConfiguration.getDoubleTapTimeout().toLong())\n" +
                "        }\n" +
                "    }\n" +
                "}";
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof ClickWithoutDisplayConstraintKotlinTemplate;
    }

    @Override
    public int hashCode() {
        return getAsRawString().hashCode();
    }


}
