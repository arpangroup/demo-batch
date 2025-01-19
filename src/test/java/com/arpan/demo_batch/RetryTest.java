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
