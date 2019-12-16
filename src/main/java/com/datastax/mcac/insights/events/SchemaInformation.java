package com.datastax.mcac.insights.events;

import com.datastax.mcac.insights.Insight;
import com.datastax.mcac.insights.InsightMetadata;
import com.datastax.mcac.utils.JacksonUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import org.apache.cassandra.cql3.QueryProcessor;
import org.apache.cassandra.cql3.UntypedResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
public class SchemaInformation extends Insight
{
    private static final String NAME = "oss.insights.events.schema_information";
    private static final String MAPPING_VERSION = "oss-node-config-v1";

    public static final String KEYSPACES = "keyspaces";
    public static final String TABLES = "tables";
    public static final String COLUMNS = "columns";
    public static final String DROPPED_COLUMNS = "dropped_columns";
    public static final String TRIGGERS = "triggers";
    public static final String VIEWS = "views";
    public static final String TYPES = "types";
    public static final String FUNCTIONS = "functions";
    public static final String AGGREGATES = "aggregates";
    public static final String INDEXES = "indexes";

    private static final Logger logger = LoggerFactory.getLogger(SchemaInformation.class);

    @JsonCreator
    public SchemaInformation(
            @JsonProperty("metadata") InsightMetadata metadata,
            @JsonProperty("data") Data data
    )
    {
        super(
                metadata,
                data
        );
    }

    public SchemaInformation(Data data)
    {
        super(new InsightMetadata(
                NAME,
                Optional.of(System.currentTimeMillis()),
                Optional.empty(),
                Optional.of(InsightMetadata.InsightType.EVENT),
                Optional.of(MAPPING_VERSION)
        ), data);
    }

    public SchemaInformation()
    {
        this(new Data());
    }

    public Data getData()
    {
        return (Data) this.data;
    }

    public static class Data
    {
        private static final Object SCHEMA_KEYSPACE_NAME = "system_schema";
        @JsonRawValue
        @JsonProperty("keyspaces")
        public final String keyspaces;

        @JsonRawValue
        @JsonProperty("tables")
        public final String tables;

        @JsonRawValue
        @JsonProperty("columns")
        public final String columns;

        @JsonRawValue
        @JsonProperty("dropped_columns")
        public final String dropped_columns;

        @JsonRawValue
        @JsonProperty("triggers")
        public final String triggers;

        @JsonRawValue
        @JsonProperty("views")
        public final String views;

        @JsonRawValue
        @JsonProperty("types")
        public final String types;

        @JsonRawValue
        @JsonProperty("functions")
        public final String functions;

        @JsonRawValue
        @JsonProperty("aggregates")
        public final String aggregates;

        @JsonRawValue
        @JsonProperty("indexes")
        public final String indexes;


        @VisibleForTesting
        @JsonCreator
        public Data(
                @JsonProperty("keyspaces") Object keyspaces,
                @JsonProperty("tables") Object tables,
                @JsonProperty("columns") Object columns,
                @JsonProperty("dropped_columns")  Object dropped_columns,
                @JsonProperty("triggers")  Object triggers,
                @JsonProperty("views") Object views,
                @JsonProperty("types") Object types,
                @JsonProperty("functions") Object functions,
                @JsonProperty("aggregates") Object aggregates,
                @JsonProperty("indexes") Object indexes)
        {
            try
            {
                this.keyspaces = JacksonUtil.writeValueAsString(keyspaces);
                this.tables = JacksonUtil.writeValueAsString(tables);
                this.columns = JacksonUtil.writeValueAsString(columns);
                this.dropped_columns = JacksonUtil.writeValueAsString(dropped_columns);
                this.triggers = JacksonUtil.writeValueAsString(triggers);
                this.views = JacksonUtil.writeValueAsString(views);
                this.types = JacksonUtil.writeValueAsString(types);
                this.functions = JacksonUtil.writeValueAsString(functions);
                this.aggregates = JacksonUtil.writeValueAsString(aggregates);
                this.indexes = JacksonUtil.writeValueAsString(indexes);
            }
            catch (JacksonUtil.JacksonUtilException e)
            {
                throw new IllegalArgumentException("Error creating schema info", e);
            }
        }

        private Data()
        {
            this.keyspaces = tableToJson(KEYSPACES);
            this.tables = tableToJson(TABLES);
            this.columns = tableToJson(COLUMNS);
            this.dropped_columns = tableToJson(DROPPED_COLUMNS);
            this.triggers = tableToJson(TRIGGERS);
            this.views = tableToJson(VIEWS);
            this.types = tableToJson(TYPES);
            this.functions = tableToJson(FUNCTIONS);
            this.aggregates = tableToJson(AGGREGATES);
            this.indexes = tableToJson(INDEXES);
        }


        private String tableToJson(String table)
        {
            String cql = String.format("SELECT JSON * from %s.%s", SCHEMA_KEYSPACE_NAME, table);
            String json = "[";
            try
            {
                UntypedResultSet rs = QueryProcessor.executeInternal(cql);
                List<String> rows = new ArrayList<>(rs.size());
                for (UntypedResultSet.Row row : rs)
                {
                    rows.add(row.getString("[json]"));
                }

                json += Joiner.on(",").join(rows);
            }
            catch (Exception e)
            {
                logger.warn("Error fetching schema information on {}.{}", SCHEMA_KEYSPACE_NAME, table, e);
            }

            json += "]";

            return json;
        }
    }
}
