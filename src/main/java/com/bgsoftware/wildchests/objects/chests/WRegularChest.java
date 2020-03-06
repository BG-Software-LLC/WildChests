package com.bgsoftware.wildchests.objects.chests;

import com.bgsoftware.wildchests.database.Query;
import com.bgsoftware.wildchests.database.StatementHolder;
import com.bgsoftware.wildchests.objects.WLocation;
import com.bgsoftware.wildchests.api.objects.chests.RegularChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;

import java.util.UUID;

public final class WRegularChest extends WChest implements RegularChest {

    public WRegularChest(UUID placer, WLocation location, ChestData chestData){
        super(placer, location, chestData);
    }

    @Override
    public void remove() {
        super.remove();
        Query.REGULAR_CHEST_DELETE.getStatementHolder()
                .setLocation(getLocation())
                .execute(true);
    }

    @Override
    public void executeInsertQuery(boolean async) {
        Query.REGULAR_CHEST_INSERT.getStatementHolder()
                .setLocation(location)
                .setString(placer.toString())
                .setString(getData().getName())
                .setString("")
                .execute(async);
    }

    @Override
    public void executeUpdateQuery(boolean async) {
        Query.REGULAR_CHEST_UPDATE.getStatementHolder()
                .setString(placer.toString())
                .setString(getData().getName())
                .setString("")
                .setLocation(location)
                .execute(async);
    }

    @Override
    public StatementHolder getSelectQuery() {
        return Query.REGULAR_CHEST_SELECT.getStatementHolder().setLocation(location);
    }
}
