package org.etg.espresso;

import org.etg.ETGProperties;
import org.etg.utils.ProcessRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.etg.espresso.EspressoTestRunner.prepareForTestRun;

public class Coverage {
    static double getTestCoverage(ETGProperties properties, EspressoTestCase espressoTestCase) throws Exception {
        prepareForTestRun(properties);
        String coverageSrcFolderPath = prepareForTestCoverage(properties);

        // delete previous coverage reports
        String rmCmd = String.format("find %s -type d -name coverage -exec rm -r {} +", properties.getApplicationFolderPath());
        ProcessRunner.runCommand(rmCmd);

        // generate a new one
        String createReportCmd = String.format("%sgradlew -p %s createDebugCoverageReport " +
                        "-Pandroid.testInstrumentationRunnerArguments.class=%s.%s",
                properties.getRootProjectPath(), properties.getRootProjectPath(),
                properties.getCompiledPackageName(), espressoTestCase.getTestName());
        String createReportCmdResult = ProcessRunner.runCommand(createReportCmd);

        // find where was located the .ec file
        String findCoverageEcCmd = String.format("find %s -type f -name \"*.ec\"", properties.getApplicationFolderPath());
        String coverageEcPath = ProcessRunner.runCommand(findCoverageEcCmd);
        if (coverageEcPath.trim().isEmpty()) {
            throw new Exception("Unable to find coverage.ec file for Test: " + espressoTestCase.getTestName()
                    + "\n" + createReportCmdResult);
        }

        // Build the Jacoco report using the coverage.ec file just obtained and the classes and source files prepared before
        ProcessRunner.runCommand(String.format("java -jar bin/jacococli.jar report \"%s\" " +
                "--classfiles %s/classes " +
                "--sourcefiles %s/java " +
                "--xml %s/jacoco_report.xml " +
                "--html %s/jacoco_html_report",
                coverageEcPath, coverageSrcFolderPath, coverageSrcFolderPath,
                coverageSrcFolderPath, coverageSrcFolderPath));

        // Get the total percentage of statements covered using the html in the report
        String indexHtmlPath = String.format("%s/jacoco_html_report/index.html", coverageSrcFolderPath);
        String xpathMissedLines = "html/body/table/tfoot/tr/td[8]/text()";
        String xpathMissedLinesCmd = String.format("xmllint --html -xpath \"%s\" %s", xpathMissedLines, indexHtmlPath);
        String missedLinesStr = ProcessRunner.runCommand(xpathMissedLinesCmd);

        String xpathTotalLines = "html/body/table/tfoot/tr/td[9]/text()";
        String xpathTotalLinesCmd = String.format("xmllint --html -xpath \"%s\" %s", xpathTotalLines, indexHtmlPath);
        String totalLinesStr = ProcessRunner.runCommand(xpathTotalLinesCmd);

        double missedLines = Double.parseDouble(missedLinesStr);
        double totalLines = Double.parseDouble(totalLinesStr);
        double coveredLines = totalLines - missedLines;

        return coveredLines/totalLines;
    }

