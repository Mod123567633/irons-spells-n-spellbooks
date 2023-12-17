package io.redspace.ironsspellbooks.gui.overlays;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.spell.SpellData;
import io.redspace.ironsspellbooks.capabilities.spellbook.SpellBookData;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.gui.overlays.network.ServerboundSelectSpell;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.setup.Messages;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SpellSelectionManager {
    public static final String MAINHAND = EquipmentSlot.MAINHAND.getName();
    public static final String OFFHAND = EquipmentSlot.OFFHAND.getName();

    private final List<SpellItem> spellItemList;
    private SpellSelection spellSelection = null;
    private int selectionIndex = -1;

    public SpellSelectionManager(Player player) {
        this.spellItemList = new ArrayList<>();
        init(player);
    }

    private void init(Player player) {
        spellSelection = ClientMagicData.getSyncedSpellData(player).getSpellSelection();

        initSpellbook(player);

        //TODO: support dynamic slot detection
        initItem(player.getMainHandItem(), MAINHAND);
        initItem(player.getOffhandItem(), OFFHAND);

        //Just in case someone wants to mixin to this
        initOther(player);

        if (selectionIndex == -1 && spellSelection.lastIndex != -1) {
            tryLastSelection();
        }
    }

    private void initSpellbook(Player player) {
        var spellbookStack = Utils.getPlayerSpellbookStack(player);
        var spellBookData = SpellBookData.getSpellBookData(spellbookStack);
        var activeSpellbookSpells = spellBookData.getActiveInscribedSpells();

        for (int i = 0; i < activeSpellbookSpells.size(); i++) {
            spellItemList.add(new SpellItem(activeSpellbookSpells.get(i), Curios.SPELLBOOK_SLOT, i));
        }

        if (spellSelection.equipmentSlot.equals(Curios.SPELLBOOK_SLOT) && spellSelection.index < spellItemList.size()) {
            selectionIndex = spellSelection.index;
        }
    }

    private void initItem(ItemStack itemStack, String slot) {
        //TODO: expand this to allow an item to have more than 1 spell
        var spellData = SpellData.getSpellData(itemStack, false);
        if (spellData != SpellData.EMPTY) {
            spellItemList.add(new SpellItem(spellData, slot, 0));
            if (spellSelection.equipmentSlot.equals(slot)) {
                selectionIndex = spellItemList.size() - 1;
            }
        }
    }

    private void initOther(Player player) {
        //Just in case someone wants to mixin to this
    }

    private void tryLastSelection() {
        if (spellSelection.lastEquipmentSlot.equals(Curios.SPELLBOOK_SLOT) && spellSelection.lastIndex >= 0) {
            var spellbookSpells = getSpellsForSlot(Curios.SPELLBOOK_SLOT);
            if (spellSelection.lastIndex < spellbookSpells.size()) {
                spellSelection = new SpellSelection(Curios.SPELLBOOK_SLOT, spellSelection.lastIndex);
            }
        } else if (spellSelection.lastEquipmentSlot.equals(MAINHAND)) {
            spellSelection = new SpellSelection(MAINHAND, 0);
        } else if (spellSelection.lastEquipmentSlot.equals(OFFHAND)) {
            spellSelection = new SpellSelection(OFFHAND, 0);
        }
    }

    public SpellSelection getCurrentSelection() {
        return spellSelection;
    }

    public void makeSelection(int index) {
        if (index >= 0 && index < spellItemList.size()) {
            var item = spellItemList.get(index);
            spellSelection.makeSelection(item.slot, item.slotIndex);
        }
        Messages.sendToServer(new ServerboundSelectSpell(spellSelection));
    }

    public SpellData getSpellData(int index) {
        return spellItemList.get(index).spellData;
    }

    public int getSelectionIndex() {
        return selectionIndex;
    }

    public SpellItem getSelectedSpellItem() {
        return spellItemList.get(selectionIndex);
    }

    public SpellData getSelectedSpellData() {
        return spellItemList.get(selectionIndex).spellData;
    }

    public List<SpellItem> getSpellsForSlot(String slot) {
        return spellItemList.stream().filter(spellItem -> spellItem.slot.equals(slot)).toList();
    }

    public SpellItem getSpellForSlot(String slot, int index) {
        var spells = getSpellsForSlot(slot);

        if (index >= 0 && index < spells.size()) {
            return spells.get(index);
        }

        //todo: maybe use an empty or option here
        return null;
    }

    public int getSpellCount() {
        return spellItemList.size();
    }

    public static class SpellItem {
        public SpellData spellData;
        public String slot;
        public int slotIndex;

        public SpellItem(SpellData spell, String slot, int slotIndex) {
            this.spellData = spell;
            this.slot = slot;
            this.slotIndex = slotIndex;
        }
    }
}
