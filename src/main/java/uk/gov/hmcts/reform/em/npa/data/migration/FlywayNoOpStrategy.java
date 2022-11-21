package uk.gov.hmcts.reform.em.npa.data.migration;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

import java.util.stream.Stream;


public class FlywayNoOpStrategy implements FlywayMigrationStrategy {
    private final Logger log = LoggerFactory.getLogger(FlywayNoOpStrategy.class);
    @Override
    public void migrate(Flyway flyway) {
        log.info("flyway migration invoked {}",
                flyway);
        Stream.of(flyway.info().all())
                .peek(info ->
                        log.info("flyway migration info {} and state {}",
                                info,
                                info.getState()))
                .filter(info -> !info.getState().isApplied())
                .findFirst()
                .ifPresent(info -> {
                    throw new PendingMigrationScriptException(info.getScript());
                });
    }
}