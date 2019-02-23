package com.bgsoftware.wildchests.objects;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import com.bgsoftware.wildchests.WildChestsPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.Arrays;

@SuppressWarnings({"unchecked", "ConstantConditions", "SameParameterValue", "OptionalGetWithoutIsPresent"})
public final class WInventory{

    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private Inventory inventory;

    private WInventory(Inventory inventory){
        this.inventory = inventory;
    }

    public Inventory getInventory(){
        return inventory;
    }

    public void setTitle(String title){
        try{
            Class minecraftInventory = Arrays.stream(this.inventory.getClass().getDeclaredClasses())
                    .filter(clazz -> clazz.getName().contains("MinecraftInventory")).findFirst().get();

            Field field = getBukkitClass("inventory.CraftInventory").getDeclaredField("inventory");
            field.setAccessible(true);
            Object inventory = field.get(this.inventory);
            field.setAccessible(false);

            Field titleField = minecraftInventory.getDeclaredField("title");
            titleField.setAccessible(true);
            try {
                titleField.set(inventory, title);
            }catch(IllegalArgumentException ex){
                Class craftChatMessageClass = getBukkitClass("util.CraftChatMessage");
                Object[] chatBaseComponent = (Object[]) craftChatMessageClass.getMethod("fromString", String.class).invoke(null, title);
                titleField.set(inventory, chatBaseComponent[0]);
            }
            titleField.setAccessible(false);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public void setSize(int size){
        try{
            Class minecraftInventory = Arrays.stream(this.inventory.getClass().getDeclaredClasses())
                    .filter(clazz -> clazz.getName().contains("MinecraftInventory")).findFirst().get();
            Class notNullListClass = getNMSClass("NonNullList");
            Class itemStackClass = getNMSClass("ItemStack");

            Field field = getBukkitClass("inventory.CraftInventory").getDeclaredField("inventory");
            field.setAccessible(true);
            Object inventory = field.get(this.inventory);
            field.setAccessible(false);

            Field itemsField = minecraftInventory.getDeclaredField("items");
            itemsField.setAccessible(true);

            Object nullItem = itemStackClass.getField("a").get(null);

            AbstractList<Object> itemsList = (AbstractList<Object>) itemsField.get(inventory);
            Method aMethod = null;

            outside: for(Method method : notNullListClass.getMethods()){
                if(method.getName().equals("a")){
                    for(Class type : method.getParameterTypes()){
                        if(type.equals(int.class)){
                            aMethod = method;
                            break outside;
                        }
                    }
                }
            }

            AbstractList<Object> newNonNullList = (AbstractList<Object>) aMethod.invoke(null, size, nullItem);

            for(int i = 0; i < itemsList.size() && i < newNonNullList.size(); i++){
                newNonNullList.set(i, itemsList.get(i));
            }

            if(newNonNullList.size() > itemsList.size()){
                for(int i = itemsList.size(); i < newNonNullList.size(); i++){
                    newNonNullList.set(i, nullItem);
                }
            }

            itemsField.set(inventory, newNonNullList);
            itemsField.setAccessible(false);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    private Class getNMSClass(String clazz) throws ClassNotFoundException{
        return Class.forName("net.minecraft.server." + WInventory.plugin.getNMSAdapter().getVersion() + "." + clazz);
    }

    private Class getBukkitClass(String clazz) throws ClassNotFoundException{
        return Class.forName("org.bukkit.craftbukkit." + WInventory.plugin.getNMSAdapter().getVersion() + "." + clazz);
    }

    public static WInventory of(int size, String title){
        return of(Bukkit.createInventory(null, size, title));
    }

    public static WInventory of(Inventory inventory){
        return new WInventory(inventory);
    }

}
