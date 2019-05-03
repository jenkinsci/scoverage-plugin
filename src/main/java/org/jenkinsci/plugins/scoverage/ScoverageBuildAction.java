package org.jenkinsci.plugins.scoverage;

import hudson.FilePath;
import hudson.Functions;
import hudson.model.Action;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import jenkins.model.RunAction2;

@ExportedBean
public class ScoverageBuildAction implements RunAction2, SimpleBuildStep.LastBuildAction {

    private transient Run<?, ?> run;
    private final FilePath buildPath;
    private final ScoverageResult result;

    public ScoverageBuildAction(FilePath buildPath, ScoverageResult result) {
        this.buildPath = buildPath;
        this.result = result;
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        run = r;
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        run = r;
    }

    public String getIconFileName() {
        return Functions.getResourcePath() + "/plugin/scoverage/images/scoverage.png";
    }

    public String getDisplayName() {
        return "Scoverage HTML Report";
    }

    public String getUrlName() {
        return ActionUrls.BUILD_URL.toString();
    }

    @Exported(name = "scoverage")
    public ScoverageResult getResult() {
        return result;
    }

    public ScoverageBuildAction getPreviousBuildAction() {
        Run<?, ?> cur = run;
        ScoverageBuildAction action = null;
        while (true) {
            cur = cur.getPreviousBuild();
            if (cur != null) {
                if (cur.getResult() != Result.SUCCESS) {
                    continue;
                }
                ScoverageBuildAction act = cur.getAction(ScoverageBuildAction.class);
                if (act != null) {
                    action = act;
                    break;
                }
            } else {
                break;
            }
        }
        return action;
    }

    public DirectoryBrowserSupport doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, InterruptedException {
        return new DirectoryBrowserSupport(this, buildPath.child(getUrlName()), "Scoverage HTML Report", "", false);
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Collections.<Action>singleton(new ScoverageProjectAction(run.getParent()));
    }
}
