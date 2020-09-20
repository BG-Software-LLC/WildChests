package com.bgsoftware.wildchests.api;

import com.bgsoftware.wildchests.api.handlers.ChestsManager;
import com.bgsoftware.wildchests.api.handlers.ProvidersManager;

public interface WildChests {

    /**
     * Get the chests manager instance.
     */
    ChestsManager getChestsManager();

    /**
     * Get the providers manager instance.
     */
    ProvidersManager getProviders();

}
