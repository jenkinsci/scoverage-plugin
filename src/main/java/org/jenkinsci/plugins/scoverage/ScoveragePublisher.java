package org.jenkinsci.plugins.scoverage;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.tasks.SimpleBuildStep;
import jenkins.util.BuildListenerAdapter;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class ScoveragePublisher extends Recorder implements SimpleBuildStep {

    private final String reportDir;
    private final String reportFile;

    @DataBoundConstructor
    public ScoveragePublisher(String reportDir, String reportFile) {
        this.reportDir = reportDir;
        this.reportFile = reportFile;
    }

    public String getReportDir() {
        return reportDir;
    }

    public String getReportFile() {
        return reportFile == null || reportFile.trim().length() == 0 ? "scoverage.xml" : reportFile;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
        throws InterruptedException, IOException {

        listener.getLogger().println("Publishing Scoverage XML and HTML report ...");

        final File buildDir = run.getRootDir();
        final FilePath buildPath = new FilePath(buildDir);
        final FilePath scovPath = workspace.child(reportDir);

        copyReport(scovPath, buildPath, new BuildListenerAdapter(listener));
        ScoverageResult result = processReport(run, buildPath);
        run.addAction(new ScoverageBuildAction(result));
    }

    private static final class ScovFinder implements FilePath.FileCallable<File> {
        private static final long serialVersionUID = 1;

        public File invoke(File dir, VirtualChannel channel) {
            Collection<File> list = FileUtils.listFiles(dir, new IOFileFilter() {
                public boolean accept(File f) {
                    try {
                        return f.isFile() &&
                            f.getName().equals("index.html") &&
                            FileUtils.readFileToString(f).contains("Scoverage Code Coverage");
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }

                public boolean accept(File dir, String s) {
                    return dir.isDirectory();
                }
            }, TrueFileFilter.INSTANCE);
            return (list == null || list.isEmpty()) ? null : list.iterator().next();
        }

        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {
            //We don't require any roles to be checked?
        }
    }

    private void copyReport(FilePath coverageDir, FilePath buildPath, BuildListener listener)
            throws IOException, InterruptedException {
        if (coverageDir.exists()) {
            final FilePath toFile = buildPath.child(getReportFile());
            coverageDir.child(getReportFile()).copyTo(toFile); // copy XML report
            // search index.html recursively and copy its parent tree
            final FilePath indexPath = new FilePath(coverageDir.act(new ScovFinder()));
            if (indexPath != null) {
                final FilePath indexDir = new FilePath(coverageDir.getChannel(), indexPath.getParent().getRemote());
                indexDir.copyRecursiveTo(buildPath.child(ActionUrls.BUILD_URL.toString())); // copy HTML report
            } else {
                listener.getLogger().println("Unable to find HTML reports under " + coverageDir.getRemote());
            }
        } else {
            throw new IOException(coverageDir.getRemote() + " not exists");
        }
    }

    private ScoverageResult processReport(Run<?, ?> build, FilePath path) throws IOException, InterruptedException {

        double statement = 0;
        double condition = 0;
        // Fix HTML reports to use relative href
        HTMLRelativeHref fixhref = new HTMLRelativeHref(path);
        fixhref.process();

        // Parse scoverage.xml
        File report = new File(path.child(reportFile).toURI());
        Pattern pattern = Pattern.compile("^.* statement-rate=\"(.+?)\" branch-rate=\"(.+?)\"");
        BufferedReader in = new BufferedReader(new FileReader(report));
        String line;
        while ((line = in.readLine()) != null) {
            boolean found = false;
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                statement = Double.parseDouble(matcher.group(1));
                condition = Double.parseDouble(matcher.group(2));
                found = true;
            }
            if (found) {
                break;
            }
        }
        return new ScoverageResult(statement, condition, build.number);
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        DescriptorImpl() {
            super(ScoveragePublisher.class);
            load();
        }

        public String getDisplayName() {
            return "Publish Scoverage Report";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject<?,?> project) {
        return Collections.emptySet();
    }
}

