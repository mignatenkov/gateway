package com.si.gateway.entry_point.http.routes;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.si.gateway.common.data_storage.DataStatus;
import com.si.gateway.common.properties_reader.PropertiesReaderSingleton;
import com.si.gateway.common.utils.EncodingConverter;
import com.si.gateway.entry_point.http.routes.url_provider.IURIProvider;
import com.si.gateway.entry_point.http.routes.url_provider.URIProviderDB;
import com.si.gateway.entry_point.http.routes.url_provider.URIProviderProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jetty.JettyHttpComponent;
import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.commons.configuration.CompositeConfiguration;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

/**
 * The EntryPointHTTP routes builder.
 * Processor are going to translate input data into inner format.
 */
@Slf4j
@Component(value = "routeEntryPointHTTP")
public class MainRouteBuilder extends RouteBuilder {

    private static final String STOP_EXCHANGE = "stop_exchange";

    private Processor processor = new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
            String input = exchange.getIn().getBody(String.class);
            log.info("EntryPointHTTP request from " +
                    String.valueOf(((HttpServletRequest) exchange.getIn().getHeader(Exchange.HTTP_SERVLET_REQUEST)).getRemoteHost()) +
                    "(" + String.valueOf(((HttpServletRequest) exchange.getIn().getHeader(Exchange.HTTP_SERVLET_REQUEST)).getRemoteAddr()) +
                    ":" + String.valueOf(((HttpServletRequest) exchange.getIn().getHeader(Exchange.HTTP_SERVLET_REQUEST)).getRemotePort()) +
                    " forwarded for " + String.valueOf(((HttpServletRequest) exchange.getIn().getHeader(Exchange.HTTP_SERVLET_REQUEST)).getHeader("X-FORWARDED-FOR")) +
                    ")");
            if (log.isDebugEnabled()) {
                log.debug("EntryPointHTTP request headers");
                for (String headerKey : exchange.getIn().getHeaders().keySet()) {
                    log.debug("  " + headerKey + " : " + String.valueOf(exchange.getIn().getHeader(headerKey)));
                }
                log.debug("  EntryPointHTTP httpRequest headers");
                HttpServletRequest req = (HttpServletRequest) exchange.getIn().getHeader(Exchange.HTTP_SERVLET_REQUEST);
                Enumeration<String> headerKeys = req.getHeaderNames();
                while (headerKeys.hasMoreElements()) {
                    String headerKey = headerKeys.nextElement();
                    log.debug("    " + headerKey + " : " + req.getHeader(headerKey));
                }
                log.debug("EntryPointHTTP input : " + String.valueOf(input));
            }

