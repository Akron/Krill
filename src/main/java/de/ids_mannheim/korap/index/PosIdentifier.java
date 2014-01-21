package de.ids_mannheim.korap.index;
import java.util.*;
import de.ids_mannheim.korap.index.DocIdentifier;

public class PosIdentifier extends DocIdentifier {
    private int pos;

    public PosIdentifier () {};

    public void setPos (int pos) {
	if (pos >= 0)
	    this.pos = pos;
    };

    public int getPos () {
	return this.pos;
    };

    public String toString () {

	if (this.docID == null) return null;

	StringBuffer sb = new StringBuffer("word-");

	// Get prefix string corpus/doc
	if (this.corpusID != null) {
	    sb.append(this.corpusID).append('!');
	};
	sb.append(this.docID);

	sb.append("-p");
	sb.append(this.pos);

	return sb.toString();
    };
};