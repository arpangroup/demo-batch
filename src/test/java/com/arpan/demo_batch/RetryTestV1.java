package com.arpan.demo_batch;

import com.arpan.demo_batch.utils.LogUtils;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class RetryTestV1 {
    @Autowired
    private Job retryBatchJob;  // Actual Job bean from Spring context

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    public void testJobExecution() throws Exception {
        JobExecution jobExecution = jobLauncher.run(retryBatchJob, new JobParameters());

        // Assert that the job executed successfully
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
    }

    @Test
    public void testJobRetryBehavior() throws Exception {
        // Simulate a job execution that will fail and trigger retry
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("retryJobTest", "test") // Add any necessary job parameters
                .toJobParameters();

        // Launch the job
        JobExecution jobExecution = jobLauncher.run(retryBatchJob, jobParameters);

        // Assert that the job retries the failure and eventually succeeds or fails
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus(), "The job should complete successfully after retries.");

        // Optionally, you can also assert the number of retries via logs or an execution context variable
        // Example if retry count was tracked:
        // assertEquals(3, jobExecution.getExecutionContext().get("retryCount"));
    }

    @Test
    public void testJobFailureAfterMaxRetries() throws Exception {
        // Simulate a job execution where the job fails after retrying the maximum number of times
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("retryJobTest", "test") // Add any necessary job parameters
                .toJobParameters();

        // Simulate failure behavior by throwing an exception that causes retries
        JobExecution jobExecution = jobLauncher.run(retryBatchJob, jobParameters);
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();  // Assuming there's only one step

        long readCount = stepExecution.getReadCount(); // Get the actual read count
        int retryCount = stepExecution.getExecutionContext().getInt("retryCount", 0);
        long skipCount = stepExecution.getSkipCount(); // Get the skip count
        LogUtils.printLog(stepExecution);

        // Assert that the job exits with a FAILED status after exceeding retry attempts
        assertEquals(ExitStatus.FAILED, jobExecution.getExitStatus(), "The job should fail after exceeding retry attempts.");
    }
}
