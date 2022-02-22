package bettergithistory;

import bettergithistory.clients.GitHubRepositoryClient;
import bettergithistory.clients.IssueTrackingClient;
import bettergithistory.clients.JiraProjectClient;
import bettergithistory.extractors.Diff;
import bettergithistory.extractors.JGit;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.github.GHPullRequest;

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
     */
    public BetterGitHistory(JGit jgit, Map<RevCommit, String> commitMap) {
        this.jgit = jgit;
        this.commitMap = commitMap;
    }

    /**
     * Retrieve a map linking commits to a corresponding pull request.
     * Assumes if a commit message refers to a pull request ID, then the commit message will contain "(#<pull-request-id>)".
     * @param gitHubRepoClient The client to use to interact with the GitHub repository.
     * @param commits The list of commits to pull information for.
     * @return A map of commits mapped to corresponding GitHub pull requests.
     * @throws IOException
     */
    public Map<RevCommit, GHPullRequest> getCommitHistoryWithPullRequests(GitHubRepositoryClient gitHubRepoClient,
                                                                          List<RevCommit> commits)
            throws IOException {
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
     * @param commits The list of commits to pull information for.
     * @return A map of commits mapped to corresponding Jira issues.
     * @throws JiraException
     */
    public Map<RevCommit, Issue> getCommitHistoryWithJiraIssue(JiraProjectClient jiraProjectClient,
                                                               List<RevCommit> commits)
            throws JiraException {
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

    public List<RevCommit> filterByCodeDiff() throws IOException {
        jgit.generateFilesFromFileCommitHistory(commitMap);
        List<RevCommit> filteredCommits = new ArrayList<>();
        Map<RevCommit, List<AbstractDelta<String>>> commitDiffMap = Diff.getCommitDiffMap(commitMap);
        for (Map.Entry<RevCommit, List<AbstractDelta<String>>> diffEntry : commitDiffMap.entrySet()) {
            RevCommit commit = diffEntry.getKey();
            List<AbstractDelta<String>> deltas = diffEntry.getValue();
            boolean containsOtherChange = false;
            for (AbstractDelta<String> delta : deltas) {
                DeltaType deltaType = delta.getType();
                if (deltaType == DeltaType.CHANGE) {
                    List<String> sourceList = delta.getSource().getLines();
                    List<String> targetList = delta.getTarget().getLines();
                    containsOtherChange = evaluateDeltaByLine(sourceList) || evaluateDeltaByLine(targetList);
                    if (containsOtherChange) break;
                } else if (deltaType == DeltaType.DELETE || deltaType == DeltaType.INSERT) {
                    List<String> lineList;
                    // The target lines list is empty. Check the source lines to see if what was deleted matters.
                    if (deltaType == DeltaType.DELETE) lineList = delta.getSource().getLines();
                    // The source lines list is empty. Check the target lines to see if what was inserted matters.
                    else lineList = delta.getTarget().getLines();
                    containsOtherChange = evaluateDeltaByLine(lineList);
                    if (containsOtherChange) break;
                }
            }
            if (containsOtherChange) {
                filteredCommits.add(commit);
            }
        }
        return filteredCommits;
    }

    private boolean evaluateDeltaByLine(List<String> lineList) {
        // TODO: Save these variables for later. Can categorize reasons for de-emphasizing commits
        boolean containsDocChange, containsAnnotationChange, containsImportChange, containsNewLineChange, containsOtherChange;
        containsDocChange = containsAnnotationChange = containsImportChange = containsNewLineChange = containsOtherChange = false;
        for (String line : lineList) {
            if (line.matches("(.*)\\*(.*)") || line.matches("(.*)/\\*(.*)") || line.matches("(.*)//(.*)")) {
                containsDocChange = true;
            } else if (line.matches("(.*)import(.*)")) {
                containsImportChange = true;
            } else if (line.matches("(.*)@[A-Za-z]+(.*)")) {
                containsAnnotationChange = true;
            } else if (line.equals("")) {
                containsNewLineChange = true;
            } else {
                containsOtherChange = true;
                break;
            }
        }
        return containsOtherChange;
    }

    public List<RevCommit> filterByCommitMessage(List<RevCommit> commits) {
        List<RevCommit> filteredCommits = new ArrayList<>();
        return filteredCommits;
    }

    public List<RevCommit> filterByIssueTracker(IssueTrackingClient client, List<RevCommit> commits) throws Exception {
        List<RevCommit> filteredCommits = new ArrayList<>();
        if (client instanceof JiraProjectClient) {
            JiraProjectClient jiraProjectClient = (JiraProjectClient) client;
            Map<RevCommit, Issue> commitsWithJira = this.getCommitHistoryWithJiraIssue(jiraProjectClient, commits);
            // TODO: Do stuff with the Jira contents
        } else if (client instanceof GitHubRepositoryClient) {
            GitHubRepositoryClient gitHubRepositoryClient = (GitHubRepositoryClient) client;
            Map<RevCommit, GHPullRequest> commitsWithGitHub = this.getCommitHistoryWithPullRequests(gitHubRepositoryClient, commits);
            // TODO: Do stuff with the GitHub content
        } else {
            throw new Exception("Issue tracking client not recognized.");
        }
        return filteredCommits;
    }

    public List<RevCommit> reduceCommitDensity(IssueTrackingClient client) throws Exception {
        List<RevCommit> firstPassCommits = this.filterByCodeDiff();
        List<RevCommit> secondPassCommits = this.filterByCommitMessage(firstPassCommits);
        return this.filterByIssueTracker(client, secondPassCommits);

        // TODO: We might want to do a final pass here to compare the reduced list with the original list, then
        // annotate the less important commits; basically, don't remove commits but de-emphasize less important ones.
    }
}
