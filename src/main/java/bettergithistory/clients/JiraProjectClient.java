package bettergithistory.clients;

import io.github.cdimascio.dotenv.Dotenv;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;

/**
 * For interfacing with Jira REST Client API.
 */
public class JiraProjectClient implements IssueTrackingClient {
    private final JiraClient jira;

    public JiraProjectClient(String jiraUrl) {
        Dotenv dotEnv = Dotenv.load();
        String jiraUser = dotEnv.get("JIRA_USER");
        String jiraPass = dotEnv.get("JIRA_PASSWORD");
        BasicCredentials credentials = new BasicCredentials(jiraUser, jiraPass);
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
