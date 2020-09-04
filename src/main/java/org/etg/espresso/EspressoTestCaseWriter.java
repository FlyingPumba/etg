package org.etg.espresso;

import org.apache.velocity.VelocityContext;
import org.etg.espresso.templates.VelocityTemplate;
import org.etg.espresso.templates.VelocityTemplateConverter;
import org.etg.utils.ProcessRunner;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.etg.espresso.codegen.TestCodeMapper.getStatementTerminator;

public class EspressoTestCaseWriter {

    public enum Option {
        PRETTIFY,
        PRODUCE_SCREENSHOTS,
        SURROUND_WITH_TRY_CATCHS,
    }

    private EspressoTestCase espressoTestCase;
    private Set<Option> options;

    private EspressoTestCaseWriter(EspressoTestCase espressoTestCase) {
        this.espressoTestCase = espressoTestCase;
        this.options = new HashSet<>();
    }

    public static EspressoTestCaseWriter write(EspressoTestCase espressoTestCase) {
        return new EspressoTestCaseWriter(espressoTestCase);
    }

    public EspressoTestCaseWriter withOption(Option option) {
        options.add(option);
        return this;
    }

    public EspressoTestCaseWriter toProject() throws Exception {
        writeToFolder(espressoTestCase.getEtgProperties().getOutputPath());
        writeCustomUsedClasses(espressoTestCase.getEtgProperties().getOutputPath());
        return this;
    }

    public EspressoTestCaseWriter toResultsFolder() throws Exception {
        String testCaseResultFolderPath = String.format("%s/code/", espressoTestCase.getTestCaseResultsPath());
        ProcessRunner.runCommand(String.format("mkdir -p %s", testCaseResultFolderPath));

        writeToFolder(testCaseResultFolderPath);
        writeCustomUsedClasses(testCaseResultFolderPath);
        return this;
    }

    private void writeToFolder(String outputFolderPath) throws Exception {
        VelocityTemplateConverter templateConverter = new VelocityTemplateConverter(createVelocityContext());
        String testContent = templateConverter.applyContextToTemplate(espressoTestCase.getTestCaseTemplate());

        String outputFilePath = String.format("%s%s.%s", outputFolderPath, espressoTestCase.getTestName(),
                espressoTestCase.getEtgProperties().getOutputExtension());;

        PrintWriter out = new PrintWriter(new FileOutputStream(outputFilePath), true);
        out.print(testContent);
        out.close();

        if (options.contains(Option.PRETTIFY)) {
            // TODO: this is a hack, there has to be a better way
            String workingFolder = System.getProperty("user.dir");
            String googleBinPath = "bin/google-java-format-1.7-all-deps.jar";
            if (!workingFolder.endsWith("etg")) {
                googleBinPath = "etg/" + googleBinPath;
            }

            // run Google Java formatter on the output file
            String formatCmd = String.format("java -jar %s -i %s", googleBinPath, outputFilePath);
            ProcessRunner.runCommand(formatCmd);
        }
    }

    private void writeCustomUsedClasses(String outputFolderPath) throws Exception {
        VelocityTemplateConverter templateConverter = new VelocityTemplateConverter(createVelocityContext());
        for (VelocityTemplate vTemplate : espressoTestCase.getCodeMapper().getNeededTemplates()) {
            String classContent = templateConverter.applyContextToTemplate(vTemplate);
            String outputFilePath = outputFolderPath + vTemplate.getName();

            PrintWriter out = new PrintWriter(new FileOutputStream(outputFilePath), true);
            out.print(classContent);
            out.close();
        }
    }

    private VelocityContext createVelocityContext() throws Exception {
        VelocityContext velocityContext = new VelocityContext();

        velocityContext.put("TestActivityName", espressoTestCase.getEtgProperties().getMainActivity());
        velocityContext.put("PackageName", espressoTestCase.getEtgProperties().getTestPackageName());
        velocityContext.put("ResourcePackageName", espressoTestCase.getEtgProperties().getPackageName());

        // TODO: improve test name based on TestCase's visitedActivities
        velocityContext.put("ClassName", espressoTestCase.getTestName());
        velocityContext.put("TestMethodName", "myTestCase");

        velocityContext.put("EspressoPackageName", espressoTestCase.getEtgProperties().getEspressoPackageName());

        velocityContext.put("AddClassOrSuperClassesNameMethod", espressoTestCase.getCodeMapper().mIsclassOrSuperClassesNameAdded);
        velocityContext.put("AddTryCatchImport", options.contains(Option.SURROUND_WITH_TRY_CATCHS));
        velocityContext.put("AddScreenshotImport", options.contains(Option.PRODUCE_SCREENSHOTS));

        velocityContext.put("TestCode", getTestCodeLines());

        velocityContext.put("longClickActionAdded", espressoTestCase.getCodeMapper().longClickActionAdded);
        velocityContext.put("clickActionAdded", espressoTestCase.getCodeMapper().clickActionAdded);
        velocityContext.put("swipeActionAdded", espressoTestCase.getCodeMapper().swipeActionAdded);
        velocityContext.put("waitForAdded", espressoTestCase.getCodeMapper().waitActionAdded);


        return velocityContext;
    }

    public List<String> getTestCodeLines() {
        List<String> testCodeLines = new ArrayList<>();
        for (int i = 0; i < espressoTestCase.getWidgetActionsCount(); i++) {
            List<String> testCodeLinesForAction = new ArrayList<>(espressoTestCase.getTestCodeLinesForWidgetActionIndex(i));

            if (options.contains(Option.PRODUCE_SCREENSHOTS)) {
                addScreenshotCall(testCodeLinesForAction, i);
            }

            if (options.contains(Option.SURROUND_WITH_TRY_CATCHS)) {
                surroundLinesWithTryCatch(testCodeLinesForAction, i);
            }

            if (espressoTestCase.getFailingWidgetActionIndexes().contains(i)) {
                // comment out failing lines
                for (int j = 0; j < testCodeLinesForAction.size(); j++) {
                    String testCodeLine = testCodeLinesForAction.get(j);
                    String[] lines = testCodeLine.split("\n");
                    for (String line: lines) {
                        testCodeLines.add("// " + line);
                    }
                }
            } else {
                testCodeLines.addAll(testCodeLinesForAction);
            }
        }

        if (options.contains(Option.PRODUCE_SCREENSHOTS)) {
            List<String> finalScreenshotCall = new ArrayList<>();
            addScreenshotCall(finalScreenshotCall, espressoTestCase.getWidgetActionsCount());
            testCodeLines.addAll(finalScreenshotCall);
        }

        return testCodeLines;
    }

    private void addScreenshotCall(List<String> actionTestCodeLines, int index) {
        String screenshotCall = String.format("getScreenshot(%d)", index) + getStatementTerminator() + "\n";
        actionTestCodeLines.add(0, screenshotCall);
    }

    private void surroundLinesWithTryCatch(List<String> actionTestCodeLines, int index) {
        String tryStr = "\ntry {\n";
        String catchStr = "\n" +
                "} catch (Exception e) {\n" +
                "System.out.println(buildPerformExceptionMessage(e, " + index + "))" + getStatementTerminator() + "\n" +
                "}";

        actionTestCodeLines.add(0, tryStr);
        actionTestCodeLines.add(catchStr);
    }
}
