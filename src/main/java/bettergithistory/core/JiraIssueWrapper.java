package bettergithistory.core;

import net.rcarz.jiraclient.Issue;

/**
 * For subclassing JIRA issues as an abstract issue.
 */
public class JiraIssueWrapper extends AbstractIssue {
    private final Issue issue;

    public JiraIssueWrapper(Issue issue) {
        this.issue = issue;
    }

    public Issue getIssue() {
        return this.issue;
    }
}
