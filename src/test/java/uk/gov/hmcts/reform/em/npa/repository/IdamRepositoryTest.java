package uk.gov.hmcts.reform.em.npa.repository;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import static java.util.Arrays.asList;

public class IdamRepositoryTest {

    @Mock
    private IdamClient idamClient;

    private IdamRepository idamRepository;

    private static final  String FORE_NAME = "ABC";
    private static final  String SURNAME = "XYZ";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        idamRepository = new IdamRepository(idamClient);
    }

    @Test
    public void getUserDetailsTestSuccess() {

        final UserInfo userInfo = UserInfo
                .builder()
                .uid("100")
                .givenName(FORE_NAME)
                .familyName(SURNAME)
                .roles(asList("Admin", "CaseWorker"))
                .build();
        Mockito.when(idamClient.getUserInfo(Mockito.anyString())).thenReturn(userInfo);
        String token = "randomValue";

        Assert.assertEquals(FORE_NAME,  idamRepository.getUserInfo(token).getGivenName());
        Assert.assertEquals(SURNAME,  idamRepository.getUserInfo(token).getFamilyName());
    }

    @Test
    public void getUserDetailsTestFailure() {

        String token = "randomValue";

        Assert.assertNull(FORE_NAME,  idamRepository.getUserInfo(token));
    }

}

