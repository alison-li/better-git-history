package bettergithistory;

import bettergithistory.distiller.Distiller;
import bettergithistory.jgit.JGit;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Driver {
    public static void main(String[] args) throws IOException {
        JGit jgit = new JGit("../kafka");
        String fileName = "streams/src/main/java/org/apache/kafka/streams/KafkaStreams.java";
        Map<RevCommit, String> commitMap = jgit.getFileCommitHistory(fileName);

        // GENERATE PROGRAM FILES
//        FileUtils.cleanDirectory(new File("out"));
//        jgit.generateFilesFromFileCommitHistory(commitMap);

        // Pairwise extract changes from each file.
        List<List<SourceCodeChange>> allSourceCodeChanges = new ArrayList<>();
        for (int i = 0; i < commitMap.size() - 1; i++) {
            File left = new File(String.format("out/ver%d.java", i));
            File right = new File(String.format("out/ver%d.java", i + 1));
            allSourceCodeChanges.add(
                    Distiller.extractSourceCodeChanges(left, right)
            );
        }

        // TODO: Want to make some observations about ChangeDistiller and what it does. Noticed the changelist returned was empty in one case. Compare the commitMap after using ChangeDistiller to filter.
    }
}
