package uk.gov.hmcts.reform.em.npa.batch;

import org.springframework.batch.item.ItemProcessor;
import uk.gov.hmcts.reform.em.npa.domain.DocumentTask;
import uk.gov.hmcts.reform.em.npa.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.npa.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.npa.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.npa.service.PdfAnnotator;

import java.io.File;

public class DocumentTaskItemProcessor implements ItemProcessor<DocumentTask, DocumentTask> {

    private final DmStoreDownloader dmStoreDownloader;
    private final PdfAnnotator pdfAnnotator;
    private final DmStoreUploader dmStoreUploader;

    public DocumentTaskItemProcessor(DmStoreDownloader dmStoreDownloader, PdfAnnotator pdfAnnotator, DmStoreUploader dmStoreUploader) {
        this.dmStoreDownloader = dmStoreDownloader;
        this.pdfAnnotator = pdfAnnotator;
        this.dmStoreUploader = dmStoreUploader;
    }

    @Override
    public DocumentTask process(DocumentTask item) {

        try {

            File originalFile = dmStoreDownloader.downloadFile(item.getInputDocumentId());

            File annotatedPdf = pdfAnnotator.annotatePdf(originalFile);

            String outputDocumentId = dmStoreUploader.uploadFile(annotatedPdf);

            item.setOutputDocumentId(outputDocumentId);

            item.setTaskState(TaskState.DONE);

        } catch (Exception e) {
            item.setTaskState(TaskState.FAILED);
            item.setFailureDescription(e.getMessage());
        }

        return item;

    }

}
