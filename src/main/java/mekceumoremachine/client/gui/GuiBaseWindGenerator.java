package mekceumoremachine.client.gui;

import java.util.ArrayList;
import java.util.List;
import mekanism.api.EnumColor;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.config.MekanismConfig;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.client.gui.GuiGenerator;
import mekanism.generators.client.gui.element.GuiStateTexture;
import mekanism.generators.common.MekanismGenerators;
import mekceumoremachine.common.inventory.container.ContainerBaseWindGenerator;
import mekceumoremachine.common.tile.generator.TileEntityBaseWindGenerator;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiBaseWindGenerator extends GuiGenerator<TileEntityBaseWindGenerator, ContainerBaseWindGenerator> {

    public GuiBaseWindGenerator(InventoryPlayer inventory, TileEntityBaseWindGenerator tile) {
        super(tile, new ContainerBaseWindGenerator(inventory, tile));
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addButton(new GuiInnerScreen(this, 48, 21, 80, 44, this::getScreenText));
        addButton(new GuiEnergyTab(this, () -> getEnergyTabText(tileEntity.getActive() ? getGenerationRate() : 0)));
        addButton(new GuiVerticalPowerBar(this, tileEntity.getEnergyContainer(), 164, 15));
        addButton(new GuiStateTexture(this, 18, 35, tileEntity::getActive,
              new ResourceLocation(MekanismGenerators.MODID, "gui/wind_on.png"),
              new ResourceLocation(MekanismGenerators.MODID, "gui/wind_off.png")));
    }

    private List<ITextComponent> getScreenText() {
        List<ITextComponent> list = new ArrayList<>();
        list.add(energy(tileEntity.getEnergy(), tileEntity.getMaxEnergy()));
        list.add(text(LangUtils.localize("gui.power") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getActive() ? getGenerationRate() : 0) + "/t"));
        list.add(text(LangUtils.localize("gui.out") + ": " + MekanismUtils.getEnergyDisplay(tileEntity.getMaxOutput()) + "/t"));
        if (!tileEntity.getActive()) {
            String key = tileEntity.isBlacklistDimension() ? "gui.noWind" : "gui.skyBlocked";
            list.add(text(EnumColor.DARK_RED + LangUtils.localize(key)));
        }
        return list;
    }

    private double getGenerationRate() {
        return MekanismConfig.current().generators.windGenerationMin.val() * tileEntity.getCurrentMultiplier() * tileEntity.processes();
    }
}
