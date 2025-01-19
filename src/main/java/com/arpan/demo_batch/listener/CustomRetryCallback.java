package com.arpan.demo_batch.listener;

import com.arpan.demo_batch.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;

@Slf4j
public class CustomRetryCallback implements RetryCallback<Transaction, Exception> {

    @Override
    public Transaction doWithRetry(RetryContext context) throws Exception {
        // Simulate some retryable logic
        Transaction transaction = new Transaction(1, 100);  // Example item
        context.setAttribute("item", transaction);  // Set the item in the retry context
        System.out.println("ITEM_FAILED: " + transaction);

        return transaction;  // Return the processed item or throw an exception to trigger retry
    }
}
