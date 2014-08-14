package org.xbib.elasticsearch.module.crypt;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.DocumentMapperParser;
import org.elasticsearch.index.mapper.ParseContext;

import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xbib.elasticsearch.action.crypt.KeyAction;
import org.xbib.elasticsearch.action.crypt.KeyRequest;
import org.xbib.elasticsearch.action.crypt.KeyResponse;

import java.io.IOException;

import static org.elasticsearch.common.io.Streams.copyToStringFromClasspath;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class CryptMappingTest extends Assert {

    private final static ESLogger logger = ESLoggerFactory.getLogger(CryptMappingTest.class.getSimpleName());

    private DocumentMapperParser mapperParser;

    private Node node1;

    private Node node2;

    private Client client;

    @BeforeClass
    public void setupMapperParser() throws IOException {
        Settings nodeSettings = ImmutableSettings.settingsBuilder()
                .put("gateway.type", "none")
                .put("index.store.type", "memory")
                .put("index.number_of_shards", 1)
                .put("index.number_of_replica", 0)
                .put("cluster.routing.schedule", "50ms")
                .build();
        node1 = NodeBuilder.nodeBuilder().settings(nodeSettings).local(true).build().start();
        node2 = NodeBuilder.nodeBuilder().settings(nodeSettings).local(true).build().start();
        client = node1.client();

        /*
        BytesReference json = jsonBuilder().startObject().array("myfield", "a","b","c").endObject().bytes();
        client.prepareIndex("test", "test", "1234").setSource(json).execute().actionGet();

        json = jsonBuilder().startObject().field("author", "John Doe").endObject().bytes();
        client.prepareIndex("test", "authorities", "1").setSource(json).execute().actionGet();

        Index index = new Index("test");
        Map<String, AnalyzerProviderFactory> analyzerFactoryFactories = Maps.newHashMap();
        analyzerFactoryFactories.put("keyword",
                new PreBuiltAnalyzerProviderFactory("keyword", AnalyzerScope.INDEX, new KeywordAnalyzer()));
        Settings settings = ImmutableSettings.Builder.EMPTY_SETTINGS;
        AnalysisService analysisService = new AnalysisService(index, settings, null, analyzerFactoryFactories, null, null, null);
        mapperParser = new DocumentMapperParser(index, settings,
                analysisService,
                new PostingsFormatService(index),
                new DocValuesFormatService(index),
                new SimilarityLookupService(index, settings));
        Settings indexSettings = ImmutableSettings.settingsBuilder()
                .put("ref_index", "test")
                .put("ref_type", "test")
                .put("ref_fields", "myfield").build();
        mapperParser.putTypeParser(CryptMapper.CONTENT_TYPE,
                new CryptMapper.TypeParser(client, indexSettings));
         */
    }

    @AfterClass
    public void shutdown() {
        client.close();
        node2.close();
        node1.close();
    }

    @Test
    public void testCrypt() {
        KeyRequest keyRequest = new KeyRequest().createKey(true);
        client.execute(KeyAction.INSTANCE, keyRequest, new ActionListener<KeyResponse>() {
            @Override
            public void onResponse(KeyResponse keyResponse) {
                logger.info("key response = {}", keyResponse);
            }

            @Override
            public void onFailure(Throwable e) {
                logger.error(e.getMessage(), e);
            }
        });
        //KeyRequestBuilder keyRequestBuilder = new KeyRequestBuilder().ex
    }

    public void testRefMappings() throws Exception {
        String mapping = copyToStringFromClasspath("/ref-mapping.json");
        DocumentMapper docMapper = mapperParser.parse(mapping);
        BytesReference json = jsonBuilder().startObject().field("_id", 1).field("someField", "1234").endObject().bytes();
        ParseContext.Document doc = docMapper.parse(json).rootDoc();
        /*
        assertNotNull(doc);
        assertNotNull(docMapper.mappers().smartName("someField"));
        assertEquals(doc.get(docMapper.mappers().smartName("someField").mapper().names().indexName()), "1234");
        assertEquals(doc.getFields("someField.ref").length, 3);
        assertEquals(doc.getFields("someField.ref")[0].stringValue(), "a");
        assertEquals(doc.getFields("someField.ref")[1].stringValue(), "b");
        assertEquals(doc.getFields("someField.ref")[2].stringValue(), "c");
        */

        // re-parse it
        String builtMapping = docMapper.mappingSource().string();
        docMapper = mapperParser.parse(builtMapping);

        json = jsonBuilder().startObject().field("_id", 1).field("someField", "1234").endObject().bytes();
        doc = docMapper.parse(json).rootDoc();

        /*
        assertEquals(doc.get(docMapper.mappers().smartName("someField").mapper().names().indexName()), "1234");
        assertEquals(doc.getFields("someField.ref").length, 3);
        assertEquals(doc.getFields("someField.ref")[0].stringValue(), "a");
        assertEquals(doc.getFields("someField.ref")[1].stringValue(), "b");
        assertEquals(doc.getFields("someField.ref")[2].stringValue(), "c");
        */
    }


    public void testRef() throws Exception {
        String mapping = copyToStringFromClasspath("/ref-mapping-authorities.json");
        DocumentMapper docMapper = mapperParser.parse(mapping);
        BytesReference json = jsonBuilder().startObject()
                .field("_id", 1)
                .field("title", "A title")
                .startObject("author")
                .field("index", "test")
                .field("type", "authorities")
                .field("id", "1")
                .field("fields", "author")
                .endObject()
                .endObject().bytes();
        ParseContext.Document doc = docMapper.parse(json).rootDoc();
        /*assertEquals(doc.getFields("author.ref").length, 1);
        assertEquals(doc.getFields("author.ref")[0].stringValue(), "John Doe");*/
    }
}
