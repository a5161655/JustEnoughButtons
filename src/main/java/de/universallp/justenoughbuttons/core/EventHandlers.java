package de.universallp.justenoughbuttons.core;

import com.mojang.realmsclient.gui.ChatFormatting;
import de.universallp.justenoughbuttons.JEIButtons;
import de.universallp.justenoughbuttons.client.ClientProxy;
import de.universallp.justenoughbuttons.client.MobOverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Level;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static de.universallp.justenoughbuttons.JEIButtons.*;

/**
 * Created by universallp on 11.08.2016 16:07.
 * This file is part of JustEnoughButtons which is licenced
 * under the MOZILLA PUBLIC LICENCE 2.0 - mozilla.org/en-US/MPL/2.0/
 * github.com/UniversalLP/JustEnoughButtons
 */
public class EventHandlers {

    private static boolean gameRuleDayCycle = false;
    private boolean isLMBDown = false;
    private boolean isRMBDown = false;
    private static BlockPos lastPlayerPos = null;

    private boolean drawMobOverlay   = false;
    private boolean magnetMode       = false;

    public static int skipSaveClickCount = 0;
    public static int skipModClickCount = 0;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent e) {
        if (ConfigHandler.showButtons && e.getGui() != null && e.getGui() instanceof GuiContainer) {
            int mouseY = JEIButtons.proxy.getMouseY();
            int mouseX = JEIButtons.proxy.getMouseX();

            if (JEIButtons.isAnyButtonHovered) {
               List<String> tip = getTooltip(JEIButtons.hoveredButton);
                if (tip != null) {
                    GuiUtils.drawHoveringText(tip, mouseX, mouseY < 17 ? 17 : mouseY, ClientProxy.mc.displayWidth, ClientProxy.mc.displayHeight, -1, ClientProxy.mc.fontRendererObj);
                    RenderHelper.disableStandardItemLighting();
                }
            }


            if (ConfigHandler.enableSubsets)
                ModSubsetButtonHandler.drawSubsetList(mouseX, mouseY);
        }


        if (e.getGui() instanceof GuiConfig) {
            GuiConfigEntries eL = ((GuiConfig) e.getGui()).entryList;
            GuiConfig cfg = (GuiConfig) e.getGui();
            if (cfg.titleLine2 != null && cfg.titleLine2.equals(ConfigHandler.CATEGORY_POSITION)) {
                int y = getInt(1, eL);
                int x = getInt(0, eL);
                GuiUtils.drawGradientRect(10, x, y, x + 75, y + 75, 0x77888888, 0x77888888);
                ClientProxy.mc.fontRendererObj.drawString("[Buttons]", x + 14, y + 10, 0xFFFFFF);
            }
        }
    }

    private static int getInt(int i, GuiConfigEntries eL) {
        if (i < eL.getSize() && eL.getListEntry(i) != null && String.valueOf(eL.getListEntry(i).getCurrentValue()).length() > 0 && String.valueOf(eL.getListEntry(i).getCurrentValue()).length() < 5
                && !String.valueOf(eL.getListEntry(i).getCurrentValue()).equals("-"))
            return Integer.valueOf(String.valueOf(eL.getListEntry(i).getCurrentValue()));
        return -1;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrawBackgroundEventPost(GuiScreenEvent.BackgroundDrawnEvent e) {
        if (JEIButtons.configHasChanged) {
            JEIButtons.configHasChanged = false;
            setUpPositions();
        }

        if (JEIButtons.isServerSidePresent && e.getGui() instanceof GuiMainMenu) {
            JEIButtons.isServerSidePresent = false;
            JEIButtons.isSpongePresent = false;
        } else if (ConfigHandler.showButtons && e.getGui() != null && e.getGui() instanceof GuiContainer) {
            int mouseY = JEIButtons.proxy.getMouseY();
            int mouseX = JEIButtons.proxy.getMouseX();
            GuiContainer g = (GuiContainer) e.getGui();
            EntityPlayerSP pl = ClientProxy.player;

            if (btnGameMode == EnumButtonCommands.SPECTATE && !ConfigHandler.enableSpectatoreMode || btnGameMode == EnumButtonCommands.ADVENTURE && !ConfigHandler.enableAdventureMode)
                btnGameMode = btnGameMode.cycle();

            JEIButtons.isAnyButtonHovered = false;
            gameRuleDayCycle = ClientProxy.mc.world.getGameRules().getBoolean("doDaylightCycle");
            {
                btnGameMode.draw(g);
                btnTrash.draw(g);
                btnSun.draw(g);
                btnRain.draw(g);
                btnDay.draw(g);
                btnNight.draw(g);
                btnNoMobs.draw(g);
                btnFreeze.draw(g);
                btnMagnet.draw(g);
            }

            if (JEIButtons.ConfigHandler.enableSaves)
                InventorySaveHandler.drawButtons(mouseX, mouseY);

            if (ConfigHandler.enableSubsets)
                ModSubsetButtonHandler.drawButtons(mouseX, mouseY, ClientProxy.getGuiTop((GuiContainer) e.getGui()));

            for (EnumButtonCommands btn : btnCustom)
                btn.draw(g);

            adjustGamemode();

            if (Mouse.isButtonDown(0) && !isLMBDown) {
                isLMBDown = true;

                if (JEIButtons.isAnyButtonHovered && JEIButtons.hoveredButton.isEnabled) { // Utility Buttons
                    String command = JEIButtons.hoveredButton.getCommand();

                    switch (JEIButtons.hoveredButton) {
                        case FREEZETIME:
                            command += " " + (gameRuleDayCycle ? "false" : "true");
                            break;
                        case DELETE:
                            ItemStack draggedStack = pl.inventory.getItemStack();
                            if (draggedStack.isEmpty()) {
                                if (GuiScreen.isShiftKeyDown() && ConfigHandler.enableClearInventory)
                                    command = "clear";
                                else
                                    command = null;
                            } else {
                                String name  = draggedStack.getItem().getRegistryName().toString();

                                command += pl.getDisplayName().getUnformattedText() + " " + name;

                                if (!GuiScreen.isShiftKeyDown()) {
                                    int data = draggedStack.getItemDamage();
                                    command += " " + data;
                                }
                                boolean ghost = draggedStack.hasTagCompound() && draggedStack.getTagCompound().getBoolean("JEI_Ghost");
                                if (ghost)
                                    pl.inventory.setItemStack(ItemStack.EMPTY);
                            }
                            break;

                        case MAGNET:
                            if (JEIButtons.isServerSidePresent) {
                                command = null;
                                CommonProxy.INSTANCE.sendToServer(new MessageMagnetMode(magnetMode));
                                magnetMode = !magnetMode;
                            } else
                                command = "tp @e[type=Item,r=" + ConfigHandler.magnetRadius + "] @p";
                            break;
                        case ADVENTURE:
                        case CREATIVE:
                        case SPECTATE:
                        case SURVIVAL:
                            JEIButtons.btnGameMode = hoveredButton.cycle();
                            break;
                    }

                    if (command != null)
                        JEIButtons.sendCommand(command);

                    JEIButtons.proxy.playClick();
                } else { // Save buttons & Mod subsets
                    if (JEIButtons.ConfigHandler.enableSaves)
                        InventorySaveHandler.click(mouseX, mouseY, false);

                    ModSubsetButtonHandler.click(mouseX, mouseY);
                }
            } else if (!Mouse.isButtonDown(0)) {
                isLMBDown = false;
            }

            if (Mouse.isButtonDown(1) && !isRMBDown) {
                isRMBDown = true;
                InventorySaveHandler.click(mouseX, mouseY, true);
            } else if (!Mouse.isButtonDown(1))
                isRMBDown = false;
        }
    }

    private void adjustGamemode() {
        GameType t = ClientProxy.mc.playerController.getCurrentGameType();
        boolean doSwitch = false;

        if (t == GameType.CREATIVE && btnGameMode == EnumButtonCommands.CREATIVE)
            doSwitch = true;
        else if (t == GameType.SURVIVAL && btnGameMode == EnumButtonCommands.SURVIVAL)
            doSwitch = true;
        else if (t == GameType.ADVENTURE && btnGameMode == EnumButtonCommands.ADVENTURE)
            doSwitch = true;

        else if (t == GameType.SPECTATOR && btnGameMode == EnumButtonCommands.SPECTATE)
            doSwitch = true;

        if (doSwitch)
            btnGameMode = btnGameMode.cycle();
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            InventorySaveHandler.init();
            if (e.getEntity() instanceof EntityPlayer) {
                ClientProxy.player = FMLClientHandler.instance().getClientPlayerEntity();
                if (((EntityPlayer) e.getEntity()).capabilities.isCreativeMode) {
                    JEIButtons.btnGameMode = btnGameMode.cycle();
                } else {
                    JEIButtons.btnGameMode = EnumButtonCommands.CREATIVE;
                }
            }
        } else {
            if (e.getEntity() != null && e.getEntity() instanceof EntityPlayerMP)
                CommonProxy.INSTANCE.sendTo(new MessageNotifyClient(), (EntityPlayerMP) e.getEntity());
        }
    }

    @SubscribeEvent
    public void onWorldLeave(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        if (SaveFileHandler.SAVE_SNAPSHOTS)
            try {
                ClientProxy.saveHandler.saveForPlayer();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
    }

    @SubscribeEvent
    public void handleKeyInputEvent(GuiScreenEvent.KeyboardInputEvent.Post e) {
        GuiScreen gui = ClientProxy.mc.currentScreen;


        if (gui != null && gui instanceof GuiContainer) {
            int keyCode = Keyboard.getEventKey();

            if (ClientProxy.makeCopyKey.isActiveAndMatches(keyCode)) {
                Slot hovered = ((GuiContainer) gui).getSlotUnderMouse();

                if (hovered != null && ClientProxy.player.inventory.getItemStack().isEmpty() && !hovered.getStack().isEmpty() && hovered.getHasStack()) {

                    ItemStack stack = hovered.getStack().copy();
                    stack.setCount(1);
                    NBTTagCompound t = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
                    t.setBoolean("JEI_Ghost", true);
                    stack.setTagCompound(t);
                    ClientProxy.player.inventory.setItemStack(stack);
                }
            } else if (Keyboard.isKeyDown(ClientProxy.hideall.getKeyCode()) && !Keyboard.isRepeatEvent()) {
                ConfigHandler.showButtons = !ConfigHandler.showButtons;
            }
        }

    }

    @SubscribeEvent
    public void onMouseEvent(GuiScreenEvent.MouseInputEvent event) {
        if (Mouse.getEventButton() == 0) {
            if (JEIButtons.isAnyButtonHovered && JEIButtons.hoveredButton == EnumButtonCommands.DELETE && !ClientProxy.player.inventory.getItemStack().isEmpty()) {
                event.setResult(Event.Result.DENY);
                if (event.isCancelable())
                    event.setCanceled(true);
            }

            if (skipSaveClickCount > 0) {
                if (event.isCancelable())
                    event.setCanceled(true);
                event.setResult(Event.Result.DENY);
                skipSaveClickCount--;
            }

            if (skipModClickCount > 0) {
                if (event.isCancelable())
                    event.setCanceled(true);
                event.setResult(Event.Result.DENY);
                skipModClickCount--;
            }
        }

        if (Mouse.getDWheel() != 0 && ModSubsetButtonHandler.isListShown) {
            ModSubsetButtonHandler.scroll(Mouse.getEventDWheel());
        }
    }

    @SubscribeEvent
    public void onKeyPressed(InputEvent.KeyInputEvent event) {
        if (enableOverlays) {
            if (ClientProxy.mobOverlay.isKeyDown())
                drawMobOverlay = !drawMobOverlay;


            if (!drawMobOverlay) {
                MobOverlayRenderer.clearCache();
                lastPlayerPos = null;
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            if (lastPlayerPos == null || !lastPlayerPos.equals(ClientProxy.player.getPosition())) {
                if (drawMobOverlay)
                    MobOverlayRenderer.cacheMobSpawns(ClientProxy.player);

                if (drawMobOverlay)
                    lastPlayerPos = ClientProxy.player.getPosition();
            }
        }
    }

    @SubscribeEvent
    public void onWorldDraw(RenderWorldLastEvent event) {
        if (drawMobOverlay)
            MobOverlayRenderer.renderMobSpawnOverlay();

        if (ClientProxy.mc.currentScreen == null) {
            skipSaveClickCount = 0;
            ModSubsetButtonHandler.isListShown = false;
        }
    }

    @SubscribeEvent
    public void onJoinServer(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        for (String ip : ConfigHandler.spongeServers) {
            if (Minecraft.getMinecraft().getCurrentServerData().serverIP.contains(ip)) {
                JEIButtons.isSpongePresent = true;
                FMLLog.log(Level.INFO, "Sponge support is enabled for this server!");
                break;
            }
        }
        FMLLog.log(Level.INFO, "Sponge support is disabled for this server!");

    }

    public List<String> getTooltip(EnumButtonCommands btn) {
        ArrayList<String> list = new ArrayList<String>();
        if (btn == null)
            return null;

        switch (btn) {
            case ADVENTURE:
                list.add(I18n.format("justenoughbuttons.switchto", I18n.format("gameMode.adventure")));
                break;
            case CREATIVE:
                list.add(I18n.format("justenoughbuttons.switchto", I18n.format("gameMode.creative")));
                break;
            case SPECTATE:
                list.add(I18n.format("justenoughbuttons.switchto", I18n.format("gameMode.spectator")));
                break;
            case SURVIVAL:
                list.add(I18n.format("justenoughbuttons.switchto", I18n.format("gameMode.survival")));
                break;
            case DAY:
                list.add(I18n.format("justenoughbuttons.switchto", I18n.format("justenoughbuttons.timeday")));
                break;
            case NIGHT:
                list.add(I18n.format("justenoughbuttons.switchto", I18n.format("justenoughbuttons.timenight")));
                break;
            case DELETE:
                ItemStack draggedStack = ClientProxy.player.inventory.getItemStack();
                if (draggedStack != null) {
                    list.add(I18n.format("justenoughbuttons.deleteall", I18n.format(draggedStack.getUnlocalizedName() + ".name")));
                    if (GuiScreen.isShiftKeyDown())
                        list.add(ChatFormatting.GRAY + I18n.format("justenoughbuttons.ignoringmeta"));
                } else {
                    list.add(I18n.format("justenoughbuttons.dragitemshere"));
                    list.add(ChatFormatting.GRAY + I18n.format("justenoughbuttons.holdshift"));
                    if (ConfigHandler.enableClearInventory)
                        list.add(ChatFormatting.GRAY + I18n.format("justenoughbuttons.clearinventory"));
                }
                break;
            case FREEZETIME:
                if (gameRuleDayCycle)
                    list.add(I18n.format("justenoughbuttons.freezetime"));
                else
                    list.add(I18n.format("justenoughbuttons.unfreezetime"));
                break;
            case NOMOBS:
                list.add(I18n.format("justenoughbuttons.nomobs"));
                break;
            case RAIN:
                list.add(I18n.format("commands.weather.rain"));
                break;
            case SUN:
                list.add(I18n.format("commands.weather.clear"));
                break;
            case MAGNET:
                if (JEIButtons.isServerSidePresent) {
                    if (magnetMode)
                        list.add(I18n.format("justenoughbuttons.magnet.off"));
                    else
                        list.add(I18n.format("justenoughbuttons.magnet.on"));
                } else
                    list.add(I18n.format("justenoughbuttons.magnetitems"));

                break;
            case CUSTOM1:
            case CUSTOM2:
            case CUSTOM3:
            case CUSTOM4:
                if (ConfigHandler.customName[btn.id].equals(""))
                    list.add(I18n.format("justenoughbuttons.customcommand", "/" + ConfigHandler.customCommand[btn.id]));
                else
                    list.add(ConfigHandler.customName[btn.id]);
                break;
        }

        return list;
    }
}
