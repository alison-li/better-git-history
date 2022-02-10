package bettergithistory.clients;

import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;

/**
 * For interfacing with Jira REST Client API.
 */
public class JiraProjectClient {
    private final JiraClient jira;
    private final BasicCredentials credentials = new BasicCredentials(
            "USERNAME",
            "PASSWORD"
    );

    public JiraProjectClient(String jiraUrl) {
        this.jira = new JiraClient(jiraUrl, credentials);
    }

    /**
     * Retrieve a specified Jira issue.
     * @param key The Jira issue key.
     * @return The Jira issue.
     * @throws JiraException If any error occurs as a response from Jira during attempt to retrieve.
     */
    public Issue getIssueById(String key) throws JiraException {
        return jira.getIssue(key);
    }
}