            if (!"POST".equalsIgnoreCase((String) exchange.getIn().getHeader(Exchange.HTTP_METHOD))) {
                exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 405);
                exchange.getOut().setHeader(STOP_EXCHANGE, "method_not_allowed");
                return;
            }

            if ((input == null) || input.isEmpty()) {
                exchange.getOut().setHeader(STOP_EXCHANGE, "empty_body");
                return;
            }
            input = input.replaceAll("\r", "");

            // Parsing params from request
            DBObject retVal = new BasicDBObject();
            JSONObject metaData = new JSONObject();
            String data = input;
            if ((input != null) && !input.isEmpty() && input.contains("\n\n")) {
                data = input.substring(input.indexOf("\n\n")+2);
                String params = input.substring(0, input.indexOf("\n\n"));
                log.debug("EntryPointHTTP: Input contains params: " + params);
                if ((params != null) && !params.isEmpty() && params.contains("\n")) {
                    for (String paramPair : params.split("\n")) {
                        metaData.put(paramPair.split(":")[0], paramPair.split(":")[1].trim());
                    }
                }
            } else {
                log.debug("EntryPointHTTP: Input contains no params!");
            }

            String uri = (String) exchange.getIn().getHeader("CamelHttpUri");
            if (uri.contains("/")) {
                String[] pathParams = uri.split("/");
                retVal.put("app", pathParams[1]);
                retVal.put("system", EncodingConverter.convertFromISOtoUTF8(pathParams[2]));
            } else {
                retVal.put("app", uri);
                retVal.put("system", EncodingConverter.convertFromISOtoUTF8(uri));
            }
            retVal.put("create_time", new Date().getTime());
            retVal.put("status", DataStatus.AVAILABLE.toString());
            retVal.put("metadata", metaData);
            retVal.put("data", data);

            log.debug("EntryPointHTTP return : " + retVal.toString());
            exchange.getOut().setBody(retVal);
        }
    };

    private Processor processor2 = new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
            log.info("EntryPointHTTP request from " +
                    String.valueOf(((HttpServletRequest) exchange.getIn().getHeader(Exchange.HTTP_SERVLET_REQUEST)).getRemoteHost()) +
                    "(" + String.valueOf(((HttpServletRequest) exchange.getIn().getHeader(Exchange.HTTP_SERVLET_REQUEST)).getRemoteAddr()) +
                    ":" + String.valueOf(((HttpServletRequest) exchange.getIn().getHeader(Exchange.HTTP_SERVLET_REQUEST)).getRemotePort()) +
                    " forwarded for " + String.valueOf(((HttpServletRequest) exchange.getIn().getHeader(Exchange.HTTP_SERVLET_REQUEST)).getHeader("X-FORWARDED-FOR")) +
                    ")");
            if (log.isDebugEnabled()) {
                log.debug("EntryPointHTTP request headers");
                for (String headerKey : exchange.getIn().getHeaders().keySet()) {
                    log.debug("  " + headerKey + " : " + String.valueOf(exchange.getIn().getHeader(headerKey)));
                }
                log.debug("  EntryPointHTTP httpRequest headers");
                HttpServletRequest req = (HttpServletRequest) exchange.getIn().getHeader(Exchange.HTTP_SERVLET_REQUEST);
                Enumeration<String> headerKeys = req.getHeaderNames();
                while (headerKeys.hasMoreElements()) {
                    String headerKey = headerKeys.nextElement();
                    log.debug("    " + headerKey + " : " + String.valueOf(req.getHeader(headerKey)));
                }
            }

            if (!"POST".equalsIgnoreCase((String) exchange.getIn().getHeader(Exchange.HTTP_METHOD))) {
                exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 405);
                exchange.getOut().setHeader(STOP_EXCHANGE, "method_not_allowed");
                return;
            }

            String input = null;
            DBObject retVal = new BasicDBObject();
            JSONObject metaData = new JSONObject();
            String data = "";
            if ((exchange.getIn().getHeader(Exchange.CONTENT_TYPE) != null) &&
                    ((String) exchange.getIn().getHeader(Exchange.CONTENT_TYPE)).contains("multipart/form-data")
            ) {
                // try to get inout from multipart form
                String filename = exchange.getIn().getHeader("file", String.class);
                if ((filename == null) || filename.isEmpty()) {
                    exchange.getOut().setHeader(STOP_EXCHANGE, "empty_filename_header");
                    return;
                }
                metaData.put("filename", filename);
                BufferedReader br = null;
                StringBuilder sb = new StringBuilder();
                try {
                    br = new BufferedReader(new InputStreamReader(exchange.getIn().getAttachment(filename).getInputStream()));
                    while ((input = br.readLine()) != null) {
                        sb.append(input);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                input = sb.toString();

                log.debug("EntryPointHTTP multipart input : " + String.valueOf(input));
                if ((input == null) || input.isEmpty()) {
                    exchange.getOut().setHeader(STOP_EXCHANGE, "empty_file_attachment");
                    return;
                }
                data = input.replaceAll("\r", "");
                // get tags from request, if any
            } else {
                // try to get input from body
                input = exchange.getIn().getBody(String.class);
                log.debug("EntryPointHTTP input : " + String.valueOf(input));
                if ((input == null) || input.isEmpty()) {
                    exchange.getOut().setHeader(STOP_EXCHANGE, "empty_body");
                    return;
                }
                input = input.replaceAll("\r", "");

                // Parsing params from request
                if ((input != null) && !input.isEmpty() && input.contains("form-data; ") && input.contains("<?xml")) {
                    String params = input.substring(input.indexOf("form-data; ") + "form-data; ".length());
                    if (params.indexOf("\n") > 0) {
                        params = params.substring(0, params.indexOf("\n"));
                    }
                    log.debug("EntryPointHTTP: Input contains params: " + params);
                    if ((params != null) && !params.isEmpty()) {
                        for (String paramPair : params.split(";")) {
                            metaData.put(
                                    paramPair.split("=")[0].replaceAll("\"", "").trim(),
                                    paramPair.split("=")[1].replaceAll("\"", "").trim()
                            );
                        }
                    }

                    data = input.substring(input.indexOf("<?xml"));
                    if (data.indexOf("-------------") > 0) {
                        data = data.substring(0, data.indexOf("-------------"));
                    }
                } else {
                    log.debug("EntryPointHTTP: Input is empty!");
                }
            }

            String uri = (String) exchange.getIn().getHeader("CamelHttpUri");
            if (uri.contains("/")) {
                String[] pathParams = uri.split("/");
                retVal.put("app", pathParams[1]);
                retVal.put("system", EncodingConverter.convertFromISOtoUTF8(pathParams[2]));
            } else {
                retVal.put("app", uri);
                retVal.put("system", EncodingConverter.convertFromISOtoUTF8(uri));
            }
            retVal.put("create_time", new Date().getTime());
            retVal.put("status", DataStatus.AVAILABLE.toString());
            retVal.put("metadata", metaData);
            retVal.put("data", data);

            log.debug("EntryPointHTTP return : " + retVal.toString());
            exchange.getOut().setBody(retVal);
        }
    };

    @Override
    public void configure() throws Exception {
        CompositeConfiguration config = PropertiesReaderSingleton.getInstance().getConfig();

        IURIProvider uriProvider = null;
        if (config.getBoolean("entry_point_http.config_db.enabled")) {
            try {
                uriProvider = new URIProviderDB();
            } catch (Exception e) {
                log.error("Error loading uri list from db: ", e);
                uriProvider = new URIProviderProperties();
            }
        } else {
            uriProvider = new URIProviderProperties();
        }

        if (config.getBoolean("entry_point_http.ssl.enabled")) {
            KeyStoreParameters ksp = new KeyStoreParameters();
            ksp.setResource(config.getString("gateway.keystore.path"));
            ksp.setPassword(config.getString("gateway.keystore.password"));
            KeyManagersParameters kmp = new KeyManagersParameters();
            kmp.setKeyStore(ksp);
            kmp.setKeyPassword(config.getString("gateway.keystore.password"));
            SSLContextParameters scp = new SSLContextParameters();
            scp.setKeyManagers(kmp);
            JettyHttpComponent jettyComponent = getContext().getComponent("jetty", JettyHttpComponent.class);
            jettyComponent.setSslContextParameters(scp);
        }

        List<String> urlsProc1 = uriProvider.getUrlsProcessor1();
        log.debug("URIs for Processor1: " + Arrays.deepToString(urlsProc1.toArray()));
        List<String> urlsProc2 = uriProvider.getUrlsProcessor2();
        log.debug("URIs for Processor2: " + Arrays.deepToString(urlsProc2.toArray()));

        for (String urlString : urlsProc1) {
            from("jetty:http"+ (config.getBoolean("entry_point_http.ssl.enabled")?"s":"") +"://" +
                    config.getString("entry_point_http.host") + ":" +
                    config.getString("entry_point_http.port") + "/" + urlString +
                    "?httpMethodRestrict=POST") // Forbidden GET,HEAD,PUT,PATCH,DELETE,TRACE,CONNECT,OPTIONS
                .process(processor)
                .to("direct:to_mongo");
        }

        for (String urlString : urlsProc2) {
            from("jetty:http"+ (config.getBoolean("entry_point_http.ssl.enabled")?"s":"") +"://" +
                    config.getString("entry_point_http.host") + ":" +
                    config.getString("entry_point_http.port") + "/" + urlString +
                    "?httpMethodRestrict=POST") // Forbidden GET,HEAD,PUT,PATCH,DELETE,TRACE,CONNECT,OPTIONS
                    .process(processor2)
                    .to("direct:to_mongo");
        }

        from("direct:to_mongo")
                .choice()
                .when(header(STOP_EXCHANGE).isNull())
                .to("mongodb:mongoDBConnection?database=" + config.getString("mongodb.name") +
                        "&collection=" + config.getString("mongodb.collection_name") +
                        "&operation=insert");

        from("timer:heartbeat?period=1s").log(LoggingLevel.INFO, "HeartBeat", "entry_point_http: on-line");
        from("jetty:http" + (config.getBoolean("entry_point_http.ssl.enabled") ? "s" : "") + "://" +
                config.getString("entry_point_http.host") + ":" +
                config.getString("entry_point_http.port") + "/version" +
                "?httpMethodRestrict=GET") // Forbidden POST,HEAD,PUT,PATCH,DELETE,TRACE,CONNECT,OPTIONS
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getOut().setBody("entry_point_http v.1.0: on-line");
                    }
                });

        log.info("EntryPointHTTP routes configured!");
    }

}
