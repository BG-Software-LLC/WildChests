package com.bgsoftware.wildchests.objects.containers;

import org.bukkit.entity.HumanEntity;

import java.util.List;

public interface TileEntityContainer {

    int getSize();

    int getViewingCount();

    List<HumanEntity> getTransaction();

    void setTransaction(List<HumanEntity> transaction);

    void openContainer(HumanEntity humanEntity);

    void closeContainer(HumanEntity humanEntity);

    void updateData();

}
