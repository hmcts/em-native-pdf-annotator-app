package uk.gov.hmcts.reform.em.npa.functional.bdd.utils;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class TestTrustManager {

    private static TestTrustManager INSTANCE;

    private static final TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

    private TestTrustManager() {
    }

    synchronized public static TestTrustManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TestTrustManager();
        }

        return INSTANCE;
    }

    public TrustStrategy getTrustStrategy() {
        return acceptingTrustStrategy;
    }

    synchronized public ClientHttpRequestFactory getTestRequestFactory() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();

        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(csf)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);

        return requestFactory;
    }

}
