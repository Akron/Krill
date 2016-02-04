package de.ids_mannheim.korap.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.junit.Test;

import de.ids_mannheim.korap.KrillIndex;
import de.ids_mannheim.korap.query.SpanAttributeQuery;
import de.ids_mannheim.korap.query.SpanClassQuery;
import de.ids_mannheim.korap.query.SpanElementQuery;
import de.ids_mannheim.korap.query.SpanFocusQuery;
import de.ids_mannheim.korap.query.SpanRelationMatchQuery;
import de.ids_mannheim.korap.query.SpanRelationQuery;
import de.ids_mannheim.korap.query.SpanTermWithIdQuery;
import de.ids_mannheim.korap.query.SpanWithAttributeQuery;
import de.ids_mannheim.korap.response.Result;

/*

within(x,y)

SpanRelationQuery->
rel("SUBJ", query1, query2)

1. return all words that are subjects of (that are linked by the “SUBJ” relation to) the string “beginnt”
xip/syntax-dep_rel:beginnt >[func=”SUBJ”] xip/syntax-dep_rel:.*
-> rel("SUBJ", highlight(query1), new TermQuery("s:beginnt")) 


SUBJ ist modelliert mit offset für den gesamten Bereich

https://de.wikipedia.org/wiki/Dependenzgrammatik

im regiert Wasser
dass die Kinder im Wasser gespielt haben
3. im#16-18$
3. >:COORD#16-25$3,4
4. Wasser#19-25$
4. <:COORD#16-25$3,4

# okay: return all main verbs that have no “SUBJ” relation specified


# Not okay: 5. return all verbs with (at least?) 3 outgoing relations [think of ditransitives such as give]

xip/morph_pos:VERB & xip/token:.* & xip/token:.* & xip/token:.* & xip/token:.* & #1 _=_#2 & #2 >[func=$x] #3 & #2 >[func=$x]#4  &  #2 >[func=$x] #5

# Okay: return all verbs that have singular SUBJects and dative OBJects

xip/morph_pos:VERB & mpt/morph_msd:Sg & mpt/morph_msd:Dat & #1 >[func=”SUBJ”] #2 & #1 >[func=”OBJ”] #3

-> [p:VVFIN](>SUBJ[nr:sg] & >OBJ[c:dat])


 */

public class TestRelationIndex {
    private KrillIndex ki;
    private Result kr;


    public TestRelationIndex () throws IOException {
        ki = new KrillIndex();
    }


