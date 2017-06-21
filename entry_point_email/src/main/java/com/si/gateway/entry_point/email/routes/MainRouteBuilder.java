package com.si.gateway.entry_point.email.routes;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.si.gateway.common.data_storage.DataStatus;
import com.si.gateway.common.properties_reader.PropertiesReaderSingleton;
import com.si.gateway.common.utils.EncodingConverter;
import com.si.gateway.entry_point.email.routes.emails_provider.EmailsProviderDB;
import com.si.gateway.entry_point.email.routes.emails_provider.EmailsProviderProperties;
import com.si.gateway.entry_point.email.routes.emails_provider.IEmailsProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.configuration.CompositeConfiguration;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.internet.MimeMultipart;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * The EntryPointEmail routes builder.
 * Processor are going to translate input data into inner format.
 */
@Slf4j
@Component(value = "routeEntryPointEmail")
public class MainRouteBuilder extends RouteBuilder {

    private static final String STOP_EXCHANGE = "stop_exchange";
    private static final String PROPERTY_TO = "to_property";

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws Exception{
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain") || bodyPart.isMimeType("text/html")) {
                result = result.concat("\n").concat( bodyPart.getContent().toString() );
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result = result.concat( getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()) );
            }
        }
        return result;
    }

    private Processor processor = new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
            Message input = exchange.getIn().getBody(Message.class);
            String contentType = input.getContentType();
            log.debug("EntryPointEmail ["+ contentType +"] : " + input);

            String from = exchange.getIn().getHeader("from", String.class);
            from = from.replaceAll("\"", "");
            String to = exchange.getIn().getHeader("to", String.class);
            to = to.replaceAll("\"", "");
            log.debug("EntryPointEmail from: " + from);
            log.debug("EntryPointEmail   to: " + to);

            if (!exchange.getIn().getHeader(PROPERTY_TO, String.class).contains("_")) {
                log.debug("EntryPointEmail stop_exchange: incorrect_email_address " + exchange.getIn().getHeader(PROPERTY_TO, String.class));
                exchange.getOut().setHeader(STOP_EXCHANGE, "incorrect_email_address");
                return;
            }
            // Here can be some spam filter logic

            String data;
            if (input.isMimeType("text/plain")) {
                data = input.getContent().toString();
            } else if (input.isMimeType("multipart/*")) {
                data = getTextFromMimeMultipart((MimeMultipart) input.getContent());
            } else {
                log.debug("EntryPointEmail stop exchange: incorrect_email_content_type!");
                exchange.getOut().setHeader(STOP_EXCHANGE, "incorrect_email_content_type");
                return;
            }

            JSONObject mailCredentials = new JSONObject();
            mailCredentials.put("received_date", simpleDateFormat.format(input.getReceivedDate()));
            mailCredentials.put("subject", input.getSubject());
            mailCredentials.put("from", from);
            mailCredentials.put("to", to);

            to = exchange.getIn().getHeader(PROPERTY_TO, String.class);

            // Parsing params from request
            JSONObject metaData = new JSONObject();
            Date curTime = new Date();
            metaData.put("filename", to.substring(0, to.indexOf("@")) + "_" +
                    simpleDateFormat.format(curTime) + "_" + String.valueOf(input.hashCode()));
            metaData.put("mail_credentials", mailCredentials);

            DBObject retVal = new BasicDBObject();
            retVal.put("app", to.substring(0, to.indexOf("_")));
            retVal.put("system", EncodingConverter.convertFromISOtoUTF8(
                    to.substring(to.indexOf("_") + 1, to.indexOf("@"))
            ));
            retVal.put("create_time", new Date().getTime());
            retVal.put("status", DataStatus.AVAILABLE.toString());
            retVal.put("metadata", metaData);
            retVal.put("data", data);

            log.debug("EntryPointEmail to DB : " + retVal.toString());
            exchange.getOut().setBody(retVal);
        }
    };

    @Override
    public void configure() throws Exception {
        CompositeConfiguration config = PropertiesReaderSingleton.getInstance().getConfig();

        IEmailsProvider emailsProvider = null;
        if (config.getBoolean("entry_point_email.config_db.enabled")) {
            try {
                emailsProvider = new EmailsProviderDB();
            } catch (Exception e) {
                log.error("Error loading email list from db: "+ e.getMessage());
                emailsProvider = new EmailsProviderProperties();
            }
        } else {
            emailsProvider = new EmailsProviderProperties();
        }

        log.debug("Emails: " + Arrays.deepToString(emailsProvider.getEmails().toArray()));

        for (String emailString : emailsProvider.getEmails()) {
            String emailLoginString = emailString.substring(0, emailString.indexOf(":"));
            String emailPasswordString = emailString.substring(emailString.indexOf(":") + 1);
            log.debug("Adding route for login/password: " + String.valueOf(emailLoginString.substring(0, emailLoginString.indexOf("@")) +
                    "@" + emailLoginString.substring(emailLoginString.indexOf("@") + 1) + ":" + emailPasswordString));

            from("imaps://" + emailLoginString.substring(0, emailLoginString.indexOf("@")) +
                    "@" + emailLoginString.substring(emailLoginString.indexOf("@") + 1) +
                    ":" + config.getString("entry_point_email.imaps.port") + "?" +
                    "password=" + emailPasswordString +
                    "&mapMailMessage=false" +
                    "&consumer.useFixedDelay=true" +
                    "&consumer.delay=" +
                    PropertiesReaderSingleton.getInstance().getConfig().getString("entry_point_email.mail_refresh_delay"))
                .setHeader(PROPERTY_TO).simple(
                        emailLoginString.substring(0, emailLoginString.indexOf("@")) +
                        "@" + emailLoginString.substring(emailLoginString.indexOf("@") + 1)
                ).process(processor)
                .to("direct:to_mongo");

            from("imap://" + emailLoginString.substring(0, emailLoginString.indexOf("@")) +
                    "@" + emailLoginString.substring(emailLoginString.indexOf("@") + 1) + "?" +
                    "password=" + emailPasswordString +
                    "&mapMailMessage=false" +
                    "&consumer.useFixedDelay=true" +
                    "&consumer.delay=" +
                    PropertiesReaderSingleton.getInstance().getConfig().getString("entry_point_email.mail_refresh_delay"))
                .setHeader(PROPERTY_TO).simple(
                        emailLoginString.substring(0, emailLoginString.indexOf("@")) +
                        "@" + emailLoginString.substring(emailLoginString.indexOf("@") + 1)
                ).process(processor)
                .to("direct:to_mongo");
        }

        from("direct:to_mongo")
            .choice()
                .when(header(STOP_EXCHANGE).isNull())
                .to("mongodb:mongoDBConnection?database=" + config.getString("mongodb.name") +
                        "&collection=" + config.getString("mongodb.collection_name") +
                        "&operation=insert");

        from("timer:heartbeat?period=1s").log(LoggingLevel.INFO, "HeartBeat", "entry_point_email: on-line");

        log.info("EntryPointEmail routes configured!");
    }

}
