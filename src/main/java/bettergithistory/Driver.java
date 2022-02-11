package bettergithistory;

import bettergithistory.clients.GitHubRepositoryClient;
import bettergithistory.clients.JiraProjectClient;
import bettergithistory.extractors.JGit;
import bettergithistory.util.CommitHistoryUtil;
import bettergithistory.util.FileUtil;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.util.Map;

public class Driver {
    public static void main(String[] args) throws IOException, JiraException {
        // Clean the temp directory used for handling generated files
        FileUtil.cleanTempDirectory();
        testGitHub();
        testJira();
    }

    public static void testGitHub() throws IOException {
        // Initialize JGit object for working for repo
        JGit jgit = new JGit("../caprine");
        String fileName = "source/browser.ts";

        // Get file's commit history
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(fileName);

        // Initialize a client for interacting with a GitHub repository.
        GitHubRepositoryClient gitHubRepoClient = new GitHubRepositoryClient("sindresorhus/caprine");
        Map<RevCommit, GHPullRequest> commitToPullRequestMap = new BetterGitHistory(jgit, commitMap)
                .getCommitHistoryWithPullRequests(gitHubRepoClient);
        CommitHistoryUtil.writeCommitHistoryWithPullRequestsToJSON(commitToPullRequestMap);
    }

    public static void testJira() throws IOException, JiraException {
        JGit jgit = new JGit("../kafka");
        String fileName = "streams/src/main/java/org/apache/kafka/streams/Topology.java";

        // Get file's commit history
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(fileName);

        // Initialize a client for interacting with a Jira repository.
        JiraProjectClient jiraProjectClient = new JiraProjectClient("https://issues.apache.org/jira/");
        Map<RevCommit, Issue> commitToJiraIssueMap = new BetterGitHistory(jgit, commitMap)
                .getCommitHistoryWithJiraIssue(jiraProjectClient);
        CommitHistoryUtil.writeCommitHistoryWithJiraIssuesToJSON(commitToJiraIssueMap);
    }
}
