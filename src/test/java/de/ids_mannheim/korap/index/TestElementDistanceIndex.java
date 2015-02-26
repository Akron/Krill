package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.ids_mannheim.korap.KorapIndex;
import de.ids_mannheim.korap.KorapMatch;
import de.ids_mannheim.korap.KrillQuery;
import de.ids_mannheim.korap.KorapResult;
import de.ids_mannheim.korap.query.QueryBuilder;
import de.ids_mannheim.korap.query.DistanceConstraint;
import de.ids_mannheim.korap.query.SpanDistanceQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanNextQuery;
import de.ids_mannheim.korap.query.wrap.SpanQueryWrapper;
import de.ids_mannheim.korap.util.QueryException;

@RunWith(JUnit4.class)
public class TestElementDistanceIndex {
	
	KorapResult kr;
    KorapIndex ki;   
    
	private FieldDocument createFieldDoc0() {
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV("base",
            "text",             
            "[(0-1)s:b|s:c|_1#0-1|<>:s#0-1$<i>1]" +
            "[(1-2)s:b|_2#1-2]" +             
            "[(2-3)s:c|_3#2-3|<>:s#2-3$<i>3]" +
            "[(3-4)s:b|_4#3-4|<>:s#3-4$<i>4]" + 
            "[(4-5)s:b|_5#4-5|<>:s#4-5$<i>5]" +             
            "[(5-6)s:b|_6#5-6]" +
            "[(6-7)s:c|_7#6-7]");
        return fd;
	}
		
	private FieldDocument createFieldDoc1() {
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV("base",
            "text",             
            "[(0-1)s:e|_1#0-1|<>:s#0-2$<i>1]" +
            "[(1-2)s:c|s:b|_2#1-2|<>:s#1-2$<i>2]" +             
            "[(2-3)s:e|_3#2-3|<>:s#2-3$<i>3]" +
            "[(3-4)s:b|_4#3-4|<>:s#3-4$<i>4]" + 
            "[(4-5)s:d|_5#4-5|<>:s#4-5$<i>5]" +             
            "[(5-6)s:c|_6#5-6|<>:s#5-6$<i>6]");
        return fd;
	}
	
	private FieldDocument createFieldDoc2() {
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV("base",
            "text",             
            "[(0-1)s:b|_1#0-1|<>:p#0-2$<i>1]" +
            "[(1-2)s:b|_2#1-2]" +             
            "[(2-3)s:b|_3#2-3|<>:p#2-3$<i>3]" +
            "[(3-4)s:d|_4#3-4|<>:p#3-4$<i>4]" + 
            "[(4-5)s:d|_5#4-5|<>:p#4-5$<i>5]" +             
            "[(5-6)s:d|_6#5-6]");
        return fd;
	}
	
	private FieldDocument createFieldDoc3() {
    	FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-3");
        fd.addTV("base",
            "text",             
            "[(0-1)s:b|_1#0-1|<>:s#0-2$<i>1]" +
            "[(1-2)s:d|_2#1-2]" +             
            "[(2-3)s:b|_3#2-3|<>:s#2-3$<i>3]" +
            "[(3-4)s:c|_4#3-4|<>:s#3-4$<i>4]" + 
            "[(4-5)s:d|_5#4-5|<>:s#4-5$<i>5]" +             
            "[(5-6)s:d|_6#5-6]");
        return fd;
	}
	
	public SpanQuery createQuery(String elementType, String x, String y, 
			int min, int max, boolean isOrdered){
		
		SpanElementQuery e = new SpanElementQuery("base", elementType);
		return new SpanDistanceQuery(        		 
        		new SpanTermQuery(new Term("base",x)), 
        		new SpanTermQuery(new Term("base",y)), 
        		new DistanceConstraint( e, min, max, isOrdered, false),
        		true);
	}
	
	
	/**	Multiple documents
	 * 	Ensure terms and elements are in the same doc
	 * 	Ensure terms are in elements
	 *  Check filter candidate list
	 * */
	@Test
	public void testCase1() throws IOException{
		//System.out.println("testCase1");
		ki = new KorapIndex();
        ki.addDoc(createFieldDoc0()); 
        ki.addDoc(createFieldDoc1());        
        ki.commit();
        
        SpanQuery sq;        
        sq = createQuery("s", "s:b", "s:c", 0, 2,true);        
        kr = ki.search(sq, (short) 10);
        
        assertEquals(kr.getTotalResults(), 4);
        assertEquals(0, kr.getMatch(0).startPos);
        assertEquals(1, kr.getMatch(0).endPos);
        assertEquals(0, kr.getMatch(1).startPos);
        assertEquals(3, kr.getMatch(1).endPos);         
	}
	
