package uk.gov.hmcts.reform.em.npa.repository;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.npa.Application;
import uk.gov.hmcts.reform.em.npa.TestSecurityConfiguration;
import uk.gov.hmcts.reform.em.npa.config.Constants;
import uk.gov.hmcts.reform.em.npa.config.audit.AuditEventConverter;
import uk.gov.hmcts.reform.em.npa.domain.PersistentAuditEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.em.npa.repository.CustomAuditEventRepository.EVENT_DATA_COLUMN_MAX_LENGTH;

/**
 * Test class for the CustomAuditEventRepository class.
 *
 * @see CustomAuditEventRepository
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
@Transactional
class CustomAuditEventRepositoryIntTest {

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private AuditEventConverter auditEventConverter;

    private CustomAuditEventRepository customAuditEventRepository;

    private PersistentAuditEvent testUserEvent;

    private PersistentAuditEvent testOtherUserEvent;

    private PersistentAuditEvent testOldUserEvent;

    @BeforeEach
    public void setup() {
        customAuditEventRepository =
                new CustomAuditEventRepository(persistenceAuditEventRepository, auditEventConverter);
        persistenceAuditEventRepository.deleteAll();

        testUserEvent = new PersistentAuditEvent();
        testUserEvent.setPrincipal("test-user");
        testUserEvent.setAuditEventType("test-type");
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        testUserEvent.setAuditEventDate(oneHourAgo);
        Map<String, String> data = new HashMap<>();
        data.put("test-key", "test-value");
        testUserEvent.setData(data);

        testOldUserEvent = new PersistentAuditEvent();
        testOldUserEvent.setPrincipal("test-user");
        testOldUserEvent.setAuditEventType("test-type");
        testOldUserEvent.setAuditEventDate(oneHourAgo.minusSeconds(10000));

        testOtherUserEvent = new PersistentAuditEvent();
        testOtherUserEvent.setPrincipal("other-test-user");
        testOtherUserEvent.setAuditEventType("test-type");
        testOtherUserEvent.setAuditEventDate(oneHourAgo);
    }

    @Test
    void addAuditEvent() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", "test-value");
        AuditEvent event = new AuditEvent("test-user", "test-type", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertEquals(1,(persistentAuditEvents).size());
        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.get(0);
        assertEquals(persistentAuditEvent.getPrincipal(), event.getPrincipal());
        assertEquals(persistentAuditEvent.getAuditEventType(), event.getType());
        assertTrue(persistentAuditEvent.getData().containsKey("test-key"));
        assertEquals("test-value", persistentAuditEvent.getData().get("test-key"));
        assertEquals(persistentAuditEvent.getAuditEventDate(),event.getTimestamp());
    }

    @Test
    void addAuditEventTruncateLargeData() {
        Map<String, Object> data = new HashMap<>();
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < EVENT_DATA_COLUMN_MAX_LENGTH + 10; i++) {
            largeData.append("a");
        }
        data.put("test-key", largeData);
        AuditEvent event = new AuditEvent("test-user", "test-type", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertEquals(1,(persistentAuditEvents).size());
        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.get(0);
        assertEquals(persistentAuditEvent.getPrincipal(), event.getPrincipal());
        assertEquals(persistentAuditEvent.getAuditEventType(), event.getType());
        assertTrue(persistentAuditEvent.getData().containsKey("test-key"));
        String actualData = persistentAuditEvent.getData().get("test-key");
        assertEquals(EVENT_DATA_COLUMN_MAX_LENGTH, actualData.length());
        assertTrue(largeData.toString().contains(actualData));
        assertEquals(persistentAuditEvent.getAuditEventDate(),event.getTimestamp());
    }

    @Test
    void testAddEventWithWebAuthenticationDetails() {
        HttpSession session = new MockHttpSession(null, "test-session-id");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        request.setRemoteAddr("1.2.3.4");
        WebAuthenticationDetails details = new WebAuthenticationDetails(request);
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", details);
        AuditEvent event = new AuditEvent("test-user", "test-type", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertEquals(1,(persistentAuditEvents).size());
        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.get(0);
        assertEquals("1.2.3.4", persistentAuditEvent.getData().get("remoteAddress"));
        assertEquals("test-session-id", persistentAuditEvent.getData().get("sessionId"));
    }

    @Test
    void testAddEventWithNullData() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", null);
        AuditEvent event = new AuditEvent("test-user", "test-type", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertEquals(1,(persistentAuditEvents).size());
        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.get(0);
        assertEquals("null", persistentAuditEvent.getData().get("test-key"));
    }

    @Test
    void addAuditEventWithAnonymousUser() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", "test-value");
        AuditEvent event = new AuditEvent(Constants.ANONYMOUS_USER, "test-type", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertEquals(0,(persistentAuditEvents).size());
    }

    @Test
    void addAuditEventWithAuthorizationFailureType() {
        Map<String, Object> data = new HashMap<>();
        data.put("test-key", "test-value");
        AuditEvent event = new AuditEvent("test-user", "AUTHORIZATION_FAILURE", data);
        customAuditEventRepository.add(event);
        List<PersistentAuditEvent> persistentAuditEvents = persistenceAuditEventRepository.findAll();
        assertEquals(0,(persistentAuditEvents).size());
    }

}
