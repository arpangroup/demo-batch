package com.arpan.demo_batch.utils;

import com.arpan.demo_batch.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

@Slf4j
public class MockItemReader  implements ItemReader<Transaction> {
    private int count = 0;

    @Override
    public Transaction read() {
        log.info("reading....");
        if (count == 0) {
            count++;
            return new Transaction(1, 100.0); // First item
        } else {
            return null; // End of input
        }
    }
}