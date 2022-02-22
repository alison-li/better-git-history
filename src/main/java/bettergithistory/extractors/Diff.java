package bettergithistory.extractors;

import bettergithistory.util.CommitHistoryUtil;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * For interfacing with java-diff-utils
 * Source: https://github.com/java-diff-utils/java-diff-utils
 */
public class Diff {
    /**
     * Get the diff between two files using java-diff-utils.
     * @throws IOException
     */
    public static List<AbstractDelta<String>> getDiff(int leftVer, int rightVer) throws IOException {
        List<String> left = Files.readAllLines(Paths.get(new File(String.format("temp/ver%d", leftVer)).getPath()));
        List<String> right = Files.readAllLines(Paths.get(new File(String.format("temp/ver%d", rightVer)).getPath()));
        Patch<String> patch = DiffUtils.diff(left, right);
        return patch.getDeltas();
    }

    /**
     * Get the diffs between each commit in a commit history.
     * @param commitMap The commit history to compute diffs for.
     * @return A list of deltas between each commit, e.g. given commits A -> B -> C, this method will return
     *          a list of deltas between blank file -> A, A -> B, and B -> C.
     * @throws IOException
     */
    public static Map<RevCommit, List<AbstractDelta<String>>> getCommitDiffMap(Map<RevCommit, String> commitMap)
            throws IOException {
        Map<RevCommit, List<AbstractDelta<String>>> commitDiffMap = new LinkedHashMap<>();
        List<RevCommit> commits = CommitHistoryUtil.getCommitsOnly(commitMap);
        Collections.reverse(commits);

        // Handling for a file's very first commit:
        // We need a dummy commit (blank file) to compare the first commit diff with
        // so that the first commit qualifies as having a diff.
        commits.add(0, null);

        for (int i = 0; i < commits.size() - 1; i++) {
            int leftVer = i;
            int rightVer = i + 1;
            // The right commit is the one changing the left, so we are more concerned with mapping
            // the right commit to the delta
            RevCommit commit = commits.get(rightVer);
            List<AbstractDelta<String>> delta = Diff.getDiff(leftVer, rightVer);
            commitDiffMap.put(commit, delta);
        }
        return commitDiffMap;
    }

    /**
     * Get the diff in a unified file format and apply it as the patch to a given text.
     */
    public static List<String> applyPatch(int leftVer, int rightVer) throws PatchFailedException, IOException {
        List<String> original = Files.readAllLines(Paths.get(new File(String.format("temp/ver%d", leftVer)).getPath()));
        List<String> patched = Files.readAllLines(Paths.get(new File(String.format("temp/ver%d", rightVer)).getPath()));
        // At first, parse the unified diff file and get the patch
        Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(patched);
        // Then apply the computed patch to the given text
        return DiffUtils.patch(original, patch);
    }
}
