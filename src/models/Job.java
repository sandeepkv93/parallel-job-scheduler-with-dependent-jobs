package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class Job implements Runnable{
    private String name;
    private List<Job> childrenJobs;
    private List<Job> parentJobs;
    private CountDownLatch latch;

    public String getName() {
        return name;
    }

    public List<Job> getChildrenJobs() {
        return childrenJobs;
    }

    public List<Job> getParentJobs() {
        return parentJobs;
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public Job(String name, Job... parentJobs) {
        this.name = name;
        this.childrenJobs = new ArrayList<>();
        this.parentJobs = new ArrayList<>();
        this.latch = new CountDownLatch(parentJobs.length);

        // Add child jobs
        for (Job parentJob : parentJobs) {
            this.parentJobs.add(parentJob);
            parentJob.childrenJobs.add(this);
        }
    }

    @Override
    public void run() {
        System.out.println(this.getName() + " started");
        
        // Simulate some work
        Random rand = new Random();
        int randomSleepTime = rand.nextInt(5) + 4; // random number between 4 and 8

        try {
            Thread.sleep(randomSleepTime * 1000);  // sleep for randomSleepTime seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println(this.getName() + " completed");
    }
}
