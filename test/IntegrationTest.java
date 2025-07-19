import models.Job;
import scheduler.ParallelJobScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

class IntegrationTest {
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
    void testFullWorkflowFromReadmeExample() {
        // Recreate the exact scenario from README
        Job jobA = new Job("Job A", testLogger, deterministicRandom);
        Job jobB = new Job("Job B", testLogger, deterministicRandom);
        Job jobC = new Job("Job C", testLogger, deterministicRandom, jobA);
        Job jobD = new Job("Job D", testLogger, deterministicRandom, jobB);
        Job jobE = new Job("Job E", testLogger, deterministicRandom, jobC, jobD);
        Job jobF = new Job("Job F", testLogger, deterministicRandom, jobE);
        Job jobG = new Job("Job G", testLogger, deterministicRandom, jobE);
        Job jobH = new Job("Job H", testLogger, deterministicRandom, jobE);
        Job jobI = new Job("Job I", testLogger, deterministicRandom, jobF, jobG, jobH);

        // Set deterministic sleep times for predictable testing
        Arrays.asList(jobA, jobB, jobC, jobD, jobE, jobF, jobG, jobH, jobI)
                .forEach(job -> job.setSleepTimeMs(50));

        List<Job> startingJobs = Arrays.asList(jobA, jobB);
        ParallelJobScheduler scheduler = new ParallelJobScheduler(4);

        long startTime = System.currentTimeMillis();
        scheduler.scheduleAllJobs(startingJobs);
        long endTime = System.currentTimeMillis();

        // Verify all jobs executed
        List<String> messages = new ArrayList<>(executionOrder);
        assertEquals(18, messages.size()); // 9 jobs * 2 messages each

        // Verify execution order constraints
        verifyExecutionOrder(messages);
        
        // Verify parallel execution efficiency
        // With proper parallelization, total time should be much less than sequential
        // Sequential would take: 9 * 50ms = 450ms
        // Parallel should take roughly: 4 levels * 50ms = 200ms (plus overhead)
        assertTrue(endTime - startTime < 400, 
                "Execution took too long, suggesting lack of parallelization");
    }

    @Test
    void testStressTestWithManyJobs() {
        List<Job> jobs = new ArrayList<>();
        
        // Create a large dependency graph
        // Level 0: 4 starting jobs
        for (int i = 0; i < 4; i++) {
            Job job = new Job("L0_Job" + i, testLogger, deterministicRandom);
            job.setSleepTimeMs(10);
            jobs.add(job);
        }
        
        // Level 1: 8 jobs depending on level 0
        for (int i = 0; i < 8; i++) {
            Job parent = jobs.get(i % 4);
            Job job = new Job("L1_Job" + i, testLogger, deterministicRandom, parent);
            job.setSleepTimeMs(10);
            jobs.add(job);
        }
        
        // Level 2: 4 jobs depending on pairs from level 1
        for (int i = 0; i < 4; i++) {
            Job parent1 = jobs.get(4 + (i * 2));
            Job parent2 = jobs.get(4 + (i * 2) + 1);
            Job job = new Job("L2_Job" + i, testLogger, deterministicRandom, parent1, parent2);
            job.setSleepTimeMs(10);
            jobs.add(job);
        }
        
        List<Job> startingJobs = jobs.subList(0, 4);
        ParallelJobScheduler scheduler = new ParallelJobScheduler(8);
        
        long startTime = System.currentTimeMillis();
        scheduler.scheduleAllJobs(startingJobs);
        long endTime = System.currentTimeMillis();
        
        // Verify all jobs executed
        assertEquals(32, executionOrder.size()); // 16 jobs * 2 messages each
        
        // Should complete reasonably quickly with parallelization
        assertTrue(endTime - startTime < 200);
    }

    @Test
    void testRealWorldScenario() {
        // Simulate a build pipeline
        Job setupJob = new Job("Setup Environment", testLogger, deterministicRandom);
        Job compileJob = new Job("Compile Code", testLogger, deterministicRandom, setupJob);
        Job testUnitJob = new Job("Unit Tests", testLogger, deterministicRandom, compileJob);
        Job testIntegrationJob = new Job("Integration Tests", testLogger, deterministicRandom, compileJob);
        Job packageJob = new Job("Package Application", testLogger, deterministicRandom, testUnitJob, testIntegrationJob);
        Job deployDevJob = new Job("Deploy to Dev", testLogger, deterministicRandom, packageJob);
        Job smokeTestJob = new Job("Smoke Tests", testLogger, deterministicRandom, deployDevJob);
        Job deployProdJob = new Job("Deploy to Production", testLogger, deterministicRandom, smokeTestJob);

        Arrays.asList(setupJob, compileJob, testUnitJob, testIntegrationJob, 
                     packageJob, deployDevJob, smokeTestJob, deployProdJob)
                .forEach(job -> job.setSleepTimeMs(30));

        ParallelJobScheduler scheduler = new ParallelJobScheduler(6);
        scheduler.scheduleAllJobs(Arrays.asList(setupJob));

        List<String> messages = new ArrayList<>(executionOrder);
        assertEquals(16, messages.size()); // 8 jobs * 2 messages each

        // Verify deployment pipeline order
        verifyJobExecutedBeforeJob(messages, "Setup Environment", "Compile Code");
        verifyJobExecutedBeforeJob(messages, "Compile Code", "Unit Tests");
        verifyJobExecutedBeforeJob(messages, "Compile Code", "Integration Tests");
        verifyJobExecutedBeforeJob(messages, "Unit Tests", "Package Application");
        verifyJobExecutedBeforeJob(messages, "Integration Tests", "Package Application");
        verifyJobExecutedBeforeJob(messages, "Package Application", "Deploy to Dev");
        verifyJobExecutedBeforeJob(messages, "Deploy to Dev", "Smoke Tests");
        verifyJobExecutedBeforeJob(messages, "Smoke Tests", "Deploy to Production");
    }

    private void verifyExecutionOrder(List<String> messages) {
        // Job A and Job B can start in parallel
        verifyJobExecutedBeforeJob(messages, "Job A", "Job C");
        verifyJobExecutedBeforeJob(messages, "Job B", "Job D");
        
        // Job E depends on both Job C and Job D
        verifyJobExecutedBeforeJob(messages, "Job C", "Job E");
        verifyJobExecutedBeforeJob(messages, "Job D", "Job E");
        
        // Job F, G, H depend on Job E
        verifyJobExecutedBeforeJob(messages, "Job E", "Job F");
        verifyJobExecutedBeforeJob(messages, "Job E", "Job G");
        verifyJobExecutedBeforeJob(messages, "Job E", "Job H");
        
        // Job I depends on Job F, G, H
        verifyJobExecutedBeforeJob(messages, "Job F", "Job I");
        verifyJobExecutedBeforeJob(messages, "Job G", "Job I");
        verifyJobExecutedBeforeJob(messages, "Job H", "Job I");
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