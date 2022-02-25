package bettergithistory;

import bettergithistory.clients.GitHubRepositoryClient;
import bettergithistory.clients.JiraProjectClient;
import bettergithistory.extractors.Diff;
import bettergithistory.extractors.JGit;
import bettergithistory.util.CommitHistoryUtil;
import com.github.difflib.patch.AbstractDelta;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.util.*;

public class Driver {
    public static void main(String[] args) throws Exception {
//        Map<String, List<AbstractDelta<String>>> res = testDiff("../kafka",
//                 "streams/src/main/java/org/apache/kafka/streams/Topology.java");
//         System.out.println(res);

        Map<RevCommit, CommitDiffCategorization> res = testReduceCommitDensity("../kafka",
                 "streams/src/main/java/org/apache/kafka/streams/Topology.java");
        CommitHistoryUtil.printAnnotatedCommitHistory(res);
    }

    public static void testFileVersionGeneration(String gitPath, String fileName)
            throws IOException {
        JGit jgit = new JGit(gitPath);
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(fileName);
        jgit.generateFilesFromFileCommitHistory(commitMap);
    }

    public static Map<RevCommit, CommitDiffCategorization> testReduceCommitDensity(String repoPath, String filePath) throws Exception {
        JGit jgit = new JGit(repoPath);
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(filePath);
        BetterGitHistory betterGitHistory = new BetterGitHistory(jgit, commitMap);
        List<String> filterWords = new ArrayList<>();
        filterWords.add("MINOR");
        // filterWords.add("refactor");
        Map<RevCommit, CommitDiffCategorization> filteredCommits = betterGitHistory.getAnnotatedCommitHistory(filterWords);
        return filteredCommits;
    }

    public static Map<String, List<AbstractDelta<String>>> testDiff(String repoPath, String filePath) throws IOException {
        JGit jgit = new JGit(repoPath);
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(filePath);
        testFileVersionGeneration(repoPath, filePath);
        Map<RevCommit, List<AbstractDelta<String>>> commitDiffMap = Diff.getCommitDiffMap(commitMap);
        Map<String, List<AbstractDelta<String>>> readableDiffMap = new LinkedHashMap<>();
        for (Map.Entry<RevCommit, List<AbstractDelta<String>>> entry : commitDiffMap.entrySet()) {
            readableDiffMap.put(entry.getKey().getShortMessage(), entry.getValue());
        }
        return readableDiffMap;
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
                .getCommitHistoryWithPullRequests(gitHubRepoClient, CommitHistoryUtil.getCommitsOnly(commitMap));
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
                .getCommitHistoryWithJiraIssue(jiraProjectClient, CommitHistoryUtil.getCommitsOnly(commitMap));
        CommitHistoryUtil.writeCommitHistoryWithJiraIssuesToJSON(commitToJiraIssueMap);
    }
}
