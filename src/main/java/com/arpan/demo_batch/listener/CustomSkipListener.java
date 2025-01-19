package com.arpan.demo_batch.listener;

import com.arpan.demo_batch.model.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
public class CustomSkipListener implements SkipListener<Transaction, Number> {

    @Override
    public void onSkipInRead(Throwable t) {
        log.info("A failure on read {} ", t.getMessage());
    }

    @Override
    public void onSkipInWrite(Number item, Throwable t) {
        log.info("A failure on write {} , {}", t.getMessage(), item);
    }

    @Override
    public void onSkipInProcess(Transaction item, Throwable t) {
        log.info("onSkipInProcess: {}", item);
        try {
            log.info("Item {}  was skipped due to the exception  {}", new ObjectMapper().writeValueAsString(item), t.getMessage());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
