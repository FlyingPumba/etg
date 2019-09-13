package org.etg.utils;

import java.util.concurrent.*;

public class TimeoutRun {
    public static boolean timeoutRun(Callable<Void> c, long milliseconds) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Void> future = executor.submit(c);
        boolean finishedWithoutTimeout = false;

        try {
            System.out.println("Starting timeout run...");
            future.get(milliseconds, TimeUnit.MILLISECONDS);
            System.out.println("Finished run before timeout.");
            finishedWithoutTimeout = true;
        } catch (TimeoutException e) {
            future.cancel(true);
            System.out.println("Finshed run due to timeout.");
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Unexpected exception in timeout run: " + e.getMessage());
            e.printStackTrace();
        }

        executor.shutdownNow();
        return finishedWithoutTimeout;
    }
}
