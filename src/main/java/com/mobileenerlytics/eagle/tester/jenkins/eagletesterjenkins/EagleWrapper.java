package com.mobileenerlytics.eagle.tester.jenkins.eagletesterjenkins;

import com.mobileenerlytics.eagle.tester.common.EagleTesterArgument;
import com.mobileenerlytics.eagle.tester.common.util.JenkinsLocalOperation;
import com.mobileenerlytics.eagle.tester.common.util.Log;
import com.mobileenerlytics.eagle.tester.common.util.NetUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EagleWrapper extends BuildWrapper {
    private final static String DEFAULT_PROJECT_NAME = "default";
    private final static String DEFAULT_AUTHOR_NAME = "noname";
    private final static String DEFAULT_AUTHOR_EMAIL = "noemail";
    private final static String DEFAULT_BRANCH = "nobranch";
    private final static String DEFAULT_COMMIT = "nocommit";

    private static final String TAG = "[eagle-tester-jenkins]";
    public static final String DATA_FOLDER = "eagle_tester_data";
    public static final String INPUT_FOLDER = "inputs";
    public static final String OUTPUT_FOLDER = "outputs";

    private static boolean auth = false;
    private String pkgName;
    private String branch = DEFAULT_BRANCH;
    private String commit = DEFAULT_COMMIT;
    private String projectName = DEFAULT_PROJECT_NAME;
    private String authorName = DEFAULT_AUTHOR_NAME;
    private String authorEmail = DEFAULT_AUTHOR_EMAIL;
    private boolean init = false;

    private EagleTesterArgument eagleTesterArgument;

    @DataBoundConstructor
    public EagleWrapper(String pkgName, String authorName, String authorEmail, String branch, String commit) {
        this.pkgName = pkgName;
        this.projectName = DEFAULT_PROJECT_NAME;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.branch = branch;
        this.commit = commit;
        eagleTesterArgument = new EagleTesterArgument();
    }

    // We'll use this from the <tt>config.jelly</tt>.
    public String getPkgName() {
        return pkgName;
    }

    //public String getProjectName() {
    //    return projectName;
    //}

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public String getBranch() {
        return branch;
    }

    public String getCommit() {
        return commit;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    static String displayName() {
        try {
            return Computer.currentComputer().getNode().getDisplayName();
        } catch (NullPointerException npe) {
            return "unnamed-slave";
        }
    }

    class EagleEnvironment extends Environment {
        @Override
        public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
            postBuild(build, listener);
            return super.tearDown(build, listener);
        }

        private boolean postBuild(AbstractBuild build, BuildListener listener) throws InterruptedException, IOException {
            EnvVars env = build.getEnvironment(listener);
            DescriptorImpl desc = getDescriptor();
            JenkinsLocalOperation localOperation = new JenkinsLocalOperation(env.expand(desc.getAdb()));
            // Check if authenticated
            if (!auth) {
                listener.fatalError(TAG + "Failed to authenticate. Tester may not produce output");
                return true;
            }
            if (!init) {
                listener.fatalError(TAG + "Seems initialization had failed. Tester may not produce output. Check logs for more details");
                return true;
            }
            listener.getLogger().printf("%s/api/upload/version_energy", desc.mServerUri);
            // Pull logs from phone
            FilePath workspace = build.getWorkspace();
            if(workspace == null) {
                listener.fatalError(TAG + "No workspace. Tester may not produce output");
                return true;
            }
            FilePath dataFolder = new FilePath(workspace, DATA_FOLDER);
            FilePath outputFolderPath = new FilePath(dataFolder, OUTPUT_FOLDER);

            File outputFolder = new File(outputFolderPath.getRemote());
            assert !outputFolderPath.isRemote();

            File fileToUpload = localOperation.after(outputFolder);
            if (fileToUpload == null) {
                listener.getLogger().printf("%s No traces found. Skip%n", TAG);
                return true;
            }

            List<String> devices = localOperation.getDevices();
            if (devices.isEmpty()) {
                listener.fatalError(TAG + "No connected device found. Tester may not produce output.");
                return true;
            }

            final URL url = new URL(String.format("%s/api/upload/version_energy", desc.mServerUri));
            listener.getLogger().printf("%s %s%n", TAG, url.toString());

            Map<String, String> fields = new HashMap<>();
            fields.put("device", devices.get(0));
            fields.put("pkg", pkgName);
            fields.put("project_name", eagleTesterArgument.PROJECT_NAME);
            fields.put("author_name", eagleTesterArgument.AUTHOR_NAME);
            fields.put("author_email", eagleTesterArgument.AUTHOR_EMAIL);
            fields.put("branch", eagleTesterArgument.BRANCH);
            fields.put("commit", eagleTesterArgument.COMMIT);
            fields.put("cur_version", eagleTesterArgument.CURRENT_VERSION);
            try {
                CloseableHttpResponse response = NetUtils.upload(fileToUpload, url, desc, fields);
                if (200 == response.getStatusLine().getStatusCode()) {
                    listener.getLogger().printf("%s See the energy report at %s%n", TAG, desc.mServerUri);
                } else {
                    listener.getLogger().printf("%s Error uploading file %s to eagle server. %s %s%n", TAG,
                            fileToUpload.getAbsolutePath(), response);
                }
            } catch(URISyntaxException e) {
                listener.getLogger().printf("%s Error uploading file %s to eagle server. %s %s%n", TAG,
                        fileToUpload.getAbsolutePath(), e.getMessage());
            }
            return true;
        }
    }


    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        Log.out = listener.getLogger();
        preBuild(build, listener);
        return new EagleEnvironment();
    }

    private boolean preBuild(AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        EnvVars env = build.getEnvironment(listener);
        DescriptorImpl desc = (DescriptorImpl) getDescriptor();
        JenkinsLocalOperation localOperation = new JenkinsLocalOperation(env.expand(desc.getAdb()));

        // Authenticate with the server
        if (!auth) {
            try {
                auth = NetUtils.authenticate(desc);
            } catch(URISyntaxException e) {
                listener.getLogger().printf("%s Error authenticating %s%n", TAG, e.getMessage());
            }
        }
        if (!auth) {
            listener.fatalError(TAG + "Failed to authenticate. Tester may not produce output");
            return true;
        }

        // Set eagle tester args the variables
        initEagleTesterArg(build, listener);

        // Mkdir data, input and output folders
        FilePath workspace = build.getWorkspace();
        if(workspace == null) {
            listener.fatalError(TAG + "No workspace. Tester may not produce output");
            return true;
        }
        FilePath dataFolder = new FilePath(workspace, DATA_FOLDER);
        dataFolder.mkdirs();

        FilePath inputFolder = new FilePath(dataFolder, INPUT_FOLDER);
        inputFolder.mkdirs();

        FilePath outputFolder = new FilePath(dataFolder, OUTPUT_FOLDER);
        outputFolder.mkdirs();

        localOperation.before();
        init = true;

        return true;
    }

    private void initEagleTesterArg(AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        EnvVars env = build.getEnvironment(listener);

        String expandedBranch = DEFAULT_BRANCH;
        if (branch != null) {
            expandedBranch = env.expand(branch);
        }
        String expandedCommit = DEFAULT_COMMIT;
        if (commit != null) {
            expandedCommit = env.expand(commit);
        }
        eagleTesterArgument.setCurrentVersion(expandedBranch, expandedCommit);

        String expandedProjectName = DEFAULT_PROJECT_NAME;
        if(projectName != null) {
            expandedProjectName = env.expand(projectName);
        }
        eagleTesterArgument.setProject(expandedProjectName);

        String expandedAuthorName = DEFAULT_AUTHOR_NAME;
        if(authorName != null) {
            expandedAuthorName = env.expand(authorName);
        }
        String expandedAuthorEmail = DEFAULT_AUTHOR_EMAIL;
        if(authorEmail != null) {
            expandedAuthorEmail = env.expand(authorEmail);
        }
        eagleTesterArgument.setAuthor(expandedAuthorName, expandedAuthorEmail);

        if (pkgName != null) {
            String expandedPkgName = env.expand(pkgName);
            eagleTesterArgument.setPkgName(expandedPkgName);
        }

        listener.getLogger().printf("%s Initialized eagle server URI %s package name %s, project name %s author (%s, %s) current version (%s:%s) %n",
                TAG,
                getDescriptor().mServerUri,
                eagleTesterArgument.PACKAGE_NAME,
                eagleTesterArgument.PROJECT_NAME,
                eagleTesterArgument.AUTHOR_NAME,
                eagleTesterArgument.AUTHOR_EMAIL,
                eagleTesterArgument.BRANCH,
                eagleTesterArgument.COMMIT);
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        private String adb = null;
        private String username;
        private String password;
        private boolean debug;
        private String license;

        String mServerUri = "https://tester.mobileenerlytics.com";

        public DescriptorImpl() {
            super(EagleWrapper.class);
            load();
            setAdb(adb);
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData)
                throws FormException {
            req.bindParameters(this);
            username = formData.getString("username");
            password = formData.getString("password");
            setEagleServerUri(formData.getString("eagleServerUri"));
            setAdb(formData.getString("adb"));
            setDebug(formData.getBoolean("debug"));
            save();
            return super.configure(req, formData);
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> abstractProject) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Eagle Tester";
        }

        public FormValidation doVerify(@QueryParameter("adb") final String adb,
                                      @QueryParameter("username") final String username,
                                      @QueryParameter("password") final String password,
                                      @QueryParameter("eagleServerUri") final String eagleServerUri,
                                      @QueryParameter("license") final String license) throws IOException, URISyntaxException {
            this.username = username;
            this.password = password;
            this.license = license;
            setEagleServerUri(eagleServerUri);
            setAdb(adb);

            return NetUtils.formAuthenticate(this);
        }

        public String getAdb() {
            return adb;
        }

        public void setAdb(String adb) {
            if (adb == null || adb.length() == 0)
                this.adb = "adb";
            else
                this.adb = adb;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEagleServerUri() {
            return mServerUri;
        }

        public String getLicense() {
            return license;
        }

        public void setEagleServerUri(String eagleServerUri) {
            if(eagleServerUri != null && eagleServerUri.length() > 0) {
                int i = eagleServerUri.length() - 1;
                // Remove all leading slashes
                while (eagleServerUri.charAt(i) == '/') {
                    eagleServerUri = eagleServerUri.substring(0, i);
                    i --;
                }
                mServerUri = eagleServerUri;
            }
        }

        public boolean isDebug() {
            return debug;
        }

        @SuppressFBWarnings({"ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD"})
        public void setDebug(boolean debug) {
            this.debug = debug;
            Log.debug = debug;
        }
    }
}
