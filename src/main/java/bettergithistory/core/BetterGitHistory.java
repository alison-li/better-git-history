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
     * Gets all of the issues from an issue tracking system that is associated with a commit.
     * @param client Specifies the issue tracking system client to use, e.g. JIRA or GitHub.
     * @return A map of commits mapped to an associated issue. Null if no issue is found linked to the commit.
     */
    public Map<RevCommit, AbstractIssue> getCommitIssues(IssueTrackingClient client) throws Exception {
        Map<RevCommit, AbstractIssue> commitToIssueMap = new LinkedHashMap<>();
        Pattern pattern;
        if (client instanceof JiraProjectClient) {
            pattern = Pattern.compile("([A-Z]+-\\d+)");
        } else if (client instanceof GHRepositoryClient) {
            pattern = Pattern.compile("(\\(#\\d+\\))");
        } else {
            throw new IllegalArgumentException("Client type not recognized. Please use a supported client.");
        }
        for (RevCommit commit : this.commitMap.keySet()) {
            String message = commit.getShortMessage();
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String issueId = matcher.group(1);
                if (client instanceof GHRepositoryClient) {
                    // Extract only the issue number for GH pull request retrieval
                    issueId = issueId.substring(2, issueId.length() - 1);
                }
                commitToIssueMap.put(commit, client.getIssueById(issueId));
            } else {
                commitToIssueMap.put(commit, null);
            }
        }
        return commitToIssueMap;
    }

    /**
     * Gets the metadata for an issue (e.g. JIRA or GitHub pull request) associated with a commit.
     * @param client The issue tracking system client to use.
     * @return A map of commits mapped to the metadata object containing information about the issue related to a commit.
     */
    public Map<RevCommit, AbstractIssueMetadata> getCommitIssuesMetadata(IssueTrackingClient client) throws Exception {
        Map<RevCommit, AbstractIssueMetadata> commitsWithIssueMetadata = new LinkedHashMap<>();
        Map<RevCommit, AbstractIssue> commitsWithJira = this.getCommitIssues(client);
        for (Map.Entry<RevCommit, AbstractIssue> entry : commitsWithJira.entrySet()) {
            RevCommit commit = entry.getKey();
            AbstractIssue issue = entry.getValue();
            AbstractIssueMetadata issueMetadata = null;
            if (issue != null) {
                issueMetadata = this.getIssueMetadata(commit, issue);
            }
            commitsWithIssueMetadata.put(commit, issueMetadata);
        }
        return commitsWithIssueMetadata;
    }

    /**
     * Generates an issue metadata object when given a commit and an issue from an issue tracking system.
     * @param commit The commit to generate issue metadata for.
     * @param issue The issue associated with the commit.
     * @return An issue metadata object holding information about a commit and the associated issue.
     */
    private AbstractIssueMetadata getIssueMetadata(RevCommit commit, AbstractIssue issue) throws IOException {
        AbstractIssueMetadata issueMetadata;
        String botNameRegex = "(.*)((\\b([Bb]ot|BOT))|(([Bb]ot|BOT)\\b))(.*)";
        if (issue instanceof JiraIssueWrapper) {
            Issue jiraIssue = ((JiraIssueWrapper) issue).getIssue();
            JiraIssueMetadata jiraIssueMetadata = new JiraIssueMetadata(commit);
            List<Comment> commentsExcludeBots = new ArrayList<>();
            Set<String> people = new HashSet<>();
            people.add(jiraIssue.getAssignee().toString());
            int numCommitAuthorComments = 0;
            for (Comment comment : jiraIssue.getComments()) {
                String commentAuthorDisplayName = comment.getAuthor().getDisplayName();
                String commentAuthorName = comment.getAuthor().getName();
                if (!commentAuthorDisplayName.matches(botNameRegex) || !commentAuthorName.matches(botNameRegex)) {
                    commentsExcludeBots.add(comment);
                    people.add(commentAuthorName);
                    String commitAuthorName = commit.getAuthorIdent().getName();
                    if (commentAuthorDisplayName.equals(commitAuthorName)) {
                        numCommitAuthorComments++;
                    }
                }
            }
            jiraIssueMetadata.setNumComments(commentsExcludeBots.size());
            jiraIssueMetadata.setNumCommitAuthorComments(numCommitAuthorComments);
            jiraIssueMetadata.setNumPeopleInvolved(people.size());
            // Specific to JIRA issues:
            jiraIssueMetadata.setPriority(jiraIssue.getPriority().toString());
            List<String> components = new ArrayList<>();
            for (Component component : jiraIssue.getComponents()) {
                components.add(component.getName());
            }
            jiraIssueMetadata.setComponents(components);
            jiraIssueMetadata.setNumIssueLinks(jiraIssue.getIssueLinks().size());
            jiraIssueMetadata.setLabels(jiraIssue.getLabels());
            jiraIssueMetadata.setNumSubTasks(jiraIssue.getSubtasks().size());
            jiraIssueMetadata.setNumVotes(jiraIssue.getVotes().getVotes());
            jiraIssueMetadata.setNumWatches(jiraIssue.getWatches().getWatchCount());
            issueMetadata = jiraIssueMetadata;
        } else if (issue instanceof GHPullRequestWrapper) {
            GHPullRequest pullRequest = ((GHPullRequestWrapper) issue).getIssue();
            GHPullRequestMetadata pullRequestMetadata = new GHPullRequestMetadata(commit);
            List<GHIssueComment> commentsExcludeBots = new ArrayList<>();
            Set<String> people = new HashSet<>();
            String commitAuthorEmail = commit.getAuthorIdent().getEmailAddress();
            people.add(pullRequest.getUser().getEmail());
            int numCommitAuthorComments = 0;
            for (GHIssueComment comment : pullRequest.getComments()) {
                String commentAuthorName = comment.getUser().getName();
                String commentAuthorEmail = comment.getUser().getEmail();
                if (!commentAuthorName.matches(botNameRegex)) {
                    commentsExcludeBots.add(comment);
                    people.add(commentAuthorEmail);
                    if (commentAuthorEmail.equals(commitAuthorEmail)) {
                        numCommitAuthorComments++;
                    }
                }
            }
            pullRequestMetadata.setNumComments(commentsExcludeBots.size());
            pullRequestMetadata.setNumCommitAuthorComments(numCommitAuthorComments);
            pullRequestMetadata.setNumPeopleInvolved(people.size());
            // Specific to GH pull requests:
            pullRequestMetadata.setNumReviews(pullRequest.getReviewComments());
            issueMetadata = pullRequestMetadata;
        } else {
            throw new IllegalArgumentException("Issue type not recognized. Please check if client and issue type are supported.");
        }
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
    public Map<RevCommit, CommitDiffCategorization> getAnnotatedCommitHistory(List<String> filterWords) throws IOException {
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
