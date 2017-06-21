package com.si.gateway.entry_point.http.routes.url_provider;

import java.util.List;

/**
 * The interface for uri providers.
 */
public interface IURIProvider {
    /**
     * Gets uris from the source.
     *
     * @return the uris
     */
    List<String> getUrlsProcessor1();

    /**
     * Gets uris from the source.
     *
     * @return the uris
     */
    List<String> getUrlsProcessor2();
}
