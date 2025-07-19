# Testing Documentation

## Overview

The codebase has been refactored to be fully unit testable with comprehensive test coverage including unit tests, integration tests, and real-world scenario tests.

## Testability Improvements Made

### 1. Dependency Injection
- **Job class**: Constructor now accepts `Consumer<String> logger` and `Random random` for controllable logging and deterministic behavior
- **ParallelJobScheduler**: Constructor accepts `threadPoolSize` parameter for configurable thread pool size

### 2. Deterministic Behavior
- **Fixed sleep times**: `Job.setSleepTimeMs(int)` method allows setting exact sleep duration for tests
- **Predictable random**: Tests use seeded `Random` objects for consistent behavior

### 3. Observability
- **Logging injection**: Tests can capture and verify log output
- **Thread safety**: Proper synchronization allows concurrent testing

## Test Structure

```
test/
├── models/
│   └── JobTest.java                    # Unit tests for Job class
├── scheduler/
│   └── ParallelJobSchedulerTest.java   # Unit tests for scheduler
└── IntegrationTest.java                # End-to-end integration tests
```

## Running Tests

### Option 1: Using Gradle (Recommended)
```bash
./gradlew test
```

### Option 2: Using Simple Test Runner
```bash
javac -d . src/models/Job.java src/scheduler/ParallelJobScheduler.java TestRunner.java
java TestRunner
```

### Option 3: Manual Compilation and JUnit
```bash
# Compile with JUnit on classpath
javac -cp ".:junit-platform-console-standalone-1.8.2.jar" -d . src/**/*.java test/**/*.java

# Run tests
java -cp ".:junit-platform-console-standalone-1.8.2.jar" org.junit.platform.console.ConsoleLauncher --class-path . --scan-class-path
```

## Test Coverage

### JobTest.java
- ✅ Constructor with no parents
- ✅ Constructor with multiple parents  
- ✅ Job execution with fixed sleep time
- ✅ Thread interruption handling
- ✅ Thread safety of children list modifications
- ✅ Defensive copying of children list
- ✅ CountDownLatch behavior

### ParallelJobSchedulerTest.java
- ✅ Simple job scheduling
- ✅ Jobs with dependencies
- ✅ Cycle detection and prevention
- ✅ Custom thread pool sizes
- ✅ Complex dependency graphs
- ✅ Empty job list handling
- ✅ Single job execution

### IntegrationTest.java
- ✅ Full README example workflow
- ✅ Stress testing with many jobs
- ✅ Real-world build pipeline scenario
- ✅ Performance verification
- ✅ Parallel execution efficiency

## Key Test Features

### 1. Deterministic Testing
```java
// Fixed seed for consistent behavior
Random deterministicRandom = new Random(42);

// Fixed sleep times instead of random
job.setSleepTimeMs(10);
```

### 2. Execution Order Verification
```java
private void verifyJobExecutedBeforeJob(List<String> messages, String jobA, String jobB) {
    // Verifies jobA completes before jobB starts
}
```

### 3. Thread Safety Testing
```java
// Tests concurrent modification of children lists
for (int i = 0; i < 10; i++) {
    Thread thread = new Thread(() -> {
        Job child = new Job("Child" + index, testLogger, deterministicRandom, parent);
    });
    threads.add(thread);
}
```

### 4. Performance Testing
```java
long startTime = System.currentTimeMillis();
scheduler.scheduleAllJobs(startingJobs);
long endTime = System.currentTimeMillis();

// Verify parallel execution is faster than sequential
assertTrue(endTime - startTime < expectedMaxTime);
```

## Test Scenarios Covered

### Unit Test Scenarios
1. **Job Creation**: Various constructor patterns
2. **Dependency Management**: Parent-child relationships
3. **Execution Lifecycle**: Start, work, completion
4. **Error Handling**: Interruption, timeouts
5. **Thread Safety**: Concurrent access patterns

### Integration Test Scenarios
1. **README Example**: Exact workflow from documentation
2. **Build Pipeline**: Real-world CI/CD scenario
3. **Stress Testing**: Large dependency graphs
4. **Edge Cases**: Empty lists, single jobs

### Performance Test Scenarios
1. **Parallel Efficiency**: Verifies jobs run concurrently
2. **Resource Management**: Proper thread pool usage
3. **Scalability**: Performance with many jobs

## Debugging Tests

The test runner includes debug output:
```java
System.out.println("Messages received: " + messages.size());
for (String msg : messages) {
    System.out.println("  " + msg);
}
```

This helps verify execution order and diagnose timing issues.

## Best Practices Implemented

1. **Isolated Tests**: Each test is independent with its own setup
2. **Fast Execution**: Short sleep times for quick test runs
3. **Clear Assertions**: Descriptive error messages
4. **Edge Case Coverage**: Empty inputs, single items, cycles
5. **Concurrent Testing**: Thread safety verification
6. **Real-world Scenarios**: Practical use case validation

## Future Test Enhancements

1. **Property-based Testing**: Generate random job graphs
2. **Load Testing**: Very large job sets
3. **Timeout Testing**: Job execution limits
4. **Resource Monitoring**: Memory and CPU usage
5. **Failure Recovery**: Partial execution scenarios