package com.bgsoftware.wildchests.objects.chests;

import com.bgsoftware.wildchests.database.Query;
import com.bgsoftware.wildchests.database.SQLHelper;
import com.bgsoftware.wildchests.objects.WLocation;
import com.bgsoftware.wildchests.api.objects.chests.RegularChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;

import java.util.UUID;

public final class WRegularChest extends WChest implements RegularChest {

    public WRegularChest(UUID placer, WLocation location, ChestData chestData){
        super(placer, location, chestData);

        SQLHelper.runIfConditionNotExist(Query.REGULAR_CHEST_SELECT.getStatementHolder().setLocation(getLocation()), () ->
            Query.REGULAR_CHEST_INSERT.getStatementHolder()
                    .setLocation(location)
                    .setString(placer.toString())
                    .setString(chestData.getName())
                    .setString("")
                    .execute(true));
    }

    @Override
    public void remove() {
        super.remove();
        Query.REGULAR_CHEST_DELETE.getStatementHolder()
                .setLocation(getLocation())
                .execute(true);
    }

    @Override
    public void saveIntoData(boolean async) {
        Query.REGULAR_CHEST_UPDATE_INVENTORY.getStatementHolder()
                .setInventories(getPages())
                .setLocation(getLocation())
                .execute(async);
    }
}
