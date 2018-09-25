package uk.gov.hmcts.reform.em.npa.batch;

import org.springframework.batch.item.ItemProcessor;
import uk.gov.hmcts.reform.em.npa.domain.DocumentTask;
import uk.gov.hmcts.reform.em.npa.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.npa.service.AnnotationSetFetcher;
import uk.gov.hmcts.reform.em.npa.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.npa.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.npa.service.PdfAnnotator;
import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.AnnotationSetDTO;
import uk.gov.hmcts.reform.em.npa.service.impl.DocumentTaskProcessingException;

import java.io.File;

public class DocumentTaskItemProcessor implements ItemProcessor<DocumentTask, DocumentTask> {

    private final DmStoreDownloader dmStoreDownloader;
    private final PdfAnnotator pdfAnnotator;
    private final DmStoreUploader dmStoreUploader;
    private final AnnotationSetFetcher annotationSetFetcher;


    public DocumentTaskItemProcessor(DmStoreDownloader dmStoreDownloader, PdfAnnotator pdfAnnotator, DmStoreUploader dmStoreUploader, AnnotationSetFetcher annotationSetFetcher) {
        this.dmStoreDownloader = dmStoreDownloader;
        this.pdfAnnotator = pdfAnnotator;
        this.dmStoreUploader = dmStoreUploader;
        this.annotationSetFetcher = annotationSetFetcher;
    }

    @Override
    public DocumentTask process(DocumentTask item) {

        try {

            File originalFile = dmStoreDownloader.downloadFile(item.getInputDocumentId());

            AnnotationSetDTO annotationSetDTO = annotationSetFetcher.fetchAnnotationSet(item.getInputDocumentId(), item.getJwt());

            File annotatedPdf = pdfAnnotator.annotatePdf(originalFile, annotationSetDTO);

            String outputDocumentId = dmStoreUploader.uploadFile(annotatedPdf);

            item.setOutputDocumentId(outputDocumentId);

            item.setTaskState(TaskState.DONE);

        } catch (DocumentTaskProcessingException e) {

            item.setTaskState(TaskState.FAILED);

            item.setFailureDescription(e.getMessage());

        }

        return item;

    }

}
