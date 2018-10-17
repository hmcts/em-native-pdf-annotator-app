package uk.gov.hmcts.reform.em.npa.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.npa.service.AnnotationSetFetcher;
import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.AnnotationSetDTO;

import java.io.IOException;
import java.net.ConnectException;


@Service
@Transactional
public class AnnotationSetFetcherImpl implements AnnotationSetFetcher {

    private final OkHttpClient okHttpClient;

    private final AuthTokenGenerator authTokenGenerator;

    private String annotationApiEndpointBase;

    private final String annotationEndpoint = "/api/annotation-sets/filter?documentId=";

    private final ObjectMapper objectMapper;


    public AnnotationSetFetcherImpl(OkHttpClient okHttpClient,
                                    AuthTokenGenerator authTokenGenerator,
                                    ObjectMapper objectMapper,
                                    @Value("${em-annotation-app.base-url}") String annotationApiEndpointBase) {
        this.okHttpClient = okHttpClient;
        this.annotationApiEndpointBase = annotationApiEndpointBase;
        this.authTokenGenerator = authTokenGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public AnnotationSetDTO fetchAnnotationSet(String documentId, String jwt) throws DocumentTaskProcessingException {

        try {

            Request request = new Request.Builder()
                    .addHeader("Authorization", jwt)
                    .addHeader("ServiceAuthorization", authTokenGenerator.generate())
                    .url(annotationApiEndpointBase+annotationEndpoint+documentId).build();

            Response response = okHttpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                AnnotationSetDTO annotationSetDTO = objectMapper.readValue(response.body().charStream(), AnnotationSetDTO.class);
                return annotationSetDTO;
            } else {
                throw new DocumentTaskProcessingException("Could not access the annotation set. Http error code " + response.code());
            }

        } catch (IOException e) {
            throw new DocumentTaskProcessingException("Could not access the annotation set:" + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new DocumentTaskProcessingException("Could not access the annotation set:" + e.getMessage(), e);
        }
    }
}
