package bettergithistory;

import bettergithistory.extractors.Distiller;
import bettergithistory.jgit.JGit;
import bettergithistory.util.FileUtil;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        if (FileUtil.isOutDirectoryEmpty()) {
            jgit.generateFilesFromFileCommitHistory(this.commitMap);
        }
    }

    /**
     * Retrieve a filtered version of the commit history using ChangeDistiller.
     * @return The filtered commit history.
     */
    public Map<RevCommit, String> getChangeDistillerCommitHistory() {
        if (this.commitMap.size() <= 2) return commitMap;

        Map<RevCommit, String> filteredCommitMap = new LinkedHashMap<>();
        List<RevCommit> commits = new ArrayList<>(commitMap.keySet());
        for (int verNum = 0; verNum < this.commitMap.size() - 1; verNum++) {
            int leftVer = verNum;
            int rightVer = verNum + 1;
            File left = new File(String.format("out/ver%d.java", leftVer));
            File right = new File(String.format("out/ver%d.java", rightVer));
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
//            File left = new File(String.format("out/ver%d.java", leftVer));
//            File right = new File(String.format("out/ver%d.java", rightVer));
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
        for (int verNum = 0; verNum < this.commitMap.size(); verNum++) {
            File left = new File(String.format("out/ver%d.java", verNum));
            File right = new File(String.format("out/ver%d.java", verNum + 1));
            List<SourceCodeChange> changes = Distiller.extractSourceCodeChanges(left, right);
            if (!changes.isEmpty()) {
                allSourceCodeChanges.add(changes);
            }
        }
        return allSourceCodeChanges;
    }
}
