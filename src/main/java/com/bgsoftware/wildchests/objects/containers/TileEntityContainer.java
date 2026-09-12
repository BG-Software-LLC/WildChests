package com.bgsoftware.wildchests.objects.containers;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import org.bukkit.entity.HumanEntity;

import java.util.List;

public interface TileEntityContainer {

    /**
     * Check whether this tile entity was created for the given chest instance.
     */
    boolean isOwner(Chest chest);

    void closeContainer(HumanEntity humanEntity);

    int getViewingCount();

    List<HumanEntity> getTransaction();

    void updateData();

}
