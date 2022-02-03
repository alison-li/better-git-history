package bettergitlog;

import bettergitlog.jgit.JGit;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Driver {
    public static void main(String[] args) throws IOException {
        JGit jgit = new JGit("../kafka");
        String fileName = "streams/src/main/java/org/apache/kafka/streams/KafkaStreams.java";
        Map<RevCommit, String> commitMap = jgit.getFileLog(fileName);
//        for (Map.Entry entry : commitMap.entrySet()) {
//            System.out.println(entry.getKey() + " : " + entry.getValue());
//        }
//        System.out.println(commitMap.size());
        File f = jgit.getFileFromCommit(commitMap.keySet().stream().findFirst().get(), fileName, "./out/firstCommit.java");
    }
}
