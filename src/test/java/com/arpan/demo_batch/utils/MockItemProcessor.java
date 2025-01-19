package com.arpan.demo_batch.utils;

import com.arpan.demo_batch.exception.UserNotFoundException;
import com.arpan.demo_batch.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
public class MockItemProcessor implements ItemProcessor<Transaction, Transaction> {
    @Override
    public Transaction process(Transaction item) throws UserNotFoundException {
        log.info("Processing: {}", item);
        throw new UserNotFoundException("Processing failed");
//        log.info("processing....{}", item);
//        if (item == null) {
//            throw new UserNotFoundException("Simulation error");
//        }
//        return item; // Pass-through processing for simplicity
    }
}
