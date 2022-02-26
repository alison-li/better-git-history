package bettergithistory;

import bettergithistory.clients.GHRepositoryClient;
import bettergithistory.clients.JiraProjectClient;
import bettergithistory.core.AbstractIssueMetadata;
import bettergithistory.core.BetterGitHistory;
import bettergithistory.core.CommitDiffCategorization;
import bettergithistory.extractors.Diff;
import bettergithistory.extractors.JGit;
import bettergithistory.util.CommitHistoryUtil;
import com.github.difflib.patch.AbstractDelta;
import net.rcarz.jiraclient.JiraException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Driver {
    public static void main(String[] args) throws Exception {
//        Map<String, List<AbstractDelta<String>>> res = testDiff("../kafka",
//                 "streams/src/main/java/org/apache/kafka/streams/Topology.java");
//         System.out.println(res);

//        Map<RevCommit, CommitDiffCategorization> res = testReduceCommitDensity("../kafka",
//                 "streams/src/main/java/org/apache/kafka/streams/Topology.java");
//        CommitHistoryUtil.printAnnotatedCommitHistory(res);

        Map<RevCommit, AbstractIssueMetadata> res = testIssueMetadataJIRA("../kafka",
                "streams/src/main/java/org/apache/kafka/streams/Topology.java");
        System.out.println(res);
    }

    public static void testFileGeneration(String gitPath, String fileName)
            throws IOException {
        JGit jgit = new JGit(gitPath);
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(fileName);
        jgit.generateFilesFromFileCommitHistory(commitMap);
    }

    public static Map<RevCommit, AbstractIssueMetadata> testIssueMetadataJIRA(String repoPath, String filePath) throws JiraException, IOException {
        JGit jgit = new JGit(repoPath);
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(filePath);
        BetterGitHistory betterGitHistory = new BetterGitHistory(jgit, commitMap);
        JiraProjectClient jiraProjectClient = new JiraProjectClient("https://issues.apache.org/jira/");
        return betterGitHistory.getCommitIssueMetadata(jiraProjectClient);
    }

    public static Map<RevCommit, CommitDiffCategorization> testReduceCommitDensity(String repoPath, String filePath) throws Exception {
        JGit jgit = new JGit(repoPath);
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(filePath);
        BetterGitHistory betterGitHistory = new BetterGitHistory(jgit, commitMap);
        List<String> filterWords = new ArrayList<>();
        filterWords.add("MINOR");
        // filterWords.add("refactor");
        return betterGitHistory.getAnnotatedCommitHistory(filterWords);
    }

    public static Map<String, List<AbstractDelta<String>>> testDiff(String repoPath, String filePath) throws IOException {
        JGit jgit = new JGit(repoPath);
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(filePath);
        testFileGeneration(repoPath, filePath);
        Map<RevCommit, List<AbstractDelta<String>>> commitDiffMap = Diff.getCommitDiffMap(commitMap);
        Map<String, List<AbstractDelta<String>>> readableDiffMap = new LinkedHashMap<>();
        for (Map.Entry<RevCommit, List<AbstractDelta<String>>> entry : commitDiffMap.entrySet()) {
            readableDiffMap.put(entry.getKey().getShortMessage(), entry.getValue());
        }
        return readableDiffMap;
    }

    public static void testGitHub() throws IOException {
        JGit jgit = new JGit("../caprine");
        String fileName = "source/browser.ts";
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(fileName);
        GHRepositoryClient gitHubRepoClient = new GHRepositoryClient("sindresorhus/caprine");
        Map<RevCommit, GHPullRequest> commitToPullRequestMap = new BetterGitHistory(jgit, commitMap)
                .getCommitHistoryWithPullRequests(gitHubRepoClient, CommitHistoryUtil.getCommitsOnly(commitMap));
        CommitHistoryUtil.writeCommitHistoryWithPullRequestsToJSON(commitToPullRequestMap);
    }
}
