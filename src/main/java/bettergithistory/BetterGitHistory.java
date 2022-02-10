package bettergithistory;

import bettergithistory.clients.GitHubRepositoryClient;
import bettergithistory.clients.JiraProjectClient;
import bettergithistory.extractors.Distiller;
import bettergithistory.extractors.JGit;
import bettergithistory.util.CommitHistoryUtil;
import bettergithistory.util.FileUtil;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.github.GHPullRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
     * @throws IOException
     */
    public BetterGitHistory(JGit jgit, Map<RevCommit, String> commitMap) throws IOException {
        this.jgit = jgit;
        this.commitMap = commitMap;
        if (FileUtil.isTempDirectoryEmpty()) {
            jgit.generateFilesFromFileCommitHistory(this.commitMap);
        }
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

    /**
     * Retrieve a filtered version of the commit history using ChangeDistiller.
     * @return The filtered commit history.
     */
    public Map<RevCommit, String> getChangeDistillerCommitHistory() {
        if (this.commitMap.size() <= 2) return commitMap;

        Map<RevCommit, String> filteredCommitMap = new LinkedHashMap<>();
        List<RevCommit> commits = CommitHistoryUtil.getCommitsOnly(commitMap);
        for (int verNum = 0; verNum < this.commitMap.size() - 1; verNum++) {
            int leftVer = verNum;
            int rightVer = verNum + 1;
            File left = new File(String.format("temp/ver%d.java", leftVer));
            File right = new File(String.format("temp/ver%d.java", rightVer));
            List<SourceCodeChange> changes = Distiller.extractSourceCodeChanges(left, right);
            if (!changes.isEmpty()) {
                System.out.printf("HIT VER %d -> VER %d%n", leftVer, rightVer);
                // Keep both commits in the history since the change list is non-empty between the two versions.
                RevCommit firstCommit = commits.get(leftVer);
                String firstPath = this.commitMap.get(firstCommit);
                RevCommit secondCommit = commits.get(rightVer);
                String secondPath = this.commitMap.get(secondCommit);
                filteredCommitMap.put(firstCommit, firstPath);
                filteredCommitMap.put(secondCommit, secondPath);
            }
        }

        return filteredCommitMap;
    }

    /**
     * Removed for now due to few results using ChangeDistiller.
     * Two pointer approach to comparing files.
     */
//    public Map<RevCommit, String> getFilteredCommitHistoryWithFileHop() {
//        if (this.commitMap.size() <= 2) return commitMap;
//
//        Map<RevCommit, String> filteredCommitMap = new LinkedHashMap<>();
//        List<RevCommit> commits = new ArrayList<>(this.commitMap.keySet());
//        int leftVer = 0;
//        int rightVer = 1;
//        while (rightVer < this.commitMap.size()) {
//            File left = new File(String.format("temp/ver%d.java", leftVer));
//            File right = new File(String.format("temp/ver%d.java", rightVer));
//            List<SourceCodeChange> changes = Distiller.extractSourceCodeChanges(left, right);
//            if (!changes.isEmpty()) {
//                System.out.printf("HIT VER %d -> VER %d%n", leftVer, rightVer);
//                // Keep both commits in the history since the change list is non-empty between the two versions.
//                RevCommit firstCommit = commits.get(leftVer);
//                String firstPath = this.commitMap.get(firstCommit);
//                RevCommit secondCommit = commits.get(rightVer);
//                String secondPath = this.commitMap.get(secondCommit);
//                filteredCommitMap.put(firstCommit, firstPath);
//                filteredCommitMap.put(secondCommit, secondPath);
//                leftVer = rightVer;
//            }
//            rightVer++;
//        }
//
//        return filteredCommitMap;
//    }

    /**
     * Retrieve all of the source code changes performed in a file.
     * @return A chronological list of the source code change operations performed.
     */
    public List<List<SourceCodeChange>> getAllChangeDistillerSourceCodeChanges() {
        List<List<SourceCodeChange>> allSourceCodeChanges = new ArrayList<>();
        // This will give us a chronological list of source code changes since the commit map
        // is ordered from most recent commit to oldest.
        // We iterate backwards starting from the oldest file version to see the subsequent changes applied.
        for (int verNum = 0; verNum < this.commitMap.size() - 1; verNum++) {
            File left = new File(String.format("temp/ver%d.java", verNum));
            File right = new File(String.format("temp/ver%d.java", verNum + 1));
            List<SourceCodeChange> changes = Distiller.extractSourceCodeChanges(left, right);
            if (!changes.isEmpty()) {
                allSourceCodeChanges.add(changes);
            }
        }
        return allSourceCodeChanges;
    }
}
