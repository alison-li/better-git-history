package bettergithistory.extractors;

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

    /**
     * Print the details of each source code change in a change list.
     * @param changes The list of source code changes to print details for.
     */
    public static void printSourceCodeChanges(List<SourceCodeChange> changes) {
        for (SourceCodeChange change : changes) {
            System.out.println("Change Type: " + change.getChangeType());
            System.out.println("Root Entity: " + change.getRootEntity());
            System.out.println("Changed Entity: " + change.getChangedEntity());
            System.out.println("Parent Entity: " + change.getParentEntity());
        }
    }
}
