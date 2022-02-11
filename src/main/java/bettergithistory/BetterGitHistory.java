package bettergithistory;

import bettergithistory.clients.GitHubRepositoryClient;
import bettergithistory.clients.JiraProjectClient;
import bettergithistory.extractors.JGit;
import bettergithistory.util.CommitHistoryUtil;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A commit history wrapper class for improving an input commit history.
 * It is assumed that the input commit history is a single file's commit history.
 */
public class BetterGitHistory {
    private final JGit jgit;
    private final Map<RevCommit, String> commitMap;

    /**
     * Instantiate a commit history abstraction.
     * @param jgit The JGit object associated with the commit history.
     * @param commitMap The original commit history to we want to improve and manipulate.
     */
    public BetterGitHistory(JGit jgit, Map<RevCommit, String> commitMap) {
        this.jgit = jgit;
        this.commitMap = commitMap;
    }

    /**
     * Retrieve a map linking commits to a corresponding pull request.
     * Assumes if a commit message refers to a pull request ID, then the commit message will contain "(#<pull-request-id>)".
     * @param gitHubRepoClient The client to use to interact with the GitHub repository.
     * @return A map of commits mapped to corresponding GitHub pull requests.
     * @throws IOException
     */
    public Map<RevCommit, GHPullRequest> getCommitHistoryWithPullRequests(GitHubRepositoryClient gitHubRepoClient)
            throws IOException {
        List<RevCommit> commits = CommitHistoryUtil.getCommitsOnly(this.commitMap);
        Map<RevCommit, GHPullRequest> commitToPullRequestMap = new LinkedHashMap<>();
        for (RevCommit commit : commits) {
            String message = commit.getShortMessage();
            Pattern pattern = Pattern.compile("(\\(#\\d+\\))");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String pullRequestId = matcher.group(1);
                int extractedId = Integer.parseInt(
                        pullRequestId.substring(2, pullRequestId.length() - 1)
                );
                commitToPullRequestMap.put(commit, gitHubRepoClient.getPullRequestById(extractedId));
            } else {
                // A commit with no linked PR could still be useful
                commitToPullRequestMap.put(commit, null);
            }
        }
        return commitToPullRequestMap;
    }

    /**
     * Retrieve a map linking commits to a corresponding Jira issue.
     * @param jiraProjectClient The client to use to interact with the Jira instance.
     * @return A map of commits mapped to corresponding Jira issues.
     * @throws JiraException
     */
    public Map<RevCommit, Issue> getCommitHistoryWithJiraIssue(JiraProjectClient jiraProjectClient)
            throws JiraException {
        List<RevCommit> commits = CommitHistoryUtil.getCommitsOnly(this.commitMap);
        Map<RevCommit, Issue> commitToJiraIssueMap = new LinkedHashMap<>();
        for (RevCommit commit : commits) {
            String message = commit.getShortMessage();
            Pattern pattern = Pattern.compile("([A-Z]+-\\d+)");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String issueKey = matcher.group(1);
                commitToJiraIssueMap.put(commit, jiraProjectClient.getIssueById(issueKey));
            } else {
                commitToJiraIssueMap.put(commit, null);
            }
        }
        return commitToJiraIssueMap;
    }
}
