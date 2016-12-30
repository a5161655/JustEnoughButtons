package de.universallp.justenoughbuttons.core;

import de.universallp.justenoughbuttons.JEIButtons;
import de.universallp.justenoughbuttons.client.ClientProxy;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;


/**
 * Created by universallp on 12.09.16 15:02.
 * This file is part of JustEnoughButtons which is licenced
 * under the MOZILLA PUBLIC LICENCE 2.0 - mozilla.org/en-US/MPL/2.0/
 * github.com/UniversalLP/JustEnoughButtons
 */
public class InventorySaveHandler {

    private static GuiButton[] saveButtons = new GuiButton[4];
    public static InventorySnapshot[] saves = new InventorySnapshot[4];
    private static final String replaceCommand = "/replaceitem entity @p %s %s %s %s %s";
    public static boolean skipClick = false;

    public static void init() {
        String load = I18n.format("justenoughbuttons.load") + " ";
        String save = I18n.format("justenoughbuttons.save") + " ";

        for (int i = 0; i < saveButtons.length; i++) {
            saveButtons[i] = new GuiButton(i, JEIButtons.ConfigHandler.xOffset, 110 + JEIButtons.ConfigHandler.yOffset + 22 * i, 50, 20, (saves[i] == null ? save : load) + (i + 1));
        }
    }

    static boolean click(int mouseX, int mouseY, boolean rightMouse) {
        boolean flag = false;

        if (!rightMouse) {
            for (int i = 0; i < saveButtons.length; i++)
                if (saveButtons[i].mousePressed(ClientProxy.mc, mouseX, mouseY)) {
                    JEIButtons.proxy.playClick();

                    if (saves[i] == null) {
                        saves[i] = new InventorySnapshot(ClientProxy.player.inventory);
                        String load = I18n.format("justenoughbuttons.load") + " ";
                        saveButtons[i].displayString = load + (i + 1);
                    } else {
                        if (!ClientProxy.player.inventory.getItemStack().func_190926_b()) {
                            saves[i].icon = ClientProxy.player.inventory.getItemStack().copy();
                        } else {
                            saves[i].giveToPlayer();
                        }
                    }

                    flag = true;
                    break;
                }
        } else {
            for (int i = 0; i < saveButtons.length; i++)
                if (saveButtons[i].mousePressed(ClientProxy.mc, mouseX, mouseY)) {
                    JEIButtons.proxy.playClick();
                    saves[i] = null;
                    String save = I18n.format("justenoughbuttons.save") + " ";
                    saveButtons[i].displayString = save + (i + 1);
                    flag = true;
                    break;
                }
        }

        return flag;
    }

    static void drawButtons(int mouseX, int mouseY) {
        boolean anyButtonHovered = false;

        if (saves == null || saves.length <= 0)
            return;

        for (GuiButton s : saveButtons) {
            if (!ClientProxy.player.canCommandSenderUseCommand(1, ""))
                s.enabled = false;
            else
                s.enabled = true;

            s.drawButton(ClientProxy.mc, mouseX, mouseY);

            if (s.isMouseOver()) {
                skipClick = true;
                anyButtonHovered = true;
            }

            if (saves[s.id] != null && saves[s.id].icon != null) {
                RenderHelper.enableStandardItemLighting();
                RenderHelper.enableGUIStandardItemLighting();
                ClientProxy.mc.getRenderItem().renderItemAndEffectIntoGUI(saves[s.id].icon, s.xPosition + s.width + 2, s.yPosition + 2);
                RenderHelper.disableStandardItemLighting();

            }
        }

        if (!anyButtonHovered)
            skipClick = false;
    }

    public static class InventorySnapshot {
        public ItemStack icon;
        NBTTagCompound[] mainInventory;
        NBTTagCompound[] armorInventory;
        NBTTagCompound offHandInventory;

