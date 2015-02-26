package de.ids_mannheim.korap.index;

import java.util.*;
import java.io.*;

import org.apache.lucene.util.Version;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Bits;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.index.FieldDocument;
import de.ids_mannheim.korap.model.MultiTermTokenStream;

import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanSegmentQuery;
import de.ids_mannheim.korap.query.SpanWithinQuery;

import de.ids_mannheim.korap.query.wrap.SpanSequenceQueryWrapper;

import org.apache.lucene.index.Term;

@RunWith(JUnit4.class)
public class TestNextIndex {

    // Todo: primary data as a non-indexed field separated.

    @Test
    public void indexExample1 () throws IOException {
        KorapIndex ki = new KorapIndex();

        // abcabcabac
        FieldDocument fd = new FieldDocument();
        fd.addTV("base",
                 "abcabcabac",
                 "[(0-1)s:a|i:a|_0#0-1|-:t$<i>10]" +
                 "[(1-2)s:b|i:b|_1#1-2]" +
                 "[(2-3)s:c|i:c|_2#2-3]" +
                 "[(3-4)s:a|i:a|_3#3-4]" +
                 "[(4-5)s:b|i:b|_4#4-5]" +
                 "[(5-6)s:c|i:c|_5#5-6]" +
                 "[(6-7)s:a|i:a|_6#6-7]" +
                 "[(7-8)s:b|i:b|_7#7-8]" +
                 "[(8-9)s:a|i:a|_8#8-9]" +
                 "[(9-10)s:c|i:c|_9#9-10]");
        ki.addDoc(fd);
        ki.commit();

        SpanQuery sq;
        KorapResult kr;

        sq = new SpanNextQuery(
            new SpanTermQuery(new Term("base", "s:a")),
            new SpanTermQuery(new Term("base", "s:b"))
        );

	kr = ki.search(sq, (short) 10);
	
	assertEquals("totalResults", kr.getTotalResults(), 3);
	assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
	assertEquals("EndPos (0)", 2, kr.getMatch(0).endPos);
	assertEquals("StartPos (1)", 3, kr.getMatch(1).startPos);
	assertEquals("EndPos (1)", 5, kr.getMatch(1).endPos);
	assertEquals("StartPos (2)", 6, kr.getMatch(2).startPos);
	assertEquals("EndPos (2)", 8, kr.getMatch(2).endPos);
	
	sq = new SpanNextQuery(
            new SpanTermQuery(new Term("base", "s:b")),
            new SpanTermQuery(new Term("base", "s:c"))
        );

	kr = ki.search(sq, (short) 10);
	
	assertEquals("totalResults", kr.getTotalResults(), 2);
	assertEquals("StartPos (0)", 1, kr.getMatch(0).startPos);
	assertEquals("EndPos (0)", 3, kr.getMatch(0).endPos);
	assertEquals("StartPos (1)", 4, kr.getMatch(1).startPos);
	assertEquals("EndPos (1)", 6, kr.getMatch(1).endPos);

	assertEquals(1, ki.numberOf("base", "documents"));
	assertEquals(10, ki.numberOf("base", "t"));


	sq = new SpanNextQuery(
            new SpanTermQuery(new Term("base", "s:a")),
	    new SpanNextQuery(
	      new SpanTermQuery(new Term("base", "s:b")),
	      new SpanTermQuery(new Term("base", "s:c"))
	    )
        );

	kr = ki.search(sq, (short) 2);
	
	assertEquals("totalResults", kr.getTotalResults(), 2);
	assertEquals("StartPos (0)", 0, kr.getMatch(0).startPos);
	assertEquals("EndPos (0)", 3, kr.getMatch(0).endPos);
	assertEquals("StartPos (1)", 3, kr.getMatch(1).startPos);
	assertEquals("EndPos (1)", 6, kr.getMatch(1).endPos);

	assertEquals(1, ki.numberOf("base", "documents"));
	assertEquals(10, ki.numberOf("base", "t"));

    };

