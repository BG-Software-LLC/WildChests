package com.bgsoftware.wildchests.objects.containers;

import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;
import com.bgsoftware.wildchests.objects.chests.WLinkedChest;
import com.bgsoftware.wildchests.objects.inventory.CraftWildInventory;
import com.bgsoftware.wildchests.utils.SyncedArray;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public final class LinkedChestsContainer {

    private final Set<LinkedChest> linkedChests = new LinkedHashSet<>();
    private final SyncedArray<CraftWildInventory> inventories;
    private final LinkedChest sourceChest;

    public LinkedChestsContainer(LinkedChest linkedChest, SyncedArray<CraftWildInventory> inventories) {
        this.inventories = inventories;
        linkChest(linkedChest);
        this.sourceChest = linkedChest;
    }

    public SyncedArray<CraftWildInventory> getInventories() {
        return inventories;
    }

    public LinkedChest getSourceChest() {
        return sourceChest;
    }

    public void linkChest(LinkedChest linkedChest) {
        if (linkedChests.add(linkedChest))
            ((WLinkedChest) linkedChest).setLinkedChestsContainerRaw(this);
    }

    public void unlinkChest(LinkedChest linkedChest) {
        if (linkedChest == this.sourceChest) {
            this.linkedChests.forEach(currentLinkedChest -> ((WLinkedChest) currentLinkedChest).setLinkedChestsContainerRaw(null));
        } else {
            if (linkedChests.remove(linkedChest))
                ((WLinkedChest) linkedChest).setLinkedChestsContainerRaw(null);
        }
    }

    public List<LinkedChest> getLinkedChests() {
        return Collections.unmodifiableList(new LinkedList<>(this.linkedChests));
    }

    public void merge(LinkedChestsContainer otherContainer) {
        otherContainer.linkedChests.forEach(this::linkChest);
    }

}
