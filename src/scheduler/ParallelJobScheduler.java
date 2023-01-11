package scheduler;

import models.Job;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelJobScheduler {
    private ExecutorService executor;

    public void scheduleAllJobs(List<Job> jobs) {
        executor = Executors.newFixedThreadPool(4);

        for (Job job : jobs) {
            executor.submit(() -> processJob(job));
        }

        // Shut down the thread pool
        executor.shutdown();
    }

    private void processJob(Job job) {
        // Wait for parent jobs to complete
        try {
            job.getLatch().await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Run the job
        job.run();

        // Start child jobs in parallel
        for (Job childJob : job.getChildrenJobs()) {
            childJob.getLatch().countDown();
        }
    }
}
