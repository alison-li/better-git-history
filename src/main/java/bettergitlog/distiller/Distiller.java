package bettergitlog.distiller;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;

import java.io.File;
import java.util.List;

/**
 * For interfacing with ChangeDistiller.
 */
public class Distiller {
    private static final FileDistiller distiller = ChangeDistiller.createFileDistiller(ChangeDistiller.Language.JAVA);

    /**
     * Retrieve the changes performed between two files.
     * @param left The initial version of the file, i.e. "before."
     * @param right The updated version of the file, i.e. "after."
     * @return A list of the source code changes performed.
     */
    public static List<SourceCodeChange> extractSourceCodeChanges(File left, File right) {
        distiller.extractClassifiedSourceCodeChanges(left, right);
        return distiller.getSourceCodeChanges();
    }
}
