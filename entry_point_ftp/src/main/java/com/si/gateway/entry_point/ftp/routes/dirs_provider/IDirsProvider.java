package com.si.gateway.entry_point.ftp.routes.dirs_provider;

import java.util.List;

/**
 * The interface for dirs providers.
 */
public interface IDirsProvider {
    /**
     * Gets dirs from the source.
     *
     * @return the dirs
     */
    List<String> getDirs();
}
