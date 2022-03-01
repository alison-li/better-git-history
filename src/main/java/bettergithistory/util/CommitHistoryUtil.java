package bettergithistory.util;

import bettergithistory.core.*;
import net.rcarz.jiraclient.Issue;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for working with the commit map (representing Git history) of a file.
 */
public class CommitHistoryUtil {
    /**
     * Prints commit information from commit to file path in an easy-to-read format.
     * @param commitMap The commits to print.
     */
    public static void printCommitHistory(Map<RevCommit, String> commitMap) {
        for (Map.Entry<RevCommit, String> entry : commitMap.entrySet()) {
            RevCommit commit = entry.getKey();
            String formatted = String.format("%-25s %-10s", commit.getAuthorIdent().getName(),
                    commit.getShortMessage());
            System.out.println(formatted);
        }
    }

    /**
     * Prints annotated commit information where a commit is annotated for removal/de-emphasis due to the content
     * of the commit being trivial.
     * @param annotatedCommitMap A commit map where each commit is mapped to annotations if it is judged to be trivial.
     */
    public static void printAnnotatedCommitHistory(Map<RevCommit, CommitDiffCategorization> annotatedCommitMap) {
        for (Map.Entry<RevCommit, CommitDiffCategorization> entry : annotatedCommitMap.entrySet()) {
            RevCommit commit = entry.getKey();
            CommitDiffCategorization diffCategorization = entry.getValue();
            String docChanges = String.format("%s : %d", CommitDiffCategorizationType.DOCUMENTATION, diffCategorization.getNumDoc());
            String annotationChanges = String.format("%s : %d", CommitDiffCategorizationType.ANNOTATION, diffCategorization.getNumAnnotation());
            String importChanges = String.format("%s : %d", CommitDiffCategorizationType.IMPORT, diffCategorization.getNumImport());
            String newLineChanges = String.format("%s : %d", CommitDiffCategorizationType.NEWLINE, diffCategorization.getNumNewLine());
            String otherChanges = String.format("%s : %d", CommitDiffCategorizationType.OTHER, diffCategorization.getNumOther());
            String messageFiltered = String.format("%s : %d", CommitDiffCategorizationType.FILTER, diffCategorization.getNumFilter());
            String formatted = String.format("%-100s [%s, %s, %s, %s, %s, %s]", commit.getShortMessage(),
                    docChanges, annotationChanges, importChanges, newLineChanges, otherChanges, messageFiltered);
            System.out.println(formatted);
        }
    }

    /**
     * Prints issue metadata information for each commit if an issue exists.
     * @param commitIssueMetadataMap A commit map where each commit is mapped to issue metadata.
     */
    public static void printCommitHistoryIssueMetadata(Map<RevCommit, AbstractIssueMetadata> commitIssueMetadataMap) throws Exception {
        for (Map.Entry<RevCommit, AbstractIssueMetadata> entry : commitIssueMetadataMap.entrySet()) {
            RevCommit commit = entry.getKey();
            AbstractIssueMetadata issueMetadata = entry.getValue();
            if (issueMetadata == null) {
                System.out.printf("%-100s%n", commit.getShortMessage());
                continue;
            }
            String numComments = String.format("NUM_COMMENTS : %d", issueMetadata.getNumComments());
            String numCommitAuthorComments = String.format("NUM_COMMIT_AUTHOR_COMMENTS : %d", issueMetadata.getNumCommitAuthorComments());
            String proportionCommitAuthorComments = String.format("PROPORTION_COMMIT_AUTHOR : %f", ((double) issueMetadata.getNumCommitAuthorComments()) / issueMetadata.getNumComments());
            String numPeopleInvolved = String.format("NUM_PEOPLE_INVOLVED : %d", issueMetadata.getNumPeopleInvolved());
            if (issueMetadata instanceof JiraIssueMetadata) {
                String priority = String.format("PRIORITY : %s", ((JiraIssueMetadata) issueMetadata).getPriority());
                String numIssueLinks = String.format("NUM_ISSUE_LINKS : %d", ((JiraIssueMetadata) issueMetadata).getNumIssueLinks());
                String numSubTasks = String.format("NUM_SUBTASKS : %d", ((JiraIssueMetadata) issueMetadata).getNumSubTasks());
                String numVotes = String.format("NUM_VOTES : %d", ((JiraIssueMetadata) issueMetadata).getNumVotes());
                String numWatches = String.format("NUM_WATCHES : %d", ((JiraIssueMetadata) issueMetadata).getNumWatches());
                String formatted = String.format("%-100s [%s, %s, %s, %s, %s, %s, %s, %s, %s]", commit.getShortMessage(),
                        numComments, numCommitAuthorComments, proportionCommitAuthorComments, numPeopleInvolved, priority,
                        numIssueLinks, numSubTasks, numVotes, numWatches);
                System.out.println(formatted);
            } else if (issueMetadata instanceof GHPullRequestMetadata) {
                String numReviews = String.format("NUM_REVIEWS : %d", ((GHPullRequestMetadata) issueMetadata).getNumReviews());
                String formatted = String.format("%-100s [%s, %s, %s, %s, %s]", commit.getShortMessage(),
                        numComments, numCommitAuthorComments, proportionCommitAuthorComments, numPeopleInvolved, numReviews);
                System.out.println(formatted);
            } else {
                throw new Exception("Issue type is not supported or recognized.");
            }
        }
    }

