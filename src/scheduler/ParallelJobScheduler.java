package scheduler;

import models.Job;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ParallelJobScheduler {
    private ExecutorService executor;

    /**
     * Schedule all jobs in the given list
     *
     * @param allJobs list of all jobs to be scheduled
     */
    public void scheduleAllJobs(List<Job> allJobs) {
        // Create a fixed thread pool
        executor = Executors.newFixedThreadPool(4);

        // Get all starting jobs (jobs with no parents)
        Set<Job> startingJobs = getAllStartingJobs(allJobs);

        // Get all child jobs in order
        List<Job> allChildrenJobs = getAllChildrenJobsInOrder(startingJobs);

        // Submit starting jobs to the thread pool
        for (Job job : startingJobs) {
            executor.submit(() -> processJob(job));
        }

        // Submit all children jobs to the thread pool
        for (Job job : allChildrenJobs) {
            executor.submit(() -> processJob(job));
        }

        // Shut down the thread pool
        executor.shutdown();
    }

    /**
     * Get all starting jobs (jobs with no parents)
     *
     * @param allJobs list of all jobs
     * @return set of starting jobs
     */
    private Set<Job> getAllStartingJobs(List<Job> allJobs) {
        return allJobs.stream().filter(job -> job.getParentJobs().isEmpty()).collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Get all child jobs in order
     *
     * @param startingJobs set of starting jobs
     * @return list of all child jobs in order
     */
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

    /**
     * This method is responsible for processing a single job in the parallel job scheduler. It first waits for all parent jobs to complete,
     * <p>
     * then runs the current job, and finally starts all child jobs in parallel.
     *
     * @param job the Job object to be processed
     */
    private void processJob(Job job) {
        // Wait for parent jobs to complete
        try {
            // The await() method is used to block the current thread until the countdown latch reaches zero.
            // This ensures that all parent jobs have completed before the current job runs.
            job.getLatch().await();
        } catch (InterruptedException e) {
            // Handle interruption exception
            e.printStackTrace();
        }

        // Run the job
        job.run();

        // Start child jobs in parallel
        for (Job childJob : job.getChildrenJobs()) {
            // The countDown() method decrements the count of the latch, which will unblock any threads waiting on the latch.
            // This allows the child jobs to start running in parallel with the current job.
            childJob.getLatch().countDown();
        }
    }
}
