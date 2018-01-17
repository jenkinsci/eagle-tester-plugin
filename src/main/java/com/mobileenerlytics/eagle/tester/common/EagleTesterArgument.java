package com.mobileenerlytics.eagle.tester.common;

public class EagleTesterArgument {
    public String AUTHOR_NAME;
    public String AUTHOR_EMAIL;
    public String BRANCH;
    public String COMMIT;
    public String CURRENT_VERSION;
    public String PACKAGE_NAME;
    public String PROJECT_NAME;

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

    public void setProject(final String projectName) {
        PROJECT_NAME = projectName;
    }
}
