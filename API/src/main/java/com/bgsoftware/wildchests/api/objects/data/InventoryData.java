package com.bgsoftware.wildchests.api.objects.data;

/**
 * InventoryData is used to save data about a page.
 */
public interface InventoryData {

    /**
     * Get the title of the page.
     */
    String getTitle();

    /**
     * Get the price of the page (unlock price).
     */
    double getPrice();

}
