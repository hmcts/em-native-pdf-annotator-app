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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.em.npa.Application;
import uk.gov.hmcts.reform.em.npa.TestSecurityConfiguration;
import uk.gov.hmcts.reform.em.npa.config.security.SecurityUtils;
import uk.gov.hmcts.reform.em.npa.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
class DmStoreUploaderImplTest {

    private static final String PDF_FILENAME = "annotationTemplate.pdf";

    DmStoreUploader dmStoreUploader;

    @Autowired
    private SecurityUtils securityUtils;

    private static Response interceptSuccess(Interceptor.Chain chain) throws IOException {

        if (chain.request().url().toString().endsWith("/binary")) {
            InputStream file = ClassLoader.getSystemResourceAsStream(PDF_FILENAME);

            return new Response.Builder()
                .body(ResponseBody.create(IOUtils.toByteArray(file), MediaType.get("application/pdf")))
                .request(chain.request())
                .message("")
                .code(200)
                .protocol(Protocol.HTTP_2)
                .build();
        } else {
            return new Response.Builder()
                .body(ResponseBody.create(
                    """
                      {
                      
                      "_embedded": {
                      
                        "documents": [
                      
                          {
                      
                            "modifiedOn": "2020-04-23T14:37:02+0000",
                      
                            "size": 19496,
                      
                            "createdBy": "7f0fd7bf-48c0-4462-9056-38c1190e391f",
                      
                            "_links": {
                      
                              "thumbnail": {
                      
                                "href": "http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb/thumbnail"
                      
                              },
                      
                              "binary": {
                      
                                "href": "http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb/binary"
                      
                              },
                      
                              "self": {
                      
                                "href": "http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb"
                      
                              }
                      
                            },
                      
                            "_embedded": {
                      
                              "allDocumentVersions": {
                      
                                "_embedded": {
                      
                                  "documentVersions": [
                      
                                    {
                      
                                      "size": 19496,
                      
                                      "createdBy": "7f0fd7bf-48c0-4462-9056-38c1190e391f",
                      
                                      "_links": {
                      
                                        "thumbnail": {
                      
                                          "href": "http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb/versions/da13dae7-f2bc-4a43-937c-83a255f2f72f/thumbnail"
                      
                        },
                      
                        "document": {
                      
                          "href": "http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb"
                      
                        },
                      
                        "binary": {
                      
                          "href": "http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb/versions/da13dae7-f2bc-4a43-937c-83a255f2f72f/binary"
                      
                                        },
                      
                                        "self": {
                      
                                          "href": "http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb/versions/da13dae7-f2bc-4a43-937c-83a255f2f72f"
                      
                                            }
                      
                                          },
                      
                                          "originalDocumentName": "stitched9163237694642183694.pdf",
                      
                                          "mimeType": "application/pdf",
                      
                                          "createdOn": "2020-04-23T14:37:02+0000"
                      
                                        }
                      
                                      ]
                      
                                    }
                      
                                  }
                      
                                },
                      
                                "lastModifiedBy": "7f0fd7bf-48c0-4462-9056-38c1190e391f",
                      
                                "originalDocumentName": "stitched9163237694642183694.pdf",
                      
                                "mimeType": "application/pdf",
                      
                                "classification": "PUBLIC",
                      
                                "createdOn": "2020-04-23T14:37:02+0000"
                      
                              }
                      
                            ]
                      
                          }
                      
                        }""", MediaType.get("application/json")))
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
            .body(ResponseBody.create(IOUtils.toByteArray(file), MediaType.get("application/pdf")))
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
    void  testUploadDocumentSuccess() throws Exception {

        setUpSuccess();
        ClassLoader classLoader = getClass().getClassLoader();
        JsonNode response = dmStoreUploader.uploadDocument(new File(classLoader.getResource(PDF_FILENAME).getFile()));

        assertNotNull(response);
        String docUrl = response.at("/_embedded/documents").get(0).at("/_links/self/href").asText();
        assertNotNull(docUrl);
        assertEquals("http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb", docUrl);
    }

    @Test
    void  testUploadDocumentFailureInvalidMime() {

        setUpFailure();
        assertThrows(DocumentTaskProcessingException.class, () ->
            dmStoreUploader.uploadDocument(new File("xyz.abc")));
    }

    @Test
    void  testUploadDocumentFailureResponse() {

        setUpFailure();
        ClassLoader classLoader = getClass().getClassLoader();
        assertThrows(DocumentTaskProcessingException.class, () ->
            dmStoreUploader.uploadDocument(new File(classLoader.getResource(PDF_FILENAME).getFile())));
    }
}
