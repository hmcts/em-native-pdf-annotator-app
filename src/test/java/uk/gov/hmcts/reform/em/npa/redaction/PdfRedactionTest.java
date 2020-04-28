package uk.gov.hmcts.reform.em.npa.redaction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.npa.Application;
import uk.gov.hmcts.reform.em.npa.TestSecurityConfiguration;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.MarkUpDTO;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
public class PdfRedactionTest {
    private static final File TEST_PDF_FILE = new File(
            ClassLoader.getSystemResource("dummy.pdf").getPath()
    );

    @Autowired
    private PdfRedaction pdfRedaction;

    private List<MarkUpDTO> markUpDTOList = new ArrayList<>();

    @Before
    public void setup() {
        initRedactionDTOList();
    }

    public void initRedactionDTOList() {
        MarkUpDTO dto = new MarkUpDTO();

        dto.setPageNumber(1);
        dto.setXcoordinate(100);
        dto.setYcoordinate(100);
        dto.setHeight(100);
        dto.setWidth(100);

        markUpDTOList.add(dto);

        for (int i = 0; i < 5 ; i++) {
            MarkUpDTO markUpDTO = new MarkUpDTO();

            markUpDTO.setPageNumber(i + 1);
            markUpDTO.setXcoordinate(100 * (i + 1));
            markUpDTO.setYcoordinate(100 * (i + 1));
            markUpDTO.setHeight(100 * (i + 1));
            markUpDTO.setWidth(100 * (i + 1));

            markUpDTOList.add(markUpDTO);
        }
    }

    @Test
    public void pdfRedactionTest() throws IOException {
        File result = pdfRedaction.redaction(TEST_PDF_FILE, markUpDTOList);
        Assert.assertTrue(result.getName().contains("altered"));
        Assert.assertTrue(result.getName().contains(".pdf"));
    }

    @Test(expected = IOException.class)
    public void pdfRedactionFailureTest() throws IOException {
        File result = pdfRedaction.redaction(new File("invalid_file"), markUpDTOList);
    }
}
