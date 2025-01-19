package com.arpan.demo_batch.processor;

import com.arpan.demo_batch.exception.UserNotFoundException;
import com.arpan.demo_batch.model.Transaction;
import com.arpan.demo_batch.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * it processes each record by hitting a REST endpoint to fetch the user’s age and postCode attributes:
 */
@Slf4j
public class RetryItemProcessor implements ItemProcessor<Transaction, Transaction> {

    @Autowired
    private RestTemplate restTemplate;

    private final String baseUrl = "http://localhost:9191/users/";

    @Override
    public Transaction process(Transaction transaction) throws UserNotFoundException {
        System.out.println(">>>>>>>>>>>>> Processing: " + transaction);
        log.info("Processing: {}", transaction);
        if (transaction.getUserId() == 2) {
            System.err.println("Id " + transaction.getUserId() + " not found for processing");
            throw new UserNotFoundException("Item not found for processing");
        }
        User user = fetchMoreUserDetails(transaction.getUserId());

        transaction.setUsername(user.getName());
        transaction.setEmail(user.getEmail());
        log.info("returning processor ....");
        return transaction;
    }



    private User fetchMoreUserDetails(int userId) throws UserNotFoundException {
        log.info("fetchMoreUserDetails for userId: {}", userId);
        User user = null;
        try {
            String url = baseUrl + userId;
            log.info("invoking: {}", url);
            user = restTemplate.getForObject(url, User.class);
        } catch (RestClientException ex) {
            log.error("Error fetchMoreUserDetails::RestClientException........ {}", ex.getMessage());
            throw new UserNotFoundException(ex.getMessage());
        } catch (Exception e) {
            log.error("Error fetchMoreUserDetails::Exception........ {}", e.getMessage());
            throw new UserNotFoundException(e.getMessage());
        }
        return user;
    }
}
