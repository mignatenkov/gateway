package com.si.gateway.router.rest;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.si.gateway.common.data_storage.DataSourceSingleton;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

@Slf4j
@Path("/api/v1/statistics/{app}/")
public class RestStatisticsService {

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @GET
    @Path("/days/{numDays}")
    @Produces(MediaType.TEXT_HTML + ";charset=utf-8")
    public Response reqNDaysStatistics(@PathParam("app") String pathParamApp,
                                       @PathParam("numDays") Integer pathParamNumDays) throws Exception {
        if (pathParamNumDays > 10) pathParamNumDays = 10;
        DBObject criteria = new BasicDBObject().append("app", pathParamApp);

        StringBuffer retVal = new StringBuffer();
        retVal.append("<table border=\"1\">");

        JSONArray res = DataSourceSingleton.getInstance().getStatistiscForNdays(pathParamNumDays, criteria);
        for (Object o : res.toArray()) {
            JSONObject date = (JSONObject) o;
            retVal.append("<tr><td>").append(date.get("create_time")).append("</td><td><table>");
            for (String key : (Set<String>) date.keySet()) {
                if (!key.equalsIgnoreCase("create_time")) {
                    retVal.append("<tr><td>").append(key).append(": ").append(date.get(key)).append("</td></tr>");
                }
            }
            retVal.append("</table></td></tr>");
        }
        retVal.append("</table>");

        return Response.status(HttpStatus.OK_200).entity(retVal.toString()).build();
    }

    @GET
    @Path("/date/{day}")
    @Produces(MediaType.TEXT_HTML + ";charset=utf-8")
    public Response reqDayStatistics(@PathParam("app") String pathParamApp,
                                     @PathParam("day") String pathParamDay) throws Exception {
        try {
            Date date = simpleDateFormat.parse(pathParamDay);
            log.debug("date: "+ date.toString());
        } catch (ParseException pe) {
            log.error("pathParamDay invalid", pe);
            return Response.status(HttpStatus.BAD_REQUEST_400).entity("Incorrect day value!").build();
        }

        DBObject criteria = new BasicDBObject().append("app", pathParamApp);

        StringBuffer retVal = new StringBuffer();
        retVal.append("<table border=\"1\">");

        JSONObject res = DataSourceSingleton.getInstance().getStatistiscForDay(simpleDateFormat.parse(pathParamDay), criteria);
        retVal.append("<tr><td>").append(res.get("create_time")).append("</td><td><table>");
        for (String key : (Set<String>) res.keySet()) {
            if (!key.equalsIgnoreCase("create_time")) {
                retVal.append("<tr><td>").append(key).append(": ").append(res.get(key)).append("</td></tr>");
            }
        }
        retVal.append("</table></td></tr>");
        retVal.append("</table>");

        return Response.status(HttpStatus.OK_200).entity(retVal.toString()).build();
    }

    @GET
    @Path("/report")
    @Produces({ "application/ms-excel"})
    public Response getReport(@PathParam("app") final String pathParamApp,
                              @QueryParam("upToDate") final String queryParamUpToDate) throws Exception {
        return Response
                .status(HttpStatus.OK_200)
                .header("Content-Disposition", "attachment; filename=\"report.csv\"")
                .entity(new StreamingOutput() {
                    public void write(OutputStream output) throws IOException, WebApplicationException {
                        try {
                            StringBuilder retVal = new StringBuilder("system,_id,filename,create_time,send_time,last_update_time,status\n");
                            DBObject criteria = new BasicDBObject().append("app", pathParamApp);

                            Date endDate = new Date();
                            if ((queryParamUpToDate != null) && !queryParamUpToDate.isEmpty()) {
                                endDate = simpleDateFormat.parse(queryParamUpToDate);
                            }

                            for (long time = 1472504400000l; time < endDate.getTime(); time += 1000 * 60 * 60) {
                                retVal.append(DataSourceSingleton.getInstance().getFullStatistiscForHour(new Date(time), criteria));
                            }

                            output.write(retVal.toString().getBytes());
                        } catch (Exception e) {
                            throw new WebApplicationException(e);
                        }
                    }
                }).build();
    }

}
