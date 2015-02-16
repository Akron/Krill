package de.ids_mannheim.korap;

import java.util.*;
import java.io.IOException;

import de.ids_mannheim.korap.*;
import de.ids_mannheim.korap.util.KorapDate;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.collection.BooleanFilter;
import de.ids_mannheim.korap.collection.FilterOperation;
import de.ids_mannheim.korap.collection.CollectionBuilder;
import de.ids_mannheim.korap.response.Notifications;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.*;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.Bits;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a Virtual Collection of documents by means of a KoralQuery
 * or by applying manual filters and extensions on Lucene fields.
 *
 * <blockquote><pre>
 *   KorapCollection kc = new KorapCollection(json);
 *   kc.filterUIDS("a1", "a2", "a3");
 * </pre></blockquote>
 *
 * <strong>Warning</strong>: This API is deprecated and will
 * be replaced in future versions. It supports legacy versions of
 * KoralQuery.
 *
 * @author diewald
 */
/*
 * TODO: Make a cache for the bits
 *       Delete it in case of an extension or a filter
 * TODO: Maybe use randomaccessfilterstrategy
 * TODO: Maybe a constantScoreQuery can make things faster?
 * See http://mail-archives.apache.org/mod_mbox/lucene-java-user/
 *     200805.mbox/%3C17080852.post@talk.nabble.com%3E
 */
public class KorapCollection extends Notifications {
    private KorapIndex index;
    private KorapDate created;
    private String id;
    private ArrayList<FilterOperation> filter;
    private int filterCount = 0;
    
