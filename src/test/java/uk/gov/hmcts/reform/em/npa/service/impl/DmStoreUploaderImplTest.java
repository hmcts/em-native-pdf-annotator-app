package uk.gov.hmcts.reform.em.npa.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.pdfbox.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.npa.Application;
import uk.gov.hmcts.reform.em.npa.TestSecurityConfiguration;
import uk.gov.hmcts.reform.em.npa.config.security.SecurityUtils;
import uk.gov.hmcts.reform.em.npa.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
public class DmStoreUploaderImplTest {

    private static final String PDF_FILENAME = "annotationTemplate.pdf";

    DmStoreUploader dmStoreUploader;

    @Autowired
    private SecurityUtils securityUtils;

    private static Response interceptSuccess(Interceptor.Chain chain) throws IOException {

        if (chain.request().url().toString().endsWith("/binary")) {
            InputStream file = ClassLoader.getSystemResourceAsStream(PDF_FILENAME);

            return new Response.Builder()
                .body(ResponseBody.create(MediaType.get("application/pdf"), IOUtils.toByteArray(file)))
                .request(chain.request())
                .message("")
                .code(200)
                .protocol(Protocol.HTTP_2)
                .build();
        } else {
            return new Response.Builder().body(ResponseBody.create(
            "{\n"
                    + "  \"_embedded\": {\n"
                    + "    \"documents\": [\n"
                    + "      {\n"
                    + "        \"modifiedOn\": \"2020-04-23T14:37:02+0000\",\n"
                    + "        \"size\": 19496,\n"
                    + "        \"createdBy\": \"7f0fd7bf-48c0-4462-9056-38c1190e391f\",\n"
                    + "        \"_links\": {\n"
                    + "          \"thumbnail\": {\n"
                    + "            \"href\": \"http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb/thumbnail\"\n"
                    + "          },\n"
                    + "          \"binary\": {\n"
                    + "            \"href\": \"http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb/binary\"\n"
                    + "          },\n"
                    + "          \"self\": {\n"
                    + "            \"href\": \"http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb\"\n"
                    + "          }\n"
                    + "        },\n"
                    + "        \"_embedded\": {\n"
                    + "          \"allDocumentVersions\": {\n"
                    + "            \"_embedded\": {\n"
                    + "              \"documentVersions\": [\n"
                    + "                {\n"
                    + "                  \"size\": 19496,\n"
                    + "                  \"createdBy\": \"7f0fd7bf-48c0-4462-9056-38c1190e391f\",\n"
                    + "                  \"_links\": {\n"
                    + "                    \"thumbnail\": {\n"
                    + "                      \"href\": \"http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb/versions/da13dae7-f2bc-4a43-937c-83a255f2f72f/thumbnail\"\n"
                    + "                    },\n"
                    + "                    \"document\": {\n"
                    + "                      \"href\": \"http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb\"\n"
                    + "                    },\n"
                    + "                    \"binary\": {\n"
                    + "                      \"href\": \"http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb/versions/da13dae7-f2bc-4a43-937c-83a255f2f72f/binary\"\n"
                    + "                    },\n"
                    + "                    \"self\": {\n"
                    + "                      \"href\": \"http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb/versions/da13dae7-f2bc-4a43-937c-83a255f2f72f\"\n"
                    + "                    }\n"
                    + "                  },\n"
                    + "                  \"originalDocumentName\": \"stitched9163237694642183694.pdf\",\n"
                    + "                  \"mimeType\": \"application/pdf\",\n"
                    + "                  \"createdOn\": \"2020-04-23T14:37:02+0000\"\n"
                    + "                }\n"
                    + "              ]\n"
                    + "            }\n"
                    + "          }\n"
                    + "        },\n"
                    + "        \"lastModifiedBy\": \"7f0fd7bf-48c0-4462-9056-38c1190e391f\",\n"
                    + "        \"originalDocumentName\": \"stitched9163237694642183694.pdf\",\n"
                    + "        \"mimeType\": \"application/pdf\",\n"
                    + "        \"classification\": \"PUBLIC\",\n"
                    + "        \"createdOn\": \"2020-04-23T14:37:02+0000\"\n"
                    + "      }\n"
                    + "    ]\n"
                    + "  }\n"
                    + "}", MediaType.get("application/json")))
                .request(chain.request())
                .message("")
                .code(200)
                .protocol(Protocol.HTTP_2)
                .build();
        }

    }

    private static Response interceptFailure(Interceptor.Chain chain) throws IOException {

        InputStream file = ClassLoader.getSystemResourceAsStream(PDF_FILENAME);

        return new Response.Builder()
            .body(ResponseBody.create(MediaType.get("application/pdf"), IOUtils.toByteArray(file)))
            .request(chain.request())
            .message("")
            .code(404)
            .protocol(Protocol.HTTP_2)
            .build();
    }


    private void setUpSuccess() {
        OkHttpClient http = new OkHttpClient
            .Builder()
            .addInterceptor(DmStoreUploaderImplTest::interceptSuccess)
            .build();

        dmStoreUploader = new DmStoreUploaderImpl(http, () -> "auth", "https://someurl.com",
            securityUtils, new ObjectMapper());
    }

    private void setUpFailure() {
        OkHttpClient http = new OkHttpClient
            .Builder()
            .addInterceptor(DmStoreUploaderImplTest::interceptFailure)
            .build();

        dmStoreUploader = new DmStoreUploaderImpl(http, () -> "auth", "https://someurl.com",
            securityUtils, new ObjectMapper());
    }

    @Test
    public void  testUploadDocumentSuccess() throws Exception {

        setUpSuccess();
        ClassLoader classLoader = getClass().getClassLoader();
        JsonNode response = dmStoreUploader.uploadDocument(new File(classLoader.getResource(PDF_FILENAME).getFile()));

        Assert.assertNotNull(response);
        String docUrl = response.at("/_embedded/documents").get(0).at("/_links/self/href").asText();
        Assert.assertNotNull(docUrl);
        Assert.assertEquals("http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb", docUrl);
    }

    @Test(expected = DocumentTaskProcessingException.class)
    public void  testUploadDocumentFailure() throws Exception {

        setUpFailure();

        dmStoreUploader.uploadDocument(new File("xyz.abc"));
    }
}
