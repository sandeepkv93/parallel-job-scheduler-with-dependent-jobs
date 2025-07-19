package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * A class that represents a Job which can be run in a Thread.
 */
public class Job implements Runnable {
    private String name;
    private List<Job> childrenJobs;
    private List<Job> parentJobs;
    private CountDownLatch latch;
    private final Random random;
    private Consumer<String> logger;
    private int sleepTimeMs;

    /**
     * Constructor for the Job class, which initializes the name, children Jobs, parent Jobs, and CountDownLatch
     *
     * @param name       the name of the Job
     * @param parentJobs the parent Jobs of the current Job
     */
    public Job(String name, Job... parentJobs) {
        this(name, System.out::println, new Random(), parentJobs);
    }
    
    /**
     * Constructor for testing with dependency injection
     *
     * @param name       the name of the Job
     * @param logger     logger function for output
     * @param random     random number generator
     * @param parentJobs the parent Jobs of the current Job
     */
    public Job(String name, Consumer<String> logger, Random random, Job... parentJobs) {
        this.name = name;
        this.childrenJobs = new ArrayList<>();
        this.parentJobs = new ArrayList<>();
        this.latch = new CountDownLatch(parentJobs.length);
        this.logger = logger;
        this.random = random;
        this.sleepTimeMs = -1; // -1 means use random

        // Add child jobs with thread safety
        for (Job parentJob : parentJobs) {
            this.parentJobs.add(parentJob);
            synchronized (parentJob.childrenJobs) {
                parentJob.childrenJobs.add(this);
            }
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
        synchronized (childrenJobs) {
            return new ArrayList<>(childrenJobs);
        }
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
     * Set a fixed sleep time for testing
     *
     * @param sleepTimeMs sleep time in milliseconds
     */
    public void setSleepTimeMs(int sleepTimeMs) {
        this.sleepTimeMs = sleepTimeMs;
    }
    
    /**
     * Implementation of the run method from the Runnable interface.
     * <p>
     * Prints the start of the Job and simulates some work before printing the completion of the Job.
     */
    @Override
    public void run() {
        logger.accept(this.getName() + " started");

        // Simulate some work
        int actualSleepTime;
        if (sleepTimeMs >= 0) {
            actualSleepTime = sleepTimeMs;
        } else {
            actualSleepTime = (random.nextInt(5) + 4) * 1000; // random number between 4 and 8 seconds
        }

        try {
            Thread.sleep(actualSleepTime);
        } catch (InterruptedException e) {
            // Restore the interrupted status
            Thread.currentThread().interrupt();
            logger.accept(this.getName() + " was interrupted");
            return; // Exit early if interrupted
        }

        logger.accept(this.getName() + " completed");
    }
}