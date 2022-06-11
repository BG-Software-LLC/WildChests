package com.bgsoftware.wildchests.objects.containers;

import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LinkedChestsContainer {

    private final List<LinkedChest> linkedChests = new ArrayList<>();
    private final LinkedChest sourceChest;

    public LinkedChestsContainer(LinkedChest sourceChest) {
        this.sourceChest = sourceChest;
    }

    public LinkedChest getSourceChest() {
        return sourceChest;
    }

    public void linkChest(LinkedChest linkedChest) {
        linkedChests.add(linkedChest);
    }

    public void unlinkChest(LinkedChest linkedChest) {
        if (linkedChest == sourceChest) {
            linkedChests.forEach(_linkedChest -> _linkedChest.linkIntoChest(null));
            linkedChests.clear();
        } else {
            linkedChests.remove(linkedChest);
        }
    }

    public List<LinkedChest> getLinkedChests() {
        return Collections.unmodifiableList(this.linkedChests);
    }

    public boolean isLinkedChest(LinkedChest linkedChest) {
        return linkedChest == sourceChest || this.linkedChests.contains(linkedChest);
    }

}
