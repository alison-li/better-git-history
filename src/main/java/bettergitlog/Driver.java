package bettergitlog;

import bettergitlog.jgit.JGit;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.List;

public class Driver {
    public static void main(String[] args) throws IOException {
        JGit jgit = new JGit("../kafka");
        String fileName = "streams/src/main/java/org/apache/kafka/streams/KafkaStreams.java";
        List<String> fileNames = jgit.getAllOldFilePaths(fileName);
        for (String s : fileNames) {
            System.out.println(s);
        }
        List<RevCommit> commitList = jgit.getFileLog(fileName);
        System.out.println(commitList.size());
        jgit.getFileFromCommit(commitList.get(commitList.size() - 1), fileName);
    }
}
