package bettergithistory.util;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for working with the commit map (representing Git history) of a file.
 */
public class CommitHistoryUtil {
    /**
     * Prints commit information in an easy-to-read format.
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
     * Takes a commit map (commit mapped to file path) and returns only the commits.
     * @param commitMap The commit history.
     * @return A list of commits only.
     */
    public static List<RevCommit> getCommitsOnly(Map<RevCommit, String> commitMap) {
        return new ArrayList<>(commitMap.keySet());
    }
}
