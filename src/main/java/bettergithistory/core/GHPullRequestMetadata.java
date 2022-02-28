package bettergithistory.core;

import org.eclipse.jgit.revwalk.RevCommit;

public class GHPullRequestMetadata extends AbstractIssueMetadata {
    private int numReviews;

    public GHPullRequestMetadata(RevCommit commit) {
        super(commit);
    }

    public int getNumReviews() {
        return this.numReviews;
    }

    public void setNumReviews(int numReviews) {
        this.numReviews = numReviews;
    }
}
