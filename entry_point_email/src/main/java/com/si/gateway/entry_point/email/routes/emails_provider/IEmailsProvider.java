package com.si.gateway.entry_point.email.routes.emails_provider;

import java.util.List;

/**
 * The interface for dirs providers.
 */
public interface IEmailsProvider {
    /**
     * Gets emails from the source.
     *
     * @return the emails
     */
    List<String> getEmails();
}
