package de.ids_mannheim.korap.collection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.queries.TermsFilter;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.RegexpQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.index.TextPrependedTokenStream;
import de.ids_mannheim.korap.util.KrillDate;

/*
 * TODO: Optimize!
 *   - Remove identical object in Boolean groups
 *   - Flatten boolean groups
 *   - create "between" ranges for multiple date objects
 *
 * TODO:
 *   - Filters are deprecated, they should be ported to queries
 */

public class CollectionBuilder {

    // Logger
    private final static Logger log = LoggerFactory
            .getLogger(KrillCollection.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;


    public CollectionBuilder.Interface term (String field, String term) {
        return new CollectionBuilder.Term(field, term);
    };


    public CollectionBuilder.Interface re (String field, String term) {
        return new CollectionBuilder.Term(field, term, true);
    };


    public CollectionBuilder.Interface text (String field, String text) {
        return new CollectionBuilder.Text(field, text);
    };
	

    public CollectionBuilder.Interface since (String field, String date) {
        int since = new KrillDate(date).floor();

        if (since == 0 || since == KrillDate.BEGINNING)
            return null;

        return new CollectionBuilder.Range(field, since, KrillDate.END);
    };

	public CollectionBuilder.Interface nothing () {

		// Requires that a field with name "0---" does not exist
        return new CollectionBuilder.Term("0---", "0");
    };


    public CollectionBuilder.Interface till (String field, String date) {
        try {
            int till = new KrillDate(date).ceil();
            if (till == 0 || till == KrillDate.END)
                return null;

            return new CollectionBuilder.Range(field, KrillDate.BEGINNING,
                    till);
        }
        catch (NumberFormatException e) {
            log.warn("Parameter of till(date) is invalid");
        };
        return null;
    };


    // This will be optimized away in future versions
    public CollectionBuilder.Interface between (String field, String start,
            String end) {
        CollectionBuilder.Interface startObj = this.since(field, start);
        if (startObj == null)
            return null;

        CollectionBuilder.Interface endObj = this.till(field, end);
        if (endObj == null)
            return null;

        return this.andGroup().with(startObj).with(endObj);
    };


    public CollectionBuilder.Interface date (String field, String date) {
        KrillDate dateDF = new KrillDate(date);

        if (dateDF.year == 0)
            return null;

        if (dateDF.day == 0 || dateDF.month == 0) {
            int begin = dateDF.floor();
            int end = dateDF.ceil();

            if (end == 0
                    || (begin == KrillDate.BEGINNING && end == KrillDate.END))
                return null;

            return new CollectionBuilder.Range(field, begin, end);
        };

        return new CollectionBuilder.Range(field, dateDF.floor(),
                dateDF.ceil());
    };


    public CollectionBuilder.Group andGroup () {
        return new CollectionBuilder.Group(false);
    };


    public CollectionBuilder.Group orGroup () {
        return new CollectionBuilder.Group(true);
    };

    public interface Interface {
        public String toString ();


        public Filter toFilter ();


        public boolean isNegative ();


        public CollectionBuilder.Interface not ();
    };

    public class Term implements CollectionBuilder.Interface {
        private boolean isNegative = false;
        private boolean regex = false;
        private String field;
        private String term;


        public Term (String field, String term) {
            this.field = field;
            this.term = term;
        };


        public Term (String field, String term, boolean regex) {
            this.field = field;
            this.term = term;
            this.regex = regex;
        };


        public Filter toFilter () {
            // Regular expression
            if (this.regex)
                return new QueryWrapperFilter(
                        new RegexpQuery(new org.apache.lucene.index.Term(
                                this.field, this.term)));

            // Simple term
            return new TermsFilter(
                    new org.apache.lucene.index.Term(this.field, this.term));
        };


        public String toString () {
            Filter filter = this.toFilter();
            if (filter == null)
                return "";
            return filter.toString();
        };


        public boolean isNegative () {
            return this.isNegative;
        };


        public CollectionBuilder.Interface not () {
            this.isNegative = true;
            return this;
        };
    };


    public class Text implements CollectionBuilder.Interface {
        private boolean isNegative = false;
        // private boolean regex = false;
        private String field;
        private String text;


        public Text (String field, String text) {
            this.field = field;
            this.text = text;
        };

		// TODO:
		//   Currently this treatment is language specific and
		//    does too mzch, I guess.
        public Filter toFilter () {
			PhraseQuery pq = new PhraseQuery();
			int pos = 0;
			try {
				TextPrependedTokenStream tpts = new TextPrependedTokenStream(this.text);
				tpts.doNotPrepend();
				CharTermAttribute term;
				tpts.reset();
				while (tpts.incrementToken()) {
					term = tpts.getAttribute(CharTermAttribute.class);
					pq.add(new org.apache.lucene.index.Term(this.field, term.toString()), pos++);
				};
				tpts.close();
			}
			catch (IOException ie) {
				System.err.println(ie);
				return null;
			};
			
			return new QueryWrapperFilter(pq);
        };


        public String toString () {
            Filter filter = this.toFilter();
            if (filter == null)
                return "";
            return filter.toString();
        };


        public boolean isNegative () {
            return this.isNegative;
        };


        public CollectionBuilder.Interface not () {
            this.isNegative = true;
            return this;
        };
    };

	
    public class Group implements CollectionBuilder.Interface {
        private boolean isOptional = false;
        private boolean isNegative = true;


        public boolean isNegative () {
            return this.isNegative;
        };


        public boolean isOptional () {
            return this.isOptional;
        };

        private ArrayList<CollectionBuilder.Interface> operands;


        public Group (boolean optional) {
            this.isOptional = optional;
            this.operands = new ArrayList<CollectionBuilder.Interface>(3);
        };


        public Group with (CollectionBuilder.Interface cb) {
            if (cb == null)
                return this;

            if (!cb.isNegative())
                this.isNegative = false;
            this.operands.add(cb);
            return this;
        };


        public Group with (String field, String term) {
            if (field == null || term == null)
                return this;
            return this.with(new CollectionBuilder.Term(field, term));
        };


        public Filter toFilter () {
            if (this.operands == null || this.operands.isEmpty())
                return null;

            if (this.operands.size() == 1)
                return this.operands.get(0).toFilter();

            // BooleanFilter bool = new BooleanFilter();
            BooleanGroupFilter bool = new BooleanGroupFilter(this.isOptional);

            Iterator<CollectionBuilder.Interface> i = this.operands.iterator();
            while (i.hasNext()) {
                CollectionBuilder.Interface cb = i.next();
                if (cb.isNegative()) {
                    bool.without(cb.toFilter());
                }
                else {
                    bool.with(cb.toFilter());
                };
            };

            return bool;
        };


        public String toString () {
            Filter filter = this.toFilter();
            if (filter == null)
                return "";
            return filter.toString();
        };


        public CollectionBuilder.Interface not () {
            this.isNegative = true;
            return this;
        };
    };

    public class Range implements CollectionBuilder.Interface {
        private boolean isNegative = false;
        private String field;
        private int start, end;


        public Range (String field, int start, int end) {
            this.field = field;
            this.start = start;
            this.end = end;
        };


        public boolean isNegative () {
            return this.isNegative;
        };


        public String toString () {
            Filter filter = this.toFilter();
            if (filter == null)
                return "";
            return filter.toString();
        };


        public Filter toFilter () {
            return NumericRangeFilter.newIntRange(this.field, this.start,
                    this.end, true, true);
        };


        public CollectionBuilder.Interface not () {
            this.isNegative = true;
            return this;
        };
    };
    
    public class CachedVC implements CollectionBuilder.Interface {

        private CachedVCData cachedCollection;
        private boolean isNegative = false;

        public CachedVC (CachedVCData cc) {
            this.cachedCollection = cc;
        }

        @Override
        public Filter toFilter () {
            return new CachedVCFilter(cachedCollection);
        }

        @Override
        public boolean isNegative () {
            return this.isNegative;
        }

        @Override
        public CollectionBuilder.Interface not () {
            this.isNegative = true;
            return this;
        }
        
    }
    
    /** Wraps a sub CollectionBuilder.Interface to allows VC caching
     *  
     * @author margaretha
     *
     */
    public class ToCacheVC implements CollectionBuilder.Interface {

        private CollectionBuilder.Interface child;
        private String cacheKey;
        
        private Map<Integer, DocIdSet> docIdMap;

        public ToCacheVC (String vcRef, Interface cbi) {
            this.child = cbi;
            this.cacheKey = vcRef;
            this.docIdMap  = new HashMap<Integer, DocIdSet>();
        }

        @Override
        public Filter toFilter () {
            return new ToCacheVCFilter(cacheKey,docIdMap, child, child.toFilter());
        }

        @Override
        public boolean isNegative () {
            return child.isNegative();
        }

        @Override
        public CollectionBuilder.Interface not () {
            // not supported
            return this;
        }
    }
    
    public Interface namedVC (CachedVCData cc) {
        return new CollectionBuilder.CachedVC(cc);
    }
    
    public Interface toCacheVC (String vcRef, Interface cbi) {
        return new CollectionBuilder.ToCacheVC(vcRef, cbi);
    }
};
