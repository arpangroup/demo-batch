package com.arpan.demo_batch.listener;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

public class CustomRetryListener implements RetryListener {

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        // Called before the first retry attempt
        return true; // Allow retry processing to proceed
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        // Called after the last retry attempt
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        // Log error details
        System.err.println("Retry failed. Retry count: " + context.getRetryCount());
        System.err.println("Exception: " + throwable.getMessage());

        // Called on every retry attempt when an error occurs
        StepExecution stepExecution = StepSynchronizationManager.getContext().getStepExecution();
        if (stepExecution != null) {
            int retryCount = context.getRetryCount();
            stepExecution.getExecutionContext().putInt("retryCount", retryCount);
            System.out.println("Retry Count Updated in Listener: " + retryCount);
        } else {
            System.err.println("StepExecution not available!");
        }
    }
}
