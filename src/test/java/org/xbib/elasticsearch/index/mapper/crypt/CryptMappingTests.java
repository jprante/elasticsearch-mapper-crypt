
package org.xbib.elasticsearch.index.mapper.crypt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.base.Charsets;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.EnvironmentModule;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexNameModule;
import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.codec.docvaluesformat.DocValuesFormatService;
import org.elasticsearch.index.codec.postingsformat.PostingsFormatService;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.DocumentMapperParser;
import org.elasticsearch.index.mapper.ParseContext;
import org.elasticsearch.index.settings.IndexSettingsModule;
import org.elasticsearch.index.similarity.SimilarityLookupService;
import org.elasticsearch.indices.analysis.IndicesAnalysisModule;
import org.elasticsearch.indices.analysis.IndicesAnalysisService;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;

import static org.elasticsearch.common.io.Streams.copyToString;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class CryptMappingTests extends Assert {

    private final Logger logger = LogManager.getLogger(CryptMappingTests.class.getName());

    private static DocumentMapperParser setupMapperParser(Settings fromSettings) throws IOException {
        Index index = new Index("test");
        Settings settings = ImmutableSettings.settingsBuilder()
                .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                .put(fromSettings)
                .build();
        Injector parentInjector = new ModulesBuilder().add(new SettingsModule(settings),
                new EnvironmentModule(new Environment(settings)),
                new IndicesAnalysisModule())
                .createInjector();
        AnalysisModule analysisModule = new AnalysisModule(settings, parentInjector.getInstance(IndicesAnalysisService.class));
        Injector injector = new ModulesBuilder().add(
                new IndexSettingsModule(index, settings),
                new IndexNameModule(index),
                analysisModule)
                .createChildInjector(parentInjector);
        AnalysisService analysisService = injector.getInstance(AnalysisService.class);
        DocumentMapperParser mapperParser = new DocumentMapperParser(index,
                settings,
                analysisService,
                new PostingsFormatService(index),
                new DocValuesFormatService(index),
                new SimilarityLookupService(index, settings),
                null
        );
        mapperParser.putTypeParser(CryptMapper.CONTENT_TYPE, new CryptMapper.TypeParser());
        return mapperParser;
    }

    @Test
    public void testSimpleCryptMapping() throws Exception {
        String mapping = copyToStringFromClasspath("sha256-mapping.json");
        DocumentMapperParser docMapperParser = setupMapperParser(ImmutableSettings.EMPTY);
        DocumentMapper docMapper = docMapperParser.parse(mapping);
        String sampleText = copyToStringFromClasspath("plaintext.txt");
        BytesReference json = jsonBuilder().startObject().field("_id", 1).field("someField", sampleText).endObject().bytes();
        ParseContext.Document doc = docMapper.parse(json).rootDoc();
        //for (IndexableField field : doc.getFields()) {
        //    logger.info("{} = {}", field.name(), field.stringValue());
        //}
        assertEquals(1, doc.getFields("someField").length);
        assertEquals("{SHA-256}cc482c9bf51da22e59ce8731719963a3fee3d2c7240ee2ee7f13cae4f27f773a", doc.getFields("someField")[0].stringValue());
        // re-parse it
        String builtMapping = docMapper.mappingSource().string();
        docMapper = docMapperParser.parse(builtMapping);
        json = jsonBuilder().startObject().field("_id", 1).field("someField", sampleText).endObject().bytes();
        doc = docMapper.parse(json).rootDoc();
        assertEquals(1, doc.getFields("someField").length);
        assertEquals("{SHA-256}cc482c9bf51da22e59ce8731719963a3fee3d2c7240ee2ee7f13cae4f27f773a", doc.getFields("someField")[0].stringValue());
    }

    @Test
    public void testSHA512CryptMapping() throws Exception {
        String mapping = copyToStringFromClasspath("sha512-mapping.json");
        DocumentMapperParser docMapperParser = setupMapperParser(ImmutableSettings.EMPTY);
        docMapperParser.putTypeParser(CryptMapper.CONTENT_TYPE, new CryptMapper.TypeParser());
        DocumentMapper docMapper = docMapperParser.parse(mapping);
        String sampleText = copyToStringFromClasspath("plaintext.txt");
        BytesReference json = XContentFactory.jsonBuilder().startObject().field("_id", 1).field("someField", sampleText).endObject().bytes();
        ParseContext.Document doc = docMapper.parse(json).rootDoc();
        //for (IndexableField field : doc.getFields()) {
        //    log.info("{} = {}", field.name(), field.stringValue());
        //}
        assertEquals(1, doc.getFields("someField").length);;
        assertEquals("{SHA-512}9ca2bab7ffacb00e1c3f5f00eb2405bc32755159b18a013092b54adbe88ff47c21445c3dba035c4721588e42ec6921f4153c52b9feb214e984f24676ad9553f9", doc.getFields("someField")[0].stringValue());
        // re-parse it
        String builtMapping = docMapper.mappingSource().string();
        docMapper = docMapperParser.parse(builtMapping);
        json = XContentFactory.jsonBuilder().startObject().field("_id", 1).field("someField", sampleText).endObject().bytes();
        doc = docMapper.parse(json).rootDoc();
        assertEquals(1, doc.getFields("someField").length);;
        assertEquals("{SHA-512}9ca2bab7ffacb00e1c3f5f00eb2405bc32755159b18a013092b54adbe88ff47c21445c3dba035c4721588e42ec6921f4153c52b9feb214e984f24676ad9553f9", doc.getFields("someField")[0].stringValue());
    }


    public String copyToStringFromClasspath(String path) throws IOException {
        return copyToString(new InputStreamReader(getClass().getResource(path).openStream(), Charsets.UTF_8));
    }
}
