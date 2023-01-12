# Parallel Job Scheduler With Dependent Jobs

Write java program for Parallel Job Scheduler. Given a list of jobs and its children jobs

For example:

-   Job A and Job B are the starting jobs which can be started in parallel
-   Job C is a child of Job A and Job D is a child of Job B. So, Job C needs to wait until Job A completes and Job D needs to wait till Job B completes
-   Job E is a child of both Job C and Job D. Therefore, it needs to wait until both Job C and Job D completes.
-   Job F, Job G and Job H are the children of Job E. So, as soon as Job E completes, all three of the children jobs can be started in parallel.
-   Finally, Job I is the child of all three jobs Job F, Job G and Job H. So, Job I needs to wait till Job F, Job G and Job H to complete.

This diagram shows the flow of the example described above

![](flow.png)

#### Implement the generic solution in Java using Multithreading
