package uk.gov.hmcts.reform.em.npa.repository;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import uk.gov.hmcts.reform.em.npa.config.Constants;
import uk.gov.hmcts.reform.em.npa.config.audit.AuditEventConverter;
import uk.gov.hmcts.reform.em.npa.domain.PersistentAuditEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.em.npa.repository.CustomAuditEventRepository.EVENT_DATA_COLUMN_MAX_LENGTH;

/**
 * Test class for the CustomAuditEventRepository class.
 *
 * @see CustomAuditEventRepository
 */
@ExtendWith(MockitoExtension.class)
class CustomAuditEventRepositoryTest {

    @Mock
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Mock
    private AuditEventConverter auditEventConverter;

    @Captor
    private ArgumentCaptor<PersistentAuditEvent> persistentAuditEventCaptor;

    private CustomAuditEventRepository customAuditEventRepository;

    @BeforeEach
    void setup() {
        customAuditEventRepository =
            new CustomAuditEventRepository(persistenceAuditEventRepository, auditEventConverter);
    }

    // --- Tests for 'add' method ---

    @Test
    void addAuditEvent() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", "test-value");
        Instant timestamp = Instant.now();
        AuditEvent event = new AuditEvent(timestamp, "test-user", "test-type", data);

        Map<String, String> convertedData = Collections.singletonMap("test-key", "test-value");
        when(auditEventConverter.convertDataToStrings(data)).thenReturn(convertedData);

        customAuditEventRepository.add(event);

        verify(persistenceAuditEventRepository).save(persistentAuditEventCaptor.capture());

