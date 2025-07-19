package scheduler;

import models.Job;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

class ParallelJobSchedulerTest {
    private Queue<String> executionOrder;
    private Consumer<String> testLogger;
    private Random deterministicRandom;

    @BeforeEach
    void setUp() {
        executionOrder = new ConcurrentLinkedQueue<>();
        testLogger = message -> {
            if (message.contains("started") || message.contains("completed")) {
                executionOrder.offer(message);
            }
        };
        deterministicRandom = new Random(42);
    }

    @Test
    void testScheduleSimpleJob() {
        ParallelJobScheduler scheduler = new ParallelJobScheduler(2);
        Job job = new Job("SimpleJob", testLogger, deterministicRandom);
        job.setSleepTimeMs(10);
        
        scheduler.scheduleAllJobs(Arrays.asList(job));
        
        // Verify execution
        List<String> messages = new ArrayList<>(executionOrder);
        assertEquals(2, messages.size());
        assertEquals("SimpleJob started", messages.get(0));
        assertEquals("SimpleJob completed", messages.get(1));
    }

    @Test
    void testScheduleJobsWithDependencies() {
        ParallelJobScheduler scheduler = new ParallelJobScheduler(4);
        
        Job jobA = new Job("JobA", testLogger, deterministicRandom);
        Job jobB = new Job("JobB", testLogger, deterministicRandom);
        Job jobC = new Job("JobC", testLogger, deterministicRandom, jobA);
        Job jobD = new Job("JobD", testLogger, deterministicRandom, jobB);
        Job jobE = new Job("JobE", testLogger, deterministicRandom, jobC, jobD);
        
        // Set short sleep times for testing
        Arrays.asList(jobA, jobB, jobC, jobD, jobE).forEach(job -> job.setSleepTimeMs(10));
        
        scheduler.scheduleAllJobs(Arrays.asList(jobA, jobB));
        
        List<String> messages = new ArrayList<>(executionOrder);
        assertEquals(10, messages.size()); // 5 jobs * 2 messages each
        
        // Verify correct execution order
        verifyJobExecutedBeforeJob(messages, "JobA", "JobC");
        verifyJobExecutedBeforeJob(messages, "JobB", "JobD");
        verifyJobExecutedBeforeJob(messages, "JobC", "JobE");
        verifyJobExecutedBeforeJob(messages, "JobD", "JobE");
    }

    @Test
    void testCycleDetection() {
        // Create a cycle: A -> B -> C -> A
        Job jobA = new Job("JobA", testLogger, deterministicRandom);
        Job jobB = new Job("JobB", testLogger, deterministicRandom, jobA);
        Job jobC = new Job("JobC", testLogger, deterministicRandom, jobB);
        
        // Create cycle by manually adding jobA as child of jobC
        synchronized (jobC.getChildrenJobs()) {
            jobC.getChildrenJobs().add(jobA);
        }
        jobA.getParentJobs().add(jobC);
        
        ParallelJobScheduler scheduler = new ParallelJobScheduler(2);
        
        assertThrows(IllegalArgumentException.class, () -> {
            scheduler.scheduleAllJobs(Arrays.asList(jobA));
        });
    }

    @Test
    void testCustomThreadPoolSize() {
        ParallelJobScheduler scheduler1 = new ParallelJobScheduler(1);
        ParallelJobScheduler scheduler2 = new ParallelJobScheduler(8);
        
        // Both should work with different thread pool sizes
        Job job1 = new Job("Job1", testLogger, deterministicRandom);
        Job job2 = new Job("Job2", testLogger, deterministicRandom);
        job1.setSleepTimeMs(10);
        job2.setSleepTimeMs(10);
        
        scheduler1.scheduleAllJobs(Arrays.asList(job1));
        scheduler2.scheduleAllJobs(Arrays.asList(job2));
        
        assertTrue(executionOrder.size() >= 2);
    }

    @Test
    void testComplexDependencyGraph() {
        ParallelJobScheduler scheduler = new ParallelJobScheduler(4);
        
        // Create the example from README
        Job jobA = new Job("JobA", testLogger, deterministicRandom);
        Job jobB = new Job("JobB", testLogger, deterministicRandom);
        Job jobC = new Job("JobC", testLogger, deterministicRandom, jobA);
        Job jobD = new Job("JobD", testLogger, deterministicRandom, jobB);
        Job jobE = new Job("JobE", testLogger, deterministicRandom, jobC, jobD);
        Job jobF = new Job("JobF", testLogger, deterministicRandom, jobE);
        Job jobG = new Job("JobG", testLogger, deterministicRandom, jobE);
        Job jobH = new Job("JobH", testLogger, deterministicRandom, jobE);
        Job jobI = new Job("JobI", testLogger, deterministicRandom, jobF, jobG, jobH);
        
        // Set short sleep times
        Arrays.asList(jobA, jobB, jobC, jobD, jobE, jobF, jobG, jobH, jobI)
                .forEach(job -> job.setSleepTimeMs(10));
        
        scheduler.scheduleAllJobs(Arrays.asList(jobA, jobB));
        
        List<String> messages = new ArrayList<>(executionOrder);
        assertEquals(18, messages.size()); // 9 jobs * 2 messages each
        
        // Verify dependencies are respected
        verifyJobExecutedBeforeJob(messages, "JobA", "JobC");
        verifyJobExecutedBeforeJob(messages, "JobB", "JobD");
        verifyJobExecutedBeforeJob(messages, "JobC", "JobE");
        verifyJobExecutedBeforeJob(messages, "JobD", "JobE");
        verifyJobExecutedBeforeJob(messages, "JobE", "JobF");
        verifyJobExecutedBeforeJob(messages, "JobE", "JobG");
        verifyJobExecutedBeforeJob(messages, "JobE", "JobH");
        verifyJobExecutedBeforeJob(messages, "JobF", "JobI");
        verifyJobExecutedBeforeJob(messages, "JobG", "JobI");
        verifyJobExecutedBeforeJob(messages, "JobH", "JobI");
    }

    @Test
    void testEmptyJobList() {
        ParallelJobScheduler scheduler = new ParallelJobScheduler(2);
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            scheduler.scheduleAllJobs(new ArrayList<>());
        });
        
        assertTrue(executionOrder.isEmpty());
    }

    @Test
    void testSingleJobWithoutDependencies() {
        ParallelJobScheduler scheduler = new ParallelJobScheduler(1);
        Job job = new Job("SingleJob", testLogger, deterministicRandom);
        job.setSleepTimeMs(10);
        
        scheduler.scheduleAllJobs(Arrays.asList(job));
        
        List<String> messages = new ArrayList<>(executionOrder);
        assertEquals(2, messages.size());
        assertEquals("SingleJob started", messages.get(0));
        assertEquals("SingleJob completed", messages.get(1));
    }

    private void verifyJobExecutedBeforeJob(List<String> messages, String jobA, String jobB) {
        int jobACompletedIndex = -1;
        int jobBStartedIndex = -1;
        
        for (int i = 0; i < messages.size(); i++) {
            String message = messages.get(i);
            if (message.equals(jobA + " completed")) {
                jobACompletedIndex = i;
            }
            if (message.equals(jobB + " started")) {
                jobBStartedIndex = i;
            }
        }
        
        assertTrue(jobACompletedIndex >= 0, jobA + " should have completed");
        assertTrue(jobBStartedIndex >= 0, jobB + " should have started");
        assertTrue(jobACompletedIndex < jobBStartedIndex, 
                jobA + " should complete before " + jobB + " starts");
    }
}