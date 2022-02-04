package bettergitlog.distiller;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.entities.ClassHistory;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;

import java.io.File;
import java.util.List;

/**
 * For interfacing with ChangeDistiller.
 */
public class Distiller {
    private static final FileDistiller distiller = ChangeDistiller.createFileDistiller(ChangeDistiller.Language.JAVA);

    public static List<SourceCodeChange> extractSourceCodeChanges(File left, File right) {
        distiller.extractClassifiedSourceCodeChanges(left, right);
        return distiller.getSourceCodeChanges();
    }
}
