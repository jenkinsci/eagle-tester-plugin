package com.mobileenerlytics.eagle.tester.common;

public class EagleTesterArgument {
    public String AUTHOR_NAME;
    public String AUTHOR_EMAIL;
    public String BRANCH;
    public String COMMIT;
    public String CURRENT_VERSION;
    public String PACKAGE_NAME;
    public String DATA_ABSOLUTE_PATH = System.getProperty("user.dir") + dataFolderName;

    public String REFERENCE_VERSION;
    public String FAIL;

    private final static String dataFolderName = "/eagle_tester_data/";

    public EagleTesterArgument() {}

    public void setAuthor(final String authorName, final String authorEmail) {
        AUTHOR_NAME = authorName;
        AUTHOR_EMAIL = authorEmail;
    }

    public void setCurrentVersion(final String branch, final String commit) {
        BRANCH = branch;
        COMMIT = commit;
        CURRENT_VERSION = branch + ":" + commit;
    }

    public void setPkgName(final String pkgName) {
        PACKAGE_NAME = pkgName;
    }

    public void setWorkspace(final String workspace) {
        DATA_ABSOLUTE_PATH = workspace + dataFolderName;
    }
}
