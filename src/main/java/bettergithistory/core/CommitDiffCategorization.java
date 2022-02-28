package bettergithistory.core;

import org.eclipse.jgit.revwalk.RevCommit;

public class CommitDiffCategorization {
    private final RevCommit commit;
    private int numDoc;
    private int numAnnotation;
    private int numImport;
    private int numNewLine;
    private int numOther;
    private int numFilter;

    public CommitDiffCategorization(RevCommit commit) {
        this.commit = commit;
        this.numDoc = 0;
        this.numAnnotation = 0;
        this.numImport = 0;
        this.numNewLine = 0;
        this.numOther = 0;
        this.numFilter = 0;
    }

    public void setNumDoc(int numDoc) {
        this.numDoc = numDoc;
    }

    public void setNumAnnotation(int numAnnotation) {
        this.numAnnotation = numAnnotation;
    }

    public void setNumImport(int numImport) {
        this.numImport = numImport;
    }

    public void setNumNewLine(int numNewLine) {
        this.numNewLine = numNewLine;
    }

    public void setNumOther(int numOther) {
        this.numOther = numOther;
    }

    public void setNumFilter(int numFilter) {
        this.numFilter = numFilter;
    }

    public RevCommit getCommit() {
        return this.commit;
    }

    public int getNumDoc() {
        return this.numDoc;
    }

    public int getNumAnnotation() {
        return this.numAnnotation;
    }

    public int getNumImport() {
        return this.numImport;
    }

    public int getNumNewLine() {
        return this.numNewLine;
    }

    public int getNumOther() {
        return this.numOther;
    }

    public int getNumFilter() {
        return this.numFilter;
    }

    /**
     * Given another commit diff categorization object, the data in that object will be merged with the data in this
     * object.
     * @param diffCategorization The categorization object to merge with.
     */
    public void mergeCommitDiffCategorization(CommitDiffCategorization diffCategorization) {
        this.numDoc += diffCategorization.getNumDoc();
        this.numAnnotation += diffCategorization.getNumAnnotation();
        this.numImport += diffCategorization.getNumImport();
        this.numNewLine += diffCategorization.getNumNewLine();
        this.numOther += diffCategorization.getNumOther();
    }
}
