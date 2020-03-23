package org.etg.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProcessRunner {
    private static final String[] WIN_RUNTIME = {"cmd.exe", "/C"};
    private static final String[] OS_LINUX_RUNTIME = {"/bin/bash", "-c"};

    private ProcessRunner() {
    }

    @SuppressWarnings("Since15")
    private static <T> T[] concat(T[] first, T[] second) {
        T[] result;
        result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static String runProcess(boolean isWin, String... command) {
        String[] allCommand = null;
        try {
            if (isWin) {
                allCommand = concat(WIN_RUNTIME, command);
            } else {
                allCommand = concat(OS_LINUX_RUNTIME, command);
            }
            ProcessBuilder pb = new ProcessBuilder(allCommand);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String _temp = null;
            List<String> line = new ArrayList<String>();
            while ((_temp = in.readLine()) != null) {
                line.add(_temp);
            }

            return String.join("\n", line);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String runCommand(String command) {
        System.out.println(String.format("Running command: %s", command));
        long startTime = System.nanoTime();

        boolean win = false;
        String os = System.getProperty("os.name");
        if (os != null && !os.contains("Linux")) {
            win = true;
        } else {
            command = "source ~/.bashrc; " + command;
        }
        String output = ProcessRunner.runProcess(win, command);

        long endTime = System.nanoTime();
        long elapsedTime = endTime - startTime;
        long seconds = TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        System.out.println("It took: " + seconds + " seconds");

        return output;
    }
}
