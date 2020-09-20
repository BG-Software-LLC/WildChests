package com.bgsoftware.wildchests.api.handlers;

import com.bgsoftware.wildchests.api.hooks.PricesProvider;

public interface ProvidersManager {

    /**
     * Set the prices provider for the core.
     * @param pricesProvider The provider to set.
     */
    void setPricesProvider(PricesProvider pricesProvider);

}
