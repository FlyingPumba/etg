package org.etg.espresso.codegen.actions;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.TestCodeMapper;
import org.etg.mate.models.Action;

import java.util.ArrayList;
import java.util.List;

public class MockServerResponseActionCodeMapper extends ActionCodeMapper {

    public MockServerResponseActionCodeMapper(ETGProperties etgProperties, Action action) {
        super(etgProperties, action);
    }

    @Override
    public String addTestCodeLines(List<String> testCodeLines, TestCodeMapper testCodeMapper, int actionIndex, int actionsCount) {
        List<List<String>> responses = filterResponses(action.getNetworkingInfo());
        for (List<String> response: responses) {
            addMockResponseStatement(testCodeLines, response);
        }
        return null;
    }

    private void addMockResponseStatement(List<String> testCodeLines, List<String> response) {
        String mockResponse = buildMockResponse(response);
        String statement = buildStatement(mockResponse);
        testCodeLines.add(statement);
    }

    private String buildStatement(String mockResponse) {
        return String.format("webServer.enqueue(%s)", mockResponse);
    }

    private String buildMockResponse(List<String> response) {
        int status = getResponseStatus(response);
        int bodySize = getResponseBodySize(response);
        String mockResponse = String.format("MockResponse().setResponseCode(%d)", status);

        if (bodySize > 0) {
            List<String> bodyLines = new ArrayList<>();
            int accumSize = 0;
            for (int i = response.size() - 2; i >= 0; i--) {
                String line = response.get(i);
                bodyLines.add(line);
                accumSize += line.length();

                if (accumSize >= bodySize) {
                    break;
                }
            }
            String bodyResponse = String.join("\n", bodyLines);
            mockResponse += String.format(".setBody(%s)", bodyResponse);
        }

        return mockResponse;
    }

    private int getResponseStatus(List<String> response) {
        String firstLine = response.get(0);
        String[] words = firstLine.split(" ");
        return Integer.valueOf(words[1]);
    }

    private int getResponseBodySize(List<String> response) {
        String lastLine = response.get(response.size()-1);
        String[] aux = lastLine.split("\\(");
        if (aux.length == 1) {
            return 0;
        }
        String count = aux[1].split("-byte")[0];
        return Integer.valueOf(count);
    }

    private List<List<String>> filterResponses(List<String> networkingInfo) {
        List<List<String>> responses = new ArrayList<>();

        List<String> currentResponse = new ArrayList<>();
        boolean inResponse = false;
        for (String line: networkingInfo) {
            if (line.startsWith("<--")) {
                if (inResponse) {
                    // we are closing a response
                    currentResponse.add(line);
                    responses.add(currentResponse);
                }

                inResponse = !inResponse;
                currentResponse = new ArrayList<>();
            }

            if (inResponse) {
                currentResponse.add(line);
            }
        }

        return responses;
    }
}
