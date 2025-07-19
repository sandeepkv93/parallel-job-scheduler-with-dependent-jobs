package models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

class JobTest {
    private List<String> logMessages;
    private Consumer<String> testLogger;
    private Random deterministicRandom;

    @BeforeEach
    void setUp() {
        logMessages = new ArrayList<>();
        testLogger = logMessages::add;
        deterministicRandom = new Random(42); // Fixed seed for deterministic tests
    }

    @Test
    void testJobConstructorWithNoParents() {
        Job job = new Job("TestJob", testLogger, deterministicRandom);
        
        assertEquals("TestJob", job.getName());
        assertTrue(job.getParentJobs().isEmpty());
        assertTrue(job.getChildrenJobs().isEmpty());
        assertEquals(0, job.getLatch().getCount());
    }

    @Test
    void testJobConstructorWithParents() {
        Job parent1 = new Job("Parent1", testLogger, deterministicRandom);
        Job parent2 = new Job("Parent2", testLogger, deterministicRandom);
        Job child = new Job("Child", testLogger, deterministicRandom, parent1, parent2);
        
        assertEquals("Child", child.getName());
        assertEquals(2, child.getParentJobs().size());
        assertTrue(child.getParentJobs().contains(parent1));
        assertTrue(child.getParentJobs().contains(parent2));
        assertEquals(2, child.getLatch().getCount());
        
        // Check that child is added to parents' children lists
        assertTrue(parent1.getChildrenJobs().contains(child));
        assertTrue(parent2.getChildrenJobs().contains(child));
    }

    @Test
    void testJobRunWithFixedSleepTime() throws InterruptedException {
        Job job = new Job("TestJob", testLogger, deterministicRandom);
        job.setSleepTimeMs(10); // Very short sleep for testing
        
        long startTime = System.currentTimeMillis();
        job.run();
        long endTime = System.currentTimeMillis();
        
        assertTrue(endTime - startTime >= 10);
        assertEquals(2, logMessages.size());
        assertEquals("TestJob started", logMessages.get(0));
        assertEquals("TestJob completed", logMessages.get(1));
    }

    @Test
    void testJobRunInterruption() throws InterruptedException {
        Job job = new Job("TestJob", testLogger, deterministicRandom);
        job.setSleepTimeMs(5000); // Long sleep
        
        Thread jobThread = new Thread(job);
        jobThread.start();
        
        // Wait a bit then interrupt
        Thread.sleep(50);
        jobThread.interrupt();
        jobThread.join(1000);
        
        assertTrue(logMessages.size() >= 1);
        assertEquals("TestJob started", logMessages.get(0));
        if (logMessages.size() > 1) {
            assertEquals("TestJob was interrupted", logMessages.get(1));
        }
    }

    @Test
    void testThreadSafetyOfChildrenList() throws InterruptedException {
        Job parent = new Job("Parent", testLogger, deterministicRandom);
        List<Thread> threads = new ArrayList<>();
        List<Job> children = new ArrayList<>();
        
        // Create multiple threads that add children concurrently
        for (int i = 0; i < 10; i++) {
            final int index = i;
            Thread thread = new Thread(() -> {
                Job child = new Job("Child" + index, testLogger, deterministicRandom, parent);
                synchronized (children) {
                    children.add(child);
                }
            });
            threads.add(thread);
        }
        
        // Start all threads
        threads.forEach(Thread::start);
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify all children were added
        assertEquals(10, parent.getChildrenJobs().size());
        assertEquals(10, children.size());
    }

    @Test
    void testGetChildrenJobsReturnsDefensiveCopy() {
        Job parent = new Job("Parent", testLogger, deterministicRandom);
        Job child = new Job("Child", testLogger, deterministicRandom, parent);
        
        List<Job> children1 = parent.getChildrenJobs();
        List<Job> children2 = parent.getChildrenJobs();
        
        // Should be different instances but same content
        assertNotSame(children1, children2);
        assertEquals(children1, children2);
        
        // Modifying returned list shouldn't affect original
        children1.clear();
        assertEquals(1, parent.getChildrenJobs().size());
    }

    @Test
    void testCountDownLatchBehavior() throws InterruptedException {
        Job parent1 = new Job("Parent1", testLogger, deterministicRandom);
        Job parent2 = new Job("Parent2", testLogger, deterministicRandom);
        Job child = new Job("Child", testLogger, deterministicRandom, parent1, parent2);
        
        assertEquals(2, child.getLatch().getCount());
        
        // Child should wait for parents
        child.getLatch().countDown();
        assertEquals(1, child.getLatch().getCount());
        assertFalse(child.getLatch().await(10, TimeUnit.MILLISECONDS));
        
        child.getLatch().countDown();
        assertEquals(0, child.getLatch().getCount());
        assertTrue(child.getLatch().await(10, TimeUnit.MILLISECONDS));
    }
}