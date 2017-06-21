package com.si.gateway.entry_point.ftp.routes;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.si.gateway.common.data_storage.DataStatus;
import com.si.gateway.common.properties_reader.PropertiesReaderSingleton;
import com.si.gateway.common.utils.EncodingConverter;
import com.si.gateway.entry_point.ftp.routes.dirs_provider.IDirsProvider;
import com.si.gateway.entry_point.ftp.routes.dirs_provider.DirsProviderDB;
import com.si.gateway.entry_point.ftp.routes.dirs_provider.DirsProviderProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.configuration.CompositeConfiguration;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Date;

/**
 * The EntryPointFTP routes builder.
 * Processor are going to translate input data into inner format.
 */
@Slf4j
@Component(value = "routeEntryPointFTP")
public class MainRouteBuilder extends RouteBuilder {

    private Processor processor = new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
            File file = exchange.getIn().getBody(File.class);
            log.debug("EntryPointFTP : filename: " + String.valueOf(exchange.getIn().getHeader("CamelFileName")));
            StringBuffer sb = new StringBuffer();

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String sCurrentLine;
                while ((sCurrentLine = br.readLine()) != null) {
                    sb.append(sCurrentLine).append("\n");
                }
            }
            String input = sb.toString().replaceAll("\r", "");

            // Parsing params from request
            DBObject retVal = new BasicDBObject();
            JSONObject metaData = new JSONObject();
            metaData.put("filename", String.valueOf(exchange.getIn().getHeader("CamelFileName")));
            String data = input;
            if ((input != null) && !input.isEmpty() && input.contains("\n\n")) {
                data = input.substring(input.indexOf("\n\n")+2);
                String params = input.substring(0, input.indexOf("\n\n"));
                log.debug("EntryPointFTP: Input contains params: " + params);
                if ((params != null) && !params.isEmpty() && params.contains("\n")) {
                    for (String paramPair : params.split("\n")) {
                        metaData.put(paramPair.split(":")[0], paramPair.split(":")[1].trim());
                    }
                }
            } else {
                log.debug("EntryPointFTP: Input contains no params!");
            }

            String dirString = (String) exchange.getIn().getHeader("dirString");
            if (dirString.contains("/")) {
                retVal.put("app", dirString.substring(0, dirString.indexOf("/")));
                retVal.put("system", EncodingConverter.convertFromISOtoUTF8(dirString.substring(dirString.indexOf("/") + 1)));
            } else {
                retVal.put("app", dirString);
                retVal.put("system", EncodingConverter.convertFromISOtoUTF8(dirString));
            }
            retVal.put("create_time", new Date().getTime());
            retVal.put("status", DataStatus.AVAILABLE.toString());
            retVal.put("metadata", metaData);
            retVal.put("data", data);

            log.debug("EntryPointFTP [" + ((String) exchange.getIn().getHeader("Content-Type")) + "] : " + retVal.toString());
            exchange.getOut().setBody(retVal);

            // Copy processed file to .done subdir for backup. Create the subdirs if necessary.
            // Camel route later will delete processed file, after all route points will finish their jobs.
            if (!Paths.get(".done/" + exchange.getIn().getHeader("CamelFileParent") + "/").toFile().exists()) {
                File f = Paths.get(".done/" + exchange.getIn().getHeader("CamelFileParent") + "/").toFile();
                f.mkdirs();
            }
            Files.copy(
                    Paths.get(file.getAbsolutePath()),
                    Paths.get(".done/" + exchange.getIn().getHeader("CamelFileParent") + "/" + exchange.getIn().getHeader("CamelFileName")),
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    };

    @Override
    public void configure() throws Exception {
        CompositeConfiguration config = PropertiesReaderSingleton.getInstance().getConfig();

        IDirsProvider dirsProvider = null;
        if (config.getBoolean("entry_point_ftp.config_db.enabled")) {
            try {
                dirsProvider = new DirsProviderDB();
            } catch (Exception e) {
                log.error("Error loading uri list from db: ", e);
                dirsProvider = new DirsProviderProperties();
            }
        } else {
            dirsProvider = new DirsProviderProperties();
        }

        log.debug("DIRs: " + Arrays.deepToString(dirsProvider.getDirs().toArray()));

        for (String dirString : dirsProvider.getDirs()) {
            from("file://" + config.getString("entry_point_ftp.dirs_path") + dirString + "?" +
                    "delete=true&" +
                    "recursive=" + config.getString("entry_point_ftp.recursive") + "&" +
                    "flatten=true&" +
                    "readLock=changed&" +
                    "readLockCheckInterval=2000&" +
                    "useFixedDelay=true&" +
                    "delay=" +
                    PropertiesReaderSingleton.getInstance().getConfig().getString("entry_point_ftp.dir_refresh_delay"))
                .setHeader("dirString").simple(dirString)
                .process(processor)
                .to("mongodb:mongoDBConnection?" +
                    "database=" + config.getString("mongodb.name") +
                    "&collection=" + config.getString("mongodb.collection_name") +
                    "&operation=insert");
        }

        from("timer:heartbeat?period=1s").log(LoggingLevel.INFO, "HeartBeat", "entry_point_ftp: on-line");

        log.info("EntryPointFTP routes configured!");
    }

}
