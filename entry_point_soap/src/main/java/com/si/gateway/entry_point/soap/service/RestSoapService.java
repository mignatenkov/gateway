package com.si.gateway.entry_point.soap.service;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.si.gateway.common.data_storage.DataSourceSingleton;
import com.si.gateway.common.data_storage.DataStatus;
import com.si.gateway.common.processor.IDataProcessor;
import com.si.gateway.common.properties_reader.PropertiesReaderSingleton;
import com.si.gateway.common.utils.EncodingConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration.CompositeConfiguration;
import org.eclipse.jetty.http.HttpStatus;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.Date;

@Slf4j
@Path("/{app}/")
public class RestSoapService {

    @GET
    @Path("/{systemPathParam}")
    @Produces(MediaType.APPLICATION_XML)
    public Response wsdlRequest(@PathParam("systemPathParam") String systemPathParam,
                                @PathParam("app") String appPathParam,
                                @QueryParam("wsdl") String wsdlQueryParam) throws Exception {
        log.debug("====================== wsdlRequest at "+ systemPathParam +" ======================");

        if (wsdlQueryParam == null) {
            log.debug("No wsdl query param! Sending 404...");
            return Response.status(HttpStatus.NOT_FOUND_404).build();
        }

        CompositeConfiguration config = PropertiesReaderSingleton.getInstance().getConfig();

        return Response.status(HttpStatus.OK_200).entity(
                readResourceFile("soap_templates/WsdlTemplate.xml")
                        .replace("@HOST@", config.getString("entry_point_soap.external_host"))
                        .replace("@PORT@", config.getString("entry_point_soap.external_port"))
                        .replace("@URL@", appPathParam + "/" + systemPathParam)
        ).build();
    }

    @POST
    @Path("/{systemPathParam}")
    @Produces(MediaType.TEXT_XML)
    public Response callMethod(@PathParam("systemPathParam") String systemPathParam,
                               @PathParam("app") String appPathParam,
                               String params) throws Exception {
        log.debug("====================== callMethod at "+ systemPathParam +" ======================");

        try {
            DBObject retVal = new BasicDBObject();
            JSONObject metaData = new JSONObject();
            String filename = "";
            String hash = "";
            String content = "";

            if ((params != null) && !params.isEmpty()) {
                log.debug("EntryPointSOAP: params: " + params);
                params = params.replaceAll("[^\\x20-\\x7e]", "").trim(); // for not accurate encoding fix

                Document doc = XMLUtilsSingleton.getInstance().getDocumentBuilder().parse(new InputSource(new StringReader(params)));
                filename = doc.getElementsByTagName("filename").item(0).getTextContent();
                hash = doc.getElementsByTagName("hash").item(0).getTextContent();
                content = doc.getElementsByTagName("content").item(0).getTextContent();

                try {
                    IDataProcessor processor = (IDataProcessor) Class.forName("com.si.gateway.entry_point.soap.processor." + systemPathParam).newInstance();
                    metaData = processor.process(content);
                } catch (ClassNotFoundException cnfe) {
                    // no class found. Keep calm and carry on...
                } catch (Exception e) {
                    log.debug("Error on processing content", e);
                }

                if ((filename != null) && !filename.isEmpty() &&
                        (hash != null) && !hash.isEmpty() &&
                        (content != null) && !content.isEmpty()) {
                    metaData.put("filename", filename);
                    metaData.put("hash", hash);
                    retVal.put("app", appPathParam);
                    retVal.put("system", EncodingConverter.convertFromISOtoUTF8(systemPathParam));
                    retVal.put("create_time", new Date().getTime());
                    retVal.put("status", DataStatus.AVAILABLE.toString());
                    retVal.put("metadata", metaData);
                    retVal.put("data", content);

                    DataSourceSingleton.getInstance().putData(retVal);
                } else {
                    log.debug("EntryPointSOAP: One of params not found!");
                    return Response.status(HttpStatus.OK_200).entity(readResourceFile("soap_templates/BadDataTemplate.xml")).build();
                }
            } else {
                log.debug("EntryPointSOAP: Input contains no params!");
                return Response.status(HttpStatus.OK_200).entity(readResourceFile("soap_templates/BadDataTemplate.xml")).build();
            }

            return Response.status(HttpStatus.OK_200).entity(readResourceFile("soap_templates/OkTemplate.xml").replace("@FILENAME@", filename)).build();
        } catch (Exception e) {
            log.error("Unexpected error at callMethod!", e);
            return Response.status(HttpStatus.OK_200).entity(readResourceFile("soap_templates/ErrorTemplate.xml")).build();
        }
    }

    private String readResourceFile(String filePath) throws IOException {
        StringBuffer sb = new StringBuffer();
        InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                sb.append(sCurrentLine).append("\n");
            }
        }
        return sb.toString().replaceAll("\r", "");
    }

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public Response versionCheck() throws Exception {
        return Response.status(HttpStatus.OK_200).entity("entry_point_soap v.1.0: on-line").build();
    }

}
