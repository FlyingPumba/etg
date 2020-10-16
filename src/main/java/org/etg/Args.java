package org.etg;

import java.util.ArrayList;
import java.util.List;
import com.beust.jcommander.Parameter;

public class Args {
    @Parameter
    private List<String> positionalParameters = new ArrayList<>();

    @Parameter(names = {"-debug", "-d"}, description = "Debug mode")
    private boolean debug = false;

    @Parameter(names = {"-translate-only", "-t"}, description = "Only translate the widget-based test cases to Espresso test, nothing else.")
    private boolean translateOnly = false;

    @Parameter(names = {"-pruning-algorithm", "-p"}, description = "Algorithm for pruning failing actions")
    private String pruningAlgorithm;

    @Parameter(names = {"-code-mapper", "-c"}, description = "Code mapper")
    private String codeMapper;

    @Parameter(names = {"-kotlin", "-k"}, description = "Output test cases in Kotlin format")
    private boolean useKotlin = false;

    @Parameter(names = {"-disable-text-matchers"}, description = "Disable text matchers")
    private boolean disableTextMatchers = false;

    @Parameter(names = {"-sleep-after-actions"}, description = "Time (in ms.) to sleep after each action.")
    private int sleepAfterActions = -1;

    @Parameter(names = {"-sleep-after-launch"}, description = "Time (in ms.) to sleep after Activity launch.")
    private int sleepAfterLaunch = -1;

    @Parameter(names = {"-results-path"}, description = "Folder path to dump results")
    private String resultsPath;

    public boolean isDebug() {
        return debug;
    }

    public boolean useKotlinFormat() {
        return useKotlin;
    }

    public boolean isTranslateOnly() {
        return translateOnly;
    }

    public String getPruningAlgorithm() {
        return pruningAlgorithm;
    }

    public String getETGConfigPath() {
        return positionalParameters.get(0);
    }

    public List<String> getJSONPaths() {
        return positionalParameters.subList(1, positionalParameters.size());
    }

    public String getResultsPath() {
        if (resultsPath != null) {
            return resultsPath;
        } else {
            String workingFolder = System.getProperty("user.dir");
            return workingFolder + "/results";
        }
    }

    public int getSleepAfterActions() {
        return sleepAfterActions;
    }

    public int getSleepAfterLaunch() {
        return sleepAfterLaunch;
    }

    public boolean disableTextMatchers() {
        return disableTextMatchers;
    }

    public String getCodeMapper() {
        return codeMapper;
    }
}
