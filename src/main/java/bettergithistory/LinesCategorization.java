package bettergithistory;

import java.util.List;

public class LinesCategorization {
    private final List<String> lineList;
    private boolean containsDoc;
    private boolean containsAnnotation;
    private boolean containsImport;
    private boolean containsNewLine;
    private boolean containsOther;

    public LinesCategorization(List<String> lineList) {
        this.lineList = lineList;
    }

    public void setContainsDoc(boolean containsDoc) {
        this.containsDoc = containsDoc;
    }

    public void setContainsAnnotation(boolean containsAnnotation) {
        this.containsAnnotation = containsAnnotation;
    }

    public void setContainsImport(boolean containsImport) {
        this.containsImport = containsImport;
    }

    public void setContainsNewLine(boolean containsNewLine) {
        this.containsNewLine = containsNewLine;
    }

    public void setContainsOther(boolean containsOther) {
        this.containsOther = containsOther;
    }

    public List<String> getLineList() {
        return this.lineList;
    }

    public boolean getContainsDoc() {
        return this.containsDoc;
    }

    public boolean getContainsAnnotation() {
        return this.containsAnnotation;
    }

    public boolean getContainsImport() {
        return this.containsImport;
    }

    public boolean getContainsNewLine() {
        return this.containsNewLine;
    }

    public boolean getContainsOther() {
        return this.containsOther;
    }
}
