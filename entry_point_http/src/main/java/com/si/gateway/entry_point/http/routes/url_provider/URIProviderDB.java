package com.si.gateway.entry_point.http.routes.url_provider;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.si.gateway.common.properties_reader.PropertiesReaderSingleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration.CompositeConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * The implementation for uri provider from MongoDB.
 */
@Slf4j
public class URIProviderDB implements IURIProvider {

    private String dbHost;
    private String dbPort;

    private DB db;
    private String dbName = "entry_point_http_collections";
    private String dbConfigCollectionNameProcessor1 = "uri_list_processor1";
    private String dbConfigCollectionNameProcessor2 = "uri_list_processor2";

    /**
     * Instantiates a new uri provider.
     *
     * @throws Exception the exception
     */
    public URIProviderDB() throws Exception {
        CompositeConfiguration config = PropertiesReaderSingleton.getInstance().getConfig();
        dbHost = config.getString("entry_point_http.config_db.host");
        dbPort = config.getString("entry_point_http.config_db.port");

        dbName = config.getString("entry_point_http.config_db.name");
        dbConfigCollectionNameProcessor1 = config.getString("entry_point_http.config_db.table_uri_list_processor1");
        dbConfigCollectionNameProcessor2 = config.getString("entry_point_http.config_db.table_uri_list_processor2");

        MongoClient mongoClient = new MongoClient(dbHost, Integer.parseInt(dbPort));
        db = mongoClient.getDB(dbName);
    }

    @Override
    public List<String> getUrlsProcessor1() {
        List<String> retVal = new ArrayList<>();
        DBCollection table = db.getCollection(dbConfigCollectionNameProcessor1);
        if (table == null) {
            return retVal;
        }

        DBCursor cursor = table.find();
        if (cursor == null) {
            return retVal;
        }

        while (cursor.hasNext()) {
            retVal.add(cursor.next().toString());
        }

        return retVal;
    }

    @Override
    public List<String> getUrlsProcessor2() {
        List<String> retVal = new ArrayList<>();
        DBCollection table = db.getCollection(dbConfigCollectionNameProcessor2);
        if (table == null) {
            return retVal;
        }

        DBCursor cursor = table.find();
        if (cursor == null) {
            return retVal;
        }

        while (cursor.hasNext()) {
            retVal.add(cursor.next().toString());
        }

        return retVal;
    }

}
