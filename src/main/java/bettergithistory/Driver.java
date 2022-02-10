package bettergithistory;

import bettergithistory.clients.GitHubRepositoryClient;
import bettergithistory.clients.JiraProjectClient;
import bettergithistory.jgit.JGit;
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
        testJira();
    }

    public static void testGitHub() throws IOException {
        // Initialize JGit object for working for repo
        JGit jgit = new JGit("../caprine");
        String fileName = "css/new-design/browser.css";

        // Get file's commit history
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(fileName);
//        System.out.println("ORIGINAL HISTORY");
//        CommitHistoryUtil.printCommitHistory(commitMap);

        // Initialize a client for interacting with a GitHub repository.
        GitHubRepositoryClient gitHubRepoClient = new GitHubRepositoryClient("sindresorhus/caprine");
        Map<RevCommit, GHPullRequest> commitToPullRequestMap = new BetterGitHistory(jgit, commitMap)
                .getCommitHistoryWithPullRequests(gitHubRepoClient);
        System.out.println(commitToPullRequestMap);
    }

    public static void testJira() throws IOException, JiraException {
        JGit jgit = new JGit("../kafka");
        String fileName = "streams/src/main/java/org/apache/kafka/streams/Topology.java";
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(fileName);

        // Initialize a client for interacting with a Jira repository.
        JiraProjectClient jiraProjectClient = new JiraProjectClient("https://issues.apache.org/jira/");
        Map<RevCommit, Issue> commitToJiraIssueMap = new BetterGitHistory(jgit, commitMap)
                .getCommitHistoryWithJiraIssue(jiraProjectClient);
        System.out.println(commitToJiraIssueMap);
    }
}
