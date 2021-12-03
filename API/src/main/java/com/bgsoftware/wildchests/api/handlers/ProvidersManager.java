package com.bgsoftware.wildchests.api.handlers;

import com.bgsoftware.wildchests.api.hooks.BankProvider;
import com.bgsoftware.wildchests.api.hooks.PricesProvider;
import com.bgsoftware.wildchests.api.hooks.StackerProvider;

public interface ProvidersManager {

    /**
     * Set the prices provider for the core.
     * @param pricesProvider The provider to set.
     */
    void setPricesProvider(PricesProvider pricesProvider);

    /**
     * Set the stacker provider for the core.
     * @param stackerProvider The provider to set.
     */
    void setStackerProvider(StackerProvider stackerProvider);

    /**
     * Register custom banks provider for the core.
     * @param banksProvider The provider to set.
     */
    void setBanksProvider(BankProvider banksProvider);

}
