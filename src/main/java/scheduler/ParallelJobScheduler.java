package scheduler;

import models.Job;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ParallelJobScheduler {
    private ExecutorService executor;
    private final int threadPoolSize;
    
    public ParallelJobScheduler() {
        this(4);
    }
    
    public ParallelJobScheduler(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    /**
     * Schedule all jobs in the given list
     *
     * @param startingJobs list of starting jobs to be scheduled
     */
    public void scheduleAllJobs(List<Job> startingJobs) {
        // Create a fixed thread pool
        executor = Executors.newFixedThreadPool(threadPoolSize);

        // Convert to set for efficient lookups
        Set<Job> startingJobsSet = new HashSet<>(startingJobs);
        
        // Detect cycles before execution
        if (hasCycle(startingJobsSet)) {
            throw new IllegalArgumentException("Cycle detected in job dependencies");
        }

        // Get all child jobs in order
        List<Job> allChildrenJobs = getAllChildrenJobsInOrder(startingJobsSet);

        // Submit starting jobs to the thread pool
        for (Job job : startingJobsSet) {
            executor.submit(() -> processJob(job));
        }

        // Submit all children jobs to the thread pool
        for (Job job : allChildrenJobs) {
            executor.submit(() -> processJob(job));
        }

        // Properly shut down the thread pool
        executor.shutdown();
        try {
            // Wait for all tasks to complete or timeout after 60 seconds
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                // Wait a bit more for tasks to respond to being cancelled
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // Cancel currently executing tasks
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Check if there is a cycle in the job dependency graph
     *
     * @param startingJobs set of starting jobs
     * @return true if cycle exists, false otherwise
     */
    private boolean hasCycle(Set<Job> startingJobs) {
        Set<Job> visited = new HashSet<>();
        Set<Job> recursionStack = new HashSet<>();
        
        for (Job job : startingJobs) {
            if (hasCycleDFS(job, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * DFS helper method for cycle detection
     *
     * @param job current job
     * @param visited set of visited jobs
     * @param recursionStack set of jobs in current recursion stack
     * @return true if cycle detected, false otherwise
     */
    private boolean hasCycleDFS(Job job, Set<Job> visited, Set<Job> recursionStack) {
        if (recursionStack.contains(job)) {
            return true; // Back edge found, cycle detected
        }
        
        if (visited.contains(job)) {
            return false; // Already processed
        }
        
        visited.add(job);
        recursionStack.add(job);
        
        for (Job child : job.getChildrenJobs()) {
            if (hasCycleDFS(child, visited, recursionStack)) {
                return true;
            }
        }
        
        recursionStack.remove(job);
        return false;
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