        public InventorySnapshot(NBTTagCompound icon, NBTTagCompound[] mainInventory, NBTTagCompound[] armorInventory, NBTTagCompound offHandInventory) {
            this.icon = new ItemStack(icon);
            this.mainInventory = mainInventory;
            this.armorInventory = armorInventory;
            this.offHandInventory = offHandInventory;
        }

        InventorySnapshot(InventoryPlayer inv) {
            this.mainInventory = new NBTTagCompound[inv.mainInventory.size()];
            this.armorInventory = new NBTTagCompound[inv.armorInventory.size()];
            this.offHandInventory = new NBTTagCompound();

            for (int i = 0; i < inv.mainInventory.size(); i++) {
                NBTTagCompound nbt = new NBTTagCompound();
                inv.mainInventory.get(i).writeToNBT(nbt);
                this.mainInventory[i] = nbt;
            }

            for (int i = 0; i < inv.armorInventory.size(); i++){
                    NBTTagCompound nbt = new NBTTagCompound();
                    inv.armorInventory.get(i).writeToNBT(nbt);
                    this.armorInventory[i] = nbt;
            }


            NBTTagCompound nbt = new NBTTagCompound();
            inv.offHandInventory.get(0).writeToNBT(nbt);
            this.offHandInventory = nbt;

        }

        void giveToPlayer() {
            if (!JEIButtons.isServerSidePresent) {
                ClientProxy.player.sendChatMessage("/clear");
                String nbt = "";
                String cmd = "";

                for (int i = 0; i < mainInventory.length; i++) {
                    if (mainInventory[i] == null)
                        continue;
                    ItemStack s = new ItemStack(mainInventory[i]);
                    nbt = s.getTagCompound() != null  ? s.getTagCompound().toString() : "";
                    if (i < 9)
                        cmd = String.format(replaceCommand, "slot.hotbar." + i,  s.getItem().getRegistryName(), s.func_190916_E(), s.getItemDamage(), nbt);
                    else
                        cmd = String.format(replaceCommand, "slot.inventory." + (i - 9),  s.getItem().getRegistryName(), s.func_190916_E(), s.getItemDamage(), nbt);
                    if (checkCommandLength(cmd))
                        ClientProxy.player.sendChatMessage(cmd);
                }

                for (int i = 0; i < armorInventory.length; i++) {
                    if (armorInventory[i] == null)
                        continue;
                    ItemStack s = new ItemStack(armorInventory[i]);
                    nbt = s.getTagCompound() != null ? s.getTagCompound().toString() : "";
                    cmd = String.format(replaceCommand, "slot.armor." + idToSlot(i),  s.getItem().getRegistryName(), s.func_190916_E(), s.getItemDamage(), nbt);
                    if (checkCommandLength(cmd))
                        ClientProxy.player.sendChatMessage(cmd);
                }

                if (offHandInventory != null) {
                    ItemStack s = new ItemStack(offHandInventory);
                    nbt = s.getTagCompound() != null ? s.getTagCompound().toString() : "";
                    cmd = String.format(replaceCommand, "slot.weapon.offhand",  s.getItem().getRegistryName(), s.func_190916_E(), s.getItemDamage(), nbt);
                    if (checkCommandLength(cmd))
                        ClientProxy.player.sendChatMessage(cmd);

                }
            } else {
                CommonProxy.INSTANCE.sendToServer(new MessageRequestStacks(mainInventory, armorInventory, offHandInventory));
            }

            ClientProxy.player.inventory.markDirty();
        }

        boolean checkCommandLength(String cmd) {
            if (cmd.length() > 100) {
                ClientProxy.player.addChatMessage(new TextComponentString(I18n.format("justenoughbuttons.nbttoolong")));
                return false;
            }
            return true;
        }

        String idToSlot(int i) {
            switch (i) {
                case 0:
                    return "feet";
                case 1:
                    return "legs";
                case 2:
                    return "chest";
                case 3:
                    return "head";
            }
            return "head";
        }
    }
}
