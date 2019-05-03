package org.jenkinsci.plugins.scoverage;

import hudson.FilePath;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.assertEquals;

public class ScoverageBuildActionTest {
    private FilePath path = new FilePath(new File("foo/bar"));
    private ScoverageResult result = new ScoverageResult(95.27, 50.0, 1);
    private ScoverageBuildAction action = new ScoverageBuildAction(path, result);

    @Test
    public void urlTest() {
        assertEquals(action.getUrlName(), ActionUrls.BUILD_URL.toString());
    }

    @Test
    public void resultTest() {
        assertEquals(action.getResult().getStatement(), 95.27);
    }

}