    @Test
    public void indexExample2 () throws IOException {
	KorapIndex ki = new KorapIndex();

	// abcabcabac
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "abcabcabac",
		 "[(0-1)s:a|i:a|_0#0-1|-:t$<i>10]" +
		 "[(1-2)s:b|i:b|_1#1-2]" +
		 "[(2-3)s:c|i:c|_2#2-3]" +
		 "[(3-4)s:a|i:a|_3#3-4|<>:x#3-4$<i>4|<>:x#3-7$<i>7]" +
		 "[(4-5)s:b|i:b|_4#4-5]" +
		 "[(5-6)s:c|i:c|_5#5-6]" +
		 "[(6-7)s:a|i:a|_6#6-7]" +
		 "[(7-8)s:b|i:b|_7#7-8]" +
		 "[(8-9)s:a|i:a|_8#8-9]" +
		 "[(9-10)s:c|i:c|_9#9-10]");
	ki.addDoc(fd);

	ki.commit();

	SpanQuery sq;
	KorapResult kr;

	sq = new SpanNextQuery(
	       new SpanTermQuery(new Term("base", "s:c")),
 	       new SpanElementQuery("base", "x")
	);
	
	kr = ki.search(sq, (short) 10);
	assertEquals("ab[cabca]bac", kr.getMatch(1).getSnippetBrackets());

    };

    @Test
    public void indexExample3 () throws IOException {
	KorapIndex ki = new KorapIndex();

	// abcabcabac
	FieldDocument fd = new FieldDocument();
	fd.addTV("base",
		 "abcabcabac",
		 "[(0-1)s:a|i:a|_0#0-1|-:t$<i>10]" +
		 "[(1-2)s:b|i:b|_1#1-2]" +
		 "[(2-3)s:c|i:c|_2#2-3]" +
		 "[(3-4)s:a|i:a|_3#3-4|<>:x#3-7$<i>7]" +
		 "[(4-5)s:b|i:b|_4#4-5]" +
		 "[(5-6)s:c|i:c|_5#5-6]" +
		 "[(6-7)s:a|i:a|_6#6-7]" +
		 "[(7-8)s:b|i:b|_7#7-8]" +
		 "[(8-9)s:a|i:a|_8#8-9]" +
		 "[(9-10)s:c|i:c|_9#9-10]");
	ki.addDoc(fd);

	ki.commit();

	SpanQuery sq;
	KorapResult kr;

	sq = new SpanNextQuery(
	       new SpanElementQuery("base", "x"),
	       new SpanTermQuery(new Term("base", "s:b"))
	);
	
	kr = ki.search(sq, (short) 10);
	assertEquals("abc[abcab]ac", kr.getMatch(0).getSnippetBrackets());
    };

    @Test
    public void indexExample4 () throws IOException {
        KorapIndex ki = new KorapIndex();

        // abcabcabac
        // abc<x>abc<x>a</x>b</x>ac
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base",
                 "abcabcabac",
                 "[(0-1)s:a|i:a|_0#0-1|-:t$<i>10]" +
                 "[(1-2)s:b|i:b|_1#1-2]" +
                 "[(2-3)s:c|i:c|_2#2-3]" +
                 "[(3-4)s:a|i:a|_3#3-4|<>:x#3-7$<i>7]" +
                 "[(4-5)s:b|i:b|_4#4-5]" +
                 "[(5-6)s:c|i:c|_5#5-6]" +
                 "[(6-7)s:a|i:a|_6#6-7]<>:x#6-8$<i>8]" +
                 "[(7-8)s:b|i:b|_7#7-8]" +
                 "[(8-9)s:a|i:a|_8#8-9]" +
                 "[(9-10)s:c|i:c|_9#9-10]");
        ki.addDoc(fd);
        
        // xbz<x>xbzx</x>bxz
        fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base",
                 "xbzxbzxbxz",
                 "[(0-1)s:x|i:x|_0#0-1|-:t$<i>10]" +
                 "[(1-2)s:b|i:b|_1#1-2]" +
                 "[(2-3)s:z|i:z|_2#2-3]" +
                 "[(3-4)s:x|i:x|_3#3-4|<>:x#3-7$<i>7]" +
                 "[(4-5)s:b|i:b|_4#4-5]" +
                 "[(5-6)s:z|i:z|_5#5-6]" +
                 "[(6-7)s:x|i:x|_6#6-7]" +
                 "[(7-8)s:b|i:b|_7#7-8]" +
                 "[(8-9)s:x|i:x|_8#8-9]" +
                 "[(9-10)s:z|i:z|_9#9-10]");
        ki.addDoc(fd);
        ki.commit();

        SpanQuery sq;
        KorapResult kr;

        sq = new SpanNextQuery(
            new SpanElementQuery("base", "x"),
            new SpanTermQuery(new Term("base", "s:b"))
        );
	
        kr = ki.search(sq, (short) 10);
        assertEquals("TotalResults", kr.getTotalResults(), 2);
        assertEquals("abc[abcab]ac", kr.getMatch(0).getSnippetBrackets());
        assertEquals("xbz[xbzxb]xz", kr.getMatch(1).getSnippetBrackets());

        sq = new SpanNextQuery(
            new SpanTermQuery(new Term("base", "s:c")),
            new SpanElementQuery("base", "x")
        );
        
        kr = ki.search(sq, (short) 10);
        assertEquals(kr.getTotalResults(), 1);
        assertEquals("ab[cabca]bac", kr.getMatch(0).getSnippetBrackets());
        
        sq = new SpanNextQuery(
            new SpanTermQuery(new Term("base", "s:z")),
            new SpanElementQuery("base", "x")
        );
        
        kr = ki.search(sq, (short) 10);
        assertEquals(1, kr.getTotalResults());
        assertEquals("xb[zxbzx]bxz", kr.getMatch(0).getSnippetBrackets());
    };
    
    /**
     * Multiple atomic indices
     * Skip to a greater doc#
     * */
	@Test
	public void indexExample5 () throws IOException {
		KorapIndex ki = new KorapIndex();
		ki.addDoc(createFieldDoc1());		
		ki.addDoc(createFieldDoc2());
		ki.commit();
		ki.addDoc(createFieldDoc3());
		ki.commit();
		
		SpanQuery sq = new SpanNextQuery(
					new SpanTermQuery(new Term("base","s:d")),
					new SpanTermQuery(new Term("base","s:b"))
				);
		KorapResult kr = ki.search(sq, (short) 10);			
				
		assertEquals("totalResults", kr.getTotalResults(), 2);
		// Match #0
		assertEquals("doc-number", 0, kr.getMatch(0).getLocalDocID());
		assertEquals("StartPos", 4, kr.getMatch(0).startPos);
		assertEquals("EndPos", 6, kr.getMatch(0).endPos);
		// Match #1
		assertEquals("doc-number", 0, kr.getMatch(1).getLocalDocID());
		assertEquals("StartPos", 1, kr.getMatch(1).startPos);
		assertEquals("EndPos", 3, kr.getMatch(1).endPos);
		
		sq = new SpanNextQuery(
				new SpanTermQuery(new Term("base","s:b")),
				new SpanTermQuery(new Term("base","s:d"))
			);
		kr = ki.search(sq, (short) 10);
		
		assertEquals("totalResults", kr.getTotalResults(), 1);
		assertEquals("doc-number", 0, kr.getMatch(0).getLocalDocID());
		assertEquals("StartPos", 2, kr.getMatch(0).startPos);
		assertEquals("EndPos", 4, kr.getMatch(0).endPos);
	}
	
	/** Skip to NextSpan */
	@Test
	public void indexExample6() throws IOException{
		KorapIndex ki = new KorapIndex();
		ki.addDoc(createFieldDoc1());		
		ki.addDoc(createFieldDoc2());		
		ki.addDoc(createFieldDoc3());
		ki.commit();
		
		SpanQuery sq = new SpanNextQuery(
				new SpanTermQuery(new Term("base","s:c")), 
				new SpanNextQuery(
						new SpanTermQuery(new Term("base","s:d")),
						new SpanTermQuery(new Term("base","s:b"))
				)
			);
		
		KorapResult kr = ki.search(sq, (short) 10);			
		assertEquals("totalResults", kr.getTotalResults(), 1);
		assertEquals("doc-number", 2, kr.getMatch(0).getLocalDocID());
		assertEquals("StartPos", 0, kr.getMatch(0).startPos);
		assertEquals("EndPos", 3, kr.getMatch(0).endPos);	
		
	}

	@Test
	public void indexExample7Distances () throws Exception{
		KorapIndex ki = new KorapIndex();
		ki.addDoc(createFieldDoc1());		
		ki.addDoc(createFieldDoc2());		
		ki.addDoc(createFieldDoc3());
		ki.addDoc(createFieldDoc4());
		ki.commit();
		
		SpanSequenceQueryWrapper sq = new SpanSequenceQueryWrapper("base");
		sq.append("i:b").append("i:d").withConstraint(1,3);
		
		KorapResult kr = ki.search(sq.toQuery(), (short) 10);			

		assertEquals("totalResults", kr.getTotalResults(), 3);
		assertEquals("doc-number", "match-doc-0-p2-5", kr.getMatch(0).getID());
		assertEquals("doc-number", "match-doc-2-p2-4", kr.getMatch(1).getID());
		assertEquals("doc-number", "match-doc-3-p2-5", kr.getMatch(2).getID());
	};

	@Test
	public void indexExample8Distances () throws Exception{
		KorapIndex ki = new KorapIndex();
		ki.addDoc(createFieldDoc1());		
		ki.addDoc(createFieldDoc2());		
		ki.addDoc(createFieldDoc3());
		ki.addDoc(createFieldDoc4());
		ki.commit();
		
		SpanSequenceQueryWrapper sq = new SpanSequenceQueryWrapper("base");
		sq.append("i:a").append("i:b").withConstraint(0, 3, "e");
		
		KorapResult kr = ki.search(sq.toQuery(), (short) 10);			

		assertEquals("totalResults", kr.getTotalResults(), 3);
		assertEquals("doc-number", "match-doc-0-p3-6", kr.getMatch(0).getID());
		assertEquals("doc-number", "match-doc-1-p1-3", kr.getMatch(1).getID());
		assertEquals("doc-number", "match-doc-3-p3-6", kr.getMatch(2).getID());
	};

	@Test
	public void indexExample9() throws IOException{
		KorapIndex ki = new KorapIndex();
		ki.addDoc(createFieldDoc1());	
		ki.commit();
		
		SpanQuery sq = new SpanNextQuery(
				new SpanOrQuery(
					new SpanTermQuery(new Term("base","s:a")),
					new SpanTermQuery(new Term("base","s:b"))),
				new SpanTermQuery(new Term("base","s:c"))
			);
		
		KorapResult kr = ki.search(sq, (short) 10);
		
		assertEquals(0, kr.getMatch(0).getStartPos());
		assertEquals(2, kr.getMatch(0).getEndPos());
		assertEquals(3, kr.getMatch(1).getStartPos());
		assertEquals(5, kr.getMatch(1).getEndPos());
	}
	
	
	private FieldDocument createFieldDoc1(){
		FieldDocument fd = new FieldDocument();
		fd.addString("ID", "doc-0");
		fd.addTV("base",
			 "bcbadb",			 
			 "[(0-1)s:b|i:b|_0#0-1]" +
			 "[(1-2)s:c|i:c|s:b|_1#1-2]" +			 
			 "[(2-3)s:b|i:b|_2#2-3]" +
			 "[(3-4)s:a|i:a|_3#3-4|<>:e#3-6$<i>6]" + 
			 "[(4-5)s:d|i:d|s:c|_4#4-5]" +			 
			 "[(5-6)s:b|i:b|_5#5-6]");
		return fd;
	}
	
	private FieldDocument createFieldDoc2(){
		FieldDocument fd = new FieldDocument();
		fd.addString("ID", "doc-1");
		fd.addTV("base",
			 "caba",			 
			 "[(0-1)s:c|i:c|_0#0-1]" +
			 "[(1-2)s:a|i:a|s:c|_1#1-2|<>:e#1-3$<i>3]" +			 
			 "[(2-3)s:b|i:b|s:a|_2#2-3]" +
			 "[(3-4)s:a|i:a|_3#3-4]");
		return fd;
	} 
	
	private FieldDocument createFieldDoc3(){
		FieldDocument fd = new FieldDocument();
		fd.addString("ID", "doc-2");
		fd.addTV("base",
			 "cdbd",			 
			 "[(0-1)s:c|i:c|_0#0-1]" +
			 "[(1-2)s:d|i:d|_1#1-2]"+
			 "[(2-3)s:b|i:b|s:a|_2#2-3]"+
			 "[(3-4)s:d|i:d|_3#3-4]");
			 	
		return fd;
	}

	private FieldDocument createFieldDoc4(){
		FieldDocument fd = new FieldDocument();
		fd.addString("ID", "doc-3");
		fd.addTV("base",
			 "bcbadb",			 
			 "[(0-1)s:b|i:b|_0#0-1]" +
			 "[(1-2)s:c|i:c|s:b|<>:s#1-3$<i>3|_1#1-2]" +			 
			 "[(2-3)s:b|i:b|_2#2-3]" +
			 "[(3-4)s:a|i:a|_3#3-4|<>:e#3-6$<i>6]" + 
			 "[(4-5)s:d|i:d|s:c|_4#4-5]" +			 
			 "[(5-6)s:b|i:b|_5#5-6]");
		return fd;
	}
	
	
};
