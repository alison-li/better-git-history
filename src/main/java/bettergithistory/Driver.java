package bettergithistory;

import bettergithistory.jgit.JGit;
import bettergithistory.util.CommitHistoryUtil;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Driver {
    public static void main(String[] args) throws IOException {
        // Clean the out directory used for handling generated files
        //FileUtil.cleanOutDirectory();

        // Initialize JGit object for working for repo
        JGit jgit = new JGit("../kafka");
        String fileName = "streams/src/main/java/org/apache/kafka/streams/KafkaStreams.java";

        // Get file's commit history
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(fileName);
        System.out.println("ORIGINAL HISTORY");
        CommitHistoryUtil.printCommitHistory(commitMap);

        System.out.println("\n");

        // Initialize BetterGitHistory object for getting file's filtered commit history
        BetterGitHistory betterGitHistory = new BetterGitHistory(jgit, commitMap);
        Map<RevCommit, String> filteredCommitMap = betterGitHistory.getFilteredCommitHistory();
        System.out.println("FILTERED HISTORY");
        CommitHistoryUtil.printCommitHistory(filteredCommitMap);

        System.out.println("\n");

        // Drill down to the changes found by ChangeDistiller
        List<List<SourceCodeChange>> allChanges = betterGitHistory.getAllSourceCodeChanges();
        // TODO: Write a method in the Distiller class for printing the changes nicely.
    }
}
