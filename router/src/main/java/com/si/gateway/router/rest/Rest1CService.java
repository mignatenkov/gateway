package com.si.gateway.router.rest;

import com.mongodb.DBObject;
import com.si.gateway.common.data_storage.DataSourceSingleton;
import com.si.gateway.common.data_storage.DataStatus;
import com.si.gateway.common.properties_reader.PropertiesReaderSingleton;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.eclipse.jetty.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

@Slf4j
@Path("/api/v1/1c/{app}/")
public class Rest1CService {

    private static final String PARAMS_DELIMITER = "\n";

    @POST
    @Path("/reqData")
    @Produces(MediaType.TEXT_PLAIN)
    public void reqData(@Suspended final AsyncResponse asyncResponse,
                        @PathParam("app") final String appParam,
                        final String params) throws Exception {
        log.debug("====================== reqData ======================");

        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject earliestData = null;
                try {
                    JSONObject jsonObject = new JSONObject();
                    if ((params != null) && !params.isEmpty() && params.contains(PARAMS_DELIMITER) && params.contains(":")) {
                        for (String paramPair : params.split(PARAMS_DELIMITER)) {
                            if (paramPair.split(":")[0].trim().equalsIgnoreCase("system")) {
                                jsonObject.put(paramPair.split(":")[0].trim(), paramPair.split(":")[1].trim());
                            } else {
                                jsonObject.put("metadata." + paramPair.split(":")[0].trim(), paramPair.split(":")[1].trim());
                            }
                        }
                        log.debug("/reqData : "+ jsonObject.toString());
                    } else {
                        log.debug("/reqData params are empty");
                    }
                    jsonObject.put("app", appParam);

                    earliestData = DataSourceSingleton.getInstance().getEarliestData(jsonObject);
                    while ((earliestData == null) || earliestData.isEmpty()) {
                        Thread.sleep(PropertiesReaderSingleton.getInstance().getConfig().getInt("core.data_check_delay", 250)); // default = 250 millisecs
                        earliestData = DataSourceSingleton.getInstance().getEarliestData(jsonObject);
                    }

                    StringBuffer retVal = new StringBuffer();
                    DBObject metadata = (DBObject) earliestData.get("metadata");
                    metadata.removeField("id");
                    metadata.removeField("system");
                    retVal.append("id: ").append(earliestData.get("_id")).append("\n");
                    retVal.append("system: ").append(earliestData.get("system")).append("\n");
                    for (String key : metadata.keySet()) {
                        retVal.append(key).append(": ").append(metadata.get(key)).append("\n");
                    }
                    retVal.append("\n").append(earliestData.get("data"));

                    asyncResponse.resume(Response.status(HttpStatus.OK_200).entity(retVal.toString()).build());
                } catch (InterruptedException ie) {
                    log.debug("Thread had been interrupted!");
                    // keep calm and carry on
                } catch (Exception e) {
                    asyncResponse.resume(Response.status(HttpStatus.BAD_REQUEST_400).build());
                    log.error("Unexpected reqData error: ", e);
                    if ((earliestData != null) && !earliestData.isEmpty()) {
                        try {
                            DataSourceSingleton.getInstance().updateStatusForEntry(
                                    earliestData.get("_id"), DataStatus.AVAILABLE.toString());
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                }

                try {
                    DataSourceSingleton.getInstance().updateStatusForEntry(
                            earliestData.get("_id"), DataStatus.SENT.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        asyncResponse.setTimeout(
                PropertiesReaderSingleton.getInstance().getConfig().getInt("longpoll.timeout"), TimeUnit.SECONDS);

        asyncResponse.setTimeoutHandler(new TimeoutHandler() {
            @Override
            public void handleTimeout(AsyncResponse asyncResp) {
                log.debug("Operation time out.");
                t.interrupt();
                JSONObject jsonTimeout = new JSONObject();
                jsonTimeout.put("errorMessage", "Operation time out");
                asyncResp.resume(Response.status(HttpStatus.REQUEST_TIMEOUT_408).entity(jsonTimeout.toString()).build());
            }
        });

        t.start();
    }

    @POST
    @Path("/ackData")
    @Produces(MediaType.TEXT_PLAIN)
    public Response ackData(String params) throws Exception {
        log.debug("====================== ackData ======================");

        if ((params == null) || params.isEmpty()) {
            log.error("/ackData params are empty");
            return Response.status(HttpStatus.BAD_REQUEST_400).build();
        }
        log.debug("/ackData : "+ params.toString());

        StringBuffer retVal = new StringBuffer();
        String delim = "";
        if (params.contains(PARAMS_DELIMITER)) {
            for (String id : params.split(PARAMS_DELIMITER)) {
                ObjectId objectId;
                try {
                    objectId = new ObjectId(id.trim());
                } catch (Exception e) {
                    log.error("Invalid id detected: \"" + id + "\". Skipped...");
                    continue;
                }
                DataSourceSingleton.getInstance().updateStatusForEntry(objectId, DataStatus.DELIVERED.toString());
                retVal.append(delim).append(id.trim());
                delim = PARAMS_DELIMITER;
            }
        } else {
            ObjectId objectId;
            try {
                objectId = new ObjectId(params.trim());
            } catch (Exception e) {
                log.error("Invalid id detected: \"" + params + "\"");
                return Response.status(HttpStatus.NOT_FOUND_404).build();
            }
            DataSourceSingleton.getInstance().updateStatusForEntry(objectId, DataStatus.DELIVERED.toString());
            retVal.append(params.trim());
        }

        return Response.status(HttpStatus.OK_200).entity(retVal.toString()).build();
    }

    @POST
    @Path("/reqSentList")
    @Produces(MediaType.TEXT_PLAIN)
    public Response reqSentList(@PathParam("app") String appParam, String params) throws Exception {
        log.debug("====================== reqSentList ======================");

        JSONObject jsonObject = new JSONObject();
        if ((params != null) && !params.isEmpty() && params.contains(PARAMS_DELIMITER) && params.contains(":")) {
            String[] paramsArr = params.split(PARAMS_DELIMITER);
            for (String paramPair : paramsArr) {
                jsonObject.put("metadata." + paramPair.split(":")[0].trim(), paramPair.split(":")[1].trim());
            }
            log.debug("/reqSentList : "+ jsonObject.toString());
        } else {
            log.debug("/reqSentList : no params");
        }
        jsonObject.put("app", appParam);

        JSONArray undeliveredDataIdList = DataSourceSingleton.getInstance().getUndeliveredDataIdList(jsonObject);
        if (!undeliveredDataIdList.isEmpty()) {
            String retVal = undeliveredDataIdList.toString();
            retVal = retVal.substring(1);
            retVal = retVal.substring(0, retVal.length()-1);
            retVal = retVal.replaceAll(",", PARAMS_DELIMITER);
            return Response.status(HttpStatus.OK_200).entity(retVal).build();
        } else {
            return Response.status(HttpStatus.OK_200).build();
        }
    }

    @POST
    @Path("/reqSentData")
    @Produces(MediaType.TEXT_PLAIN)
    public Response reqSentData(String params) throws Exception {
        log.debug("====================== reqSentData ======================");

        if ((params == null) || params.isEmpty()) {
            log.error("/reqSentData params are empty");
            return Response.status(HttpStatus.BAD_REQUEST_400).build();
        }
        log.debug("/reqSentData : " + params.toString());

        ObjectId id;
        try {
            id = new ObjectId(params.trim());
        } catch (Exception e) {
            log.error("Invalid id detected: \"" + params + "\"");
            return Response.status(HttpStatus.NOT_FOUND_404).build();
        }

        JSONObject undeliveredData = DataSourceSingleton.getInstance().getUndeliveredData(id);
        if (!undeliveredData.isEmpty()) {
            StringBuffer retVal = new StringBuffer();
            DBObject metadata = (DBObject) undeliveredData.get("metadata");
            metadata.removeField("id");
            metadata.removeField("system");
            retVal.append("id: ").append(undeliveredData.get("_id")).append("\n");
            retVal.append("system: ").append(undeliveredData.get("system")).append("\n");
            for (String key : metadata.keySet()) {
                retVal.append(key).append(": ").append(metadata.get(key)).append("\n");
            }
            retVal.append("\n").append((String) undeliveredData.get("data"));

            return Response.status(HttpStatus.OK_200).entity(retVal.toString()).build();
        } else {
            return Response.status(HttpStatus.OK_200).build();
        }
    }

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public Response versionCheck() throws Exception {
        return Response.status(HttpStatus.OK_200).entity("router v.1.0: on-line").build();
    }

}
