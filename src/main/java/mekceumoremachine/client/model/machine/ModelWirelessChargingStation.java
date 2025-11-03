package mekceumoremachine.client.model.machine;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelWirelessChargingStation extends ModelBase {

     ModelRenderer head;
     ModelRenderer north2;
     ModelRenderer east2;
     ModelRenderer south2;
     ModelRenderer west2;
     ModelRenderer baseRim;
     ModelRenderer base;
     ModelRenderer post1a;
     ModelRenderer post1b;
     ModelRenderer post1c;
     ModelRenderer post1d;
     ModelRenderer north;
     ModelRenderer plate;
     ModelRenderer plateConnector2;
     ModelRenderer wire;
     ModelRenderer plateConnector;
     ModelRenderer east;
     ModelRenderer plate2;
     ModelRenderer plateConnector3;
     ModelRenderer wire2;
     ModelRenderer plateConnector4;
     ModelRenderer south;
     ModelRenderer plate3;
     ModelRenderer plateConnector5;
     ModelRenderer wire3;
     ModelRenderer plateConnector6;
     ModelRenderer west;
     ModelRenderer plate4;
     ModelRenderer plateConnector7;
     ModelRenderer wire4;
     ModelRenderer plateConnector8;


    public ModelWirelessChargingStation() {
        textureWidth = 256;
        textureHeight = 256;

        head = new ModelRenderer(this);
        head.setRotationPoint(0.0F, -48.0F, 0.0F);
        head.cubeList.add(new ModelBox(head, 28, 14, -4.0F, -4.0F, -4.0F, 8, 8, 8, 0.0F, false));

        north2 = new ModelRenderer(this);
        north2.setRotationPoint(0.0F, -3.0F, 0.0F);
        head.addChild(north2);
        north2.cubeList.add(new ModelBox(north2, 28, 46, -1.0F, 2.0F, -10.0F, 2, 2, 6, 0.0F, false));
        north2.cubeList.add(new ModelBox(north2, 60, 18, -5.0F, -7.0F, -11.0F, 10, 20, 1, 0.0F, false));

        east2 = new ModelRenderer(this);
        east2.setRotationPoint(0.0F, -3.0F, 0.0F);
        head.addChild(east2);
        setRotation(east2, 0.0F, 1.5708F, 0.0F);
        east2.cubeList.add(new ModelBox(east2, 28, 46, -1.0F, 2.0F, -10.0F, 2, 2, 6, 0.0F, false));
        east2.cubeList.add(new ModelBox(east2, 60, 18, -5.0F, -7.0F, -11.0F, 10, 20, 1, 0.0F, false));

        south2 = new ModelRenderer(this);
        south2.setRotationPoint(0.0F, -3.0F, 0.0F);
        head.addChild(south2);
        setRotation(south2, 0.0F, 3.1416F, 0.0F);
        south2.cubeList.add(new ModelBox(south2, 28, 46, -1.0F, 2.0F, -10.0F, 2, 2, 6, 0.0F, false));
        south2.cubeList.add(new ModelBox(south2, 60, 18, -5.0F, -7.0F, -11.0F, 10, 20, 1, 0.0F, false));

        west2 = new ModelRenderer(this);
        west2.setRotationPoint(0.0F, -3.0F, 0.0F);
        head.addChild(west2);
        setRotation(west2, 0.0F, -1.5708F, 0.0F);
        west2.cubeList.add(new ModelBox(west2, 28, 46, -1.0F, 2.0F, -10.0F, 2, 2, 6, 0.0F, false));
        west2.cubeList.add(new ModelBox(west2, 60, 18, -5.0F, -7.0F, -11.0F, 10, 20, 1, 0.0F, false));

        baseRim = new ModelRenderer(this);
        baseRim.setRotationPoint(-6.0F, 21.0F, -6.0F);
        baseRim.cubeList.add(new ModelBox(baseRim, 28, 0, 0.0F, 0.0F, 0.0F, 12, 2, 12, 0.0F, false));

        base = new ModelRenderer(this);
        base.setRotationPoint(-8.0F, 22.0F, -8.0F);
        base.cubeList.add(new ModelBox(base, 60, 0, 0.0F, 0.0F, 0.0F, 16, 2, 16, 0.0F, false));

        post1a = new ModelRenderer(this);
        post1a.setRotationPoint(0.0F, -46.0F, 0.0F);
        setRotation(post1a, -0.0349F, 0.0F, 0.0349F);
        post1a.cubeList.add(new ModelBox(post1a, 0, 0, -2.5F, 0.0F, -2.5F, 5, 68, 5, 0.0F, false));

        post1b = new ModelRenderer(this);
        post1b.setRotationPoint(0.0F, -46.0F, 0.0F);
        setRotation(post1b, 0.0349F, 0.0F, -0.0349F);
        post1b.cubeList.add(new ModelBox(post1b, 0, 0, -2.5F, 0.0F, -2.5F, 5, 68, 5, 0.0F, false));

        post1c = new ModelRenderer(this);
        post1c.setRotationPoint(0.0F, -46.0F, 0.0F);
        setRotation(post1c, 0.0347F, 0.0F, 0.0347F);
        post1c.cubeList.add(new ModelBox(post1c, 0, 0, -2.5F, 0.0F, -2.5F, 5, 68, 5, 0.0F, false));

        post1d = new ModelRenderer(this);
        post1d.setRotationPoint(0.0F, -46.0F, 0.0F);
        setRotation(post1d, -0.0347F, 0.0F, -0.0347F);
        post1d.cubeList.add(new ModelBox(post1d, 0, 0, -2.5F, 0.0F, -2.5F, 5, 68, 5, 0.0F, false));

        north = new ModelRenderer(this);
        north.setRotationPoint(0.0F, 24.0F, 0.0F);


        plate = new ModelRenderer(this);
        plate.setRotationPoint(-4.0F, -12.0F, -8.0F);
        north.addChild(plate);
        plate.cubeList.add(new ModelBox(plate, 60, 39, 0.0F, 0.0F, 0.0F, 8, 8, 1, 0.0F, false));

        plateConnector2 = new ModelRenderer(this);
        plateConnector2.setRotationPoint(-3.0F, -11.0F, -7.0F);
        north.addChild(plateConnector2);
        plateConnector2.cubeList.add(new ModelBox(plateConnector2, 28, 30, 0.0F, 0.0F, 0.0F, 6, 6, 10, 0.0F, false));

        wire = new ModelRenderer(this);
        wire.setRotationPoint(0.0F, -70.0F, -1.5F);
        north.addChild(wire);
        setRotation(wire, -0.0349F, 0.0F, 0.0F);
        wire.cubeList.add(new ModelBox(wire, 20, 0, -1.0F, 0.0F, -1.1F, 2, 65, 2, 0.0F, false));

        plateConnector = new ModelRenderer(this);
        plateConnector.setRotationPoint(-2.0F, -5.0F, -5.5F);
        north.addChild(plateConnector);
        plateConnector.cubeList.add(new ModelBox(plateConnector, 28, 54, 0.0F, 0.0F, 0.0F, 4, 2, 2, 0.0F, false));

        east = new ModelRenderer(this);
        east.setRotationPoint(0.0F, 24.0F, 0.0F);
        setRotation(east, 0.0F, 1.5708F, 0.0F);


        plate2 = new ModelRenderer(this);
        plate2.setRotationPoint(-4.0F, -12.0F, -8.0F);
        east.addChild(plate2);
        plate2.cubeList.add(new ModelBox(plate2, 60, 39, 0.0F, 0.0F, 0.0F, 8, 8, 1, 0.0F, false));

        plateConnector3 = new ModelRenderer(this);
        plateConnector3.setRotationPoint(-3.0F, -11.0F, -7.0F);
        east.addChild(plateConnector3);
        plateConnector3.cubeList.add(new ModelBox(plateConnector3, 28, 30, 0.0F, 0.0F, 0.0F, 6, 6, 10, 0.0F, false));

        wire2 = new ModelRenderer(this);
        wire2.setRotationPoint(0.0F, -70.0F, -1.5F);
        east.addChild(wire2);
        setRotation(wire2, -0.0349F, 0.0F, 0.0F);
        wire2.cubeList.add(new ModelBox(wire2, 20, 0, -1.0F, 0.0F, -1.1F, 2, 65, 2, 0.0F, false));

        plateConnector4 = new ModelRenderer(this);
        plateConnector4.setRotationPoint(-2.0F, -5.0F, -5.5F);
        east.addChild(plateConnector4);
        plateConnector4.cubeList.add(new ModelBox(plateConnector4, 28, 54, 0.0F, 0.0F, 0.0F, 4, 2, 2, 0.0F, false));

        south = new ModelRenderer(this);
        south.setRotationPoint(0.0F, 24.0F, 0.0F);
        setRotation(south, 0.0F, 3.1416F, 0.0F);


        plate3 = new ModelRenderer(this);
        plate3.setRotationPoint(-4.0F, -12.0F, -8.0F);
        south.addChild(plate3);
        plate3.cubeList.add(new ModelBox(plate3, 60, 39, 0.0F, 0.0F, 0.0F, 8, 8, 1, 0.0F, false));

        plateConnector5 = new ModelRenderer(this);
        plateConnector5.setRotationPoint(-3.0F, -11.0F, -7.0F);
        south.addChild(plateConnector5);
        plateConnector5.cubeList.add(new ModelBox(plateConnector5, 28, 30, 0.0F, 0.0F, 0.0F, 6, 6, 10, 0.0F, false));

        wire3 = new ModelRenderer(this);
        wire3.setRotationPoint(0.0F, -70.0F, -1.5F);
        south.addChild(wire3);
        setRotation(wire3, -0.0349F, 0.0F, 0.0F);
        wire3.cubeList.add(new ModelBox(wire3, 20, 0, -1.0F, 0.0F, -1.1F, 2, 65, 2, 0.0F, false));

        plateConnector6 = new ModelRenderer(this);
        plateConnector6.setRotationPoint(-2.0F, -5.0F, -5.5F);
        south.addChild(plateConnector6);
        plateConnector6.cubeList.add(new ModelBox(plateConnector6, 28, 54, 0.0F, 0.0F, 0.0F, 4, 2, 2, 0.0F, false));

        west = new ModelRenderer(this);
        west.setRotationPoint(0.0F, 24.0F, 0.0F);
        setRotation(west, 0.0F, -1.5708F, 0.0F);


        plate4 = new ModelRenderer(this);
        plate4.setRotationPoint(-4.0F, -12.0F, -8.0F);
        west.addChild(plate4);
        plate4.cubeList.add(new ModelBox(plate4, 60, 39, 0.0F, 0.0F, 0.0F, 8, 8, 1, 0.0F, false));

        plateConnector7 = new ModelRenderer(this);
        plateConnector7.setRotationPoint(-3.0F, -11.0F, -7.0F);
        west.addChild(plateConnector7);
        plateConnector7.cubeList.add(new ModelBox(plateConnector7, 28, 30, 0.0F, 0.0F, 0.0F, 6, 6, 10, 0.0F, false));

        wire4 = new ModelRenderer(this);
        wire4.setRotationPoint(0.0F, -70.0F, -1.5F);
        west.addChild(wire4);
        setRotation(wire4, -0.0349F, 0.0F, 0.0F);
        wire4.cubeList.add(new ModelBox(wire4, 20, 0, -1.0F, 0.0F, -1.1F, 2, 65, 2, 0.0F, false));

        plateConnector8 = new ModelRenderer(this);
        plateConnector8.setRotationPoint(-2.0F, -5.0F, -5.5F);
        west.addChild(plateConnector8);
        plateConnector8.cubeList.add(new ModelBox(plateConnector8, 28, 54, 0.0F, 0.0F, 0.0F, 4, 2, 2, 0.0F, false));
    }


    public void renderModel(float size) {
        head.render(size);
        baseRim.render(size);
        base.render(size);
        post1a.render(size);
        post1b.render(size);
        post1c.render(size);
        post1d.render(size);
        north.render(size);
        east.render(size);
        south.render(size);
        west.render(size);
    }


    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.rotateAngleX = x;
        model.rotateAngleY = y;
        model.rotateAngleZ = z;
    }
}
