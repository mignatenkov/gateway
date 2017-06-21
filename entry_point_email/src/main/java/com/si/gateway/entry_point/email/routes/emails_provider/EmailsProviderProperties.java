package com.si.gateway.entry_point.email.routes.emails_provider;

import com.si.gateway.common.properties_reader.PropertiesReaderSingleton;

import java.util.List;

/**
 * The implementation for emails provider of properties.
 */
public class EmailsProviderProperties implements IEmailsProvider {

    @Override
    public List<String> getEmails() {
        return PropertiesReaderSingleton.getInstance().getConfig().getList("entry_point_email.imap.addresses");
    }

}
