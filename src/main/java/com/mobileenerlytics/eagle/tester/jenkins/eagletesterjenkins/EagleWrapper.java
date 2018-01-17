package com.mobileenerlytics.eagle.tester.jenkins.eagletesterjenkins;

import com.mobileenerlytics.eagle.tester.common.EagleTesterArgument;
import com.mobileenerlytics.eagle.tester.common.util.Log;
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
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class EagleWrapper extends BuildWrapper {
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
    private String authorName = DEFAULT_AUTHOR_NAME;
    private String authorEmail = DEFAULT_AUTHOR_EMAIL;
    private boolean init = false;

    private EagleTesterArgument eagleTesterArgument;

    @DataBoundConstructor
    public EagleWrapper(String pkgName, String authorName, String authorEmail, String branch, String commit) {
        this.pkgName = pkgName;
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
            DescriptorImpl desc = getDescriptor();
            JenkinsLocalOperation localOperation = desc.getLocalOperation();
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

            // Upload traces to server
            List<String> devices = localOperation.getDevices();
            if (devices.isEmpty()) {
                listener.fatalError(TAG + "No connected device found. Tester may not produce output.");
                return true;
            }
            final String url = String.format("%s/api/upload/version_energy", desc.mServerUri);
            listener.getLogger().printf("%s %s%n", TAG, url);
            final Client client = getClient(desc);
            WebTarget webTarget = client.target(url);
            MultiPart multiPart = new FormDataMultiPart()
                    .field("device", devices.get(0))
                    .field("pkg", pkgName)
                    .field("author_name", eagleTesterArgument.AUTHOR_NAME)
                    .field("author_email", eagleTesterArgument.AUTHOR_EMAIL)
                    .field("branch", eagleTesterArgument.BRANCH)
                    .field("commit", eagleTesterArgument.COMMIT)
                    .field("cur_version", eagleTesterArgument.CURRENT_VERSION);
            multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

            FileDataBodyPart fileDataBodyPart = new FileDataBodyPart(fileToUpload.getName(),
                    fileToUpload, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            fileDataBodyPart.setContentDisposition(
                    FormDataContentDisposition.name("file")
                            .fileName(fileToUpload.getName()).build());
            multiPart.bodyPart(fileDataBodyPart);

            Response response = webTarget.request()
                    .post(Entity.entity(multiPart, multiPart.getMediaType()));
            if (200 == response.getStatusInfo().getStatusCode()) {
                listener.getLogger().printf("%s See the energy report at %s/eagle/%n", TAG, desc.mServerUri);
            } else {
                listener.getLogger().printf("%s Error uploading file %s to eagle server. %s%n", TAG,
                        fileToUpload.getAbsolutePath(), response);
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
        JenkinsLocalOperation localOperation = getDescriptor().getLocalOperation();
        // Authenticate with the server
        if (!auth) {
            DescriptorImpl desc = (DescriptorImpl) getDescriptor();
            auth = authenticate(getClient(desc));
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

        // Install TesterApp on the phone
        localOperation.before();
        init = true;

        return true;
    }

    static Client getClient(EagleWrapper.DescriptorImpl descriptor) {
        return ClientBuilder.newBuilder()
                .register(MultiPartFeature.class)
                .register(new EagleAuthenticator(descriptor))
                .build();
    }

    static boolean authenticate(Client client) {
        String url = "https://tester.mobileenerlytics.com/auth/";
        WebTarget webTarget = client.target(url);
        Response response = webTarget.request().get();
        if (200 == response.getStatusInfo().getStatusCode()) {
            Log.i("Authed ~");
            return true;
        }
        Log.w("Failed to authenticate. Check username, password");
        return false;
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

        listener.getLogger().printf("%s Initialized eagle server URI %s package name %s author (%s, %s) current version (%s:%s) %n",
                TAG,
                getDescriptor().mServerUri,
                eagleTesterArgument.PACKAGE_NAME,
                eagleTesterArgument.AUTHOR_NAME,
                eagleTesterArgument.AUTHOR_EMAIL,
                eagleTesterArgument.BRANCH,
                eagleTesterArgument.COMMIT);
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        private String adb;
        private String username;
        private String password;
        private JenkinsLocalOperation localOperation;

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

        public FormValidation doSetup(@QueryParameter("adb") final String adb,
                                      @QueryParameter("username") final String username,
                                      @QueryParameter("password") final String password,
                                      @QueryParameter("eagleServerUri") final String eagleServerUri) {
            this.username = username;
            this.password = password;
            setEagleServerUri(eagleServerUri);
            Client client = EagleWrapper.getClient(this);
            if (!EagleWrapper.authenticate(client))
                return FormValidation.error("Authentication failed. Incorrect username: " + username + " or password: " + password);

            setAdb(adb);
            localOperation = JenkinsLocalOperation.getInstance(this.adb);
            try {
                localOperation.prepareDevice();
            } catch (IOException e) {
                return FormValidation.error("Failed to install Eagle Tester app. Please make sure that the phone is " +
                        "connected and has \"USB Debugging\" enabled in \"Settings > Developer Options\".");
            }

            return FormValidation.ok("Success");
        }

        public String getAdb() {
            return adb;
        }

        public void setAdb(String adb) {
            if (adb == null || adb.length() == 0)
                this.adb = "adb";
            else
                this.adb = adb;
            localOperation = JenkinsLocalOperation.getInstance(this.adb);
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

        JenkinsLocalOperation getLocalOperation() {
            return localOperation;
        }
    }
}
