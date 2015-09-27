package org.xbib.elasticsearch.index.mapper.crypt;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.analysis.NamedAnalyzer;
import org.elasticsearch.index.codec.docvaluesformat.DocValuesFormatProvider;
import org.elasticsearch.index.codec.postingsformat.PostingsFormatProvider;
import org.elasticsearch.index.fielddata.FieldDataType;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.Mapper;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.mapper.ParseContext;
import org.elasticsearch.index.mapper.core.AbstractFieldMapper;
import org.elasticsearch.index.mapper.core.StringFieldMapper;
import org.elasticsearch.index.similarity.SimilarityProvider;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.mapper.core.TypeParsers.parseField;
import static org.elasticsearch.index.mapper.core.TypeParsers.parseMultiField;

public class CryptMapper extends StringFieldMapper {

    public static final String CONTENT_TYPE = "crypt";

    @Override
    public FieldDataType defaultFieldDataType() {
        return new FieldDataType("string");
    }

    @Override
    protected String contentType() {
        return CONTENT_TYPE;
    }

    public static class Builder extends StringFieldMapper.Builder {

        private String algo;

        public Builder(String name) {
            super(name);
            this.builder = this;
            this.algo =  "SHA-256";
        }

        public Builder algo(String algo) {
            this.algo = algo;
            return this;
        }

        @Override
        public CryptMapper build(BuilderContext context) {
            if (positionOffsetGap > 0) {
                indexAnalyzer = new NamedAnalyzer(indexAnalyzer, positionOffsetGap);
                searchAnalyzer = new NamedAnalyzer(searchAnalyzer, positionOffsetGap);
                searchQuotedAnalyzer = new NamedAnalyzer(searchQuotedAnalyzer, positionOffsetGap);
            }
            FieldType defaultFieldType = new FieldType(Defaults.FIELD_TYPE);
            if (fieldType.indexed() && !fieldType.tokenized()) {
                defaultFieldType.setOmitNorms(true);
                defaultFieldType.setIndexOptions(FieldInfo.IndexOptions.DOCS_ONLY);
                if (!omitNormsSet && boost == Defaults.BOOST) {
                    fieldType.setOmitNorms(true);
                }
                if (!indexOptionsSet) {
                    fieldType.setIndexOptions(FieldInfo.IndexOptions.DOCS_ONLY);
                }
            }
            defaultFieldType.freeze();
            CryptMapper fieldMapper = new CryptMapper(buildNames(context),
                    boost, fieldType, defaultFieldType, Boolean.FALSE, nullValue,
                    indexAnalyzer, searchAnalyzer, searchQuotedAnalyzer,
                    positionOffsetGap, ignoreAbove, postingsProvider, docValuesProvider, similarity, normsLoading,
                    fieldDataSettings, context.indexSettings(), multiFieldsBuilder.build(this, context), copyTo, algo);
            fieldMapper.includeInAll(includeInAll);
            return fieldMapper;
        }
    }

    public static class TypeParser implements Mapper.TypeParser {

        @SuppressWarnings({"unchecked","rawtypes"})
        @Override
        public Mapper.Builder parse(String name, Map<String, Object> node, ParserContext parserContext)
                throws MapperParsingException {
            CryptMapper.Builder builder = new Builder(name);
            parseField(builder, name, node, parserContext);
            for (Map.Entry<String, Object> entry : node.entrySet()) {
                String propName = Strings.toUnderscoreCase(entry.getKey());
                Object propNode = entry.getValue();
                if (propName.equals("algo")) {
                    builder.algo(propNode.toString());
                } else {
                    parseMultiField(builder, name, parserContext, propName, propNode);
                }
            }
            return builder;
        }
    }

    private String nullValue;
    private int ignoreAbove;
    private final String algo;

