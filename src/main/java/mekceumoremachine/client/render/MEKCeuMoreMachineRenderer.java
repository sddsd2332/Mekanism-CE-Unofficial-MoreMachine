package mekceumoremachine.client.render;

import mekceumoremachine.client.render.tileentity.machine.RenderTierChemicalDissolutionChamber;
import mekceumoremachine.client.render.tileentity.machine.RenderTierIsotopicCentrifuge;
import mekceumoremachine.client.render.tileentity.machine.RenderTierNutritionalLiquifier;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MEKCeuMoreMachineRenderer {

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new MEKCeuMoreMachineRenderer());
    }

    @SubscribeEvent
    public void onStitch(TextureStitchEvent.Pre event) {
        RenderTierChemicalDissolutionChamber.resetDisplayInts();
        RenderTierIsotopicCentrifuge.resetDisplayInts();
        RenderTierNutritionalLiquifier.resetDisplayInts();
    }
}
