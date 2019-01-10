package xyz.wildseries.wildchests.api.objects.chests;

import java.util.List;

public interface LinkedChest extends RegularChest, Chest{

    void linkIntoChest(LinkedChest linkedChest);

    LinkedChest getLinkedChest();

    boolean isLinkedIntoChest();

    List<LinkedChest> getAllLinkedChests();

}
