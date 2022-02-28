package bettergithistory.core;

import org.eclipse.jgit.revwalk.RevCommit;

/**
 * General data record class for carrying metadata about a GitHub pull request or JIRA issue.
 */
public abstract class AbstractIssueMetadata {
    private final RevCommit commit;
    private int numComments; // excludes bot comments
    private int numCommitAuthorComments; // proportion of comments that are by the commit author
    private int numPeopleInvolved; // total number of people involved in issue, including commenters and assignee

    public AbstractIssueMetadata(RevCommit commit) {
        this.commit = commit;
    }

    public RevCommit getCommit() {
        return this.commit;
    }

    public int getNumComments() {
        return this.numComments;
    }

    public int getNumPeopleInvolved() {
        return this.numPeopleInvolved;
    }

    private int getNumCommitAuthorComments() {
        return this.numCommitAuthorComments;
    }

    public void setNumComments(int numComments) {
        this.numComments = numComments;
    }

    public void setNumCommitAuthorComments(int numCommitAuthorComments) {
        this.numCommitAuthorComments = numCommitAuthorComments;
    }

    public void setNumPeopleInvolved(int numPeopleInvolved) {
        this.numPeopleInvolved = numPeopleInvolved;
    }
}
