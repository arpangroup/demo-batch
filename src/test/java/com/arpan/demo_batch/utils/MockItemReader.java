package com.arpan.demo_batch.utils;

import com.arpan.demo_batch.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class MockItemReader  implements ItemReader<Transaction> {
    private int count = 0;

    @Override
    public Transaction read() {
        log.info("Reading item...");
        if (count == 0) {
            count++;
            return new Transaction(1, 100.0); // First item
        }
        /*else if (count == 1) {
            count++;
            return new Transaction(2, 200.0); // Second item
        } */
        else {
            return null; // End of input
        }
    }


    /*private final List<Transaction> transactions;
    private int currentIndex = 0;

    public MockItemReader(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    @Override
    public Transaction read() {
        log.info("Reading item...");
        if (currentIndex < transactions.size()) {
            return transactions.get(currentIndex++);  // Return the current item and increment the index
        } else {
            return null;  // End of input
        }
    }*/
}