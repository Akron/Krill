package de.ids_mannheim.korap.query.spans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add a payload to the span with an identification number (a class)
 * And the start and end position of the span, so this information
 * can bubble up for later processing (similar to captures in regular
 * expression).
 *
 * @author diewald
 */

public class ClassSpans extends Spans {
    private List<byte[]> classedPayload;
    private final Spans spans;
    private byte number;
    private SpanQuery operand;
    private Boolean hasmorespans = false;

    private ByteBuffer bb = ByteBuffer.allocate(9);

    private final static Logger log = LoggerFactory.getLogger(ClassSpans.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    /**
     * Construct a new ClassSpans object.
     *
     * @param operand An arbitrary nested {@link SpanQuery}.
     * @param context The {@link AtomicReaderContext}.
     * @param acceptDocs Bit vector representing the documents
     *        to be searched in.
     * @param termContexts A map managing {@link TermState TermStates}.
     * @param number The identifying class number.
     */
    public ClassSpans (SpanQuery operand,
                       AtomicReaderContext context,
                       Bits acceptDocs,
                       Map<Term,TermContext> termContexts,
                       byte number) throws IOException {
        spans = operand.getSpans(context, acceptDocs, termContexts);

        // The number of the class
        this.number  = number;

        // The current operand
        this.operand = operand;

        // The highlighted payload
        this.classedPayload = new ArrayList<byte[]>(3);
    };


    @Override
    public Collection<byte[]> getPayload () throws IOException {
        return classedPayload;
    };


    @Override
    public boolean isPayloadAvailable () {
        // We set payloads here - so it's always true
        return true;
    };


    @Override
    public int doc () {
        return spans.doc();
    };


    @Override
    public int start () {
        return spans.start();
    };


    @Override
    public int end () {
        return spans.end();
    };


    @Override
    public boolean next () throws IOException {
        if (DEBUG) log.trace("Forward next");

        if (spans.next())
            return this.addClassPayload();

        hasmorespans = false;
        return false;
    };
    
    private boolean addClassPayload () throws IOException {
    	hasmorespans = true;

	    classedPayload.clear();

        // Subquery has payloads
	    if (spans.isPayloadAvailable()) {
            classedPayload.addAll(spans.getPayload());
            if (DEBUG) log.trace("Found payload in nested SpanQuery");
	    };

	    if (DEBUG) {
            log.trace(
                "Wrap class {} around span {} - {}",
                number,
                spans.start(),
                spans.end()
            );
        };

	    // Todo: Better allocate using a Factory!
	    bb.clear();
	    bb.putInt(spans.start()).putInt(spans.end()).put(number);

	    // Add highlight information as byte array
	    classedPayload.add(bb.array());
        return true;
	};


    @Override
    public boolean skipTo (int target) throws IOException {
        classedPayload.clear();

        if (DEBUG) log.trace("Skip ClassSpans {} -> {}",
                             spans.doc(), target);

        if (hasmorespans && spans.doc() < target &&
            spans.skipTo(target))
            return this.addClassPayload();
        return false;
    };


    @Override
    public String toString () {
        return getClass().getName() + "(" + this.operand.toString() + ")@" +
            (doc() + ":" + start() + "-" + end());
    };


    @Override
    public long cost() {
        return spans.cost();
    };
};