	/** Ensure terms and elements are in the same doc
	 * */
	@Test
	public void testCase2() throws IOException{
		//System.out.println("testCase2");
		ki = new KorapIndex();
        ki.addDoc(createFieldDoc0()); 
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc2());
        ki.commit();
        
        SpanQuery sq;
        sq = createQuery("p", "s:b", "s:d", 1, 1,true);
        kr = ki.search(sq, (short) 10);
        
        assertEquals(kr.getTotalResults(), 1);
        assertEquals(2, kr.getMatch(0).getLocalDocID());
        assertEquals(2, kr.getMatch(0).startPos);
        assertEquals(4, kr.getMatch(0).endPos);
        
	}	
	
	/** Skip to */
	@Test
	public void testCase3() throws IOException{
		//System.out.println("testCase3");
		ki = new KorapIndex();
		ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.addDoc(createFieldDoc3());
        ki.commit();
        
        SpanQuery sq, edq;
        edq = createQuery("s", "s:b", "s:c", 1, 1,true);
		
        sq = new SpanNextQuery(edq, 
        		new SpanTermQuery(new Term("base", "s:d")));
        
        kr = ki.search(sq, (short) 10);
        
        assertEquals(kr.getTotalResults(), 1);
        assertEquals(2, kr.getMatch(0).getLocalDocID());
        assertEquals(2, kr.getMatch(0).startPos);
        assertEquals(5, kr.getMatch(0).endPos);
        
	}
	
	/** Same tokens in different elements */
	@Test
	public void testCase4() throws IOException{
		//System.out.println("testCase4");
		ki = new KorapIndex();
		ki.addDoc(createFieldDoc0());       
        ki.commit();
        
        SpanQuery sq;
        sq = createQuery("s", "s:b", "s:b", 1, 2,true);
        kr = ki.search(sq, (short) 10);
    		
        assertEquals(kr.getTotalResults(), 2);
        assertEquals(0, kr.getMatch(0).startPos);
        assertEquals(4, kr.getMatch(0).endPos);
        assertEquals(3, kr.getMatch(1).startPos);
        assertEquals(5, kr.getMatch(1).endPos);
        
    }
	
	/** Test query from json */
	@Test
	public void testCase5() throws Exception{
		//System.out.println("testCase4");
		ki = new KorapIndex();
		ki.addDocFile(getClass().getResource("/wiki/00001.json.gz").getFile(),true);       
        ki.commit();
        
        InputStream is = getClass().getResourceAsStream("/queries/cosmas1.json");        
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String json;
        while((json = bufferedReader.readLine()) != null){
            sb.append(json);
        }                
        json = sb.toString();
        
        SpanQueryWrapper sqwi;
        try {    	    
    	    sqwi = new KrillQuery("tokens").fromJson(json);
    	}
    	catch (QueryException e) {
    	    fail(e.getMessage());
    	    sqwi = new QueryBuilder("tokens").seg("???");
    	};        
    	
        
        SpanQuery sq;
        sq = sqwi.toQuery();
        kr = ki.search(sq, (short) 10);
    	
        assertEquals((long) 3, kr.getTotalResults());
        assertEquals(14, kr.getMatch(0).startPos);
        assertEquals(19, kr.getMatch(0).endPos);
        assertEquals(30, kr.getMatch(1).startPos);
        assertEquals(33, kr.getMatch(1).endPos);
        
      /*  for (KorapMatch km : kr.getMatches()){		
        	System.out.println(km.getStartPos() +","+km.getEndPos()+" "
        			+km.getSnippetBrackets());
        }*/
    }
	
		
}
