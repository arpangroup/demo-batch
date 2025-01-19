package com.arpan.demo_batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

@Slf4j
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
        log.error("Retry failed. Retry count: {}", context.getRetryCount());
        log.error("Exception: {}", throwable.getMessage());

        // Get the retry item from the context (it is usually stored as an attribute in the context)
        T retryItem = (T) context.getAttribute("item");  // "item" is the default attribute name for the retry item
        log.info("Retrying item: {}", retryItem);

        // Called on every retry attempt when an error occurs
        StepExecution stepExecution = StepSynchronizationManager.getContext().getStepExecution();
        if (stepExecution != null) {
            int retryCount = context.getRetryCount();
            stepExecution.getExecutionContext().putInt("retryCount", retryCount);
            log.info("Retry Count Updated in Listener: {}", retryCount);
        } else {
            System.err.println("StepExecution not available!");
        }
    }
}
