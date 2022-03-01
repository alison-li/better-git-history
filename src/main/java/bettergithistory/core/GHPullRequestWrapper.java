package bettergithistory.core;

import org.kohsuke.github.GHPullRequest;

/**
 * For subclassing GitHub pull requests as an abstract issue.
 */
public class GHPullRequestWrapper extends AbstractIssue {
    private final GHPullRequest pullRequest;

    public GHPullRequestWrapper(GHPullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }

    public GHPullRequest getIssue() {
        return this.pullRequest;
    }
}
