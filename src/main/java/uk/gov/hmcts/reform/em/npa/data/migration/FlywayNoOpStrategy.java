package uk.gov.hmcts.reform.em.npa.data.migration;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

import java.util.stream.Stream;

@Slf4j
public class FlywayNoOpStrategy implements FlywayMigrationStrategy {

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