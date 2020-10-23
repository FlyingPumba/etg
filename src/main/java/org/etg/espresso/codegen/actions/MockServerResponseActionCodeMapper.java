package org.etg.espresso.codegen.actions;

import org.etg.ETGProperties;
import org.etg.espresso.codegen.codeMapper.StandardTestCodeMapper;
import org.etg.mate.models.Action;

import java.util.ArrayList;
import java.util.List;

import static org.etg.espresso.util.StringHelper.boxString;

public class MockServerResponseActionCodeMapper extends ActionCodeMapper {

    public MockServerResponseActionCodeMapper(ETGProperties etgProperties, Action action) {
        super(etgProperties, action);
    }

    @Override
    public String addTestCodeLines(List<String> testCodeLines, StandardTestCodeMapper testCodeMapper, int actionIndex, int actionsCount) {
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
            int openedBrackets = 0;
            for (int i = response.size() - 2; i >= 0; i--) {
                String line = response.get(i);
                int lineBytes = line.getBytes().length;

                if (accumSize + lineBytes <= bodySize) {
                    bodyLines.add(0, line);
                    accumSize += lineBytes;
                } else {
                    break;
                }

                openedBrackets += line.length() - line.replace("}", "").length();
                openedBrackets -= line.length() - line.replace("{", "").length();
                if (openedBrackets == 0) {
                    break;
                }
            }
            String bodyResponse = buildBodyResponse(bodyLines);
            mockResponse += String.format(".setBody(%s)", bodyResponse);
        }

        return mockResponse;
    }

    private String buildBodyResponse(List<String> bodyLines) {
        String unsafeString = String.join("\n", bodyLines);
        String safeString = boxString(unsafeString);
        return safeString;
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
        return Integer.parseInt(count);
    }

    /**
     * Parse and group networking info by responses.
     * We use the "<--" string to determine when a response is beginning or ending.
     * The end line of a response also contains the "END HTTP" response.
     * It's important to take into account that responses may be interleaved. For example:
     * <-- 200 https://route1.com
     * <-- 200 https://route2.com
     * <-- END HTTP (100-byte body)
     * <-- END HTTP (200-byte body)
     * The above has a full response dumped in the middle of a previous one.
     * We will assume that when a HTTP response ends is found, it belongs to the response that was opened more recently.
     *
     * @param networkingInfo
     * @return
     */
    private List<List<String>> filterResponses(List<String> networkingInfo) {
        List<List<String>> responses = new ArrayList<>();

        List<List<String>> currentResponses = new ArrayList<>();
        for (String line: networkingInfo) {
            if (line.startsWith("<--")) {
                if (line.contains("END HTTP")) {
                    // we are closing a response
                    currentResponses.get(0).add(line);
                    responses.add(currentResponses.get(0));
                    currentResponses.remove(0);
                } else {
                    // we are starting a response
                    currentResponses.add(0, new ArrayList<>());
                    currentResponses.get(0).add(line);
                }
            } else {
                // are we in the middle of a response?
                if (!currentResponses.isEmpty()) {
                    currentResponses.get(0).add(line);
                }
            }
        }

        return responses;
    }
}
