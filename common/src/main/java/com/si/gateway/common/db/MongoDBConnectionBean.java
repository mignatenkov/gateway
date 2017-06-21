package com.si.gateway.common.db;

import com.mongodb.Mongo;
import com.si.gateway.common.properties_reader.PropertiesReaderSingleton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.UnknownHostException;

@Slf4j
@Component("mongoDBConnection")
public class MongoDBConnectionBean extends Mongo {

    public MongoDBConnectionBean() throws UnknownHostException {
        super(PropertiesReaderSingleton.getInstance().getConfig().getString("mongodb.host"),
                PropertiesReaderSingleton.getInstance().getConfig().getInt("mongodb.port"));
    }

}
