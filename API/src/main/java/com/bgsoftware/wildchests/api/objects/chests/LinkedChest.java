package com.bgsoftware.wildchests.api.objects.chests;

import java.util.List;

/**
 * LinkedChests are regular chests that can be linked into other linked chests.
 * It means that all of their stats and contents will be synced with their linked chests.
 */
public interface LinkedChest extends RegularChest, Chest{

    /**
     * Link this chest into another chest.
     *
     * If the target link is not linked to any chests,
     * this chest will be the source chest, and the other chests will link into this chest.
     * Otherwise, this chest will be linked into the source chest of the target chest.
     *
     * @param linkedChest The target chest.
     *                    If null, you'll unlink this chest with all the linked chests.
     */
    void linkIntoChest(LinkedChest linkedChest);

    /**
     * Get the source chest of this chest.
     */
    LinkedChest getLinkedChest();

    /**
     * Checks if this chest is linked into another chest, and it's not the source chest.
     */
    boolean isLinkedIntoChest();

    /**
     * Get all the linked chests of the source chest of this chest, excluding the source chest.
     */
    List<LinkedChest> getAllLinkedChests();

}