    // Logger
    private final static Logger log = LoggerFactory.getLogger(KorapCollection.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    public KorapCollection (KorapIndex ki) {
        this.index = ki;
        this.filter = new ArrayList<FilterOperation>(5);
    };


    /**
     * Construct a new KorapCollection by passing a KoralQuery.
     * This supports collections with the key "collection" and
     * legacy collections with the key "collections".
     *
     * @param jsonString The virtual collection as a KoralQuery.
     */
    public KorapCollection (String jsonString) {
        ObjectMapper mapper = new ObjectMapper();
        this.filter = new ArrayList<FilterOperation>(5);

        try {
            JsonNode json = mapper.readTree(jsonString);
            
            // Deserialize from recent collections
            if (json.has("collection")) {
                this.fromJSON(json.get("collection"));
            }
	    
            // Legacy collection serialization
            // This will be removed!
            else if (json.has("collections")) {
                this.addMessage(
                    850,
                    "Collections are deprecated in favour of a single collection"
                );
                for (JsonNode collection : json.get("collections")) {
                    this.fromJSONLegacy(collection);
                };
            };
        }
        // Some exceptions ...
        catch (QueryException qe) {
            this.addError(qe.getErrorCode(), qe.getMessage());
        }
        catch (IOException e) {
            this.addError(
                621,
                "Unable to parse JSON",
                "KorapCollection",
                e.getLocalizedMessage()
            );
        };
    };


    /**
     * Construct a new KorapCollection.
     */
    public KorapCollection () {
        this.filter = new ArrayList<FilterOperation>(5);
    };


    /**
     * Import the "collection" part of a KoralQuery.
     *
     * @param jsonString The "collection" part of a KoralQuery.
     * @throws QueryException
     */
    public void fromJSON (String jsonString) throws QueryException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.fromJSON((JsonNode) mapper.readTree(jsonString));
        }
        catch (Exception e) {
            this.addError(621, "Unable to parse JSON", "KorapCollection");
        };
    };


    /**
     * Import the "collection" part of a KoralQuery.
     *
     * @param json The "collection" part of a KoralQuery
     *        as a {@link JsonNode} object.
     * @throws QueryException
     */
    public void fromJSON (JsonNode json) throws QueryException {
        this.filter(new CollectionBuilder(json));
    };


    /**
     * Import the "collections" part of a KoralQuery.
     * This method is deprecated and will vanish in future versions.
     *
     * @param jsonString The "collections" part of a KoralQuery.
     * @throws QueryException
     */
    @Deprecated
    public void fromJSONLegacy (String jsonString) throws QueryException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.fromJSONLegacy((JsonNode) mapper.readValue(jsonString, JsonNode.class));
        }
        catch (Exception e) {
            this.addError(621, "Unable to parse JSON", "KorapCollection");
        };
    };


    /**
     * Import the "collections" part of a KoralQuery.
     * This method is deprecated and will vanish in future versions.
     *
     * @param json The "collections" part of a KoralQuery
     *        as a {@link JsonNode} object.
     * @throws QueryException
     */
    public void fromJSONLegacy (JsonNode json) throws QueryException {
        if (!json.has("@type"))
            throw new QueryException(701, "JSON-LD group has no @type attribute");

        if (!json.has("@value"))
            throw new QueryException(851, "Legacy filter need @value fields");

        String type = json.get("@type").asText();

        CollectionBuilder kf = new CollectionBuilder();
        kf.setBooleanFilter(kf.fromJSONLegacy(json.get("@value"), "tokens"));

        // Filter the collection
        if (type.equals("korap:meta-filter")) {
            if (DEBUG)
                log.trace("Add Filter LEGACY");
            this.filter(kf);
        }
        
        // Extend the collection
        else if (type.equals("korap:meta-extend")) {
            if (DEBUG)
                log.trace("Add Extend LEGACY");
            this.extend(kf);
        };
    };


    /**
     * Set the {@link KorapIndex} the virtual collection refers to.
     *
     * @param index The {@link KorapIndex} the virtual collection refers to.
     */
    public void setIndex (KorapIndex index) {
        this.index = index;
    };


    /**
     * Add a filter by means of a {@link BooleanFilter}.
     *
     * <strong>Warning</strong>: Filters are part of the collections
     * legacy API and may vanish without warning.
     *
     * @param filter The filter to add to the collection.
     * @return The {@link KorapCollection} object for chaining.
     */
    // TODO: The checks may not be necessary
    public KorapCollection filter (BooleanFilter filter) {
        if (DEBUG)
            log.trace("Added filter: {}", filter.toString());
        
        if (filter == null) {
            this.addWarning(830, "Filter was empty");
            return this;
        };

        Filter f = (Filter) new QueryWrapperFilter(filter.toQuery());
        if (f == null) {
            this.addWarning(831, "Filter is not wrappable");
            return this;
        };
        FilterOperation fo = new FilterOperation(f, false);
        if (fo == null) {
            this.addWarning(832, "Filter operation is invalid");
            return this;
        };
        this.filter.add(fo);
        this.filterCount++;
        return this;
    };


    /**
     * Add a filter by means of a {@link CollectionBuilder} object.
     *
     * <strong>Warning</strong>: Filters are part of the collections
     * legacy API and may vanish without warning.
     *
     * @param filter The filter to add to the collection.
     * @return The {@link KorapCollection} object for chaining.
     */
    public KorapCollection filter (CollectionBuilder filter) {
        return this.filter(filter.getBooleanFilter());
    };


    /**
     * Add an extension by means of a {@link BooleanFilter}.
     *
     * <strong>Warning</strong>: Extensions are part of the collections
     * legacy API and may vanish without warning.
     *
     * @param extension The extension to add to the collection.
     * @return The {@link KorapCollection} object for chaining.
     */
    public KorapCollection extend (BooleanFilter extension) {
        if (DEBUG)
            log.trace("Added extension: {}", extension.toString());

        this.filter.add(
            new FilterOperation(
                (Filter) new QueryWrapperFilter(extension.toQuery()),
                true
            )
        );
        this.filterCount++;
        return this;
    };


    /**
     * Add an extension by means of a {@link CollectionBuilder} object.
     *
     * <strong>Warning</strong>: Extensions are part of the collections
     * legacy API and may vanish without warning.
     *
     * @param extension The extension to add to the collection.
     * @return The {@link KorapCollection} object for chaining.
     */
    public KorapCollection extend (CollectionBuilder extension) {
        return this.extend(extension.getBooleanFilter());
    };


    /**
     * Add a filter based on a list of unique document identifiers.
     * UIDs may be indexed in the field "UID".
     *
     * This filter is not part of the legacy API!
     *
     * @param uids The list of unique document identifier.
     * @return The {@link KorapCollection} object for chaining.
     */
    public KorapCollection filterUIDs (String ... uids) {
        BooleanFilter filter = new BooleanFilter();
        filter.or("UID", uids);
        if (DEBUG)
            log.debug("UID based filter: {}", filter.toString());
        return this.filter(filter);
    };


    /**
     * Get the list of filters constructing the collection.
     *
     * <strong>Warning</strong>: This is part of the collections
     * legacy API and may vanish without warning.
     *
     * @return The list of filters.
     */
    public List<FilterOperation> getFilters () {
        return this.filter;
    };


    /**
     * Get a certain {@link FilterOperation} from the list of filters
     * constructing the collection by its numerical index.
     *
     * <strong>Warning</strong>: This is part of the collections
     * legacy API and may vanish without warning.
     *
     * @param index The index position of the requested {@link FilterOperation}.
     * @return The {@link FilterOperation} at the certain list position.
     */
    public FilterOperation getFilter (int index) {
        return this.filter.get(index);
    };


    /**
     * Get the number of filter operations constructing this collection.
     *
     * <strong>Warning</strong>: This is part of the collections
     * legacy API and may vanish without warning.
     *
     * @return The number of filter operations constructing this collection.
     */
    public int getCount() {
        return this.filterCount;
    };


    /**
     * Generate a string representatio of the virtual collection.
     *
     * <strong>Warning</strong>: This currently does not generate a valid
     * KoralQuery string, so this may change in a future version.
     *
     * @return A string representation of the virtual collection.
     */
    public String toString () {
        StringBuilder sb = new StringBuilder();
        for (FilterOperation fo : this.filter) {
            sb.append(fo.toString()).append("; ");
        };
        return sb.toString();
    };


    /**
     * Search in the virtual collection.
     * This is mostly used for testing purposes
     * and <strong>is not recommended</strong>
     * as a common search API.
     *
     * Please use {@link KorapQuery#run} instead.
     *
     * @param query a {@link SpanQuery} to apply on the
     *        virtual collection.
     * @return A {@link KorapResult} object representing the search's
     *         result.
     */
    public KorapResult search (SpanQuery query) {
        return this.index.search(
            this,
            query,
            0,
            (short) 20,
            true, (short) 5,
            true, (short) 5
        );
    };


    /**
     * Create a bit vector representing the live documents of the
     * virtual collection to be used in searches.
     *
     * @param The {@link AtomicReaderContext} to search in.
     * @return A bit vector representing the live documents of the
     *         virtual collection.
     * @throws IOException
     */
    public FixedBitSet bits (AtomicReaderContext atomic) throws IOException  {
        // TODO: Probably use Bits.MatchAllBits(int len)
        boolean noDoc = true;
        FixedBitSet bitset;
        
        // There are filters set
        if (this.filterCount > 0) {
            bitset = new FixedBitSet(atomic.reader().maxDoc());

            ArrayList<FilterOperation> filters =
                (ArrayList<FilterOperation>) this.filter.clone();

            FilterOperation kcInit = filters.remove(0);
            if (DEBUG)
                log.trace("FILTER: {}", kcInit);
            
            // Init vector
            DocIdSet docids = kcInit.filter.getDocIdSet(atomic, null);

            DocIdSetIterator filterIter = docids.iterator();
            
            // The filter has an effect
            if (filterIter != null) {
                if (DEBUG) log.trace("InitFilter has effect");
                bitset.or(filterIter);
                noDoc = false;
            };
            
            // Apply all filters sequentially
            for (FilterOperation kc : filters) {
                if (DEBUG) log.trace("FILTER: {}", kc);

                // TODO: BUG???
                docids = kc.filter.getDocIdSet(atomic, kc.isExtension() ? null : bitset);
                filterIter = docids.iterator();

                if (filterIter == null) {
                    // There must be a better way ...
                    if (kc.isFilter()) {
                        // TODO: Check if this is really correct!
                        // Maybe here is the bug
                        bitset.clear(0, bitset.length());
                        noDoc = true;
                    };
                    continue;
                };
                if (kc.isExtension())
                    bitset.or(filterIter);
                else
                    bitset.and(filterIter);
            };

            if (!noDoc) {
                FixedBitSet livedocs = (FixedBitSet) atomic.reader().getLiveDocs();
                if (livedocs != null)
                    bitset.and(livedocs);
            };
        }
        else {
            bitset = (FixedBitSet) atomic.reader().getLiveDocs();
        };

        return bitset;
    };


    /**
     * Search for the number of occurrences of different types,
     * e.g. <i>documents</i>, <i>sentences</i> etc. in the virtual
     * collection.
     *
     * @param field The field containing the textual data and the
     *        annotations as a string.
     * @param type The type of meta information,
     *        e.g. <i>documents</i> or <i>sentences</i> as a string.
     * @return The number of the occurrences.
     * @throws IOException
     * @see KorapIndex#numberOf
     */
    public long numberOf (String field, String type) throws IOException {
        if (this.index == null)
            return (long) -1;

        return this.index.numberOf(this, field, type);
    };


    /**
     * Search for the number of occurrences of different types,
     * e.g. <i>documents</i>, <i>sentences</i> etc. in the virtual
     * collection, in the <i>base</i> foundry.
     *
     * @param type The type of meta information,
     *        e.g. <i>documents</i> or <i>sentences</i> as a string.
     * @return The number of the occurrences.
     * @throws IOException
     * @see KorapIndex#numberOf
     */
    public long numberOf (String type) throws IOException {
        if (this.index == null)
            return (long) -1;

        return this.index.numberOf(this, "tokens", type);
    };


    @Deprecated
    public HashMap getTermRelation (String field) throws Exception {
        if (this.index == null) {
            HashMap<String,Long> map = new HashMap<>(1);
            map.put("-docs", (long) 0);
            return map;
        };

        return this.index.getTermRelation(this, field);
    };


    @Deprecated
    public String getTermRelationJSON (String field) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        sw.append("{\"field\":");
        mapper.writeValue(sw,field);
        sw.append(",");

        try {
            HashMap<String, Long> map = this.getTermRelation(field);

            sw.append("\"documents\":");
            mapper.writeValue(sw,map.remove("-docs"));
            sw.append(",");

            String[] keys = map.keySet().toArray(new String[map.size()]);

            HashMap<String,Integer> setHash = new HashMap<>(20);
            ArrayList<HashMap<String,Long>> set = new ArrayList<>(20);
            ArrayList<Long[]> overlap = new ArrayList<>(100);
	    
            int count = 0;
            for (String key : keys) {
                if (!key.startsWith("#__")) {
                    HashMap<String,Long> simpleMap = new HashMap<>();
                    simpleMap.put(key, map.remove(key));
                    set.add(simpleMap);
                    setHash.put(key, count++);
                };
            };

            keys = map.keySet().toArray(new String[map.size()]);
            for (String key : keys) {
                String[] comb = key.substring(3).split(":###:");
                Long[] l = new Long[3];
                l[0] = (long) setHash.get(comb[0]);
                l[1] = (long) setHash.get(comb[1]);
                l[2] = map.remove(key);
                overlap.add(l);
            };

            sw.append("\"sets\":");
            mapper.writeValue(sw, (Object) set);
            sw.append(",\"overlaps\":");
            mapper.writeValue(sw, (Object) overlap);
            sw.append(",\"error\":null");
        }
        catch (Exception e) {
            sw.append("\"error\":");
            mapper.writeValue(sw,e.getMessage());
        };

        sw.append("}");
        return sw.getBuffer().toString();
    };
};
