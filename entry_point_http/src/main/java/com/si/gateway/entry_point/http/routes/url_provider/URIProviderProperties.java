package com.si.gateway.entry_point.http.routes.url_provider;

import com.si.gateway.common.properties_reader.PropertiesReaderSingleton;

import java.util.List;

/**
 * The implementation for uri provider of properties.
 */
public class URIProviderProperties implements IURIProvider {

    @Override
    public List<String> getUrlsProcessor1() {
        return PropertiesReaderSingleton.getInstance().getConfig().getList("entry_point_http.uri_list_processor1");
    }

    @Override
    public List<String> getUrlsProcessor2() {
        return PropertiesReaderSingleton.getInstance().getConfig().getList("entry_point_http.uri_list_processor2");
    }

}
