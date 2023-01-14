# Parallel Job Scheduler With Dependent Jobs

### Problem Statement
Write java program for Parallel Job Scheduler. Implement the generic solution in Java using Multithreading, given a list of jobs and its children jobs.

For example:
-   Job A and Job B are the starting jobs which can be started in parallel
-   Job C is a child of Job A and Job D is a child of Job B. So, Job C needs to wait until Job A completes and Job D needs to wait till Job B completes
-   Job E is a child of both Job C and Job D. Therefore, it needs to wait until both Job C and Job D completes.
-   Job F, Job G and Job H are the children of Job E. So, as soon as Job E completes, all three of the children jobs can be started in parallel.
-   Finally, Job I is the child of all three jobs Job F, Job G and Job H. So, Job I needs to wait till Job F, Job G and Job H to complete.

This diagram shows the flow of the example described above

![](flow.png)


### Solution

This repository provides an implementation of a parallel job scheduler that can schedule dependent jobs in parallel. The scheduler uses a fixed thread pool to execute the jobs and ensures that child jobs are not executed before their parent jobs are completed.

### Getting Started
To use this scheduler, you need to define the jobs and their dependencies in the form of a directed acyclic graph (DAG). Each job is represented by an instance of the Job class and can have zero or more parent jobs.

For example, to create a job named Job A with no parent jobs, you can use the following code:
```java
Job jobA = new Job("Job A");
```
To create a job named Job B with Job A as its parent job, you can use the following code:
```java
Job jobB = new Job("Job B", jobA);
```

Once you have defined the jobs and their dependencies, you can use the ParallelJobScheduler class to schedule the jobs for execution. The scheduler takes a list of starting jobs as input and schedules all the jobs in the DAG for execution.

For example, to schedule the jobs Job A and Job B for execution, you can use the following code:
```java
List<Job> startingJobs = Arrays.asList(jobA, jobB);
ParallelJobScheduler parallelJobScheduler = new ParallelJobScheduler();
parallelJobScheduler.scheduleAllJobs(startingJobs);
```
The scheduler uses a fixed thread pool with 4 threads for execution. This can be configured by modifying the code in the scheduleAllJobs method.

### Limitations
This scheduler is designed to handle jobs with dependencies in the form of a DAG. If the dependencies form a cycle, the scheduler may not function correctly.
