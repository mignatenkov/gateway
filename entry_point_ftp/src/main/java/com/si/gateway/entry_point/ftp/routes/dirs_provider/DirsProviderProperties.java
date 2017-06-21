package com.si.gateway.entry_point.ftp.routes.dirs_provider;

import com.si.gateway.common.properties_reader.PropertiesReaderSingleton;

import java.util.List;

/**
 * The implementation for uri provider of properties.
 */
public class DirsProviderProperties implements IDirsProvider {

    @Override
    public List<String> getDirs() {
        return PropertiesReaderSingleton.getInstance().getConfig().getList("entry_point_ftp.dirs");
    }

}
