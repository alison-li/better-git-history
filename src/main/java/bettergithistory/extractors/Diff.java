package bettergithistory.extractors;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * For interfacing with java-diff-utils
 * {@link https://github.com/java-diff-utils/java-diff-utils}
 */
public class Diff {
    /**
     * Get the diff between two files using java-diff-utils.
     * @throws IOException
     */
    public static void getDiff(int leftVer, int rightVer) throws IOException {
        List<String> left = Files.readAllLines(Paths.get(new File(String.format("out/ver%d.java", leftVer)).getPath()));
        List<String> right = Files.readAllLines(Paths.get(new File(String.format("out/ver%d.java", rightVer)).getPath()));
        Patch<String> patch = DiffUtils.diff(left, right);
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            System.out.println(delta);
        }
    }
}
