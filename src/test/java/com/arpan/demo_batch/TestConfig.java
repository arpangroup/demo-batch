package com.arpan.demo_batch;

import com.arpan.demo_batch.listener.CustomRetryCallback;
import com.arpan.demo_batch.listener.CustomSkipListener;
import com.arpan.demo_batch.listener.MyJobExecutionListener;
import com.arpan.demo_batch.exception.UserNotFoundException;
import com.arpan.demo_batch.listener.CustomRetryListener;
import com.arpan.demo_batch.model.Transaction;
import com.arpan.demo_batch.utils.MockItemProcessor;
import com.arpan.demo_batch.utils.MockItemReader;
import com.arpan.demo_batch.utils.MockItemWriter;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

@TestConfiguration
public class TestConfig {

    @Autowired
    JobRepository jobRepository;


    /*@Bean
    public JobRepository jobRepository(DataSource dataSource, PlatformTransactionManager transactionManager) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        factory.setDatabaseType("H2");
        return factory.getObject();
    }*/

    @Bean
    public RetryPolicy retryPolicy() {
        return new SimpleRetryPolicy(3); // Maximum 3 retries
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3); // Max 3 retries

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.registerListener(new CustomRetryListener());
        return retryTemplate;
    }



    @Bean
    public Job retryBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager, RetryTemplate retryTemplate) throws Exception {
        return new JobBuilder("retryBatchJob", jobRepository)
                .listener(new MyJobExecutionListener())
                .start(retryStep(jobRepository, transactionManager, retryTemplate))
                .build();
    }

    @Bean
    public Step retryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, RetryTemplate retryTemplate) throws Exception {

        // Create a RetryTemplate
        /*RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3); // Set maximum retry attempts
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.registerListener(new CustomRetryListener());
        retryTemplate.registerListener(new CustomRetryListener());*/

        return new StepBuilder("retryStep", jobRepository)
                .<Transaction, Transaction>chunk(5, transactionManager)
                .reader(mockItemReader())
                //.processor(mockItemProcessor())
                .processor(item -> retryTemplate.execute(context -> {
                    context.setAttribute("itemId", item.getUserId()); // Pass the item ID
                    context.setAttribute("maxAttempts", 3);      // Max retry attempts
                    return mockItemProcessor().process(item);
                }))
                .writer(mockItemWriter())
                .faultTolerant()
                .retry(UserNotFoundException.class)  // Retry on UserNotFoundException
                .retryLimit(4)  // Retry Limit
                .listener(new CustomRetryListener())
                .skipLimit(100)
                .skip(UserNotFoundException.class)
                .listener(new CustomSkipListener())
                .build();
    }

    @Bean
    public ItemReader<Transaction> mockItemReader() throws Exception {
        System.out.println("mockItemReader........");
        return new MockItemReader();
    }

    @Bean
    public ItemProcessor<Transaction, Transaction> mockItemProcessor() {
        System.out.println("mockItemProcessor........");
        return new MockItemProcessor();
    }

    @Bean
    public ItemWriter<Transaction> mockItemWriter() {
        System.out.println("mockItemWriter........");
        return new MockItemWriter();
    }




   /* @Retryable(value = UserNotFoundException.class, maxAttempts = 3)
    public void processTransaction(Transaction transaction) {
        // Retry logic
    }

    @Recover
    public void recover(UserNotFoundException e, Transaction transaction) {
        // Recovery logic after retries are exhausted
        System.out.println("Recovery logic after retries exhausted.");
    }*/

}