    /**
     * This method takes care of preparing both sources and class files that will be used by Jacoco report generator
     * @param properties
     * @throws Exception
     * @return
     */
    private static String prepareForTestCoverage(ETGProperties properties) throws Exception {
        String coverageSrcFolderPath = String.format("%s.src/", properties.getCompiledPackageName());
        ProcessRunner.runCommand(String.format("rm -rf %s", coverageSrcFolderPath));
        ProcessRunner.runCommand(String.format("mkdir %s", coverageSrcFolderPath));

        String[] classFiles = ProcessRunner.runCommand(
                String.format("find %s -name \"*.class\" -type f",
                properties.getApplicationFolderPath())).split("\n");

        String buildVariant = "debug";
        String[] productFlavors = properties.getProductFlavors();
        if (productFlavors.length > 0) {
            StringBuilder productFlavorsCombined = new StringBuilder();
            for (int i = 0; i < productFlavors.length; i++) {
                String flavor = productFlavors[i];
                if (i == 0) {
                    productFlavorsCombined.append(flavor.toLowerCase());
                } else {
                    String cap = flavor.substring(0, 1).toUpperCase() + flavor.substring(1);
                    productFlavorsCombined.append(cap);
                }
            }

            String cap = properties.getBuildType().substring(0, 1).toUpperCase() +
                    properties.getBuildType().substring(1);
            buildVariant = productFlavorsCombined + cap;
        }

        List<String> filteredClassFiles = new ArrayList<>();
        List<String> filteredClassFileNames = new ArrayList<>();

        for (String classFile : classFiles) {
            if (classFile.contains(buildVariant)
                && !classFile.contains("AndroidTest")
                && !classFile.contains("UnitTest")
                && !classFile.contains("R$")
                && !classFile.contains("R.class")
                && !classFile.contains("BuildConfig.class")
                && !classFile.contains("/EmmaInstrument/")
                && !classFile.contains("/android/")
                && !classFile.contains("/jacoco/")
                && !classFile.contains("/kapt3/")
            ) {
                filteredClassFiles.add(classFile);
            }
        }

        String coverageClassesFolderPath = String.format("%sclasses/", coverageSrcFolderPath);
        ProcessRunner.runCommand(String.format("mkdir -p %s", coverageClassesFolderPath));

        // Proceed to find the root folder where package name start for each class file. For example:
        // 'subjects/com.a42crash.iarcuschin.a42crash/app/build/tmp/kotlin-classes/debug/com/a42crash/iarcuschin/a42crash/MainActivity.class'
        // -> We should copy from the /debug/ folder onwards
        // 'subjects/com.a42crash.iarcuschin.a42crash/app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/a42crash/iarcuschin/a42crash/MainActivity_ViewBinding.class'
        // -> We should copy from the /classes/ folder onwards
        List<String> classFolders = new ArrayList<>();
        String packageNamePath = String.join("/", properties.getPackageName().split("\\."));
        for (String classFile : filteredClassFiles) {
            File aux = new File(classFile);
            String classFileFolder = aux.getParentFile().getAbsolutePath();

            filteredClassFileNames.add(aux.getName());

            if (classFileFolder.endsWith(packageNamePath)) {
                String classFolder = classFileFolder.split(packageNamePath)[0];

                if (!classFolders.contains(classFolder)) {
                    classFolders.add(classFolder);
                }
            }
        }

        // copy class folders with their directory structure
        for (String classFolder : classFolders) {
            ProcessRunner.runCommand(String.format("rsync -a %s/ %s", classFolder, coverageClassesFolderPath));
        }

        // delete all files we were not supposed to copy
        String[] copiedFiles = ProcessRunner.runCommand(String.format("find %s -type f", coverageClassesFolderPath))
                .split("\n");

        StringBuilder filesToDelete = new StringBuilder();
        for (String copiedFile : copiedFiles) {
            if (copiedFile.isEmpty()) {
                continue;
            }

            File aux = new File(copiedFile);
            String copiedFileName = aux.getName();
            if (!filteredClassFileNames.contains(copiedFileName)) {
                // we should delete this file
                String aux2 = copiedFile.replace("$", "\\$");
                filesToDelete.append(" \"");
                filesToDelete.append(aux2);
                filesToDelete.append("\"");
            }

        }

        ProcessRunner.runCommand(String.format("rm %s", filesToDelete.toString()));

        // delete remaining empty folders
        ProcessRunner.runCommand(String.format("find %s -type d -delete > /dev/null 2>&1", coverageClassesFolderPath));

        // Find and copy source root folder
        File appFolder = new File(classFolders.get(0)).getParentFile();
        while (true) {
            File[] files = appFolder.listFiles();

            boolean found = false;
            if (files != null) {
                for (File file : files) {
                    if ("src".equals(file.getName())) {
                        appFolder = file;
                        found = true;
                        break;
                    }
                }
            }

            if (found) {
                break;
            }

            appFolder = appFolder.getParentFile();
            if (appFolder.getAbsolutePath().endsWith(properties.getPackageName())) {
                throw new Exception("Unable to find source root folder for package name " + properties.getPackageName());
            }
        }

        ProcessRunner.runCommand(String.format("cp -r  %s/main/java %s",
                appFolder.getAbsolutePath(), coverageSrcFolderPath));

        File mainFolder = new File(appFolder.getAbsolutePath() + File.separator + "main");
        String[] mainFiles = mainFolder.list();
        if (mainFiles != null && Arrays.asList(mainFiles).contains("kotlin")) {
            ProcessRunner.runCommand("cp -r  {source_root}/main/kotlin {mate_server_src_folder_path}");
        }

        return coverageSrcFolderPath;
    }
}
