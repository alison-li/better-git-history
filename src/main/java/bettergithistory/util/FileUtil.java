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
    public static boolean isTempDirectoryEmpty() throws IOException {
        Path tempDirPath = Paths.get(new File("temp").getAbsolutePath());
        if (Files.isDirectory(tempDirPath)) {
            try (Stream<Path> entries = Files.list(tempDirPath)) {
                return !entries.findFirst().isPresent();
            }
        }
        return false;
    }

    public static void cleanTempDirectory() throws IOException {
        FileUtils.cleanDirectory(new File("temp"));
    }
}
