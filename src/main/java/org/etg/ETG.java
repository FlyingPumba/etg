package org.etg;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etg.espresso.*;
import org.etg.espresso.codegen.TestCodeGenerator;
import org.etg.espresso.pruning.PruningAlgorithm;
import org.etg.espresso.pruning.PruningAlgorithmFactory;
import org.etg.mate.models.WidgetTestCase;
import org.etg.mate.parser.TestCaseParser;
import org.etg.utils.ProcessRunner;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ETG {

    public static void main(String[] argv) {
        System.out.println("ETG");

        Args args = new Args();
        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse(argv);

        try {
            ETGProperties properties = ETGProperties.loadProperties(args.getETGConfigPath(), args);
            System.out.println("Working on file with path: " + properties.getJsonPath() + " and package name: " + properties.getPackageName());
            System.out.println("JSON file MD5: " + properties.getJsonMD5());

            cleanOutputPath(properties);
            cleanETGResultsPath(properties);
            System.out.println("ETG Results folder: " + properties.getETGResultsPath());

            System.out.println("Parsing widget test cases");
            List<WidgetTestCase> widgetTestCases = parseWidgetTestCases(properties.getJsonPath());

            if (widgetTestCases.isEmpty()) {
                throw new Exception(properties.getJsonPath() + " JSON file is empty");
            }

            // get root permissions for adb
            ProcessRunner.runCommand("adb root");

            TestCodeGenerator codeGenerator = new TestCodeGenerator(properties);
            List<EspressoTestCase> espressoTestCases = codeGenerator.getEspressoTestCases(widgetTestCases);

            if (!args.isTranslateOnly()) {
                // calculate base coverage of all tests in project
                System.out.println("Calculating base test suite overall coverage");
                double baseOverallCoverage = CoverageFetcher.forProject(properties,
                        String.format("%s/%s", properties.getETGResultsPath(), "base-test-suite"))
                        .fetch();
                System.out.println(String.format("BASE-OVERALL-COVERAGE: %.8f", baseOverallCoverage));

                for (EspressoTestCase espressoTestCase : espressoTestCases) {

                    System.out.println("Producing screenshots for Espresso test" + espressoTestCase.getTestName());
                    ScreenshotsProducer screenshotsProducer = new ScreenshotsProducer(espressoTestCase);
                    screenshotsProducer.produce();
                    screenshotsProducer.dumpToResultsFolder();
                    screenshotsProducer.removeFromDevice();

                    System.out.println("Pruning failing performs from Espresso test" + espressoTestCase.getTestName());
                    PruningAlgorithm pruningAlgorithm = PruningAlgorithmFactory.getPruningAlgorithm(args.getPruningAlgorithm());
                    pruningAlgorithm.pruneFailingPerforms(espressoTestCase, properties);

                    // calculate and output coverage after pruning
                    System.out.println("Fetching coverage for Espresso test" + espressoTestCase.getTestName());
                    double testCoverage = CoverageFetcher.forTestCase(espressoTestCase).fetch();
                    System.out.println(String.format("TEST: %s COVERAGE: %.8f",
                            espressoTestCase.getTestName(), testCoverage));

                    System.out.println("Fetching increased overall coverage for Espresso test" + espressoTestCase.getTestName());
                    double increasedOveralCoverage = CoverageFetcher.forProject(properties,
                            espressoTestCase.getTestCaseResultsPath())
                            .fetch();
                    System.out.println(String.format("TEST: %s OVERALL-COVERAGE-INCLUDING-TEST: %.8f",
                            espressoTestCase.getTestName(), increasedOveralCoverage));

                    pruningAlgorithm.printSummary(espressoTestCase);
                    cleanOutputPath(properties);
                }

                // calculate combined coverage of all Espresso tests generated
                System.out.println("Calculating ETG-only test suite overall coverage");
                for (EspressoTestCase espressoTestCase : espressoTestCases) {
                    EspressoTestCaseWriter.write(espressoTestCase)
                            .withOption(EspressoTestCaseWriter.Option.SURROUND_WITH_TRY_CATCHS)
                            .toProject();
                }
                double etgOnlyOverallCoverage = CoverageFetcher.forTestCases(
                        properties,
                        String.format("%s/%s", properties.getETGResultsPath(), "etg-only-test-suite"),
                        espressoTestCases.toArray(new EspressoTestCase[0]))
                        .fetch();
                System.out.println(String.format("ETG-ONLY-OVERALL-COVERAGE: %.8f", etgOnlyOverallCoverage));
            }

            // write down all test cases once we have finished analysis of them
            for (EspressoTestCase espressoTestCase : espressoTestCases) {
                EspressoTestCaseWriter.write(espressoTestCase)
                        .withOption(EspressoTestCaseWriter.Option.PRETTIFY)
                        .toProject()
                        .toResultsFolder();
            }

            System.out.println("ETG finished");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void cleanETGResultsPath(ETGProperties properties) {
        String resultsPath = properties.getETGResultsPath();
        String rmCmd = String.format("rm -r %s/*", resultsPath);
        ProcessRunner.runCommand(rmCmd);
    }

    private static void cleanOutputPath(ETGProperties properties) {
        // delete existing ETG test cases
        String rmCmd = String.format("rm %s/%s*.%s", properties.getOutputPath(),
                TestCodeGenerator.getETGTestCaseNamePrefix(), properties.getOutputExtension());
        ProcessRunner.runCommand(rmCmd);
    }

    private static List<WidgetTestCase> parseWidgetTestCases(String filePath) throws Exception {
        String content = readFile(filePath, StandardCharsets.UTF_8);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(content);

        return TestCaseParser.parseList(mapper, jsonNode);
    }

    private static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
