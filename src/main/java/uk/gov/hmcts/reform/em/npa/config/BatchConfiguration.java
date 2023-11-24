package uk.gov.hmcts.reform.em.npa.config;

import jakarta.persistence.EntityManagerFactory;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import uk.gov.hmcts.reform.em.npa.batch.EntityValueProcessor;
import uk.gov.hmcts.reform.em.npa.domain.EntityAuditEvent;

import java.util.Random;
import javax.sql.DataSource;

@EnableBatchProcessing
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT3M", defaultLockAtLeastFor = "PT5S")
@Configuration
@ConditionalOnProperty(name = "scheduling.enabled")
public class BatchConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchConfiguration.class);

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    JobRepository jobRepository;
    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    EntityValueProcessor entityValueProcessor;

    @Value("${spring.batch.entityValueCopy.pageSize}")
    int entryValueCopyPageSize;

    @Value("${spring.batch.entityValueCopy.maxItemCount}")
    int entryValueMaxItemCount;

    @Value("${spring.batch.entityValueCopy.chunkSize}")
    int entryValueCopyChunkSize;

    @Value("${spring.batch.entityValueCopy.enabled}")
    boolean entryValueCopyEnabled;

    private final Random random = new Random();


    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(new JdbcTemplate(dataSource), transactionManager);
    }

    @Bean
    public <T> JpaItemWriter<T> itemWriter() {
        //Below line needs to be removed once the access issue is resolved.
        System.setProperty("pdfbox.fontcache", "/tmp");
        JpaItemWriter<T> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }


    @Scheduled(cron = "${spring.batch.entityValueCopy.cronJobSchedule}")
    @SchedulerLock(name = "${task.env}-entityValueCopyCronJob")
    public void scheduleCopyEntityValueToV2() throws JobParametersInvalidException,
            JobExecutionAlreadyRunningException,
            JobRestartException,
            JobInstanceAlreadyCompleteException {
        LOGGER.info("Entity Value copying enabled : " + entryValueCopyEnabled);
        if (entryValueCopyEnabled) {
            LOGGER.info("Entity Value copying invoked");
            jobLauncher.run(copyEntityValues(copyEntityValuesStep()), new JobParametersBuilder()
                    .addString("date",
                            System.currentTimeMillis() + "-" + random.nextInt(1500, 1800))
                    .toJobParameters());
        }
    }

    @Bean
    public Job copyEntityValues(Step copyEntityValuesStep) {
        return new JobBuilder("copyEntityValues", this.jobRepository)
                .start(copyEntityValuesStep)
                .build();
    }

    @Bean
    public Step copyEntityValuesStep() {
        return new StepBuilder("copyEntityValuesStep", this.jobRepository)
                .<EntityAuditEvent,EntityAuditEvent>chunk(entryValueCopyChunkSize, transactionManager)
                .reader(copyEntityValueReader())
                .processor(entityValueProcessor)
                .writer(itemWriter())
                .build();

    }

    @Bean
    public JpaPagingItemReader<EntityAuditEvent> copyEntityValueReader() {
        return new JpaPagingItemReaderBuilder<EntityAuditEvent>()
                .name("copyEntityValueReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT eae FROM EntityAuditEvent eae "
                        + "WHERE eae.entityValueMigrated = false")
                .pageSize(entryValueCopyPageSize)
                .maxItemCount(entryValueMaxItemCount)
                .build();
    }

}