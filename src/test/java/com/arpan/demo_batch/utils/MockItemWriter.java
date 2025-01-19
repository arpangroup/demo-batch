package com.arpan.demo_batch.utils;

import com.arpan.demo_batch.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@Slf4j
public class MockItemWriter implements ItemWriter<Transaction> {
    @Override
    public void write(Chunk<? extends Transaction> items) {
        log.info("writing....{}", items);
        // Simply do nothing for the test
    }
}