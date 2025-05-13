package uk.gov.hmcts.reform.em.npa.rest.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaginationUtilTest {

    private static final String BASE_URL = "/api/items";
    private static final String BASE_URL_WITH_PARAM = "/api/items?sort=name";


    @Mock
    private Page<String> page;

    @Test
    void generateHttpHeadersForFirstPage() {

        long totalElements = 100L;
        int totalPages = 10;
        int pageSize = 10;
        int pageNumber = 0;

        when(page.getTotalElements()).thenReturn(totalElements);
        when(page.getTotalPages()).thenReturn(totalPages);
        when(page.getSize()).thenReturn(pageSize);
        when(page.getNumber()).thenReturn(pageNumber);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, BASE_URL);

        assertNotNull(headers);
        assertEquals(Long.toString(totalElements), headers.getFirst("X-Total-Count"));

        List<String> links = headers.get(HttpHeaders.LINK);
        assertNotNull(links);
        assertEquals(1, links.size());
        String linkHeader = links.getFirst();

        assertTrue(linkHeader.contains("rel=\"next\""), "Should contain next link");
        assertFalse(linkHeader.contains("rel=\"prev\""), "Should not contain prev link");
        assertTrue(linkHeader.contains("rel=\"last\""), "Should contain last link");
        assertTrue(linkHeader.contains("rel=\"first\""), "Should contain first link");

        assertTrue(linkHeader.contains("<" + BASE_URL + "?page=1&size=10>; rel=\"next\""));
        assertTrue(linkHeader.contains("<" + BASE_URL + "?page=9&size=10>; rel=\"last\"")); // last = totalPages - 1
        assertTrue(linkHeader.contains("<" + BASE_URL + "?page=0&size=10>; rel=\"first\""));
    }

    @Test
    void generateHttpHeadersForMiddlePage() {

        long totalElements = 100L;
        int totalPages = 10;
        int pageSize = 10;
        int pageNumber = 5;

        when(page.getTotalElements()).thenReturn(totalElements);
        when(page.getTotalPages()).thenReturn(totalPages);
        when(page.getSize()).thenReturn(pageSize);
        when(page.getNumber()).thenReturn(pageNumber);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, BASE_URL);

        assertNotNull(headers);
        assertEquals(Long.toString(totalElements), headers.getFirst("X-Total-Count"));

        List<String> links = headers.get(HttpHeaders.LINK);
        assertNotNull(links);
        assertEquals(1, links.size());
        String linkHeader = links.getFirst();

        assertTrue(linkHeader.contains("rel=\"next\""), "Should contain next link");
        assertTrue(linkHeader.contains("rel=\"prev\""), "Should contain prev link");
        assertTrue(linkHeader.contains("rel=\"last\""), "Should contain last link");
        assertTrue(linkHeader.contains("rel=\"first\""), "Should contain first link");

        assertTrue(linkHeader.contains("<" + BASE_URL + "?page=6&size=10>; rel=\"next\""));
        assertTrue(linkHeader.contains("<" + BASE_URL + "?page=4&size=10>; rel=\"prev\""));
        assertTrue(linkHeader.contains("<" + BASE_URL + "?page=9&size=10>; rel=\"last\""));
        assertTrue(linkHeader.contains("<" + BASE_URL + "?page=0&size=10>; rel=\"first\""));
    }

    @Test
    void generateHttpHeadersForLastPage() {

        long totalElements = 100L;
        int totalPages = 10;
        int pageSize = 10;
        int pageNumber = 9;

        when(page.getTotalElements()).thenReturn(totalElements);
        when(page.getTotalPages()).thenReturn(totalPages);
        when(page.getSize()).thenReturn(pageSize);
        when(page.getNumber()).thenReturn(pageNumber);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, BASE_URL);

        assertNotNull(headers);
        assertEquals(Long.toString(totalElements), headers.getFirst("X-Total-Count"));

        List<String> links = headers.get(HttpHeaders.LINK);
        assertNotNull(links);
        assertEquals(1, links.size());
        String linkHeader = links.getFirst();

        assertFalse(linkHeader.contains("rel=\"next\""), "Should not contain next link");
        assertTrue(linkHeader.contains("rel=\"prev\""), "Should contain prev link");
        assertTrue(linkHeader.contains("rel=\"last\""), "Should contain last link");
        assertTrue(linkHeader.contains("rel=\"first\""), "Should contain first link");

        assertTrue(linkHeader.contains("<" + BASE_URL + "?page=8&size=10>; rel=\"prev\""));
        assertTrue(linkHeader.contains("<" + BASE_URL + "?page=9&size=10>; rel=\"last\""));
        assertTrue(linkHeader.contains("<" + BASE_URL + "?page=0&size=10>; rel=\"first\""));
    }

    @Test
    void generateHttpHeadersForSinglePage() {

        long totalElements = 5L;
        int totalPages = 1;
        int pageSize = 10;
        int pageNumber = 0;

        when(page.getTotalElements()).thenReturn(totalElements);
        when(page.getTotalPages()).thenReturn(totalPages);
        when(page.getSize()).thenReturn(pageSize);
        when(page.getNumber()).thenReturn(pageNumber);


        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, BASE_URL);


        assertNotNull(headers);
        assertEquals(Long.toString(totalElements), headers.getFirst("X-Total-Count"));

        List<String> links = headers.get(HttpHeaders.LINK);
        assertNotNull(links);
        assertEquals(1, links.size());
        String linkHeader = links.getFirst();


        assertFalse(linkHeader.contains("rel=\"next\""), "Should not contain next link");
        assertFalse(linkHeader.contains("rel=\"prev\""), "Should not contain prev link");
        assertTrue(linkHeader.contains("rel=\"last\""), "Should contain last link");
        assertTrue(linkHeader.contains("rel=\"first\""), "Should contain first link");

        assertTrue(linkHeader.contains("<" + BASE_URL + "?page=0&size=10>; rel=\"last\"")); // last = totalPages - 1 = 0
        assertTrue(linkHeader.contains("<" + BASE_URL + "?page=0&size=10>; rel=\"first\""));
    }

    @Test
    void generateHttpHeadersForEmptyPage() {

        long totalElements = 0L;
        int totalPages = 0;
        int pageSize = 10;
        int pageNumber = 0;

        when(page.getTotalElements()).thenReturn(totalElements);
        when(page.getTotalPages()).thenReturn(totalPages);
        when(page.getSize()).thenReturn(pageSize);
        when(page.getNumber()).thenReturn(pageNumber);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, BASE_URL);

        assertNotNull(headers);
        assertEquals(Long.toString(totalElements), headers.getFirst("X-Total-Count"));

        List<String> links = headers.get(HttpHeaders.LINK);
        assertNotNull(links);
        assertEquals(1, links.size());
        String linkHeader = links.getFirst();

        assertFalse(linkHeader.contains("rel=\"next\""), "Should not contain next link");
        assertFalse(linkHeader.contains("rel=\"prev\""), "Should not contain prev link");
        assertTrue(linkHeader.contains("rel=\"last\""), "Should contain last link");
        assertTrue(linkHeader.contains("rel=\"first\""), "Should contain first link");

        assertTrue(linkHeader.contains("<" + BASE_URL + "?page=0&size=10>; rel=\"last\""));
        assertTrue(linkHeader.contains("<" + BASE_URL + "?page=0&size=10>; rel=\"first\""));
    }

    @Test
    void generateHttpHeadersWithExistingQueryParam() {

        long totalElements = 100L;
        int totalPages = 10;
        int pageSize = 10;
        int pageNumber = 5;

        when(page.getTotalElements()).thenReturn(totalElements);
        when(page.getTotalPages()).thenReturn(totalPages);
        when(page.getSize()).thenReturn(pageSize);
        when(page.getNumber()).thenReturn(pageNumber);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, BASE_URL_WITH_PARAM);

        assertNotNull(headers);
        assertEquals(Long.toString(totalElements), headers.getFirst("X-Total-Count"));

        List<String> links = headers.get(HttpHeaders.LINK);
        assertNotNull(links);
        assertEquals(1, links.size());
        String linkHeader = links.getFirst();

        assertTrue(linkHeader.contains("<" + BASE_URL_WITH_PARAM + "&page=6&size=10>; rel=\"next\""));
        assertTrue(linkHeader.contains("<" + BASE_URL_WITH_PARAM + "&page=4&size=10>; rel=\"prev\""));
        assertTrue(linkHeader.contains("<" + BASE_URL_WITH_PARAM + "&page=9&size=10>; rel=\"last\""));
        assertTrue(linkHeader.contains("<" + BASE_URL_WITH_PARAM + "&page=0&size=10>; rel=\"first\""));
    }

}