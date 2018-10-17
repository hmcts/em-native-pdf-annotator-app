package uk.gov.hmcts.reform.em.npa.service.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.npa.Application;
import uk.gov.hmcts.reform.em.npa.domain.DocumentTask;
import uk.gov.hmcts.reform.em.npa.service.DmStoreUploader;

import java.io.File;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@Transactional
public class DmStoreUploaderImplTest {

    @Autowired
    DmStoreUploader dmStoreUploader;

    @Test(expected = DocumentTaskProcessingException.class)
    public void uploadFile() throws Exception {
        DocumentTask task = new DocumentTask();
        task.setJwt("xxx");
        dmStoreUploader.uploadFile(new File("xyz.abc"), task);
    }
}