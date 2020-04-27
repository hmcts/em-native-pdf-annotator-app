package uk.gov.hmcts.reform.em.npa.ccd.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.junit.Test;
import uk.gov.hmcts.reform.em.npa.ccd.dto.CcdCallbackDto;
import uk.gov.hmcts.reform.em.npa.ccd.exception.CallbackException;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.Assert.assertThrows;

public class CcdDataApiCaseUpdaterTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    private CcdDataApiCaseUpdater ccdDataApiCaseUpdater;

    @Test
    public void executeUpdate() throws Exception {
        ccdDataApiCaseUpdater = buildTestedService(200, "OK");
        CcdCallbackDto ccdCallbackDto = new CcdCallbackDto();
        ccdCallbackDto.setCcdPayload(objectMapper.readTree("{}"));
        ccdDataApiCaseUpdater.executeUpdate(ccdCallbackDto, "jwt");
        assertTrue(true, "No exceptions");
    }

    @Test
    public void executeUpdateWith400Response() throws Exception {
        ccdDataApiCaseUpdater = buildTestedService(400, "OK");
        CcdCallbackDto ccdCallbackDto = new CcdCallbackDto();
        ccdCallbackDto.setCcdPayload(objectMapper.readTree("{}"));
        assertThrows(CallbackException.class, () ->
                ccdDataApiCaseUpdater.executeUpdate(ccdCallbackDto, "jwt"));
    }

    private OkHttpClient mockHttp(int httpStatus, String responseBody) {
        return new OkHttpClient
                .Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .body(
                                ResponseBody.create(
                                        MediaType.get("application/json"),
                                        responseBody
                                )
                        )
                        .request(chain.request())
                        .message("")
                        .code(httpStatus)
                        .protocol(Protocol.HTTP_2)
                        .build())
                .build();
    }

    private CcdDataApiCaseUpdater buildTestedService(int httpStatus, String responseBody) {
        return new CcdDataApiCaseUpdater(mockHttp(httpStatus, responseBody), () -> "auth", "http://fake.url.com",
                objectMapper);
    }
}
