package org.etg;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etg.espresso.EspressoTestCase;
import org.etg.espresso.EspressoTestCaseWriter;
import org.etg.espresso.EspressoTestRunner;
import org.etg.espresso.codegen.TestCodeGenerator;
import org.etg.espresso.pruning.PruningAlgorithm;
import org.etg.espresso.pruning.PruningAlgorithmFactory;
import org.etg.mate.models.Action;
import org.etg.mate.models.WidgetTestCase;
import org.etg.mate.parser.TestCaseParser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.server.ExportException;
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
            ETGProperties properties = ETGProperties.loadProperties(args.getETGConfigPath());
            System.out.println("Working on file with path: " + properties.getJsonPath() + " and package name: " + properties.getPackageName());
            System.out.println("JSON file MD5: " + properties.getJsonMD5());

            EspressoTestRunner.cleanOutputPath(properties);
            System.out.println("Output folder: " + args.getResultsPath());

            System.out.println("Parsing widget test cases");
            List<WidgetTestCase> widgetTestCases = parseWidgetTestCases(properties.getJsonPath());

            if (widgetTestCases.isEmpty()) {
                throw new Exception(properties.getJsonPath() + " JSON file is empty");
            }

            TestCodeGenerator codeGenerator = new TestCodeGenerator(properties);
            List<EspressoTestCase> espressoTestCases = codeGenerator.getEspressoTestCases(widgetTestCases);

            System.out.println("Pruning failing performs from Espresso tests");
            for (EspressoTestCase espressoTestCase : espressoTestCases) {

                if (!args.isTranslateOnly()) {
                    PruningAlgorithm pruningAlgorithm = PruningAlgorithmFactory.getPruningAlgorithm(args.getPruningAlgorithm());
                    pruningAlgorithm.pruneFailingPerforms(espressoTestCase, properties);

                    // calculate and output coverage after pruning
                    double coverage = espressoTestCase.getCoverage(properties, args.getResultsPath());
                    System.out.println(String.format("TEST: %s COVERAGE: %.8f",
                            espressoTestCase.getTestName(), coverage));

                    pruningAlgorithm.printSummary(espressoTestCase);
                }

                EspressoTestCaseWriter.write(espressoTestCase)
                        .withOption(EspressoTestCaseWriter.Option.PRETTIFY)
                        .toProject();
            }

            System.out.println("ETG finished");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
