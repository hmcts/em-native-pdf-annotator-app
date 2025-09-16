package uk.gov.hmcts.reform.em.npa.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IdamRepositoryTest {

    @Mock
    private IdamClient idamClient;

    private IdamRepository idamRepository;

    private static final  String FORE_NAME = "ABC";
    private static final  String SURNAME = "XYZ";

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        idamRepository = new IdamRepository(idamClient);
    }

    @Test
    void getUserDetailsTestSuccess() {

        final UserInfo userInfo = UserInfo
                .builder()
                .uid("100")
                .givenName(FORE_NAME)
                .familyName(SURNAME)
                .roles(asList("Admin", "CaseWorker"))
                .build();
        Mockito.when(idamClient.getUserInfo(Mockito.anyString())).thenReturn(userInfo);
        String token = "randomValue";

        assertEquals(FORE_NAME,  idamRepository.getUserInfo(token).getGivenName());
        assertEquals(SURNAME,  idamRepository.getUserInfo(token).getFamilyName());
    }

    @Test
    void getUserDetailsTestFailure() {
        String token = "randomValue";

        assertNull(idamRepository.getUserInfo(token));
    }
}

