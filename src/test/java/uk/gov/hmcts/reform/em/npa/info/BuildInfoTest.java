package uk.gov.hmcts.reform.em.npa.info;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.actuate.info.Info;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class BuildInfoTest {

    @Test
    public void shouldAddBuildInfoToBuilder() throws Exception {
        BuildInfo buildInfo = new BuildInfo("name","env","project");

        Info.Builder builder = new Info.Builder();
        buildInfo.contribute(builder);


        Map<String,Object> buildInfoMap = new HashMap<>();
        buildInfoMap.put("environment", "env");
        buildInfoMap.put("project", "project");
        buildInfoMap.put("name", "name");
        buildInfoMap.put("version", "unknown");
        buildInfoMap.put("date", "unknown");
        buildInfoMap.put("commit", "unknown");
        buildInfoMap.put("extra", Collections.EMPTY_MAP);

        Map<String,Object> map = new HashMap<>();
        map.put("buildInfo",buildInfoMap);

        Info info = builder.build();

        assertThat(info.getDetails(), CoreMatchers.equalTo(map));
    }

    @Test
    public void shouldAddBuildInfoToBuilderWithBuildValue() throws Exception {
        BuildInfo buildInfo = new BuildInfo("name","env","project");

        Info.Builder builder = new Info.Builder();
        buildInfo.contribute(builder);


        Map<String,Object> buildInfoMap = new HashMap<>();
        buildInfoMap.put("environment", "env");
        buildInfoMap.put("project", "project");
        buildInfoMap.put("name", "name");
        buildInfoMap.put("build", "123");
        buildInfoMap.put("version", "V1");
        buildInfoMap.put("date", "unknown");
        buildInfoMap.put("commit", "unknown");
        buildInfoMap.put("extra", Collections.EMPTY_MAP);

        Map<String,Object> map = new HashMap<>();
        map.put("buildInfo",buildInfoMap);

        Info info = builder.build();

        Assert.assertEquals("V1-123", info.getDetails().get("version"));
    }
}
