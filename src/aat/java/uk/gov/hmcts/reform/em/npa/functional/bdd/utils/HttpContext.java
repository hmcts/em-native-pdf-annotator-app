package uk.gov.hmcts.reform.em.npa.functional.bdd.utils;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class HttpContext {

    private String idamAuthorRef;
    private String idamServiceRef;

    //private String rawResponseString;
    private int httpResponseStatusCode;
    private ResponseEntity responseEntity;

    public ResponseEntity getResponseEntity() {
        return responseEntity;
    }

    public void setResponseBodyAndStatesForResponse(ResponseEntity responseEntity) {
        this.responseEntity = responseEntity;
        //rawResponseString = responseEntity.getBody();
        httpResponseStatusCode = responseEntity.getStatusCodeValue();
    }

//    public void setResponseBodyAndStatesForResponse(HttpClientErrorException hcee) {
//        rawResponseString = hcee.getResponseBodyAsString();
//        httpResponseStatusCode = hcee.getRawStatusCode();
//    }

//    public String getRawResponseString() {
//        return rawResponseString;
//    }

    public int getHttpResponseStatusCode() {
        return httpResponseStatusCode;
    }

    public String getIdamAuthorRef() {
        return idamAuthorRef;
    }

    public void setIdamAuthorRef(String idamAuthorRef) {
        this.idamAuthorRef = idamAuthorRef;
    }

    public String getIdamServiceRef() {
        return idamServiceRef;
    }

    public void setIdamServiceRef(String idamServiceRef) {
        this.idamServiceRef = idamServiceRef;
    }
}
