/*
 * HexxitGear
 * Copyright (C) 2013  Ryan Cohen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package sct.hexxitgear.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import sct.hexxitgear.HexxitGear;
import sct.hexxitgear.core.ability.*;
import sct.hexxitgear.core.buff.*;
import sct.hexxitgear.item.ItemHexxitArmor;

import java.util.*;

public class ArmorSet {

    public static ArmorSet tribalSet = new ArmorSet("Tribal", "http://hexxit.sctgaming.com/capes/brownrags.png",
            Arrays.asList(HexxitGear.tribalHelmet, HexxitGear.tribalChest, HexxitGear.tribalLeggings, HexxitGear.tribalShoes), new BuffTribalSet(), new AbilityKnockback());
    public static ArmorSet thiefSet = new ArmorSet("Thief", "http://hexxit.sctgaming.com/capes/redcape.png",
            Arrays.asList(HexxitGear.thiefHelmet, HexxitGear.thiefChest, HexxitGear.thiefLeggings, HexxitGear.thiefBoots), new BuffThiefSet(), new AbilityInvisibility());
    public static ArmorSet scaleSet = new ArmorSet("Scale", "http://hexxit.sctgaming.com/capes/purplecape.png",
            Arrays.asList(HexxitGear.scaleHelmet, HexxitGear.scaleChest, HexxitGear.scaleLeggings, HexxitGear.scaleBoots), new BuffScaleSet(), new AbilityShield());
    public static ArmorSet magicSet = new ArmorSet("Magician", "http://hexxit.sctgaming.com/capes/sagecape.png",
            Arrays.asList(HexxitGear.magicHelmet, HexxitGear.magicChest, HexxitGear.magicLeggings, HexxitGear.magicBoots), new BuffMagicSet(), new AbilityNull());

    private static List<ArmorSet> armorSets;
    private static Map<String, ArmorSet> playerMap = new HashMap<String, ArmorSet>();
    private static List<String> activeArmors = new ArrayList<String>();
    private static Map<String, List<IBuffHandler>> buffMap = new HashMap<String, List<IBuffHandler>>();

    private List<ItemHexxitArmor> armors = new ArrayList<ItemHexxitArmor>();
    private String capeUrl;
    private String name;
    private IBuffHandler buffHandler;
    private Ability ability;

    public static List<ArmorSet> getArmorSets() {
        return armorSets;
    }

    public static ArmorSet getArmorSetAtIndex(int index) {
        return armorSets.get(index);
    }

    public static void getMatchingSet(EntityPlayer player) {
        List<Item> playerSet = getPlayerArmors(player);

        boolean foundMatch = false;
        List<IBuffHandler> shouldHaveBuffs = new ArrayList<IBuffHandler>(4);

        for (ArmorSet armorSet : getArmorSets()) {
            int matched = 0;
            for (ItemHexxitArmor armor : armorSet.getArmors()) {
                if (playerSet.contains(armor)) {
                    IBuffHandler armorBuff = armor.getBuffHandler();
                    if (armorBuff != null)
                        shouldHaveBuffs.add(armorBuff);
                    matched++;
                }
            }
            if (matched == 4) {
                shouldHaveBuffs.clear();
                shouldHaveBuffs.add(armorSet.buffHandler);
                if (getPlayerArmorSet(player.getDisplayName()) == null || !getPlayerArmorSet(player.getDisplayName()).equals(armorSet)) {
                    addPlayerArmorSet(player.getDisplayName(), armorSet);
                }
                foundMatch = true;
            }
        }

        if (!foundMatch && getPlayerArmorSet(player.getDisplayName()) != null) {
            removePlayerArmorSet(player.getDisplayName());
        }

        List<IBuffHandler> actualBuffs;
        if (buffMap.containsKey(player.getDisplayName()))
            actualBuffs = buffMap.get(player.getDisplayName());
        else
            actualBuffs = new ArrayList<IBuffHandler>(0);

        for (IBuffHandler buff : actualBuffs) {
            if (!shouldHaveBuffs.contains(buff))
                buff.removePlayerBuffs(player);
        }

        for (IBuffHandler buff : shouldHaveBuffs) {
            buff.applyPlayerBuffs(player);
        }

        buffMap.put(player.getDisplayName(), shouldHaveBuffs);
    }

    private static List<Item> getPlayerArmors(EntityPlayer player) {
        List<Item> playerSet = new ArrayList<Item>(4);

        for (int i = 0; i < 4; i++) {
            if (player.getCurrentArmor(i) != null) {
                playerSet.add(player.getCurrentArmor(i).getItem());
            }
        }

        return playerSet;
    }

    public static ArmorSet getPlayerArmorSet(String playerName) {
        return playerMap.get(playerName);
    }

    public static void addPlayerArmorSet(String playerName, ArmorSet armorSet) {
        playerMap.put(playerName, armorSet);
        if (armorSet.getCapeUrl() != null) {
            CapeHandler.registerCape(playerName, armorSet.getCapeUrl());
        }
    }

    public static void removePlayerArmorSet(String playerName) {
        if (getPlayerArmorSet(playerName) != null && getPlayerArmorSet(playerName).getCapeUrl() != null) {
            CapeHandler.removeCape(playerName);
        }
        playerMap.remove(playerName);
    }

    public static void readArmorPacket(String playerName) {
        ArmorSet as = getPlayerArmorSet(playerName);
        if (as != null && !activeArmors.contains(playerName)) {
            activeArmors.add(playerName);
        }
    }

    public ArmorSet(String name, String capeUrl, List<ItemHexxitArmor> armor, IBuffHandler buffHandler, Ability ability) {
        this.name = name;
        this.armors = armor;
        this.capeUrl = capeUrl;
        this.buffHandler = buffHandler;
        this.ability = ability;

        if (armorSets == null) armorSets = new ArrayList<ArmorSet>();

        armorSets.add(this);
    }

    public ArmorSet(String name, List<ItemHexxitArmor> armor, IBuffHandler buffHandler, Ability ability) {
        this(name, null, armor, buffHandler, ability);
    }

    public String getName() {
        return name;
    }

    public List<ItemHexxitArmor> getArmors() {
        return armors;
    }

    public String getCapeUrl() {
        return capeUrl;
    }

    public Ability getAbility() {
        return ability;
    }

    public void applyBuffs(EntityPlayer player) {
        buffHandler.applyPlayerBuffs(player);
    }

    public void removeBuffs(EntityPlayer player) {
        buffHandler.removePlayerBuffs(player);
    }
}
