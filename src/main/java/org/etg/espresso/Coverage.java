package org.etg.espresso;

import org.etg.ETGProperties;
import org.etg.utils.ProcessRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Coverage {
    static double getTestCoverage(EspressoTestCase espressoTestCase, String testCaseResultsPath) throws Exception {
        String workingFolder = System.getProperty("user.dir");

        String coverageSrcFolderPath = prepareForTestCoverage(espressoTestCase, testCaseResultsPath);
        String coverageEcPath = String.format("%scoverage.ec", coverageSrcFolderPath);

        // pull coverage.ec file
        String pullCmd = String.format("adb pull %s %s", getRemoteCoverageEcPath(espressoTestCase.getEtgProperties()),
                coverageSrcFolderPath);
        String pullCmdResult = ProcessRunner.runCommand(pullCmd);

        if (pullCmdResult.contains("error")) {
            throw new Exception("Unable to fetch coverage.ec file: " + pullCmdResult);
        }

        // TODO: this is a hack, there has to be a better way
        String jacocoBinPath = "bin/jacococli.jar";
        if (!workingFolder.endsWith("etg")) {
            jacocoBinPath = "etg/" + jacocoBinPath;
        }

        // Build the Jacoco report using the coverage.ec file just obtained and the classes and source files prepared before
        String jacocoCmd = String.format("java -jar %s report \"%s\" " +
                        "--classfiles %s/classes " +
                        "--sourcefiles %s/java " +
                        "--xml %s/jacoco_report.xml " +
                        "--html %s/jacoco_html_report",
                jacocoBinPath,
                coverageEcPath, coverageSrcFolderPath, coverageSrcFolderPath,
                coverageSrcFolderPath, coverageSrcFolderPath);
        String jacocoCmdResult = ProcessRunner.runCommand(jacocoCmd);

        // Get the total percentage of statements covered using the html in the report
        String indexHtmlPath = new File(String.format("%s/jacoco_html_report/index.html", coverageSrcFolderPath))
                .getAbsolutePath();
        String xpathMissedLines = "html/body/table/tfoot/tr/td[8]/text()";
        String xpathMissedLinesCmd = String.format("xmllint --html -xpath \"%s\" %s", xpathMissedLines, indexHtmlPath);
        String missedLinesStr = ProcessRunner.runCommand(xpathMissedLinesCmd);

        String xpathTotalLines = "html/body/table/tfoot/tr/td[9]/text()";
        String xpathTotalLinesCmd = String.format("xmllint --html -xpath \"%s\" %s", xpathTotalLines, indexHtmlPath);
        String totalLinesStr = ProcessRunner.runCommand(xpathTotalLinesCmd);

        double missedLines = Double.parseDouble(missedLinesStr.replace(",", ""));
        double totalLines = Double.parseDouble(totalLinesStr.replace(",", ""));
        double coveredLines = totalLines - missedLines;

        double coverage = coveredLines / totalLines;
        return coverage;
    }

    /**
     * This method takes care of preparing both sources and class files that will be used by Jacoco report generator
     * @throws Exception
     * @return
     */
    private static String prepareForTestCoverage(EspressoTestCase espressoTestCase, String testCaseResultsPath) throws Exception {
        ETGProperties properties = espressoTestCase.getEtgProperties();

        String packageNamePath = String.join("/", properties.getPackageName().split("\\."));

        String coverageSrcFolderPath = String.format("%s/coverage/", testCaseResultsPath);
        ProcessRunner.runCommand(String.format("rm -rf %s", coverageSrcFolderPath));
        ProcessRunner.runCommand(String.format("mkdir -p %s", coverageSrcFolderPath));

        String[] classFiles = ProcessRunner.runCommand(
                String.format("find %s -name \"*.class\" -type f",
                properties.getApplicationFolderPath())).split("\n");

        String buildVariant = "debug";
        String buildVariantPath = buildVariant;

        String[] productFlavors = properties.getProductFlavors();
        if (productFlavors.length > 0) {
            StringBuilder productFlavorsCombined = new StringBuilder();
            StringBuilder productFlavorsCombinedPath = new StringBuilder();
            for (int i = 0; i < productFlavors.length; i++) {
                String flavor = productFlavors[i];
                if (i == 0) {
                    productFlavorsCombined.append(flavor.toLowerCase());
                } else {
                    String cap = flavor.substring(0, 1).toUpperCase() + flavor.substring(1);
                    productFlavorsCombined.append(cap);
                }

                productFlavorsCombinedPath.append("/");
                productFlavorsCombinedPath.append(flavor.toLowerCase());
            }

            String cap = properties.getBuildType().substring(0, 1).toUpperCase() +
                    properties.getBuildType().substring(1);
            buildVariant = productFlavorsCombined + cap;

            buildVariantPath = productFlavorsCombinedPath.toString() + "/" + properties.getBuildType() + "/";

            if (packageNamePath.endsWith(productFlavorsCombined.toString())) {
                packageNamePath = packageNamePath.split("/" + productFlavorsCombined.toString())[0];
            }
        }

        List<String> filteredClassFiles = new ArrayList<>();
        for (String classFile : classFiles) {
            if ((classFile.contains(buildVariant) || classFile.contains(buildVariantPath))
                && !classFile.contains("AndroidTest")
                && !classFile.contains("androidTest")
                && !classFile.contains("UnitTest")
                && !classFile.contains("R$")
                && !classFile.contains("R.class")
                && !classFile.contains("BuildConfig.class")
                && !classFile.contains("/EmmaInstrument/")
                && !classFile.contains("/jacoco_instrumented_classes/")
                && !classFile.contains("/jacoco/")
                && !classFile.contains("/transforms/")
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
        for (String classFile : filteredClassFiles) {
            boolean shouldInspect = true;
            for (String folder: classFolders) {
                if (classFile.startsWith(folder)) {
                    shouldInspect = false;
                    break;
                }
            }
            if (!shouldInspect) {
                // we already have the folder for this class file
                continue;
            }


            File aux = new File(classFile).getParentFile();
            String classFileFolder = aux.getAbsolutePath();
            while (!classFileFolder.endsWith("subjects")) {
                if (classFileFolder.endsWith(packageNamePath)) {
                    String classFolder = classFileFolder.split(packageNamePath)[0];

                    if (!classFolders.contains(classFolder)) {
                        classFolders.add(classFolder);
                    }

                    break;
                } else {
                    aux = aux.getParentFile();
                    classFileFolder = aux.getAbsolutePath();
                }
            }
        }

        // copy class folders with their directory structure
        ProcessRunner.runCommand(String.format("mkdir -p %s/%s", coverageClassesFolderPath, packageNamePath));
        for (String classFolder : classFolders) {
            ProcessRunner.runCommand(String.format("rsync -a --prune-empty-dirs --exclude=\"*EmmaInstrument*/\" " +
                            "--exclude=\"*AndroidTest*/\" --exclude=\"*UnitTest*/\" --exclude=\"*kapt3*/\" " +
                            "--exclude=\"*jacoco_instrumented_classes*/\" --exclude=\"R\\$*.class\" " +
                            "--exclude=\"*jacoco*/\" --exclude=\"*androidTest*/\" --exclude=\"*transforms*/\" " +
                            "--exclude=\"BuildConfig.class\" --exclude=\"R.class\" --include=\"*.class\" " +
                            "--include=\"*/\" --exclude=\"*\" " +
                            "%s/%s/ %s/%s",
                    classFolder, packageNamePath, coverageClassesFolderPath, packageNamePath));
        }

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

    public static String getRemoteCoverageEcPath(ETGProperties properties) {
        return String.format("/data/user/0/%s/files/coverage.ec", properties.getCompiledPackageName());
    }

    public static String getRemoteCoverageEcFolderPath(ETGProperties properties) {
        return String.format("/data/user/0/%s/files/", properties.getCompiledPackageName());
    }
}
