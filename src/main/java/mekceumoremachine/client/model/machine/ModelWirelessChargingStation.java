package mekceumoremachine.client.model.machine;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelWirelessChargingStation extends ModelBase {

    ModelRenderer lights;
    ModelRenderer panel31_r1;
    ModelRenderer panel32_r1;
    ModelRenderer panel33_r1;
    ModelRenderer panel34_r1;
    ModelRenderer ports;
    ModelRenderer port_side;
    ModelRenderer port_side2;
    ModelRenderer port_side3;
    ModelRenderer port_side4;
    ModelRenderer port_bottom;
    ModelRenderer body;
    ModelRenderer top;
    ModelRenderer panel34_r2;
    ModelRenderer panel33_r2;
    ModelRenderer panel32_r2;
    ModelRenderer panel31_r2;
    ModelRenderer top_side;
    ModelRenderer top_side2;
    ModelRenderer base2;
    ModelRenderer pole;

    public ModelWirelessChargingStation() {
        textureWidth = 128;
        textureHeight = 128;

        lights = new ModelRenderer(this);
        lights.setRotationPoint(0.0F, 0.0F, 0.0F);
        lights.cubeList.add(new ModelBox(lights, 82, 5, -2.0F, -17.0F, -2.0F, 4, 1, 4, 0.0F, false));
        lights.cubeList.add(new ModelBox(lights, 82, 11, -2.0F, -19.0F, -2.0F, 4, 1, 4, 0.0F, false));
        lights.cubeList.add(new ModelBox(lights, 88, 50, -1.0F, -13.0F, -4.0F, 2, 3, 2, 0.0F, false));
        lights.cubeList.add(new ModelBox(lights, 88, 50, -1.0F, -13.0F, 2.0F, 2, 3, 2, 0.0F, false));
        lights.cubeList.add(new ModelBox(lights, 34, 86, 2.0F, -15.0F, -1.0F, 2, 5, 2, 0.0F, false));
        lights.cubeList.add(new ModelBox(lights, 34, 86, -4.0F, -15.0F, -1.0F, 2, 5, 2, 0.0F, false));
        lights.cubeList.add(new ModelBox(lights, 41, 28, -4.0F, -14.5F, -7.0F, 8, 1, 0, 0.0F, false));
        lights.cubeList.add(new ModelBox(lights, 41, 28, -4.0F, -14.5F, 7.0F, 8, 1, 0, 0.0F, false));

        panel31_r1 = new ModelRenderer(this);
        panel31_r1.setRotationPoint(4.0F, -13.0F, -7.0F);
        lights.addChild(panel31_r1);
        setRotation(panel31_r1, 0.0F, -0.7854F, 0.0F);
        panel31_r1.cubeList.add(new ModelBox(panel31_r1, 65, 17, 0.0F, -1.5F, 0.0F, 4, 1, 0, 0.0F, false));

        panel32_r1 = new ModelRenderer(this);
        panel32_r1.setRotationPoint(-4.0F, -13.0F, -7.0F);
        lights.addChild(panel32_r1);
        setRotation(panel32_r1, 0.0F, 0.7854F, 0.0F);
        panel32_r1.cubeList.add(new ModelBox(panel32_r1, 65, 19, -4.0F, -1.5F, 0.0F, 4, 1, 0, 0.0F, false));

        panel33_r1 = new ModelRenderer(this);
        panel33_r1.setRotationPoint(4.0F, -13.0F, 7.0F);
        lights.addChild(panel33_r1);
        setRotation(panel33_r1, 0.0F, 0.7854F, 0.0F);
        panel33_r1.cubeList.add(new ModelBox(panel33_r1, 65, 19, 0.0F, -1.5F, 0.0F, 4, 1, 0, 0.0F, false));

        panel34_r1 = new ModelRenderer(this);
        panel34_r1.setRotationPoint(-4.0F, -13.0F, 7.0F);
        lights.addChild(panel34_r1);
        setRotation(panel34_r1, 0.0F, -0.7854F, 0.0F);
        panel34_r1.cubeList.add(new ModelBox(panel34_r1, 65, 17, -4.0F, -1.5F, 0.0F, 4, 1, 0, 0.0F, false));

        ports = new ModelRenderer(this);
        ports.setRotationPoint(0.0F, 0.0F, 0.0F);


        port_side = new ModelRenderer(this);
        port_side.setRotationPoint(-7.0F, 20.0F, -4.0F);
        ports.addChild(port_side);
        port_side.cubeList.add(new ModelBox(port_side, 17, 76, 3.0F, -8.0F, -4.0F, 8, 8, 1, 0.0F, false));
        port_side.cubeList.add(new ModelBox(port_side, 74, 17, 4.0F, -7.0F, -3.0F, 6, 6, 3, 0.0F, false));

        port_side2 = new ModelRenderer(this);
        port_side2.setRotationPoint(-7.0F, 20.0F, -4.0F);
        ports.addChild(port_side2);
        port_side2.cubeList.add(new ModelBox(port_side2, 50, 42, 14.0F, -8.0F, 0.0F, 1, 8, 8, 0.0F, false));
        port_side2.cubeList.add(new ModelBox(port_side2, 69, 45, 11.0F, -7.0F, 1.0F, 3, 6, 6, 0.0F, false));

        port_side3 = new ModelRenderer(this);
        port_side3.setRotationPoint(-7.0F, 20.0F, -4.0F);
        ports.addChild(port_side3);
        port_side3.cubeList.add(new ModelBox(port_side3, 17, 76, 3.0F, -8.0F, 11.0F, 8, 8, 1, 0.0F, false));
        port_side3.cubeList.add(new ModelBox(port_side3, 36, 76, 4.0F, -7.0F, 8.0F, 6, 6, 3, 0.0F, false));

        port_side4 = new ModelRenderer(this);
        port_side4.setRotationPoint(-7.0F, 20.0F, -4.0F);
        ports.addChild(port_side4);
        port_side4.cubeList.add(new ModelBox(port_side4, 50, 42, -1.0F, -8.0F, 0.0F, 1, 8, 8, 0.0F, false));
        port_side4.cubeList.add(new ModelBox(port_side4, 69, 58, 0.0F, -7.0F, 1.0F, 3, 6, 6, 0.0F, false));

        port_bottom = new ModelRenderer(this);
        port_bottom.setRotationPoint(0.0F, 24.0F, 0.0F);
        ports.addChild(port_bottom);
        port_bottom.cubeList.add(new ModelBox(port_bottom, 33, 32, -4.0F, -1.0F, -4.0F, 8, 1, 8, 0.0F, false));

        body = new ModelRenderer(this);
        body.setRotationPoint(0.0F, 0.0F, 0.0F);


        top = new ModelRenderer(this);
        top.setRotationPoint(0.0F, 0.0F, 0.0F);
        body.addChild(top);
        top.cubeList.add(new ModelBox(top, 0, 49, -3.0F, -16.0F, -3.0F, 6, 5, 6, 0.0F, false));
        top.cubeList.add(new ModelBox(top, 17, 86, -2.0F, -21.0F, -2.0F, 4, 1, 4, 0.0F, false));
        top.cubeList.add(new ModelBox(top, 69, 37, -3.0F, -18.0F, -3.0F, 6, 1, 6, 0.0F, false));
        top.cubeList.add(new ModelBox(top, 69, 37, -3.0F, -20.0F, -3.0F, 6, 1, 6, 0.0F, false));

        panel34_r2 = new ModelRenderer(this);
        panel34_r2.setRotationPoint(-4.0F, -13.0F, 7.0F);
        top.addChild(panel34_r2);
        setRotation(panel34_r2, 0.0F, -0.7854F, 0.0F);
        panel34_r2.cubeList.add(new ModelBox(panel34_r2, 88, 45, -4.0F, -2.5F, -1.0F, 4, 3, 1, 0.0F, false));

        panel33_r2 = new ModelRenderer(this);
        panel33_r2.setRotationPoint(4.0F, -13.0F, 7.0F);
        top.addChild(panel33_r2);
        setRotation(panel33_r2, 0.0F, 0.7854F, 0.0F);
        panel33_r2.cubeList.add(new ModelBox(panel33_r2, 0, 87, 0.0F, -2.5F, -1.0F, 4, 3, 1, 0.0F, false));

        panel32_r2 = new ModelRenderer(this);
        panel32_r2.setRotationPoint(-4.0F, -13.0F, -7.0F);
        top.addChild(panel32_r2);
        setRotation(panel32_r2, 0.0F, 0.7854F, 0.0F);
        panel32_r2.cubeList.add(new ModelBox(panel32_r2, 83, 86, -4.0F, -2.5F, 0.0F, 4, 3, 1, 0.0F, false));

        panel31_r2 = new ModelRenderer(this);
        panel31_r2.setRotationPoint(4.0F, -13.0F, -7.0F);
        top.addChild(panel31_r2);
        setRotation(panel31_r2, 0.0F, -0.7854F, 0.0F);
        panel31_r2.cubeList.add(new ModelBox(panel31_r2, 55, 76, 0.0F, -2.5F, 0.0F, 4, 3, 1, 0.0F, false));

        top_side = new ModelRenderer(this);
        top_side.setRotationPoint(0.0F, 0.0F, 0.0F);
        top.addChild(top_side);
        top_side.cubeList.add(new ModelBox(top_side, 17, 61, -1.0F, -15.0F, -6.0F, 2, 2, 3, 0.0F, false));
        top_side.cubeList.add(new ModelBox(top_side, 72, 81, -4.0F, -15.5F, -7.0F, 8, 3, 1, 0.0F, false));

        top_side2 = new ModelRenderer(this);
        top_side2.setRotationPoint(0.0F, 0.0F, 0.0F);
        top.addChild(top_side2);
        top_side2.cubeList.add(new ModelBox(top_side2, 17, 61, -1.0F, -15.0F, 3.0F, 2, 2, 3, 0.0F, false));
        top_side2.cubeList.add(new ModelBox(top_side2, 82, 0, -4.0F, -15.5F, 6.0F, 8, 3, 1, 0.0F, false));

        base2 = new ModelRenderer(this);
        base2.setRotationPoint(0.0F, 0.0F, 0.0F);
        body.addChild(base2);
        base2.cubeList.add(new ModelBox(base2, 0, 0, -8.0F, 21.99F, -8.0F, 16, 2, 16, 0.0F, false));
        base2.cubeList.add(new ModelBox(base2, 0, 19, -5.0F, 20.0F, -5.0F, 10, 2, 10, 0.0F, false));
        base2.cubeList.add(new ModelBox(base2, 0, 32, -4.0F, 12.0F, -4.0F, 8, 8, 8, 0.0F, false));

        pole = new ModelRenderer(this);
        pole.setRotationPoint(3.0F, -8.0F, -3.0F);
        body.addChild(pole);
        pole.cubeList.add(new ModelBox(pole, 33, 42, -5.0F, -3.0F, 1.0F, 4, 21, 4, 0.0F, false));
        pole.cubeList.add(new ModelBox(pole, 66, 28, -6.0F, 18.0F, 0.0F, 6, 2, 6, 0.0F, false));
    }


    public void renderModelTier(float size){
        lights.render(size);
    }

    public void renderModel(float size) {
        ports.render(size);
        body.render(size);
    }


    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.rotateAngleX = x;
        model.rotateAngleY = y;
        model.rotateAngleZ = z;
    }
}
