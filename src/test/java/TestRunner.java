import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;

/**
 * Simple test runner for environments without Gradle/Maven
 */
public class TestRunner {
    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("=== Running Unit Tests ===\n");
        
        try {
            // Manually run test methods
            runJobTests();
            runSchedulerTests();
            runIntegrationTests();
            
            System.out.println("\n=== Test Summary ===");
            System.out.println("Tests run: " + testsRun);
            System.out.println("Passed: " + testsPassed);
            System.out.println("Failed: " + testsFailed);
            
            if (testsFailed > 0) {
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Test execution failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void runJobTests() {
        System.out.println("Running Job Tests...");
        runTest("Job Constructor Test", TestRunner::testJobConstructor);
        runTest("Job Execution Test", () -> {
            try {
                testJobExecution();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        runTest("Job Dependencies Test", TestRunner::testJobDependencies);
    }
    
    private static void runSchedulerTests() {
        System.out.println("\nRunning Scheduler Tests...");
        runTest("Simple Scheduling Test", TestRunner::testSimpleScheduling);
        runTest("Dependency Scheduling Test", TestRunner::testDependencyScheduling);
    }
    
    private static void runIntegrationTests() {
        System.out.println("\nRunning Integration Tests...");
        runTest("Full Workflow Test", TestRunner::testFullWorkflow);
    }
    
    private static void runTest(String testName, Runnable test) {
        testsRun++;
        try {
            test.run();
            System.out.println("✓ " + testName + " PASSED");
            testsPassed++;
        } catch (Exception e) {
            System.out.println("✗ " + testName + " FAILED: " + e.getMessage());
            testsFailed++;
        }
    }
    
    // Simple test implementations
    private static void testJobConstructor() {
        models.Job job = new models.Job("TestJob");
        if (!job.getName().equals("TestJob")) {
            throw new RuntimeException("Job name not set correctly");
        }
        if (!job.getParentJobs().isEmpty()) {
            throw new RuntimeException("Job should have no parents");
        }
    }
    
    private static void testJobExecution() throws InterruptedException {
        List<String> messages = new ArrayList<>();
        java.util.function.Consumer<String> logger = messages::add;
        java.util.Random random = new java.util.Random(42);
        
        models.Job job = new models.Job("TestJob", logger, random);
        job.setSleepTimeMs(10);
        job.run();
        
        if (messages.size() != 2) {
            throw new RuntimeException("Expected 2 log messages, got " + messages.size());
        }
        if (!messages.get(0).equals("TestJob started")) {
            throw new RuntimeException("Wrong start message");
        }
        if (!messages.get(1).equals("TestJob completed")) {
            throw new RuntimeException("Wrong completion message");
        }
    }
    
    private static void testJobDependencies() {
        List<String> messages = new ArrayList<>();
        java.util.function.Consumer<String> logger = messages::add;
        java.util.Random random = new java.util.Random(42);
        
        models.Job parent = new models.Job("Parent", logger, random);
        models.Job child = new models.Job("Child", logger, random, parent);
        
        if (child.getParentJobs().size() != 1) {
            throw new RuntimeException("Child should have 1 parent");
        }
        if (parent.getChildrenJobs().size() != 1) {
            throw new RuntimeException("Parent should have 1 child");
        }
    }
    
    private static void testSimpleScheduling() {
        List<String> messages = new ArrayList<>();
        java.util.function.Consumer<String> logger = messages::add;
        java.util.Random random = new java.util.Random(42);
        
        models.Job job = new models.Job("SimpleJob", logger, random);
        job.setSleepTimeMs(10);
        
        scheduler.ParallelJobScheduler scheduler = new scheduler.ParallelJobScheduler(1);
        scheduler.scheduleAllJobs(java.util.Arrays.asList(job));
        
        if (messages.size() != 2) {
            throw new RuntimeException("Expected 2 messages, got " + messages.size());
        }
    }
    
    private static void testDependencyScheduling() {
        List<String> messages = new ArrayList<>();
        java.util.function.Consumer<String> logger = messages::add;
        java.util.Random random = new java.util.Random(42);
        
        models.Job jobA = new models.Job("JobA", logger, random);
        models.Job jobB = new models.Job("JobB", logger, random, jobA);
        
        jobA.setSleepTimeMs(10);
        jobB.setSleepTimeMs(10);
        
        scheduler.ParallelJobScheduler scheduler = new scheduler.ParallelJobScheduler(2);
        scheduler.scheduleAllJobs(java.util.Arrays.asList(jobA));
        
        // Verify JobA completed before JobB started
        int aCompletedIndex = -1;
        int bStartedIndex = -1;
        
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).equals("JobA completed")) {
                aCompletedIndex = i;
            }
            if (messages.get(i).equals("JobB started")) {
                bStartedIndex = i;
            }
        }
        
        if (aCompletedIndex == -1 || bStartedIndex == -1 || aCompletedIndex >= bStartedIndex) {
            throw new RuntimeException("JobA should complete before JobB starts");
        }
    }
    
    private static void testFullWorkflow() {
        List<String> messages = new ArrayList<>();
        java.util.function.Consumer<String> logger = messages::add;
        java.util.Random random = new java.util.Random(42);
        
        models.Job jobA = new models.Job("JobA", logger, random);
        models.Job jobB = new models.Job("JobB", logger, random);
        models.Job jobC = new models.Job("JobC", logger, random, jobA);
        
        jobA.setSleepTimeMs(10);
        jobB.setSleepTimeMs(10);
        jobC.setSleepTimeMs(10);
        
        scheduler.ParallelJobScheduler scheduler = new scheduler.ParallelJobScheduler(4);
        scheduler.scheduleAllJobs(java.util.Arrays.asList(jobA, jobB));
        
        // Debug: print actual messages
        System.out.println("Messages received: " + messages.size());
        for (String msg : messages) {
            System.out.println("  " + msg);
        }
        
        if (messages.size() < 4) { // At least jobA and jobB should execute
            throw new RuntimeException("Expected at least 4 messages, got " + messages.size());
        }
    }
}