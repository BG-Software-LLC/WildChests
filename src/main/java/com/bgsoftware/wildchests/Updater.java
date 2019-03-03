package com.bgsoftware.wildchests;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@SuppressWarnings("WeakerAccess")
public final class Updater {

    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();
    private static String latestVersion, versionDescription;

    static{
        setLatestVersion();
    }

    public static boolean isOutdated(){
        return !plugin.getDescription().getVersion().equals(latestVersion);
    }

    public static String getLatestVersion(){
        return latestVersion;
    }

    public static String getVersionDescription(){
        return versionDescription;
    }

    private static void setLatestVersion(){
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("http://wildseries.xyz/versions.json").openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB;     rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 (.NET CLR 3.5.30729)");
            connection.setDoInput(true);

            try(InputStream reader = connection.getInputStream()){
                JsonObject jsonObject = new Gson().fromJson(new InputStreamReader(reader), JsonObject.class);
                JsonObject plugin = jsonObject.get("wildchests").getAsJsonObject();
                latestVersion = plugin.get("version").getAsString();
                versionDescription = plugin.get("description").getAsString();
            }
        } catch(Exception ex){
            //Something went wrong...
            latestVersion = plugin.getDescription().getVersion();
        }
    }

}
