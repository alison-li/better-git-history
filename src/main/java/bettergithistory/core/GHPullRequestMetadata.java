package bettergithistory.core;

import org.eclipse.jgit.revwalk.RevCommit;

public class GHPullRequestMetadata extends AbstractIssueMetadata {
    private int numReviewers;
    private int numReviews;

    public GHPullRequestMetadata(RevCommit commit) {
        super(commit);
    }
}
