package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * A class that represents a Job which can be run in a Thread.
 */
public class Job implements Runnable {
    private String name;
    private List<Job> childrenJobs;
    private List<Job> parentJobs;
    private CountDownLatch latch;

    /**
     * Constructor for the Job class, which initializes the name, children Jobs, parent Jobs, and CountDownLatch
     *
     * @param name       the name of the Job
     * @param parentJobs the parent Jobs of the current Job
     */
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

    /**
     * Getter method for the name of the Job
     *
     * @return the name of the Job
     */
    public String getName() {
        return name;
    }

    /**
     * Getter method for the children Jobs of the current Job
     *
     * @return the children Jobs of the current Job
     */
    public List<Job> getChildrenJobs() {
        return childrenJobs;
    }

    /**
     * Getter method for the parent Jobs of the current Job
     *
     * @return the parent Jobs of the current Job
     */
    public List<Job> getParentJobs() {
        return parentJobs;
    }

    /**
     * Getter method for the CountDownLatch of the current Job
     *
     * @return the CountDownLatch of the current Job
     */
    public CountDownLatch getLatch() {
        return latch;
    }

    /**
     * Implementation of the run method from the Runnable interface.
     * <p>
     * Prints the start of the Job and simulates some work before printing the completion of the Job.
     */
    @Override
    public void run() {
        System.out.println(this.getName() + " started");

        // Simulate some work
        Random rand = new Random();
        int randomSleepTime = rand.nextInt(5) + 4; // random number between 4 and 8

        try {
            Thread.sleep(randomSleepTime * 1000); // sleep for randomSleepTime seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(this.getName() + " completed");
    }
}