    private FieldDocument createFieldDoc0 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-0");
        fd.addTV(
                "base",
                "ceccecdeed",
                "[(0-1)s:c$<s>1|_0$<i>0<i>1"
						+ "|>:xip/syntax-dep_rel$<b>32<i>6<s>1<s>1]"
                        + "[(1-2)s:e$<s>1|_1$<i>1<i>2|"
						+ "<:xip/syntax-dep_rel$<b>32<i>9<s>1<s>1|"
						+ ">:xip/syntax-dep_rel$<b>32<i>4<s>1<s>1]"
                        + "[(2-3)s:c|_2$<i>2<i>3]"
						+ "[(3-4)s:c$<s>1|s:b$<s>2|_3$<i>3<i>4|<:xip/syntax-dep_rel$<b>32<i>9<s>1<s>1]"
						+ "[(4-5)s:e$<s>1|s:d$<s>2|_4$<i>4<i>5|<:xip/syntax-dep_rel$<b>32<i>1<s>1<s>1]"
                        + "[(5-6)s:c|_5$<i>5<i>6]"
						+ "[(6-7)s:d$<s>1|_6$<i>6<i>7|<:xip/syntax-dep_rel$<b>32<i>1<s>1<s>1]"
                        + "[(7-8)s:e|_7$<i>7<i>8]"
                        + "[(8-9)s:e|s:b|_8$<i>8<i>9]"
						+ "[(9-10)s:d$<s>1|_9$<i>9<i>10|"
						+ ">:xip/syntax-dep_rel$<b>32<i>1<s>1<s>1|"
						+ ">:xip/syntax-dep_rel$<b>32<i>3<s>1<s>1]");
        return fd;
    }


    private FieldDocument createFieldDoc1 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-1");
        fd.addTV(
                "base",
                "ceccecdeed",
                "[(0-1)s:c$<s>2|<>:p$<b>64<i>0<i>3<i>3<b>0<s>1|_0$<i>0<i>1|"
						+ ">:xip/syntax-dep_rel$<b>35<i>3<i>6<i>9<s>1<s>1<s>2|"
						+ ">:xip/syntax-dep_rel$<b>33<i>6<i>9<s>2<s>1|"
                        + "@:func=subj$<b>18<s>2]"
                        + "[(1-2)s:e|_1$<i>1<i>2|<>:p$<b>64<i>1<i>3<i>3<b>0<s>1]"
                        + "[(2-3)s:c|_2$<i>2<i>3]"
                        + "[(3-4)s:c|s:b|_3$<i>3<i>4]"
                        + "[(4-5)s:e|s:d|_4$<i>4<i>5]"
                        + "[(5-6)s:c|_5$<i>5<i>6]"
                        + "[(6-7)s:d$<s>2|<>:p$<b>64<i>6<i>9<i>9<b>0<s>1|_6$<i>6<i>7|"						
						+ ">:xip/syntax-dep_rel$<b>34<i>9<i>9<s>1<s>1|"
						+ "<:xip/syntax-dep_rel$<b>35<i>9<i>1<i>3<s>1<s>1<s>2|"
						+ "<:xip/syntax-dep_rel$<b>34<i>9<i>1<s>1<s>2|"
                        + "@:func=obj$<b>18<s>2]"
                        + "[(7-8)s:e|_7$<i>7<i>8]"
                        + "[(8-9)s:e|s:b|_8$<i>8<i>9]"
						+ "[(9-10)s:d$<s>1|_9$<i>9<i>10|<"
						+ ":xip/syntax-dep_rel$<b>33<i>6<i>9<s>2<s>1]");
        return fd;
    }


    public static FieldDocument createFieldDoc2 () {
        FieldDocument fd = new FieldDocument();
        fd.addString("ID", "doc-2");
        fd.addTV(
                "base",
                "Ich kaufe die Blümen für meine Mutter.",
                "[(0-3)s:Ich|_0$<i>0<i>3|pos:NN$<s>1|<>:s$<b>64<i>0<i>38<i>7<b>0<s>2|<>:np$<b>64<i>0<i>3<i>1<b>0<s>3|"
						+ ">:child-of$<b>33<i>0<i>7<s>3<s>2<s>4|"
						+ ">:child-of$<b>33<i>0<i>1<s>1<s>3|"
						+ "<:child-of$<b>32<i>0<s>3<s>1|"
						+ "<:child-of$<b>35<i>7<i>0<i>1<s>2<s>3|"
						+ "<:child-of$<b>35<i>7<i>1<i>7<s>2<s>2|"
						+ "<:dep$<b>32<i>0<s>1<s>1|"
                        + "@:func=sbj$<b>18<i>7<s>4]"
                        +

                        "[(1-2)s:kaufe|_1$<i>4<i>9|pos:V$<s>1|<>:vp$<b>64<i>4<i>38<i>7<b>0<s>2|"
						+ ">:child-of$<b>35<i>7<i>0<i>7<s>2<s>2|"
						+ ">:child-of$<b>33<i>1<i>7<s>2<s>7|"
						+ "<:child-of$<b>34<i>7<i>2<s>2<s>1|"
						+ "<:child-of$<b>35<i>7<i>2<i>7<s>2<s>4|"
						+ ">:dep$<b>32<i>0<s>1<s>1|"
						+ ">:dep$<b>32<i>3<s>1<s>1]"
                        +

						"[(2-3)s:die|_2$<i>10<i>13|pos:ART$<s>1|tt:DET$<s>2|"
						+ "<>:np$<b>64<i>10<i>20<i>4<b>0<s>3|<>:np$<b>64<i>10<i>38<i>7<b>0<s>4|"
						+ ">:child-of$<b>35<i>4<i>2<i>7<s>3<s>4|"
						+ ">:child-of$<b>33<i>2<i>4<s>1<s>3|"
						+ ">:child-of$<b>35<i>7<i>1<i>7<s>4<s>2|"
						+ "<:child-of$<b>34<i>4<i>2<s>3<s>1|"
						+ "<:child-of$<b>34<i>4<i>3<s>3<s>1|"
						+ "<:child-of$<b>35<i>7<i>2<i>4<s>4<s>3|"
						+ "<:child-of$<b>35<i>7<i>4<i>7<s>4<s>2|"
						+ ">:parent-of$<b>35<i>7<i>4<i>7<s>4<s>2|"
						+ "<:dep$<b>32<i>3<s>1<s>1<s>3|"
                        + "@:func=head$<b>18<i>4<s>3]"

                        + "[(3-4)s:Blümen|_3$<i>14<i>20|pos:NN$<s>1|"
						+ ">:child-of$<b>33<i>2<i>4<s>1<s>3|"
						+ "<:dep$<b>32<i>1<s>1<s>1|"
						+ ">:dep$<b>32<i>2<s>1<s>1<s>2|"
						+ ">:dep$<b>32<i>4<s>1<s>1|"
						+ "@:func=obj$<b>18<i>4<s>2]"


                        + "[(4-5)s:für|_4$<i>21<i>24|pos:PREP$<s>1|<>:pp$<b>64<i>21<i>38<i>7<b>0<s>2|"
						+ ">:child-of$<b>33<i>4<i>7<s>1<s>2|"
						+ ">:child-of$<b>35<i>7<i>2<i>7<s>2<s>4|"
						+ "<:child-of$<b>34<i>7<i>5<s>2<s>1|"
						+ "<:child-of$<b>35<i>7<i>5<i>7<s>2<s>2|"
						+ "<:dep$<b>32<i>3<s>1<s>1|"
						+ ">:dep$<b>32<i>6<s>1<s>1]"
                        +

                        "[(5-6)s:meine|_5$<i>25<i>30|pos:ART$<s>1|<>:np$<b>64<i>25<i>38<i>7<b>0<s>2|"
						+ ">:child-of$<b>33<i>5<i>7<s>1<s>2|"
						+ ">:child-of$<b>35<i>7<i>4<i>7<s>2<s>2|"
						+ "<:child-of$<b>34<i>7<i>5<s>2<s>1|"
						+ "<:child-of$<b>34<i>7<i>6<s>2<s>1|"
						+ "<:dep$<b>32<i>6<s>1<s>1<s>3|"
                        + "@:func=head$<b>18<i>7<s>3]"
        
                        + "[(6-7)s:Mutter.|_6$<i>31<i>38|pos:NN$<s>1|"
						+ ">:child-of$<b>33<i>5<i>7<s>1<s>2|"
						+ ">:dep$<b>32<i>5<s>1<s>1|"
						+ "<:dep$<b>32<i>4<s>1<s>1]");
                        

        return fd;
    }


    /**
     * Relations: token to token, token to span, span to span
     * */
    @Test
    public void testCase1 () throws IOException {
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.commit();

        SpanRelationQuery sq = new SpanRelationQuery(new SpanTermQuery(
                new Term("base", ">:xip/syntax-dep_rel")), true);
        kr = ki.search(sq, (short) 10);

        assertEquals((long) 7, kr.getTotalResults());

        // token to token
        assertEquals(0, kr.getMatch(0).getLocalDocID());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(1, kr.getMatch(0).getEndPos());
        assertEquals(1, kr.getMatch(1).getStartPos());
        assertEquals(2, kr.getMatch(1).getEndPos());
        assertEquals(9, kr.getMatch(2).getStartPos());
        assertEquals(10, kr.getMatch(2).getEndPos());
        assertEquals(9, kr.getMatch(3).getStartPos());
        assertEquals(10, kr.getMatch(3).getEndPos());

        // token to span
        assertEquals(1, kr.getMatch(4).getLocalDocID());
        assertEquals(0, kr.getMatch(4).getStartPos());
        assertEquals(1, kr.getMatch(4).getEndPos());
        assertEquals(0, kr.getMatch(5).getStartPos());
        assertEquals(3, kr.getMatch(5).getEndPos());

        // span to span
        assertEquals(6, kr.getMatch(6).getStartPos());
        assertEquals(9, kr.getMatch(6).getEndPos());

        // check target
    }


    /**
     * Relation span to token
     * */
    @Test
    public void testCase2 () throws IOException {
        ki.addDoc(createFieldDoc0());
        ki.addDoc(createFieldDoc1());
        ki.commit();

        SpanRelationQuery sq = new SpanRelationQuery(new SpanTermQuery(
                new Term("base", "<:xip/syntax-dep_rel")), true);
        kr = ki.search(sq, (short) 10);

        assertEquals((long) 7, kr.getTotalResults());
        // token to token
        assertEquals(0, kr.getMatch(0).getLocalDocID());
        assertEquals(1, kr.getMatch(0).getStartPos());
        assertEquals(2, kr.getMatch(0).getEndPos());
        assertEquals(3, kr.getMatch(1).getStartPos());
        assertEquals(4, kr.getMatch(1).getEndPos());
        assertEquals(4, kr.getMatch(2).getStartPos());
        assertEquals(5, kr.getMatch(2).getEndPos());
        assertEquals(6, kr.getMatch(3).getStartPos());
        assertEquals(7, kr.getMatch(3).getEndPos());

        assertEquals(1, kr.getMatch(4).getLocalDocID());
        // span to token
        assertEquals(6, kr.getMatch(4).getStartPos());
        assertEquals(9, kr.getMatch(4).getEndPos());
        assertEquals(6, kr.getMatch(5).getStartPos());
        assertEquals(9, kr.getMatch(5).getEndPos());
        // span to span
        assertEquals(9, kr.getMatch(6).getStartPos());
        assertEquals(10, kr.getMatch(6).getEndPos());
    }


    /**
     * Relations only
     * */
    @Test
    public void testCase3 () throws IOException {
        ki.addDoc(createFieldDoc2());
        ki.commit();

        // child-of relations
        SpanRelationQuery srq = new SpanRelationQuery(new SpanTermQuery(
                new Term("base", ">:child-of")), true);
        kr = ki.search(srq, (short) 20);

        assertEquals((long) 13, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(1, kr.getMatch(0).getEndPos());
        assertEquals(0, kr.getMatch(1).getStartPos());
        assertEquals(1, kr.getMatch(1).getEndPos());
        assertEquals(1, kr.getMatch(2).getStartPos());
        assertEquals(2, kr.getMatch(2).getEndPos());
        assertEquals(1, kr.getMatch(3).getStartPos());
        assertEquals(7, kr.getMatch(3).getEndPos());
        assertEquals(2, kr.getMatch(4).getStartPos());
        assertEquals(3, kr.getMatch(4).getEndPos());
        assertEquals(2, kr.getMatch(5).getStartPos());
        assertEquals(4, kr.getMatch(5).getEndPos());
    }


    /**
     * Relations only with/out attribute
     * */
    @Test
    public void testCase4 () throws IOException {
        ki.addDoc(createFieldDoc2());
        ki.commit();

        SpanRelationQuery srq = new SpanRelationQuery(new SpanTermQuery(
                new Term("base", ">:child-of")), true);

        SpanFocusQuery fq = new SpanFocusQuery(srq, srq.getTempClassNumbers());
        fq.setMatchTemporaryClass(true);
        fq.setRemoveTemporaryClasses(true);
        fq.setSorted(false);

        SpanAttributeQuery saq = new SpanAttributeQuery(new SpanTermQuery(
                new Term("base", "@:func=sbj")), true);

        kr = ki.search(saq, (short) 20);

        // child-of with attr func=sbj
		SpanWithAttributeQuery wq;
		// wq = new SpanWithAttributeQuery(fq, saq, true);
		// kr = ki.search(wq, (short) 20);
		// assertEquals((long) 1, kr.getTotalResults());
		// assertEquals(0, kr.getMatch(0).getStartPos()); // token
		// assertEquals(7, kr.getMatch(0).getEndPos());

        // child-of without attr func=sbj
        wq = new SpanWithAttributeQuery(fq,
                new SpanAttributeQuery(new SpanTermQuery(new Term("base",
 "@:func=sbj")), true, true),
                true);
        kr = ki.search(wq, (short) 20);
        assertEquals((long) 12, kr.getTotalResults());
    }


    /**
     * Relation directions <br/>
     * Relation with specific sources, return the sources
     * */
    @Test
    public void testCase6 () throws IOException {
        ki.addDoc(createFieldDoc2());
        ki.commit();

        // return all children that are NP
        SpanElementQuery seq1 = new SpanElementQuery("base", "np");
        SpanClassQuery scq1 = new SpanClassQuery(seq1, (byte) 1);

        SpanRelationQuery srq = new SpanRelationQuery(new SpanTermQuery(
                new Term("base", ">:child-of")), true);

        SpanRelationMatchQuery rm = new SpanRelationMatchQuery(srq, scq1, true);
        SpanFocusQuery rv = new SpanFocusQuery(rm, (byte) 1);
        rv.setSorted(false);

        assertEquals(
                "focus(1: focus(#[1,2]spanSegment(spanRelation(base:>:child-of), {1: <base:np />})))",
                rv.toString());

        kr = ki.search(rv, (short) 10);

        assertEquals(4, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(1, kr.getMatch(0).getEndPos());
        assertEquals(2, kr.getMatch(1).getStartPos());
        assertEquals(4, kr.getMatch(1).getEndPos());
        assertEquals(2, kr.getMatch(2).getStartPos());
        assertEquals(7, kr.getMatch(2).getEndPos());
        assertEquals(5, kr.getMatch(3).getStartPos());
        assertEquals(7, kr.getMatch(3).getEndPos());

        // return all parents that are NP
        srq = new SpanRelationQuery(new SpanTermQuery(new Term("base",
                "<:child-of")), true);
        rm = new SpanRelationMatchQuery(srq, scq1, true);
        rv = new SpanFocusQuery(rm, (byte) 1);
        kr = ki.search(rv, (short) 10);

        assertEquals(7, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(1, kr.getMatch(0).getEndPos());
        assertEquals(2, kr.getMatch(1).getStartPos());
        assertEquals(4, kr.getMatch(1).getEndPos());
        assertEquals(2, kr.getMatch(2).getStartPos());
        assertEquals(4, kr.getMatch(2).getEndPos());
        assertEquals(2, kr.getMatch(3).getStartPos());
        assertEquals(7, kr.getMatch(3).getEndPos());
        assertEquals(2, kr.getMatch(4).getStartPos());
        assertEquals(7, kr.getMatch(4).getEndPos());
        assertEquals(5, kr.getMatch(5).getStartPos());
        assertEquals(7, kr.getMatch(5).getEndPos());
        assertEquals(5, kr.getMatch(6).getStartPos());
        assertEquals(7, kr.getMatch(6).getEndPos());
    }


    /**
     * Dependency relations with attribute
     * */
    @Test
    public void testCase5 () throws IOException {
        ki.addDoc(createFieldDoc2());
        ki.commit();

        // target of a dependency relation
        SpanRelationQuery srq = new SpanRelationQuery(new SpanTermQuery(
                new Term("base", "<:dep")), true);
        kr = ki.search(srq, (short) 10);
        assertEquals((long) 6, kr.getTotalResults());

        SpanFocusQuery fq = new SpanFocusQuery(srq, srq.getTempClassNumbers());
        fq.setMatchTemporaryClass(true);
        fq.setRemoveTemporaryClasses(true);
        // fq.setSorted(false);

        kr = ki.search(fq, (short) 10);
        assertEquals((long) 6, kr.getTotalResults());

        SpanAttributeQuery aq = new SpanAttributeQuery(new SpanTermQuery(
                new Term("base", "@:func=head")), true);
        kr = ki.search(aq, (short) 10);

        // dependency relation, which is also a head
        SpanWithAttributeQuery wq = new SpanWithAttributeQuery(fq, aq, true);

        kr = ki.search(wq, (short) 20);

        assertEquals((long) 2, kr.getTotalResults());
        assertEquals(2, kr.getMatch(0).getStartPos());
        assertEquals(4, kr.getMatch(0).getEndPos());
        assertEquals(5, kr.getMatch(1).getStartPos());
        assertEquals(7, kr.getMatch(1).getEndPos());

    }


    /**
     * Relation with specific sources, return any targets
     * */
    @Test
    public void testCase7 () throws IOException {
        ki.addDoc(createFieldDoc2());
        ki.commit();

        // match all children that are NP
        SpanElementQuery seq1 = new SpanElementQuery("base", "np");
        SpanClassQuery scq1 = new SpanClassQuery(seq1, (byte) 1);

        SpanRelationQuery srq = new SpanRelationQuery(new SpanTermQuery(
                new Term("base", ">:child-of")), true);
        srq.setTargetClass((byte) 2);

        SpanRelationMatchQuery rm = new SpanRelationMatchQuery(srq, scq1, true);
        // SpanQuery rv = new SpanFocusQuery(rm, (byte) 1);

        //return all parents of np
        SpanFocusQuery rv2 = new SpanFocusQuery(rm, (byte) 2);
        rv2.setSorted(false);

        assertEquals(
                "focus(2: focus(#[1,2]spanSegment({2: target:spanRelation(base:>:child-of)}, {1: <base:np />})))",
                rv2.toString());

        kr = ki.search(rv2, (short) 10);

        assertEquals((long) 4, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(7, kr.getMatch(0).getEndPos());
        assertEquals(1, kr.getMatch(1).getStartPos());
        assertEquals(7, kr.getMatch(1).getEndPos());
        assertEquals(2, kr.getMatch(2).getStartPos());
        assertEquals(7, kr.getMatch(2).getEndPos());
        assertEquals(4, kr.getMatch(3).getStartPos());
        assertEquals(7, kr.getMatch(3).getEndPos());
        // id problem (solved)

        // return all parents of np that are PP

    }


    /**
     * Relations whose source/target do not embed
     * its counterparts.
     * */
    @Test
    public void testCase8 () throws IOException {
        ki.addDoc(createFieldDoc2());
        ki.commit();

        //return source of dep relations to pos:NN

        SpanTermWithIdQuery tq = new SpanTermWithIdQuery(new Term("base",
                "pos:NN"), true);
        SpanRelationQuery srq = new SpanRelationQuery(new SpanTermQuery(
                new Term("base", "<:dep")), true);
        srq.setSourceClass((byte) 1);
        SpanRelationMatchQuery rm = new SpanRelationMatchQuery(srq, tq, true);
        SpanQuery rv = new SpanFocusQuery(rm, (byte) 1);

        kr = ki.search(rv, (short) 10);
        assertEquals((long) 3, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(1, kr.getMatch(0).getEndPos());
        assertEquals(1, kr.getMatch(1).getStartPos());
        assertEquals(2, kr.getMatch(1).getEndPos());
        assertEquals(4, kr.getMatch(2).getStartPos());
        assertEquals(5, kr.getMatch(2).getEndPos());

        // return target of dep relations from pos:NN
        srq = new SpanRelationQuery(
                new SpanTermQuery(new Term("base", ">:dep")), true);
        srq.setTargetClass((byte) 1);
        rm = new SpanRelationMatchQuery(srq, tq, true);
        rv = new SpanFocusQuery(rm, (byte) 1);

        kr = ki.search(rv, (short) 10);
        assertEquals((long) 3, kr.getTotalResults());
        assertEquals(2, kr.getMatch(0).getStartPos());
        assertEquals(3, kr.getMatch(0).getEndPos());
        assertEquals(4, kr.getMatch(1).getStartPos());
        assertEquals(5, kr.getMatch(1).getEndPos());
        assertEquals(5, kr.getMatch(2).getStartPos());
        assertEquals(6, kr.getMatch(2).getEndPos());

    }


    /**
     * Relation with specific sources and return any targets <br/>
     * Relation with specific sources and targets, return the targets
     * <br/>
     * Relation with specific sources and targets, return the sources
     * 
     * @throws IOException
     * */
    @Test
    public void testCase9 () throws IOException {
        ki.addDoc(createFieldDoc2());
        ki.commit();

        // return all children of np
        SpanElementQuery seq1 = new SpanElementQuery("base", "np");
        SpanClassQuery scq1 = new SpanClassQuery(seq1, (byte) 1);
        SpanRelationQuery srq = new SpanRelationQuery(new SpanTermQuery(
                new Term("base", "<:child-of")), true);
        srq.setSourceClass((byte) 2);
        SpanRelationMatchQuery rm = new SpanRelationMatchQuery(srq, scq1, true);
        SpanFocusQuery rv = new SpanFocusQuery(rm, (byte) 2);
        rv.setSorted(false);

        assertEquals(
                "focus(2: focus(#[1,2]spanSegment({2: source:spanRelation(base:<:child-of)}, {1: <base:np />})))",
                rv.toString());

        kr = ki.search(rv, (short) 10);

        assertEquals((long) 7, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(1, kr.getMatch(0).getEndPos());
        assertEquals(2, kr.getMatch(1).getStartPos());
        assertEquals(3, kr.getMatch(1).getEndPos());
        assertEquals(2, kr.getMatch(2).getStartPos());
        assertEquals(4, kr.getMatch(2).getEndPos());
        assertEquals(3, kr.getMatch(3).getStartPos());
        assertEquals(4, kr.getMatch(3).getEndPos());
        assertEquals(4, kr.getMatch(4).getStartPos());
        assertEquals(7, kr.getMatch(4).getEndPos());
        assertEquals(5, kr.getMatch(5).getStartPos());
        assertEquals(6, kr.getMatch(5).getEndPos());
        assertEquals(6, kr.getMatch(6).getStartPos());
        assertEquals(7, kr.getMatch(6).getEndPos());

        // return all children of np that are articles

        SpanTermWithIdQuery tiq = new SpanTermWithIdQuery(new Term("base",
                "pos:ART"), true);
        SpanClassQuery scq2 = new SpanClassQuery(tiq, (byte) 2);

        srq = new SpanRelationQuery(new SpanTermQuery(new Term("base",
                ">:child-of")), true);
        rm = new SpanRelationMatchQuery(srq, scq2, scq1, true);
        rv = new SpanFocusQuery(rm, (byte) 2);

        assertEquals(
                "focus(2: focus(#[1,2]spanSegment(focus(#2: spanSegment(spanRelation(base:>:child-of), {2: spanTermWithId(base:pos:ART)})), {1: <base:np />})))",
                rv.toString());

        kr = ki.search(rv, (short) 10);

        assertEquals((long) 2, kr.getTotalResults());
        assertEquals(2, kr.getMatch(0).getStartPos());
        assertEquals(3, kr.getMatch(0).getEndPos());
        assertEquals(5, kr.getMatch(1).getStartPos());
        assertEquals(6, kr.getMatch(1).getEndPos());

        // return all nps whose children are articles
        SpanFocusQuery sf = new SpanFocusQuery(rm, (byte) 1);
        assertEquals(
                "focus(1: focus(#[1,2]spanSegment(focus(#2: spanSegment(spanRelation(base:>:child-of), {2: spanTermWithId(base:pos:ART)})), {1: <base:np />})))",
                sf.toString());

        kr = ki.search(sf, (short) 10);

        assertEquals((long) 2, kr.getTotalResults());
        assertEquals(2, kr.getMatch(0).getStartPos());
        assertEquals(4, kr.getMatch(0).getEndPos());
        assertEquals(5, kr.getMatch(1).getStartPos());
        assertEquals(7, kr.getMatch(1).getEndPos());

    }


    @Test
    public void testCase10 () throws IOException {
        ki.addDoc(createFieldDoc2());
        ki.commit();
        SpanElementQuery seq1 = new SpanElementQuery("base", "np");
        SpanElementQuery seq2 = new SpanElementQuery("base", "np");
        SpanClassQuery scq1 = new SpanClassQuery(seq1, (byte) 1);
        SpanClassQuery scq2 = new SpanClassQuery(seq2, (byte) 2);

        SpanRelationQuery srq = new SpanRelationQuery(new SpanTermQuery(
                new Term("base", ">:child-of")), true);
        SpanRelationMatchQuery rq = new SpanRelationMatchQuery(srq, scq2, true);

        kr = ki.search(rq, (short) 10);

        assertEquals((long) 4, kr.getTotalResults());
        assertEquals(0, kr.getMatch(0).getStartPos());
        assertEquals(7, kr.getMatch(0).getEndPos());
        assertEquals(1, kr.getMatch(1).getStartPos());
        assertEquals(7, kr.getMatch(1).getEndPos());
        assertEquals(2, kr.getMatch(2).getStartPos());
        assertEquals(7, kr.getMatch(2).getEndPos());
        assertEquals(4, kr.getMatch(3).getStartPos());
        assertEquals(7, kr.getMatch(3).getEndPos());
    }
}
