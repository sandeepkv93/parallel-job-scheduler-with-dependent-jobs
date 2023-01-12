package scheduler;

import models.Job;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ParallelJobScheduler {
    private ExecutorService executor;

    public void scheduleAllJobs(List<Job> allJobs) {
        executor = Executors.newFixedThreadPool(4);

        Set<Job> startingJobs = getAllStartingJobs(allJobs);
        List<Job> allChildrenJobs = getAllChildrenJobsInOrder(startingJobs);

        for (Job job : startingJobs) {
            executor.submit(() -> processJob(job));
        }

        for (Job job : allChildrenJobs) {
            executor.submit(() -> processJob(job));
        }

        // Shut down the thread pool
        executor.shutdown();
    }

    private Set<Job> getAllStartingJobs(List<Job> allJobs) {
        return allJobs
                .stream()
                .filter(job -> job.getParentJobs().isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private List<Job> getAllChildrenJobsInOrder(Set<Job> startingJobs) {
        List<Job> allChildrenJobs = new ArrayList<>();
        Set<Job> allChildrenJobsSet = new HashSet<>();
        Queue<Job> queue = new LinkedList<>(startingJobs);
        while (!queue.isEmpty()) {
            Job job = queue.poll();
            if (!startingJobs.contains(job) && !allChildrenJobsSet.contains(job)) {
                allChildrenJobsSet.add(job);
                allChildrenJobs.add(job);
            }
            for (Job childJob : job.getChildrenJobs()) {
                if (!allChildrenJobsSet.contains(childJob)) {
                    queue.add(childJob);
                }
            }
        }

        return allChildrenJobs;
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
