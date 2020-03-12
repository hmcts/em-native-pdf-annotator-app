package uk.gov.hmcts.reform.em.npa.service.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.npa.Application;
import uk.gov.hmcts.reform.em.npa.TestSecurityConfiguration;
import uk.gov.hmcts.reform.em.npa.service.AnnotationSetDTOToPDAnnotationMapper;
import uk.gov.hmcts.reform.em.npa.service.PdfAnnotator;

import java.io.File;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
@Transactional
public class PdfAnnotatorImplTest {

    @Autowired
    private PdfAnnotator pdfAnnotator;

    @Test(expected = DocumentTaskProcessingException.class)
    public void annotatePdf() throws Exception {
        pdfAnnotator.annotatePdf(new File("xyz.abc"), null);
    }
}