package bettergithistory.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Utility methods for handling files and directories in this repository.
 */
public class FileUtil {
    public static boolean isOutDirectoryEmpty() throws IOException {
        Path outDirPath = Paths.get(new File("out").getAbsolutePath());
        if (Files.isDirectory(outDirPath)) {
            try (Stream<Path> entries = Files.list(outDirPath)) {
                return !entries.findFirst().isPresent();
            }
        }
        return false;
    }

    public static void cleanOutDirectory() throws IOException {
        FileUtils.cleanDirectory(new File("out"));
    }
}