    /**
     * Writes the commits and linked pull requests to a JSON file.
     * @param commitMap The commits to write.
     */
    public static void writeCommitHistoryWithPullRequestsToJSON(Map<RevCommit, GHPullRequest> commitMap)
            throws IOException {
        FileWriter file = new FileWriter("json/commitsToPullRequests.json");
        JSONArray jsonArray = new JSONArray();
        for (Map.Entry<RevCommit, GHPullRequest> entry : commitMap.entrySet()) {
            RevCommit commit = entry.getKey();
            GHPullRequest pullRequest = entry.getValue();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("commitAuthorName", commit.getAuthorIdent().getName());
            jsonObject.put("commitAuthorEmail", commit.getAuthorIdent().getEmailAddress());
            jsonObject.put("commitTime", commit.getCommitTime());
            jsonObject.put("commitShortMessage", commit.getShortMessage());
            jsonObject.put("commitFullMessage", commit.getFullMessage());
            if (pullRequest != null) {
                jsonObject.put("pullRequestUser", pullRequest.getUser().getName());
                jsonObject.put("pullRequestTitle", pullRequest.getTitle());
                jsonObject.put("pullRequestBody", pullRequest.getBody());

                // Avoid circular hierarchy exception
                JSONArray jsonCommentArray = new JSONArray();
                for (GHIssueComment comment : pullRequest.getComments()) {
                    JSONObject jsonCommentObject = new JSONObject();
                    jsonCommentObject.put("commentBody", comment.getBody());
                    jsonCommentObject.put("commentUser", comment.getUser().getName());
                    jsonCommentArray.add(jsonCommentObject);
                }

                jsonObject.put("pullRequestComments", jsonCommentArray);
            }
            jsonArray.add(jsonObject);
        }
        file.write(jsonArray.toString());
        file.close();
    }

    /**
     * Writes the commits and linked Jira issue information to a JSON file.
     * @param commitMap The commits to write.
     * @throws IOException
     */
    public static void writeCommitHistoryWithJiraIssuesToJSON(Map<RevCommit, Issue> commitMap)
            throws IOException {
        FileWriter file = new FileWriter("json/commitsToJiraIssues.json");
        JSONArray jsonArray = new JSONArray();
        for (Map.Entry<RevCommit, Issue> entry : commitMap.entrySet()) {
            RevCommit commit = entry.getKey();
            Issue issue = entry.getValue();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("commitAuthorName", commit.getAuthorIdent().getName());
            jsonObject.put("commitAuthorEmail", commit.getAuthorIdent().getEmailAddress());
            jsonObject.put("commitTime", commit.getCommitTime());
            jsonObject.put("commitShortMessage", commit.getShortMessage());
            jsonObject.put("commitFullMessage", commit.getFullMessage());
            if (issue != null) {
                jsonObject.put("issueKey", issue.getKey());
                jsonObject.put("issueAssignee", String.valueOf(issue.getAssignee()));
                jsonObject.put("issuePriority", String.valueOf(issue.getPriority()));
                jsonObject.put("issueSummary", issue.getSummary());
                jsonObject.put("issueDescription", issue.getDescription());
                jsonObject.put("issueSubtasks", issue.getSubtasks());
                jsonObject.put("issueLinks", issue.getIssueLinks());

                // Avoid extraneous information
                JsonConfig commentConfig = new JsonConfig();
                commentConfig.setExcludes(new String[] {"self", "updateAuthor", "createdDate", "updatedDate"});
                JSONArray comments = JSONArray.fromObject(issue.getComments(), commentConfig);
                if (!comments.isEmpty()) {
                    for (Object obj : comments) {
                        JSONObject jsonComment = (JSONObject) obj;
                        jsonComment.remove("url");
                        jsonComment.remove("id");
                        jsonComment.getJSONObject("author").remove("active");
                        jsonComment.getJSONObject("author").remove("avatarUrls");
                        jsonComment.getJSONObject("author").remove("id");
                        jsonComment.getJSONObject("author").remove("self");
                        jsonComment.getJSONObject("author").remove("url");
                    }
                }
                jsonObject.put("issueComments", comments);
            }
            jsonArray.add(jsonObject);
        }
        file.write(jsonArray.toString());
        file.close();
    }

    /**
     * Takes a commit map (commit mapped to file path) and returns only the commits.
     * @param commitMap The commit history.
     * @return A list of commits only.
     */
    public static List<RevCommit> getCommitsOnly(Map<RevCommit, String> commitMap) {
        return new ArrayList<>(commitMap.keySet());
    }
}
