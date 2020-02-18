package org.etg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etg.espresso.EspressoTestCase;
import org.etg.espresso.codegen.TestCodeGenerator;
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
    public static void main(String[] args) {
        System.out.println("ETG");

        try {
            ETGProperties properties = ETGProperties.loadProperties(args[0]);
            System.out.println("Working on file with path: " + properties.getJsonPath() + " and package name: " + properties.getPackageName());
            System.out.println("JSON file MD5: " + properties.getJsonMD5());

            System.out.println("Parsing widget test cases");
            List<WidgetTestCase> widgetTestCases = parseWidgetTestCases(properties.getJsonPath());

            if (widgetTestCases.isEmpty()) {
                throw new Exception(properties.getJsonPath() + " JSON file is empty");
            }

            TestCodeGenerator codeGenerator = new TestCodeGenerator(properties);
            List<EspressoTestCase> espressoTestCases = codeGenerator.getEspressoTestCases(widgetTestCases);

            System.out.println("Pruning failing performs from Espresso tests");
            for (EspressoTestCase espressoTestCase : espressoTestCases) {
                espressoTestCase.pruneFailingPerforms(properties);
                espressoTestCase.addToProject(properties, false);
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
