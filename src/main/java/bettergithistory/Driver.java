package bettergithistory;

import bettergithistory.clients.GitHubRepositoryClient;
import bettergithistory.clients.JiraProjectClient;
import bettergithistory.extractors.Diff;
import bettergithistory.extractors.JGit;
import bettergithistory.util.CommitHistoryUtil;
import bettergithistory.util.FileUtil;
import com.github.difflib.patch.AbstractDelta;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Driver {
    public static void main(String[] args) throws IOException, JiraException {
//        testGitHub();
//        testJira();
         List<List<AbstractDelta<String>>> res = testDiff("../kafka",
                 "streams/src/main/java/org/apache/kafka/streams/Topology.java");
         System.out.println(res);
    }

    public static void testFileVersionGeneration(String gitPath, String fileName)
            throws IOException {
        FileUtil.cleanTempDirectory();
        JGit jgit = new JGit(gitPath);
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(fileName);
        jgit.generateFilesFromFileCommitHistory(commitMap);
    }

    public static List<List<AbstractDelta<String>>> testDiff(String repoPath, String filePath) throws IOException {
        JGit jgit = new JGit(repoPath);
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(filePath);

        testFileVersionGeneration(repoPath, filePath);

        List<List<AbstractDelta<String>>> deltasPerCommit = new ArrayList<>();
        for (int i = 0; i < commitMap.size() - 1; i++) {
            int leftVer = i;
            int rightVer = i + 1;
            List<AbstractDelta<String>> delta = Diff.getDiff(leftVer, rightVer);
            deltasPerCommit.add(delta);
        }
        return deltasPerCommit;
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
