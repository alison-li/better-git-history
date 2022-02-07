package bettergithistory;

import bettergithistory.distiller.Distiller;
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
    public Map<RevCommit, String> getFilteredCommitHistory() {
        Map<RevCommit, String> filteredCommitMap = new LinkedHashMap<>();
        List<RevCommit> commits = new ArrayList<>(commitMap.keySet());
        for (int verNum = 0; verNum < this.commitMap.size() - 1; verNum++) {
            File left = new File(String.format("out/ver%d.java", verNum));
            File right = new File(String.format("out/ver%d.java", verNum + 1));
            List<SourceCodeChange> changes = Distiller.extractSourceCodeChanges(left, right);
            if (!changes.isEmpty()) {
                // Keep both commits in the history since the change list is non-empty between the two versions.
                RevCommit firstCommit = commits.get(verNum);
                String firstPath = this.commitMap.get(firstCommit);
                RevCommit secondCommit = commits.get(verNum + 1);
                String secondPath = this.commitMap.get(secondCommit);
                filteredCommitMap.put(firstCommit, firstPath);
                filteredCommitMap.put(secondCommit, secondPath);
            }
        }
        return filteredCommitMap;
    }

    /**
     * Retrieve all of the source code changes performed in a file.
     * @return A chronological list of the source code change operations performed.
     */
    public List<List<SourceCodeChange>> getAllSourceCodeChanges() {
        List<List<SourceCodeChange>> allSourceCodeChanges = new ArrayList<>();
        // This will give us a chronological list of source code changes since the commit map
        // is ordered from most recent commit to oldest.
        // We iterate backwards starting from the oldest file version to see the subsequent changes applied.
        for (int verNum = this.commitMap.size() - 1; verNum > 0; verNum--) {
            File left = new File(String.format("out/ver%d.java", verNum));
            File right = new File(String.format("out/ver%d.java", verNum - 1));
            List<SourceCodeChange> changes = Distiller.extractSourceCodeChanges(left, right);
            if (!changes.isEmpty()) {
                allSourceCodeChanges.add(changes);
            }
        }
        return allSourceCodeChanges;
    }
}
