package com.bgsoftware.wildchests.objects.containers;

import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LinkedChestsContainer {

    private final Set<LinkedChest> linkedChests = new HashSet<>();
    private final LinkedChest sourceChest;

    public LinkedChestsContainer(LinkedChest sourceChest){
        this.sourceChest = sourceChest;
        linkedChests.add(sourceChest);
    }

    public LinkedChest getSourceChest() {
        return sourceChest;
    }

    public void linkChest(LinkedChest linkedChest){
        linkedChests.add(linkedChest);
    }

    public void unlinkChest(LinkedChest linkedChest){
        if(linkedChest == sourceChest){
            linkedChests.forEach(_linkedChest -> _linkedChest.linkIntoChest(null));
        }
        else {
            linkedChests.remove(linkedChest);
        }
    }

    public List<LinkedChest> getLinkedChests() {
        return new ArrayList<>(linkedChests);
    }
}
