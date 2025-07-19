import models.Job;
import scheduler.ParallelJobScheduler;

import java.util.Arrays;
import java.util.List;

/**
 * The below code creates all jobs (Job A, Job B, Job C, Job D, Job E, Job F, Job G, Job H and Job I) with their respective parent jobs.
 * Job A and Job B are the starting jobs as they don't have any parent jobs.
 * A list of all starting jobs is created and passed to the ParallelJobScheduler's scheduleAllJobs method, which schedules all jobs in parallel.
 */
public class Main {
    public static void main(String[] args) {
        // Create all jobs
        Job jobA = new Job("Job A");
        Job jobB = new Job("Job B");
        Job jobC = new Job("Job C", jobA);
        Job jobD = new Job("Job D", jobB);
        Job jobE = new Job("Job E", jobC, jobD);
        Job jobF = new Job("Job F", jobE);
        Job jobG = new Job("Job G", jobE);
        Job jobH = new Job("Job H", jobE);
        Job jobI = new Job("Job I", jobF, jobG, jobH);

        // Create a list of starting jobs (jobs with no parent dependencies)
        List<Job> startingJobs = Arrays.asList(jobA, jobB);

        // Create an instance of ParallelJobScheduler
        ParallelJobScheduler parallelJobScheduler = new ParallelJobScheduler();

        // Schedule all jobs using the scheduler
        parallelJobScheduler.scheduleAllJobs(startingJobs);
    }
}

