package bettergithistory.util;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.Map;

/**
 * Utility methods for working with the commit map (representing Git history) of a file.
 */
public class CommitHistoryUtil {
    public static void printCommitHistory(Map<RevCommit, String> commitMap) {
        for (Map.Entry<RevCommit, String> entry : commitMap.entrySet()) {
            RevCommit commit = entry.getKey();
            String filePath = entry.getValue();
            System.out.println(commit.getShortMessage());
        }
    }
}
