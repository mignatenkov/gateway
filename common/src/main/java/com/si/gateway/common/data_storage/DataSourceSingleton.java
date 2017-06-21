package com.si.gateway.common.data_storage;

import com.mongodb.*;
import com.si.gateway.common.properties_reader.PropertiesReaderSingleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration.CompositeConfiguration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class DataSourceSingleton {

    private String dbHost;
    private String dbPort;
    private String dbLogin;
    private String dbPassword;

    private DB db;
    private String dbName;
    private String dbDataCollectionName;

    private static DataSourceSingleton instance = new DataSourceSingleton();

    private final ReentrantLock lock = new ReentrantLock();
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd");
    private static final SimpleDateFormat simpleDateFormatMillisecs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static DataSourceSingleton getInstance() throws Exception {
        return instance;
    }

    private DataSourceSingleton() {
        CompositeConfiguration config = PropertiesReaderSingleton.getInstance().getConfig();
        dbHost = config.getString("mongodb.host");
        dbPort = config.getString("mongodb.port");

        dbName = config.getString("mongodb.name");
        dbDataCollectionName = config.getString("mongodb.collection_name");

        try {
            MongoClient mongoClient = new MongoClient(dbHost, Integer.parseInt(dbPort));
            db = mongoClient.getDB(dbName);

            if (config.getBoolean("mongodb.auth.enabled")) {
                dbLogin = config.getString("mongodb.auth.login");
                dbPassword = config.getString("mongodb.auth.password");
                if (!db.authenticate(dbLogin, dbPassword.toCharArray())) {
                    throw new Exception("Incorrect login or password!");
                }
            }
        } catch (Exception e) {
            log.error("Error! Not connected to MongoDB.", e);
            throw new IllegalStateException("Error! Not connected to MongoDB.", e);
        }

        log.debug("DataSourceSingleton initialized!");
    }

    public void putData(DBObject obj) {
        DBCollection table = db.getCollection(dbDataCollectionName);
        if (table == null) {
            return;
        }
        log.debug("Data to be put: " + obj.toString());
        table.save(obj);
    }

    public JSONObject getEarliestData(JSONObject key) {
        JSONObject retVal = new JSONObject();
        lock.lock();
        try {
            DBCollection table = db.getCollection(dbDataCollectionName);

            DBObject criteria = new BasicDBObject();
            if (!key.isEmpty()) {
                criteria.putAll(key);
            }
            criteria.put("status", DataStatus.AVAILABLE.toString());
            DBObject sort = new BasicDBObject();
            sort.put("create_time", 1); // ascending order
            log.debug("criteria: " + criteria.toString() + " asc order");

            DBCursor cursor = table.find(criteria).sort(sort);
            if ((cursor != null) && cursor.hasNext()) {
                DBObject obj = cursor.next();
                updateStatusForEntry(obj.get("_id"), DataStatus.PROCESSING.toString());
                for (String k : obj.keySet()) {
                    retVal.put(k, obj.get(k));
                }
            }
        } finally {
            lock.unlock();
        }

        log.debug("EarliestData found: " + retVal.toString());
        return retVal;
    }

    public JSONArray getUndeliveredDataIdList(JSONObject key) {
        JSONArray retVal = new JSONArray();
        DBCollection table = db.getCollection(dbDataCollectionName);

        DBObject criteria = new BasicDBObject();
        if (!key.isEmpty()) {
            criteria.putAll(key);
        }
        criteria.put("status", DataStatus.SENT.toString());
        log.debug("criteria: " + criteria.toString());

        DBCursor cursor = table.find(criteria);
        while ((cursor != null) && cursor.hasNext()) {
            retVal.add(cursor.next().get("_id"));
        }

        log.debug("UndeliveredDataIds found: " + retVal.toString());
        return retVal;
    }

    public JSONObject getUndeliveredData(Object id) {
        JSONObject retVal = new JSONObject();
        DBCollection table = db.getCollection(dbDataCollectionName);

        DBObject criteria = new BasicDBObject();
        criteria.put("_id", id);
        criteria.put("status", DataStatus.SENT.toString());
        log.debug("criteria: " + criteria.toString());

        DBCursor cursor = table.find(criteria);
        if ((cursor != null) && cursor.hasNext()) {
            DBObject obj = cursor.next();
            for (String k : obj.keySet()) {
                retVal.put(k, obj.get(k));
            }
        }

        log.debug("UndeliveredData found: " + retVal.toString());
        return retVal;
    }

    public void updateStatusForEntry(Object id, String newStatus) {
        DBCollection table = db.getCollection(dbDataCollectionName);
        DBObject updateObj = new BasicDBObject().append("status", newStatus).append("last_update_time", new Date().getTime());
        if (DataStatus.SENT.toString().equalsIgnoreCase(newStatus)) {
            updateObj.put("send_time", new Date().getTime());
        }
        table.update(new BasicDBObject().append("_id", id), new BasicDBObject().append("$set", updateObj));
    }

    public JSONArray getStatistiscForNdays(int numDays, DBObject criteria) {
        log.debug("getStatistiscForNdays: "+ numDays +" days for "+ criteria.toString());
        JSONArray retVal = new JSONArray();
        DBCollection table = db.getCollection(dbDataCollectionName);

        if (criteria == null) {
            criteria = new BasicDBObject();
        }
        criteria.put("system", new BasicDBObject().append("$exists", true));

        long pageTimeLongitude = 1000*60*60*24;
        Date startDate = new Date();
        startDate.setHours(0);
        startDate.setMinutes(0);
        startDate.setSeconds(0);
        Date endDate = new Date();

        for (int i = 0; i < numDays; i++) {
            JSONObject res = new JSONObject();
            res.put("create_time", simpleDateFormat.format(startDate));

            criteria.put("create_time", new BasicDBObject().append("$exists", true)
                    .append("$gt", startDate.getTime()).append("$lte", endDate.getTime()));
            DBCursor cursor = table
                    .find(criteria, new BasicDBObject().append("system", 1).append("create_time", 1).append("_id", 1))
                    .sort(new BasicDBObject().append("create_time", 1));

            while ((cursor != null) && cursor.hasNext()) {
                DBObject o = cursor.next();
                String systemName = (String) o.get("system");
                Long cnt = 1l;
                if (res.containsKey(systemName)) {
                    cnt = (Long) res.get(systemName) + 1;
                }
                res.put(systemName, cnt);
            }

            retVal.add(res);

            endDate = startDate;
            startDate = new Date(startDate.getTime() - pageTimeLongitude);
        }
        log.debug("retVal: " + retVal);

        return retVal;
    }

    public JSONObject getStatistiscForDay(Date day, DBObject criteria) {
        log.debug("getStatistiscForDay: "+ criteria.toString() +" for "+ day.toString());
        JSONObject retVal = new JSONObject();
        DBCollection table = db.getCollection(dbDataCollectionName);

        if (criteria == null) {
            criteria = new BasicDBObject();
        }
        criteria.put("system", new BasicDBObject().append("$exists", true));

        long pageTimeLongitude = 1000*60*60*24;
        Date startDate = day;
        startDate.setHours(0);
        startDate.setMinutes(0);
        startDate.setSeconds(0);
        Date endDate = new Date(day.getTime() + pageTimeLongitude);

        retVal.put("create_time", simpleDateFormat.format(startDate));

        criteria.put("create_time", new BasicDBObject().append("$exists", true)
                .append("$gt", startDate.getTime()).append("$lte", endDate.getTime()));
        DBCursor cursor = table
                .find(criteria, new BasicDBObject().append("system", 1).append("create_time", 1).append("_id", 1))
                .sort(new BasicDBObject().append("create_time", 1));

        while ((cursor != null) && cursor.hasNext()) {
            DBObject o = cursor.next();
            String systemName = (String) o.get("system");
            Long cnt = 1l;
            if (retVal.containsKey(systemName)) {
                cnt = (Long) retVal.get(systemName) + 1;
            }
            retVal.put(systemName, cnt);
        }

        log.debug("retVal: " + retVal);

        return retVal;
    }

    public String getFullStatistiscForHour(Date date, DBObject criteria) {
        log.debug("getFullStatistiscForHour for "+ date.toString());
        StringBuilder retVal = new StringBuilder();
        DBCollection table = db.getCollection(dbDataCollectionName);

        if (criteria == null) {
            criteria = new BasicDBObject();
        }

        long pageTime = 1000*60*60;
        Date startDate = date;
        Date endDate = new Date(date.getTime() + pageTime);
        criteria.put("create_time", new BasicDBObject().append("$exists", true)
                .append("$gt", startDate.getTime()).append("$lte", endDate.getTime()));

        DBCursor cursor = table.find(criteria
                                    , new BasicDBObject()
                                            .append("system", 1)
                                            .append("_id", 1)
                                            .append("metadata.filename", 1)
                                            .append("create_time", 1)
                                            .append("send_time", 1)
                                            .append("last_update_time", 1)
                                            .append("status", 1)
        );

        while ((cursor != null) && cursor.hasNext()) {
            DBObject o = cursor.next();
            retVal.append(String.valueOf(o.get("system"))).append(",");
            retVal.append(String.valueOf(o.get("_id"))).append(",");
            if (o.get("metadata") != null) {
                retVal.append(String.valueOf(((DBObject) o.get("metadata")).get("filename")));
            } else {
                retVal.append("null");
            }
            retVal.append(",");
            if (o.get("create_time") != null) {
                retVal.append(simpleDateFormatMillisecs.format(new Date((Long) o.get("create_time"))).toString()).append(",");
            } else retVal.append("null,");
            if (o.get("send_time") != null) {
                retVal.append(simpleDateFormatMillisecs.format( new Date((Long) o.get("send_time")) ).toString()).append(",");
            } else retVal.append("null,");
            if (o.get("last_update_time") != null) {
                retVal.append(simpleDateFormatMillisecs.format( new Date((Long) o.get("last_update_time")) ).toString()).append(",");
            } else retVal.append("null,");
            retVal.append(String.valueOf(o.get("status"))).append("\n");
        }

        return retVal.toString();
    }

}
