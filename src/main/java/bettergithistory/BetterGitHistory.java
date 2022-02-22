package bettergithistory;

import bettergithistory.clients.GitHubRepositoryClient;
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

    /**
     * Helper method for reducing the commit history of this BetterGitHistory instance.
     * file by using regex to detect "trivial" code diffs between commits.
     * @return A list of the filtered commits.
     */
    private Map<RevCommit, List<LineCategorizationType>> filterByCodeDiff() throws IOException {
        jgit.generateFilesFromFileCommitHistory(commitMap);
        Map<RevCommit, List<LineCategorizationType>> enrichedCommits = new LinkedHashMap<>();
        // Each commit is associated with a list of deltas.
        Map<RevCommit, List<AbstractDelta<String>>> commitDiffMap = Diff.getCommitDiffMap(commitMap);
        for (Map.Entry<RevCommit, List<AbstractDelta<String>>> diffEntry : commitDiffMap.entrySet()) {
            RevCommit commit = diffEntry.getKey();
            List<AbstractDelta<String>> deltas = diffEntry.getValue();
            boolean containsOtherChange = false;
            // Each delta has a "source" list of lines affected and a "target" list of lines affected.
            // The source is the original version's lines and the target is the new version's lines.
            LinesCategorization linesCategorization = new LinesCategorization(null);
            for (AbstractDelta<String> delta : deltas) {
                DeltaType deltaType = delta.getType();
                if (deltaType == DeltaType.CHANGE) {
                    List<String> sourceLines = delta.getSource().getLines();
                    List<String> targetLines = delta.getTarget().getLines();
                    LinesCategorization sourceLinesCategorization = evaluateDeltaByLine(sourceLines);
                    LinesCategorization targetLinesCategorization = evaluateDeltaByLine(targetLines);
                    containsOtherChange = sourceLinesCategorization.getContainsOther()
                            || targetLinesCategorization.getContainsOther();
                    if (containsOtherChange) break;
                    linesCategorization = new LinesCategorization(targetLines);
                    linesCategorization.setContainsDoc(sourceLinesCategorization.getContainsDoc()
                            && targetLinesCategorization.getContainsDoc());
                    linesCategorization.setContainsAnnotation(sourceLinesCategorization.getContainsAnnotation()
                            && targetLinesCategorization.getContainsAnnotation());
                    linesCategorization.setContainsImport(sourceLinesCategorization.getContainsImport()
                            && targetLinesCategorization.getContainsImport());
                    linesCategorization.setContainsNewLine(sourceLinesCategorization.getContainsNewLine()
                            && targetLinesCategorization.getContainsNewLine());
                    linesCategorization.setContainsOther(sourceLinesCategorization.getContainsOther()
                            && targetLinesCategorization.getContainsOther());
                } else if (deltaType == DeltaType.DELETE || deltaType == DeltaType.INSERT) {
                    List<String> lineList;
                    // The target lines list is empty. Check the source lines to see if what was deleted matters.
                    if (deltaType == DeltaType.DELETE) lineList = delta.getSource().getLines();
                    // The source lines list is empty. Check the target lines to see if what was inserted matters.
                    else lineList = delta.getTarget().getLines();
                    linesCategorization = evaluateDeltaByLine(lineList);
                    containsOtherChange = linesCategorization.getContainsOther();
                    if (containsOtherChange) break;
                }
            }
            if (containsOtherChange) {
                enrichedCommits.put(commit, null);
            } else {
                List<LineCategorizationType> annotations = new ArrayList<>();
                if (linesCategorization.getContainsDoc()) annotations.add(LineCategorizationType.DOCUMENTATION);
                if (linesCategorization.getContainsAnnotation()) annotations.add(LineCategorizationType.ANNOTATION);
                if (linesCategorization.getContainsImport()) annotations.add(LineCategorizationType.IMPORT);
                if (linesCategorization.getContainsNewLine()) annotations.add(LineCategorizationType.NEWLINE);
                enrichedCommits.put(commit, annotations);
            }
        }
        return enrichedCommits;
    }

    /**
     * Helper method for filtering commits by code diff.
     * @param lineList A list of lines associated with a delta.
     * @return An object representing the trivial changes found in the set of lines.
     */
    private LinesCategorization evaluateDeltaByLine(List<String> lineList) {
        boolean containsDoc = false, containsAnnotation = false, containsImport = false,
                containsNewLine = false, containsOther = false;
        for (String line : lineList) {
            if (line.matches("(.*)\\*(.*)")
                    || line.matches("(.*)/\\*(.*)")
                    || line.matches("(.*)//(.*)")) {
                containsDoc = true;
            } else if (line.matches("(.*)import(.*)")) {
                containsImport = true;
            } else if (line.matches("(.*)@[A-Za-z]+(.*)")) {
                containsAnnotation = true;
            } else if (line.equals("")) {
                containsNewLine = true;
            } else {
                containsOther = true;
                break;
            }
        }
        LinesCategorization deltaCategorization = new LinesCategorization(lineList);
        deltaCategorization.setContainsDoc(containsDoc);
        deltaCategorization.setContainsAnnotation(containsAnnotation);
        deltaCategorization.setContainsImport(containsImport);
        deltaCategorization.setContainsNewLine(containsNewLine);
        deltaCategorization.setContainsOther(containsOther);
        return deltaCategorization;
    }

    /**
     * Helper method for allowing the user to decide what words appearing in commits they would prefer filter out.
     * Examines the filter words given in a commit's full message.
     * @param commits The list of commits to filter.
     * @param customFilterWords The list of words to search for in trivial commits.
     * @return A list of filtered commits.
     */
    private List<RevCommit> filterByCommitMessage(List<RevCommit> commits, List<String> customFilterWords) {
        if (customFilterWords.isEmpty()) return commits;
        List<RevCommit> filteredCommits = new ArrayList<>();
        for (RevCommit commit : commits) {
            String commitMessage = commit.getFullMessage();
            boolean containsTrivialWord = false;
            for (String trivialWord : customFilterWords) {
                if (commitMessage.contains(trivialWord)) {
                    containsTrivialWord = true;
                    break;
                }
            }
            if (!containsTrivialWord) {
                filteredCommits.add(commit);
            }
        }
        return filteredCommits;
    }

    public Map<RevCommit, List<LineCategorizationType>> getAnnotatedCommitHistory(List<String> filterWords)
            throws IOException {
        Map<RevCommit, List<LineCategorizationType>> annotatedCommits = this.filterByCodeDiff();
        if (filterWords.isEmpty()) return annotatedCommits;
        List<RevCommit> firstPassCommits = new ArrayList<>();
        for (Map.Entry<RevCommit, List<LineCategorizationType>> entry : annotatedCommits.entrySet()) {
            RevCommit commit = entry.getKey();
            List<LineCategorizationType> lineCategories = entry.getValue();
            if (lineCategories == null) {
                firstPassCommits.add(commit);
            }
        }
        List<RevCommit> secondPassCommits = this.filterByCommitMessage(firstPassCommits, filterWords);
        for (RevCommit commit : annotatedCommits.keySet()) {
            if (!secondPassCommits.contains(commit)) {
                List<LineCategorizationType> lineCategorizations = annotatedCommits.get(commit);
                if (lineCategorizations == null) lineCategorizations = new ArrayList<>();
                lineCategorizations.add(LineCategorizationType.FILTER);
                annotatedCommits.put(commit, lineCategorizations);
            }
        }
        return annotatedCommits;
    }
}
