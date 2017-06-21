package com.si.gateway.entry_point.ftp.routes.dirs_provider;

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
 * The implementation for dirs provider from MongoDB.
 */
@Slf4j
public class DirsProviderDB implements IDirsProvider {

    private String dbHost;
    private String dbPort;

    private DB db;
    private String dbName;
    private String dbConfigCollectionName;

    /**
     * Instantiates a new dirs provider.
     *
     * @throws Exception the exception
     */
    public DirsProviderDB() throws Exception {
        CompositeConfiguration config = PropertiesReaderSingleton.getInstance().getConfig();

        dbHost = config.getString("entry_point_ftp.config_db.host");
        dbPort = config.getString("entry_point_ftp.config_db.port");

        dbName = config.getString("entry_point_ftp.config_db.name");
        dbConfigCollectionName = config.getString("entry_point_ftp.config_db.table");

        MongoClient mongoClient = new MongoClient(dbHost, Integer.parseInt(dbPort));
        db = mongoClient.getDB(dbName);
    }

    @Override
    public List<String> getDirs() {
        List<String> retVal = new ArrayList<>();
        DBCollection table = db.getCollection(dbConfigCollectionName);
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
