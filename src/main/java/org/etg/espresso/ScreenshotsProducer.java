package org.etg.espresso;

import org.etg.utils.ProcessRunner;

public class ScreenshotsProducer {
    private EspressoTestCase espressoTestCase;

    public ScreenshotsProducer(EspressoTestCase espressoTestCase) {
        this.espressoTestCase = espressoTestCase;
    }

    public void produce() throws Exception {
        removeFromDevice();

        EspressoTestCaseWriter.write(espressoTestCase)
                .withOption(EspressoTestCaseWriter.Option.PRODUCE_SCREENSHOTS)
                .withOption(EspressoTestCaseWriter.Option.SURROUND_WITH_TRY_CATCHS)
                .toProject();

        EspressoTestRunner.runTestCase(espressoTestCase, false);
    }

    public void dumpToResultsFolder() {
        String screenshotsFolderPath = String.format("%s/screenshots/", espressoTestCase.getTestCaseResultsPath());
        ProcessRunner.runCommand(String.format("rm -rf %s", screenshotsFolderPath));
        ProcessRunner.runCommand(String.format("mkdir -p %s", screenshotsFolderPath));

        String pullCmd = String.format("cd %s; adb shell 'ls /storage/emulated/0/Pictures/screenshots/*.png' | xargs -n1 adb pull",
                screenshotsFolderPath);
        ProcessRunner.runCommand(pullCmd);
    }

    public void removeFromDevice() {
        String rmCmd = "adb shell 'rm /storage/emulated/0/Pictures/screenshots/*.png'";
        ProcessRunner.runCommand(rmCmd);
    }
}
