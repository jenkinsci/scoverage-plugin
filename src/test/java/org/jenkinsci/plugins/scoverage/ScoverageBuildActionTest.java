package org.jenkinsci.plugins.scoverage;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ScoverageBuildActionTest {
    private ScoverageResult result = new ScoverageResult(95.27, 50.0, 1);
    private ScoverageBuildAction action = new ScoverageBuildAction(result);

    @Test
    public void urlTest() {
        assertEquals(action.getUrlName(), ActionUrls.BUILD_URL.toString());
    }

    @Test
    public void resultTest() {
        assertEquals(action.getResult().getStatement(), 95.27);
    }

}
