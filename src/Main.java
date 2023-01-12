import models.Job;
import scheduler.ParallelJobScheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Job jobA = new Job("Job A");
        Job jobB = new Job("Job B");
        Job jobC = new Job("Job C", jobA);
        Job jobD = new Job("Job D", jobB);
        Job jobE = new Job("Job E", jobC, jobD);
        Job jobF = new Job("Job F", jobE);
        Job jobG = new Job("Job G", jobE);
        Job jobH = new Job("Job H", jobE);
        Job jobI = new Job("Job I", jobF, jobG, jobH);

        List<Job> startingJobs = Arrays.asList(jobA, jobB);

        ParallelJobScheduler parallelJobScheduler = new ParallelJobScheduler();
        parallelJobScheduler.scheduleAllJobs(startingJobs);
    }
}