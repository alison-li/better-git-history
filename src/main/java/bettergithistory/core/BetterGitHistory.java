package bettergithistory.core;

import bettergithistory.clients.GHRepositoryClient;
import bettergithistory.clients.IssueTrackingClient;
import bettergithistory.clients.JiraProjectClient;
import bettergithistory.extractors.Diff;
import bettergithistory.extractors.JGit;
import bettergithistory.util.CommitHistoryUtil;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import net.rcarz.jiraclient.Comment;
import net.rcarz.jiraclient.Component;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.util.*;
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
     * @param ghRepositoryClient The client to use to interact with the GitHub repository.
     * @param commits The list of commits to pull information for.
     * @return A map of commits mapped to corresponding GitHub pull requests.
     */
    public Map<RevCommit, GHPullRequest> getCommitHistoryWithPullRequests(GHRepositoryClient ghRepositoryClient,
                                                                          List<RevCommit> commits)
            throws IOException {
        Map<RevCommit, GHPullRequest> commitToPullRequestMap = new LinkedHashMap<>();
        for (RevCommit commit : commits) {
            String message = commit.getShortMessage();
            Pattern pattern = Pattern.compile("(\\(#\\d+\\))");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String pullRequestId = matcher.group(1);
                int extractedId = Integer.parseInt(pullRequestId.substring(2, pullRequestId.length() - 1));
                commitToPullRequestMap.put(commit, ghRepositoryClient.getPullRequestById(extractedId));
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
     * Gets the metadata for an issue (e.g. JIRA or GitHub pull request) associated with a commit.
     * @param client The issue tracking system client to use.
     * @return A map of commits mapped to the metadata object containing information about the issue related to a commit.
     */
    public Map<RevCommit, AbstractIssueMetadata> getCommitIssueMetadata(IssueTrackingClient client)
            throws IllegalArgumentException, JiraException, IOException {
        Map<RevCommit, AbstractIssueMetadata> commitsWithIssueMetadata = new LinkedHashMap<>();
        if (client instanceof JiraProjectClient) {
            Map<RevCommit, Issue> commitsWithJira = this.getCommitHistoryWithJiraIssue((JiraProjectClient) client,
                    CommitHistoryUtil.getCommitsOnly(this.commitMap));
            for (Map.Entry<RevCommit, Issue> entry : commitsWithJira.entrySet()) {
                RevCommit commit = entry.getKey();
                Issue issue = entry.getValue();
                AbstractIssueMetadata issueMetadata = null;
                if (issue != null) {
                    issueMetadata = this.getIssueMetadata(commit, issue);
                }
                commitsWithIssueMetadata.put(commit, issueMetadata);
            }
        } else if (client instanceof GHRepositoryClient) {
            Map<RevCommit, GHPullRequest> commitsWithGitHub = this.getCommitHistoryWithPullRequests((GHRepositoryClient) client,
                    CommitHistoryUtil.getCommitsOnly(this.commitMap));
            for (Map.Entry<RevCommit, GHPullRequest> entry : commitsWithGitHub.entrySet()) {
                RevCommit commit = entry.getKey();
                GHPullRequest pullRequest = entry.getValue();
                AbstractIssueMetadata issueMetadata = null;
                if (pullRequest != null) {
                    issueMetadata = this.getIssueMetadata(commit, pullRequest);
                }
                commitsWithIssueMetadata.put(commit, issueMetadata);
            }
        } else {
            throw new IllegalArgumentException("Client type not recognized. Please use a supported client.");
        }
        return commitsWithIssueMetadata;
    }

    /**
     * Helper method for handling a JIRA issue.
     * @param commit The commit associated with a JIRA issue.
     * @param issue The JIRA issue associated with a commit.
     * @return The metadata object containing information about the JIRA issue.
     */
    private AbstractIssueMetadata getIssueMetadata(RevCommit commit, Issue issue) {
        JiraIssueMetadata issueMetadata = new JiraIssueMetadata(commit);
        List<Comment> commentsExcludeBots = new ArrayList<>();
        Set<String> people = new HashSet<>();
        people.add(issue.getAssignee().toString());
        int numCommitAuthorComments = 0;
        for (Comment comment : issue.getComments()) {
            String commentAuthorDisplayName = comment.getAuthor().getDisplayName();
            String commentAuthorName = comment.getAuthor().getName();
            String botRegex = "(.*)((\\b([Bb]ot|BOT))|(([Bb]ot|BOT)\\b))(.*)";
            if (!commentAuthorDisplayName.matches(botRegex) || !commentAuthorName.matches(botRegex)) {
                commentsExcludeBots.add(comment);
                people.add(commentAuthorName);
                String commitAuthorName = commit.getAuthorIdent().getName();
                if (commentAuthorDisplayName.equals(commitAuthorName)) {
                    numCommitAuthorComments++;
                }
            }
        }
        issueMetadata.setNumComments(commentsExcludeBots.size());
        issueMetadata.setNumCommitAuthorComments(numCommitAuthorComments);
        issueMetadata.setNumPeopleInvolved(people.size());
        // Specific to JIRA issues:
        issueMetadata.setPriority(issue.getPriority().toString());
        List<String> components = new ArrayList<>();
        for (Component component : issue.getComponents()) {
            components.add(component.getName());
        }
        issueMetadata.setComponents(components);
        issueMetadata.setNumIssueLinks(issue.getIssueLinks().size());
        issueMetadata.setLabels(issue.getLabels());
        issueMetadata.setNumSubTasks(issue.getSubtasks().size());
        issueMetadata.setNumVotes(issue.getVotes().getVotes());
        issueMetadata.setNumWatches(issue.getWatches().getWatchCount());
        return issueMetadata;
    }

    /**
     * Helper method for handling a GitHub pull request.
     * @param commit The commit associated with a JIRA issue.
     * @param pullRequest The GitHub pull request associated with a commit.
     * @return The metadata object containing information about the GH pull request.
     */
    private AbstractIssueMetadata getIssueMetadata(RevCommit commit, GHPullRequest pullRequest) throws IOException {
        GHPullRequestMetadata issueMetadata = new GHPullRequestMetadata(commit);
        List<GHIssueComment> commentsExcludeBots = new ArrayList<>();
        Set<String> people = new HashSet<>();
        String commitAuthorEmail = commit.getAuthorIdent().getEmailAddress();
        people.add(pullRequest.getUser().getEmail());
        int numCommitAuthorComments = 0;
        for (GHIssueComment comment : pullRequest.getComments()) {
            String commentAuthorName = comment.getUser().getName();
            String commentAuthorEmail = comment.getUser().getEmail();
            String botRegex = "(.*)((\\b([Bb]ot|BOT))|(([Bb]ot|BOT)\\b))(.*)";
            if (!commentAuthorName.matches(botRegex)) {
                commentsExcludeBots.add(comment);
                people.add(commentAuthorEmail);
                if (commentAuthorEmail.equals(commitAuthorEmail)) {
                    numCommitAuthorComments++;
                }
            }
        }
        issueMetadata.setNumComments(commentsExcludeBots.size());
        issueMetadata.setNumCommitAuthorComments(numCommitAuthorComments);
        issueMetadata.setNumPeopleInvolved(people.size());
        // Specific to GH pull requests:
        issueMetadata.setNumReviews(pullRequest.getReviewComments());
        return issueMetadata;
    }

    /**
     * Helper method for reducing the commit history of this BetterGitHistory instance.
     * file by using regex to detect "trivial" code diffs between commits.
     * @return A list of the filtered commits.
     */
    private Map<RevCommit, CommitDiffCategorization> filterByCodeDiff() throws IOException {
        jgit.generateFilesFromFileCommitHistory(this.commitMap);
        Map<RevCommit, CommitDiffCategorization> categorizedCommits = new LinkedHashMap<>();
        // Each commit is associated with a list of deltas.
        Map<RevCommit, List<AbstractDelta<String>>> commitDiffMap = Diff.getCommitDiffMap(this.commitMap);
        for (Map.Entry<RevCommit, List<AbstractDelta<String>>> diffEntry : commitDiffMap.entrySet()) {
            RevCommit commit = diffEntry.getKey();
            List<AbstractDelta<String>> deltas = diffEntry.getValue();
            // Each delta has a "source" list of lines affected and a "target" list of lines affected.
            // The source is the original version's lines and the target is the new version's lines.
            CommitDiffCategorization commitDiffCategorization = new CommitDiffCategorization(commit);
            for (AbstractDelta<String> delta : deltas) {
                DeltaType deltaType = delta.getType();
                CommitDiffCategorization tempDiffCategorization = new CommitDiffCategorization(commit);
                if (deltaType == DeltaType.CHANGE) {
                    List<String> sourceLines = delta.getSource().getLines();
                    List<String> targetLines = delta.getTarget().getLines();
                    CommitDiffCategorization sourceCommitDiffCategorization = evaluateDeltaByLine(sourceLines);
                    CommitDiffCategorization targetCommitDiffCategorization = evaluateDeltaByLine(targetLines);
                    tempDiffCategorization.setNumDoc(Math.max(sourceCommitDiffCategorization.getNumDoc(),
                                            targetCommitDiffCategorization.getNumDoc()));
                    tempDiffCategorization.setNumAnnotation(Math.max(sourceCommitDiffCategorization.getNumAnnotation(),
                            targetCommitDiffCategorization.getNumAnnotation()));
                    tempDiffCategorization.setNumImport(Math.max(sourceCommitDiffCategorization.getNumImport(),
                            targetCommitDiffCategorization.getNumImport()));
                    tempDiffCategorization.setNumNewLine(Math.max(sourceCommitDiffCategorization.getNumNewLine(),
                            targetCommitDiffCategorization.getNumNewLine()));
                    tempDiffCategorization.setNumOther(Math.max(sourceCommitDiffCategorization.getNumOther(),
                            targetCommitDiffCategorization.getNumOther()));
                } else if (deltaType == DeltaType.DELETE || deltaType == DeltaType.INSERT) {
                    List<String> lineList;
                    if (deltaType == DeltaType.DELETE) {
                        // The target lines list is empty. Check the source lines to see if what was deleted matters.
                        lineList = delta.getSource().getLines();
                    } else {
                        // The source lines list is empty. Check the target lines to see if what was inserted matters.
                        lineList = delta.getTarget().getLines();
                    }
                    tempDiffCategorization = evaluateDeltaByLine(lineList);
                }
                commitDiffCategorization.mergeCommitDiffCategorization(tempDiffCategorization);
            }
            categorizedCommits.put(commit, commitDiffCategorization);
        }
        return categorizedCommits;
    }

    /**
     * Helper method for filtering commits by code diff.
     * @param lineList A list of lines associated with a delta.
     * @return An object representing the trivial changes found in the set of lines.
     */
    private CommitDiffCategorization evaluateDeltaByLine(List<String> lineList) {
        int numDoc = 0, numAnnotation = 0, numImport = 0, numNewLine = 0, numOther = 0;
        for (String line : lineList) {
            if (line.matches("(.*)\\*(.*)")
                    || line.matches("(.*)/\\*(.*)")
                    || line.matches("(.*)//(.*)")) {
                numDoc++;
            } else if (line.matches("(.*)import(.*)")) {
                numImport++;
            } else if (line.matches("(.*)@[A-Za-z]+(.*)")) {
                numAnnotation++;
            } else if (line.equals("")) {
                numNewLine++;
            } else {
                numOther++;
            }
        }
        CommitDiffCategorization deltaCategorization = new CommitDiffCategorization(null);
        deltaCategorization.setNumDoc(numDoc);
        deltaCategorization.setNumAnnotation(numAnnotation);
        deltaCategorization.setNumImport(numImport);
        deltaCategorization.setNumNewLine(numNewLine);
        deltaCategorization.setNumOther(numOther);
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

    /**
     * Takes the initialized commit history and reduces the density of the history by examining the diffs
     * between each commit and tagging less useful commits.
     * Uses regex patterns to detect diffs containing trivial changes and a list of
     * given filter words for filtering commit messages.
     * @param filterWords A list of words to use for filtering based on commit message content.
     * @return A map of commits mapped to a set of tags for less useful commits. The set is null if the commit is
     *          not tagged as less useful.
     */
    public Map<RevCommit, CommitDiffCategorization> getAnnotatedCommitHistory(List<String> filterWords)
            throws IOException {
        Map<RevCommit, CommitDiffCategorization> annotatedCommits = this.filterByCodeDiff();
        if (filterWords.isEmpty()) return annotatedCommits;
        List<RevCommit> commitsFilteredByMessage = this.filterByCommitMessage(
                CommitHistoryUtil.getCommitsOnly(this.commitMap), filterWords
        );
        for (RevCommit commit : annotatedCommits.keySet()) {
            if (!commitsFilteredByMessage.contains(commit)) {
                CommitDiffCategorization lineCategorizations = annotatedCommits.get(commit);
                lineCategorizations.setNumFilter(1);
                annotatedCommits.put(commit, lineCategorizations);
            }
        }
        return annotatedCommits;
    }
}
