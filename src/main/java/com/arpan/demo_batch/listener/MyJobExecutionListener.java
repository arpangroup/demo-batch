package com.arpan.demo_batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

@Slf4j
public class MyJobExecutionListener implements JobExecutionListener {
    @Override
    public void beforeJob(JobExecution jobExecution) {
        // Log or debug the retry count
        Integer retryCount = jobExecution.getExecutionContext().getInt("retryCount", 0);
        System.out.println("Retry Count: " + retryCount);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("Inside MyJobExecutionListener......");
        // Log or debug the retry count
        Integer retryCount = jobExecution.getExecutionContext().getInt("retryCount", 0);
        System.out.println("Final Retry Count: " + retryCount);

        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.error("Job failed with exit code: {}", jobExecution.getExitStatus());
        }
    }
}