    public CryptMapper(
            FieldMapper.Names names,
            float boost,
            FieldType fieldType,
            FieldType defaultFieldType,
            Boolean docValues,
            String nullValue,
            NamedAnalyzer indexAnalyzer,
            NamedAnalyzer searchAnalyzer,
            NamedAnalyzer searchQuotedAnalyzer,
            int positionOffsetGap,
            int ignoreAbove,
            PostingsFormatProvider postingsFormat,
            DocValuesFormatProvider docValuesFormat,
            SimilarityProvider similarity,
            FieldMapper.Loading normsLoading,
            Settings fieldDataSettings,
            Settings indexSettings,
            AbstractFieldMapper.MultiFields multiFields,
            AbstractFieldMapper.CopyTo copyTo,
            String algo

    ) {
        super(names, boost, fieldType, defaultFieldType, docValues, nullValue,
                indexAnalyzer, searchAnalyzer, searchQuotedAnalyzer, positionOffsetGap,
                ignoreAbove, postingsFormat, docValuesFormat, similarity, normsLoading,
                fieldDataSettings, indexSettings, multiFields, copyTo);
        this.nullValue = nullValue;
        this.ignoreAbove = ignoreAbove;
        this.algo = algo;
    }

    @Override
    protected void parseCreateField(ParseContext context, List<Field> fields) throws IOException {
        ValueAndBoost valueAndBoost = parseCreateFieldForCrypt(context, nullValue, boost, algo);
        if (valueAndBoost.value() == null) {
            return;
        }
        if (ignoreAbove > 0 && valueAndBoost.value().length() > ignoreAbove) {
            return;
        }
        if (fieldType.indexed() || fieldType.stored()) {
            Field field = new Field(names.indexName(), valueAndBoost.value(), fieldType);
            field.setBoost(valueAndBoost.boost());
            fields.add(field);
        }
        if (hasDocValues()) {
            fields.add(new SortedSetDocValuesField(names.indexName(), new BytesRef(valueAndBoost.value())));
        }
        if (fields.isEmpty()) {
            context.ignoredValue(names.indexName(), valueAndBoost.value());
        }
    }

    static ValueAndBoost parseCreateFieldForCrypt(ParseContext context, String nullValue, float defaultBoost, String algo) throws IOException {
        if (context.externalValueSet()) {
            return new ValueAndBoost((String) context.externalValue(), defaultBoost);
        }
        XContentParser parser = context.parser();
        if (parser.currentToken() == XContentParser.Token.VALUE_NULL) {
            return new ValueAndBoost(nullValue, defaultBoost);
        }
        if (parser.currentToken() == XContentParser.Token.START_OBJECT) {
            XContentParser.Token token;
            String currentFieldName = null;
            String value = nullValue;
            float boost = defaultBoost;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else {
                    if ("value".equals(currentFieldName) || "_value".equals(currentFieldName)) {
                        value = crypt(parser.textOrNull(), algo);
                    } else if ("boost".equals(currentFieldName) || "_boost".equals(currentFieldName)) {
                        boost = parser.floatValue();
                    } else {
                        throw new ElasticsearchIllegalArgumentException("unknown property [" + currentFieldName + "]");
                    }
                }
            }
            return new ValueAndBoost(value, boost);
        }
        return new ValueAndBoost(crypt(parser.textOrNull(), algo), defaultBoost);
    }

    @Override
    protected void doXContentBody(XContentBuilder builder, boolean includeDefaults, Params params) throws IOException {
        super.doXContentBody(builder, includeDefaults, params);
        builder.field("algo", algo);
    }

    private static String crypt(String plainText, String algo) {
        if (plainText == null) {
            return null;
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algo);
            digest.update(plainText.getBytes(UTF8));
            return '{' + algo + '}' + bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
        }
        return null;
    }

    private final static Charset UTF8 = Charset.forName("UTF-8");

    private static String bytesToHex(byte[] b) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            buf.append(hexDigit[(b[i] >> 4) & 0x0f]).append(hexDigit[b[i] & 0x0f]);
        }
        return buf.toString();
    }
    private final static char[] hexDigit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

}