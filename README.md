## Spring Batch Retry

Enable Logs for SpringBatch
````properties
logging.level.org.springframework.batch=DEBUG
````

in `RetryTest.java`:
````java
package com.arpan.demo_batch;

import com.arpan.demo_batch.exception.UserNotFoundException;
import com.arpan.demo_batch.model.Transaction;
import com.arpan.demo_batch.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.batch.core.*;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBatchTest
@EnableAutoConfiguration
@Import(TestConfig.class)
public class RetryTest {
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Mock
    private ItemReader<Transaction> itemReader;

    @Mock
    private ItemProcessor<Transaction, Transaction> itemProcessor;


    @BeforeEach
    public void setup() {
        // Manually initialize the schema before running tests
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
        dataSource.setUsername("sa");
        dataSource.setPassword("password");

        dataSourceInitializer.setDataSource(dataSource);
        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
        databasePopulator.addScript(new ClassPathResource("schema-h2.sql"));
        dataSourceInitializer.setDatabasePopulator(databasePopulator);
        dataSourceInitializer.afterPropertiesSet(); // Initializes the schema
    }

    @Test
    public void whenEndpointAlwaysFail_thenJobFails() throws Exception {
        Transaction transaction = new Transaction(1, 100);
        when(itemReader.read()).thenReturn(transaction);


        // Mock the ItemProcessor to simulate a failure during processing
        when(itemProcessor.process(any())).thenThrow(new UserNotFoundException("Processing failed"));


        // Launch job
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(defaultJobParameters());
        JobInstance actualJobInstance = jobExecution.getJobInstance();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        // Debugging: print job execution details
        System.out.println("Job Execution Status: " + actualJobExitStatus.getExitCode());
        //System.out.println("Job Execution Exit Description: " + actualJobExitStatus.getExitDescription());

        // Retrieve StepExecution from JobExecution and get retry count from its ExecutionContext
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();  // Assuming there's only one step
        int actualRetryCount = stepExecution.getExecutionContext().getInt("retryCount", 0);



        // Assertions
        assertEquals("retryBatchJob", actualJobInstance.getJobName());
        assertEquals("FAILED", actualJobExitStatus.getExitCode());
        assertEquals(4, actualRetryCount, "Retry count mismatch");

    }

    private JobParameters defaultJobParameters() {
        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        paramsBuilder.addString("jobID", String.valueOf(System.currentTimeMillis()));
        return paramsBuilder.toJobParameters();
    }
}

````

in `TestConfig.java`
````java
package com.arpan.demo_batch;

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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@TestConfiguration
public class TestConfig {

    @Bean
    public JobRepository jobRepository(DataSource dataSource, PlatformTransactionManager transactionManager) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        factory.setDatabaseType("H2");
        return factory.getObject();
    }


    @Bean
    public Job retryBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        return new JobBuilder("retryBatchJob", jobRepository)
                .listener(new MyJobExecutionListener())
                .start(retryStep(jobRepository, transactionManager))
                .build();
    }

    @Bean
    public Step retryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
                return new StepBuilder("retryStep", jobRepository)
                .<Transaction, Transaction>chunk(5, transactionManager)
                .reader(mockItemReader())
                .processor(mockItemProcessor())
                .writer(mockItemWriter())
                .faultTolerant()
                .retry(UserNotFoundException.class)  // Retry on UserNotFoundException
                .retryLimit(4)  // Retry Limit
                /*.listener(new StepExecutionListenerSupport() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        // Initialize retry count in ExecutionContext
                        stepExecution.getExecutionContext().putInt("retryCount", 0);
                    }

                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        // Log the final retry count
                        int retryCount = stepExecution.getExecutionContext().getInt("retryCount", 0);
                        System.out.println("Final Retry Count: " + retryCount);
                        return super.afterStep(stepExecution);
                    }
                })*/
                .listener(new CustomRetryListener())
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
}

````