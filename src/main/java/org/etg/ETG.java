package org.etg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etg.espresso.codegen.TestCodeGenerator;
import org.etg.mate.models.TestCase;
import org.etg.mate.parser.TestCaseParser;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ETG {
    public static void main(String[] args) {
        System.out.println("ETG");

        String filePath = args[0];
        String packageName = args[1];
        String testPackageName = args[2];
        String outputFolderPath = args[3];

        System.out.println("Working on file with path: " + filePath + " and package name: " + packageName);

        try {
            String content = readFile(filePath, StandardCharsets.US_ASCII);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(content);

            List<TestCase> testCases = TestCaseParser.parseList(mapper, jsonNode);

            TestCodeGenerator codeGenerator = new TestCodeGenerator(packageName, testPackageName);
            List<String> espressoTestCases = codeGenerator.getEspressoTestCases(testCases);

            for (int i = 0; i < espressoTestCases.size(); i++) {
                String testContent = espressoTestCases.get(i);
                String outputFilePath = outputFolderPath + "TestCase" + i + ".java";

                PrintWriter out = new PrintWriter(new FileOutputStream(outputFilePath), true);
                out.print(testContent);
                out.close();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
