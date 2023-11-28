package uk.gov.hmcts.reform.em.npa.batch;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import uk.gov.hmcts.reform.em.npa.repository.LargeObjectRepository;

public class RemoveLargeObjectsTasklet implements Tasklet {

    private final LargeObjectRepository largeObjectRepository;
    private final int limit;


    public RemoveLargeObjectsTasklet(LargeObjectRepository largeObjectRepository, int limit) {
        this.largeObjectRepository = largeObjectRepository;
        this.limit = limit;

    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        largeObjectRepository.removeLargeObjects(limit);
        return RepeatStatus.FINISHED;
    }
}