        PersistentAuditEvent persistentAuditEvent = persistentAuditEventCaptor.getValue();
        assertEquals(event.getPrincipal(), persistentAuditEvent.getPrincipal());
        assertEquals(event.getType(), persistentAuditEvent.getAuditEventType());
        assertNotNull(persistentAuditEvent.getData());
        assertTrue(persistentAuditEvent.getData().containsKey("test-key"));
        assertEquals("test-value", persistentAuditEvent.getData().get("test-key"));
        assertEquals(event.getTimestamp(), persistentAuditEvent.getAuditEventDate());
    }

    @Test
    void addAuditEventTruncateLargeData() {
        Map<String, Object> data = new HashMap<>();
        StringBuilder largeData = new StringBuilder();
        largeData.append("a".repeat(EVENT_DATA_COLUMN_MAX_LENGTH + 10));
        data.put("test-key", largeData.toString());
        Instant timestamp = Instant.now();
        AuditEvent event = new AuditEvent(timestamp, "test-user", "test-type", data);

        Map<String, String> convertedData = Collections.singletonMap("test-key", largeData.toString());
        when(auditEventConverter.convertDataToStrings(data)).thenReturn(convertedData);

        customAuditEventRepository.add(event);

        verify(persistenceAuditEventRepository).save(persistentAuditEventCaptor.capture());

        PersistentAuditEvent persistentAuditEvent = persistentAuditEventCaptor.getValue();
        assertEquals(event.getPrincipal(), persistentAuditEvent.getPrincipal());
        assertEquals(event.getType(), persistentAuditEvent.getAuditEventType());
        assertNotNull(persistentAuditEvent.getData());
        assertTrue(persistentAuditEvent.getData().containsKey("test-key"));
        String actualData = persistentAuditEvent.getData().get("test-key");
        assertEquals(EVENT_DATA_COLUMN_MAX_LENGTH, actualData.length());
        assertTrue(largeData.toString().startsWith(actualData));
        assertEquals(event.getTimestamp(), persistentAuditEvent.getAuditEventDate());
    }

    @Test
    void testAddEventWithWebAuthenticationDetails() {
        HttpSession session = new MockHttpSession(null, "test-session-id");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        request.setRemoteAddr("1.2.3.4");
        WebAuthenticationDetails details = new WebAuthenticationDetails(request);
        Map<String, Object> data = new HashMap<>();
        data.put("details", details);

        Map<String, String> convertedData = new HashMap<>();
        convertedData.put("remoteAddress", "1.2.3.4");
        convertedData.put("sessionId", "test-session-id");
        when(auditEventConverter.convertDataToStrings(data)).thenReturn(convertedData);

        Instant timestamp = Instant.now();
        AuditEvent event = new AuditEvent(timestamp, "test-user", "test-type", data);
        customAuditEventRepository.add(event);

        verify(persistenceAuditEventRepository).save(persistentAuditEventCaptor.capture());

        PersistentAuditEvent persistentAuditEvent = persistentAuditEventCaptor.getValue();
        assertNotNull(persistentAuditEvent.getData());
        assertEquals("1.2.3.4", persistentAuditEvent.getData().get("remoteAddress"));
        assertEquals("test-session-id", persistentAuditEvent.getData().get("sessionId"));
    }

    @Test
    void testAddEventWithNullDataValue() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", null);
        Instant timestamp = Instant.now();
        AuditEvent event = new AuditEvent(timestamp, "test-user", "test-type", data);

        Map<String, String> convertedData = new HashMap<>();
        convertedData.put("test-key", null);
        when(auditEventConverter.convertDataToStrings(data)).thenReturn(convertedData);

        customAuditEventRepository.add(event);

        verify(persistenceAuditEventRepository).save(persistentAuditEventCaptor.capture());

        PersistentAuditEvent persistentAuditEvent = persistentAuditEventCaptor.getValue();
        assertNotNull(persistentAuditEvent.getData());
        assertTrue(persistentAuditEvent.getData().containsKey("test-key"));
        assertNull(persistentAuditEvent.getData().get("test-key"));
    }

    @Test
    void addAuditEventWhenConverterReturnsNullMap() {

        Map<String, Object> originalEventData = new HashMap<>();
        originalEventData.put("some-key", "some-value");
        Instant timestamp = Instant.now();
        AuditEvent event = new AuditEvent(timestamp, "test-user", "test-type", originalEventData);

        when(auditEventConverter.convertDataToStrings(originalEventData)).thenReturn(null);

        customAuditEventRepository.add(event);

        verify(persistenceAuditEventRepository).save(persistentAuditEventCaptor.capture());

        PersistentAuditEvent persistentAuditEvent = persistentAuditEventCaptor.getValue();
        assertEquals(event.getPrincipal(), persistentAuditEvent.getPrincipal());
        assertEquals(event.getType(), persistentAuditEvent.getAuditEventType());
        assertEquals(event.getTimestamp(), persistentAuditEvent.getAuditEventDate());

        assertNotNull(persistentAuditEvent.getData());
        assertTrue(persistentAuditEvent.getData().isEmpty());
    }

    @Test
    void testAddEventWithNullDataValueConvertedToString() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", null);
        Instant timestamp = Instant.now();
        AuditEvent event = new AuditEvent(timestamp, "test-user", "test-type", data);

        Map<String, String> convertedData = new HashMap<>();
        convertedData.put("test-key", "null");
        when(auditEventConverter.convertDataToStrings(data)).thenReturn(convertedData);

        customAuditEventRepository.add(event);

        verify(persistenceAuditEventRepository).save(persistentAuditEventCaptor.capture());

        PersistentAuditEvent persistentAuditEvent = persistentAuditEventCaptor.getValue();
        assertNotNull(persistentAuditEvent.getData());
        assertTrue(persistentAuditEvent.getData().containsKey("test-key"));
        assertEquals("null", persistentAuditEvent.getData().get("test-key"));
    }

    @Test
    void addAuditEventWithAnonymousUser() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", "test-value");
        Instant timestamp = Instant.now();
        AuditEvent event = new AuditEvent(timestamp, Constants.ANONYMOUS_USER, "test-type", data);

        customAuditEventRepository.add(event);

        verify(persistenceAuditEventRepository, never()).save(any(PersistentAuditEvent.class));
        verify(auditEventConverter, never()).convertDataToStrings(any());
    }

    @Test
    void addAuditEventWithAuthorizationFailureType() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", "test-value");
        Instant timestamp = Instant.now();
        AuditEvent event = new AuditEvent(timestamp, "test-user", "AUTHORIZATION_FAILURE", data);

        customAuditEventRepository.add(event);

        verify(persistenceAuditEventRepository, never()).save(any(PersistentAuditEvent.class));
        verify(auditEventConverter, never()).convertDataToStrings(any());
    }

    @Test
    void findByPrincipalAndDateAfterAndType() {

        String principal = "test-user";
        Instant after = Instant.now().minus(1, ChronoUnit.DAYS);
        String type = "test-type";

        PersistentAuditEvent persistentEvent1 = new PersistentAuditEvent();
        persistentEvent1.setId(1L);
        persistentEvent1.setPrincipal(principal);
        persistentEvent1.setAuditEventType(type);
        persistentEvent1.setAuditEventDate(after.plusSeconds(10));

        PersistentAuditEvent persistentEvent2 = new PersistentAuditEvent();
        persistentEvent2.setId(2L);
        persistentEvent2.setPrincipal(principal);
        persistentEvent2.setAuditEventType(type);
        persistentEvent2.setAuditEventDate(after.plusSeconds(20));

        List<PersistentAuditEvent> persistentEvents = Arrays.asList(persistentEvent1, persistentEvent2);

        AuditEvent auditEvent1 = new AuditEvent(
            persistentEvent1.getAuditEventDate(), principal, type, Collections.emptyMap());
        AuditEvent auditEvent2 = new AuditEvent(
            persistentEvent2.getAuditEventDate(), principal, type, Collections.emptyMap());
        List<AuditEvent> expectedAuditEvents = Arrays.asList(auditEvent1, auditEvent2);

        when(persistenceAuditEventRepository
            .findByPrincipalAndAuditEventDateAfterAndAuditEventType(principal, after, type))
            .thenReturn(persistentEvents);

        when(auditEventConverter.convertToAuditEvent(persistentEvents))
            .thenReturn(expectedAuditEvents);

        List<AuditEvent> actualAuditEvents = customAuditEventRepository.find(principal, after, type);

        assertEquals(expectedAuditEvents, actualAuditEvents);
        verify(persistenceAuditEventRepository)
            .findByPrincipalAndAuditEventDateAfterAndAuditEventType(principal, after, type);
        verify(auditEventConverter).convertToAuditEvent(persistentEvents);
    }

    @Test
    void findWhenNoEventsFound() {

        String principal = "non-existent-user";
        Instant after = Instant.now().minus(1, ChronoUnit.HOURS);
        String type = "some-type";

        List<PersistentAuditEvent> emptyPersistentList = Collections.emptyList();
        List<AuditEvent> expectedEmptyAuditList = Collections.emptyList();

        when(persistenceAuditEventRepository
            .findByPrincipalAndAuditEventDateAfterAndAuditEventType(principal, after, type))
            .thenReturn(emptyPersistentList);

        when(auditEventConverter
            .convertToAuditEvent(emptyPersistentList))
            .thenReturn(expectedEmptyAuditList);

        List<AuditEvent> actualAuditEvents = customAuditEventRepository.find(principal, after, type);

        assertEquals(expectedEmptyAuditList, actualAuditEvents);
        assertTrue(actualAuditEvents.isEmpty());
        verify(persistenceAuditEventRepository)
            .findByPrincipalAndAuditEventDateAfterAndAuditEventType(principal, after, type);
        verify(auditEventConverter).convertToAuditEvent(emptyPersistentList);
    }
}