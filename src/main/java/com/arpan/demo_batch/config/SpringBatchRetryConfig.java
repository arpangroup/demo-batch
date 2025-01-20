package com.arpan.demo_batch.config;

import com.arpan.demo_batch.exception.UserNotFoundException;
import com.arpan.demo_batch.listener.CustomRetryListener;
import com.arpan.demo_batch.listener.CustomSkipListener;
import com.arpan.demo_batch.listener.MyJobExecutionListener;
import com.arpan.demo_batch.processor.RetryItemProcessor;
import com.arpan.demo_batch.model.Transaction;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import writer.CustomJsonFileItemWriter;

@Configuration
//@ConditionalOnProperty(name = "spring.batch.job.enabled", havingValue = "true")
@Slf4j
public class SpringBatchRetryConfig {
    private static final String[] tokens = { "userid", "amount" };

    @Value("file:input/recordRetry.csv")
    private Resource  inputCsv;

    @Value("file:output/retryOutput.json")
    private WritableResource outputXml;

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3); // Max 3 retries

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.registerListener(new CustomRetryListener());
        return retryTemplate;
    }

    @Bean(name = "retryBatchJob")
    public Job retryJob(JobRepository jobRepository, @Qualifier("retryStep") Step retryStep) {
        log.info("retryJob........");
        return new JobBuilder("retryBatchJob", jobRepository)
                .listener(new MyJobExecutionListener())
                //.incrementer(new RunIdIncrementer())
                //.listener(customerJobExecutionListener)
                .start(retryStep)
                .build();
    }


    @Bean
    public Step retryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, RetryTemplate retryTemplate) throws Exception {
        log.info("Starting retryStep.......");
        return new StepBuilder("retryStep", jobRepository)
                .<Transaction, Transaction>chunk(5, transactionManager) // <I, O> represent the input & output types of the chunk processing
                .reader(itemReader(inputCsv))
                //.processor(retryItemProcessor())
                .processor(item -> retryTemplate.execute(context -> {
                    context.setAttribute("itemId", item.getUserId()); // Pass the item ID
                    context.setAttribute("maxAttempts", 3);      // Max retry attempts
                    return retryItemProcessor().process(item);
                }))
                .writer(jsonFileItemWriter())
                .faultTolerant()
                .retry(UserNotFoundException.class) // Retry step-level exceptions
                .retryLimit(3)
                .listener(new CustomRetryListener())
                .skipLimit(100)
                .skip(UserNotFoundException.class)
                .listener(new CustomSkipListener())
                .build();
    }

    // #################################################################################################
    // #################################################################################################

    @Bean
    public ItemReader<Transaction> itemReader(Resource inputData) throws Exception {
        log.info("inside itemReader........");
        FlatFileItemReader<Transaction> flatFileItemReader = new FlatFileItemReader<>();
        //flatFileItemReader.setResource(new ClassPathResource("customers.csv"));
        flatFileItemReader.setResource(inputData);
        flatFileItemReader.setName("CSV-Reader");
        flatFileItemReader.setLinesToSkip(1);
        flatFileItemReader.setLineMapper(lineMapper());

        // Debugging: Log the items read
        flatFileItemReader.setSaveState(false); // Disable state saving for debugging
        flatFileItemReader.open(new ExecutionContext());
        Transaction transaction;
        while ((transaction = flatFileItemReader.read()) != null) {
            System.out.println("Read item: " + transaction);
        }
        flatFileItemReader.close();


        return flatFileItemReader;
    }

    @Bean
    public ItemProcessor<Transaction, Transaction> retryItemProcessor() {
        return new RetryItemProcessor();
    }

    @Bean
    public JsonFileItemWriter<Transaction> jsonFileItemWriter() {
        log.info("Inside writer ....");
        /*return new JsonFileItemWriterBuilder<Transaction>()
                .jsonObjectMarshaller(new JacksonJsonObjectMarshaller<>())
                //.resource(new FileSystemResource("output/transactions.json"))
                .resource(outputXml)
                .name("transactionJsonFileItemWriter")
                .build();*/
        return new CustomJsonFileItemWriter<>(outputXml, new JacksonJsonObjectMarshaller<>(), retryTemplate());
    }

    public LineMapper<Transaction> lineMapper() {
        log.info("inside lineMapper........");
        DefaultLineMapper<Transaction> lineMapper = new DefaultLineMapper<>();

        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setStrict(false);
        lineTokenizer.setNames(tokens);

        //DefaultLineMapper<Transaction> lineMapper = new DefaultLineMapper<>();
        BeanWrapperFieldSetMapper<Transaction> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Transaction.class);

        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);

        return lineMapper;
    }

    @Bean
    public FixedBackOffPolicy fixedBackOffPolicy() {
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(0); // 2 seconds fixed delay between retries
        return fixedBackOffPolicy;
    }

    @PostConstruct
    public void init() {
        System.out.println("######################################");
        log.info("Output JSON file path: {}", outputXml.getFilename());
        log.info("Output JSON file exists: {}", outputXml.exists());
        System.out.println("######################################");
    }

    /*@Bean
    public PlatformTransactionManager platformTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }*/
}
