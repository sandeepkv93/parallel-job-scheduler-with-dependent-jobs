# parallel-job-scheduler-with-dependent-jobs

Write java program for Parallel Job Scheduler. Given a list of jobs and its children jobs

For example:
  - Job C is a child of Job A and Job C is again a child of Job B. Therefore Job A and Job B can be started in parallel. But Job C needs to wait till both of its parents are completed.
  - Job D and Job E are the children of Job C. Therefore as soon as the Job C completes, both Job D and Job E can be started in parallel. 

Implement the generic solution in Java using Multithreading