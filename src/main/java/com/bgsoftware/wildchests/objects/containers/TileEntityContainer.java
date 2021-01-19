package com.bgsoftware.wildchests.objects.containers;

import org.bukkit.entity.HumanEntity;

import java.util.List;

public interface TileEntityContainer {

    int getViewingCount();

    List<HumanEntity> getTransaction();

    void updateData();

}
