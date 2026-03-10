package mekceumoremachine.client.model.machine;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelVoidMineralGenerator extends ModelBase {

    ModelRenderer plate3;
    ModelRenderer baseBack;
    ModelRenderer pole4;
    ModelRenderer shaft2;
    ModelRenderer shaft1;
    ModelRenderer plate2;
    ModelRenderer pole3;
    ModelRenderer frameRight5;
    ModelRenderer baseRight;
    ModelRenderer baseFront;
    ModelRenderer baseLeft;
    ModelRenderer frameRight3;
    ModelRenderer pole1;
    ModelRenderer frameRight4;
    ModelRenderer frameRight1;
    ModelRenderer frameRight2;
    ModelRenderer frameLeft5;
    ModelRenderer frameLeft4;
    ModelRenderer frameLeft2;
    ModelRenderer frameLeft1;
    ModelRenderer pole2;
    ModelRenderer frameLeft3;
    ModelRenderer plate1;
    ModelRenderer rivet10;
    ModelRenderer rivet5;
    ModelRenderer rivet1;
    ModelRenderer rivet6;
    ModelRenderer rivet2;
    ModelRenderer rivet7;
    ModelRenderer rivet3;
    ModelRenderer rivet8;
    ModelRenderer rivet4;
    ModelRenderer rivet9;
    ModelRenderer bone;
    ModelRenderer frameBack1;
    ModelRenderer frameBack2;
    ModelRenderer frameBack3;
    ModelRenderer frameBack4;
    ModelRenderer frameBack5;
    ModelRenderer bone2;
    ModelRenderer frameBack6;
    ModelRenderer frameBack7;
    ModelRenderer frameBack8;
    ModelRenderer frameBack9;
    ModelRenderer frameBack10;
    ModelRenderer tier;
    ModelRenderer motor;
    ModelRenderer top;
    ModelRenderer rivet11;
    ModelRenderer rivet12;
    ModelRenderer rivet13;
    ModelRenderer rivet14;
    ModelRenderer rivet15;
    ModelRenderer rivet16;


    public ModelVoidMineralGenerator() {
        textureWidth = 128;
        textureHeight = 64;

        plate3 = new ModelRenderer(this);
        plate3.setRotationPoint(-4.0F, 22.0F, -4.0F);
        plate3.cubeList.add(new ModelBox(plate3, 36, 42, 0.0F, 0.0F, 0.0F, 8, 2, 8, 0.0F, true));

        baseBack = new ModelRenderer(this);
        baseBack.setRotationPoint(-8.0F, 19.0F, 5.0F);
        baseBack.cubeList.add(new ModelBox(baseBack, 0, 26, 0.0F, 0.0F, 0.0F, 16, 5, 3, 0.0F, true));

        pole4 = new ModelRenderer(this);
        pole4.setRotationPoint(6.5F, -6.0F, 6.5F);
        pole4.cubeList.add(new ModelBox(pole4, 0, 34, 0.0F, 0.0F, 0.0F, 1, 25, 1, 0.0F, true));

        shaft2 = new ModelRenderer(this);
        shaft2.setRotationPoint(-1.5F, -5.0F, -1.5F);
        shaft2.cubeList.add(new ModelBox(shaft2, 16, 34, 0.0F, 0.0F, 0.0F, 3, 11, 3, 0.0F, true));

        shaft1 = new ModelRenderer(this);
        shaft1.setRotationPoint(-1.0F, 6.0F, -1.0F);
        shaft1.cubeList.add(new ModelBox(shaft1, 8, 34, 0.0F, 0.0F, 0.0F, 2, 15, 2, 0.0F, true));

        plate2 = new ModelRenderer(this);
        plate2.setRotationPoint(-2.0F, 21.0F, -2.0F);
        plate2.cubeList.add(new ModelBox(plate2, 48, 0, 0.0F, 0.0F, 0.0F, 4, 2, 4, 0.0F, true));

        pole3 = new ModelRenderer(this);
        pole3.setRotationPoint(6.5F, -6.0F, -7.5F);
        pole3.cubeList.add(new ModelBox(pole3, 0, 34, 0.0F, 0.0F, 0.0F, 1, 25, 1, 0.0F, true));

        frameRight5 = new ModelRenderer(this);
        frameRight5.setRotationPoint(6.485F, 7.0F, -7.5F);
        setRotationAngle(frameRight5, 0.8378F, 0.0F, 0.0F);
        frameRight5.cubeList.add(new ModelBox(frameRight5, 4, 34, 0.0F, 0.0F, 0.0F, 1, 19, 1, 0.0F, true));

        baseRight = new ModelRenderer(this);
        baseRight.setRotationPoint(5.0F, 19.0F, -5.0F);
        baseRight.cubeList.add(new ModelBox(baseRight, 38, 18, 0.0F, 0.0F, 0.0F, 3, 5, 10, 0.0F, true));

        baseFront = new ModelRenderer(this);
        baseFront.setRotationPoint(-8.0F, 19.0F, -8.0F);
        baseFront.cubeList.add(new ModelBox(baseFront, 0, 18, 0.0F, 0.0F, 0.0F, 16, 5, 3, 0.0F, true));

        baseLeft = new ModelRenderer(this);
        baseLeft.setRotationPoint(-8.0F, 19.0F, -5.0F);
        baseLeft.cubeList.add(new ModelBox(baseLeft, 38, 18, 0.0F, 0.0F, 0.0F, 3, 5, 10, 0.0F, true));

        frameRight3 = new ModelRenderer(this);
        frameRight3.setRotationPoint(6.5F, 6.0F, -6.5F);
        frameRight3.cubeList.add(new ModelBox(frameRight3, 64, 27, 0.0F, 0.0F, 0.0F, 1, 1, 13, 0.0F, true));

        pole1 = new ModelRenderer(this);
        pole1.setRotationPoint(-7.5F, -6.0F, -7.5F);
        pole1.cubeList.add(new ModelBox(pole1, 0, 34, 0.0F, 0.0F, 0.0F, 1, 25, 1, 0.0F, true));

        frameRight4 = new ModelRenderer(this);
        frameRight4.setRotationPoint(6.49F, 7.0F, 7.5F);
        setRotationAngle(frameRight4, -0.8378F, 0.0F, 0.0F);
        frameRight4.cubeList.add(new ModelBox(frameRight4, 4, 34, 0.0F, 0.0F, -1.0F, 1, 19, 1, 0.0F, true));

        frameRight1 = new ModelRenderer(this);
        frameRight1.setRotationPoint(6.485F, -6.0F, -7.5F);
        setRotationAngle(frameRight1, 0.8378F, 0.0F, 0.0F);
        frameRight1.cubeList.add(new ModelBox(frameRight1, 4, 34, 0.0F, 0.0F, 0.0F, 1, 19, 1, 0.0F, true));

        frameRight2 = new ModelRenderer(this);
        frameRight2.setRotationPoint(6.49F, -6.0F, 7.5F);
        setRotationAngle(frameRight2, -0.8378F, 0.0F, 0.0F);
        frameRight2.cubeList.add(new ModelBox(frameRight2, 4, 34, 0.0F, 0.0F, -1.0F, 1, 19, 1, 0.0F, true));

        frameLeft5 = new ModelRenderer(this);
        frameLeft5.setRotationPoint(-7.485F, 7.0F, -7.5F);
        setRotationAngle(frameLeft5, 0.8378F, 0.0F, 0.0F);
        frameLeft5.cubeList.add(new ModelBox(frameLeft5, 4, 34, 0.0F, 0.0F, 0.0F, 1, 19, 1, 0.0F, true));

        frameLeft4 = new ModelRenderer(this);
        frameLeft4.setRotationPoint(-7.49F, 7.0F, 7.5F);
        setRotationAngle(frameLeft4, -0.8378F, 0.0F, 0.0F);
        frameLeft4.cubeList.add(new ModelBox(frameLeft4, 4, 34, 0.0F, 0.0F, -1.0F, 1, 19, 1, 0.0F, true));

        frameLeft2 = new ModelRenderer(this);
        frameLeft2.setRotationPoint(-7.485F, -6.0F, -7.5F);
        setRotationAngle(frameLeft2, 0.8378F, 0.0F, 0.0F);
        frameLeft2.cubeList.add(new ModelBox(frameLeft2, 4, 34, 0.0F, 0.0F, 0.0F, 1, 19, 1, 0.0F, true));

        frameLeft1 = new ModelRenderer(this);
        frameLeft1.setRotationPoint(-7.49F, -6.0F, 7.5F);
        setRotationAngle(frameLeft1, -0.8378F, 0.0F, 0.0F);
        frameLeft1.cubeList.add(new ModelBox(frameLeft1, 4, 34, 0.0F, 0.0F, -1.0F, 1, 19, 1, 0.0F, true));

        pole2 = new ModelRenderer(this);
        pole2.setRotationPoint(-7.5F, -6.0F, 6.5F);
        pole2.cubeList.add(new ModelBox(pole2, 0, 34, 0.0F, 0.0F, 0.0F, 1, 25, 1, 0.0F, true));

        frameLeft3 = new ModelRenderer(this);
        frameLeft3.setRotationPoint(-7.5F, 6.0F, -6.5F);
        frameLeft3.cubeList.add(new ModelBox(frameLeft3, 64, 27, 0.0F, 0.0F, 0.0F, 1, 1, 13, 0.0F, true));

        plate1 = new ModelRenderer(this);
        plate1.setRotationPoint(-5.0F, -6.0F, -5.0F);
        plate1.cubeList.add(new ModelBox(plate1, 76, 0, 0.0F, 0.0F, 0.0F, 10, 1, 12, 0.0F, true));

        rivet10 = new ModelRenderer(this);
        rivet10.setRotationPoint(3.5F, -5.5F, 3.5F);
        rivet10.cubeList.add(new ModelBox(rivet10, 0, 0, 0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F, true));

        rivet5 = new ModelRenderer(this);
        rivet5.setRotationPoint(-4.5F, -5.5F, 3.5F);
        rivet5.cubeList.add(new ModelBox(rivet5, 0, 0, 0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F, true));

        rivet1 = new ModelRenderer(this);
        rivet1.setRotationPoint(-4.5F, -5.5F, -4.5F);
        rivet1.cubeList.add(new ModelBox(rivet1, 0, 0, 0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F, true));

        rivet6 = new ModelRenderer(this);
        rivet6.setRotationPoint(3.5F, -5.5F, -4.5F);
        rivet6.cubeList.add(new ModelBox(rivet6, 0, 0, 0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F, true));

        rivet2 = new ModelRenderer(this);
        rivet2.setRotationPoint(-4.5F, -5.5F, -2.5F);
        rivet2.cubeList.add(new ModelBox(rivet2, 0, 0, 0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F, true));

        rivet7 = new ModelRenderer(this);
        rivet7.setRotationPoint(3.5F, -5.5F, -2.5F);
        rivet7.cubeList.add(new ModelBox(rivet7, 0, 0, 0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F, true));

        rivet3 = new ModelRenderer(this);
        rivet3.setRotationPoint(-4.5F, -5.5F, -0.5F);
        rivet3.cubeList.add(new ModelBox(rivet3, 0, 0, 0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F, true));

        rivet8 = new ModelRenderer(this);
        rivet8.setRotationPoint(3.5F, -5.5F, -0.5F);
        rivet8.cubeList.add(new ModelBox(rivet8, 0, 0, 0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F, true));

        rivet4 = new ModelRenderer(this);
        rivet4.setRotationPoint(-4.5F, -5.5F, 1.5F);
        rivet4.cubeList.add(new ModelBox(rivet4, 0, 0, 0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F, true));

        rivet9 = new ModelRenderer(this);
        rivet9.setRotationPoint(3.5F, -5.5F, 1.5F);
        rivet9.cubeList.add(new ModelBox(rivet9, 0, 0, 0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F, true));

        bone = new ModelRenderer(this);
        bone.setRotationPoint(0.0F, 24.0F, 0.0F);


        frameBack1 = new ModelRenderer(this);
        frameBack1.setRotationPoint(7.5F, -30.0F, 6.49F);
        bone.addChild(frameBack1);
        setRotationAngle(frameBack1, 0.0F, 0.0F, 0.8378F);
        frameBack1.cubeList.add(new ModelBox(frameBack1, 4, 34, -1.0F, 0.0F, 0.0F, 1, 19, 1, 0.0F, true));

        frameBack2 = new ModelRenderer(this);
        frameBack2.setRotationPoint(-7.5F, -30.0F, 6.49F);
        bone.addChild(frameBack2);
        setRotationAngle(frameBack2, 0.0F, 0.0F, -0.8378F);
        frameBack2.cubeList.add(new ModelBox(frameBack2, 4, 34, 0.0F, 0.0F, 0.0F, 1, 19, 1, 0.0F, true));

        frameBack3 = new ModelRenderer(this);
        frameBack3.setRotationPoint(-6.5F, -18.0F, 6.5F);
        bone.addChild(frameBack3);
        frameBack3.cubeList.add(new ModelBox(frameBack3, 36, 52, 0.0F, 0.0F, 0.0F, 13, 1, 1, 0.0F, true));

        frameBack4 = new ModelRenderer(this);
        frameBack4.setRotationPoint(-7.5F, -17.0F, 6.49F);
        bone.addChild(frameBack4);
        setRotationAngle(frameBack4, 0.0F, 0.0F, -0.8378F);
        frameBack4.cubeList.add(new ModelBox(frameBack4, 4, 34, 0.0F, 0.0F, 0.0F, 1, 19, 1, 0.0F, true));

        frameBack5 = new ModelRenderer(this);
        frameBack5.setRotationPoint(7.5F, -17.0F, 6.49F);
        bone.addChild(frameBack5);
        setRotationAngle(frameBack5, 0.0F, 0.0F, 0.8378F);
        frameBack5.cubeList.add(new ModelBox(frameBack5, 4, 34, -1.0F, 0.0F, 0.0F, 1, 19, 1, 0.0F, true));

        bone2 = new ModelRenderer(this);
        bone2.setRotationPoint(0.0F, 24.0F, 0.0F);


        frameBack6 = new ModelRenderer(this);
        frameBack6.setRotationPoint(7.5F, -30.0F, -7.51F);
        bone2.addChild(frameBack6);
        setRotationAngle(frameBack6, 0.0F, 0.0F, 0.8378F);
        frameBack6.cubeList.add(new ModelBox(frameBack6, 4, 34, -1.0F, 0.0F, 0.0F, 1, 19, 1, 0.0F, true));

        frameBack7 = new ModelRenderer(this);
        frameBack7.setRotationPoint(-7.5F, -30.0F, -7.51F);
        bone2.addChild(frameBack7);
        setRotationAngle(frameBack7, 0.0F, 0.0F, -0.8378F);
        frameBack7.cubeList.add(new ModelBox(frameBack7, 4, 34, 0.0F, 0.0F, 0.0F, 1, 19, 1, 0.0F, true));

        frameBack8 = new ModelRenderer(this);
        frameBack8.setRotationPoint(-6.5F, -18.0F, -7.5F);
        bone2.addChild(frameBack8);
        frameBack8.cubeList.add(new ModelBox(frameBack8, 36, 52, 0.0F, 0.0F, 0.0F, 13, 1, 1, 0.0F, true));

        frameBack9 = new ModelRenderer(this);
        frameBack9.setRotationPoint(-7.5F, -17.0F, -7.51F);
        bone2.addChild(frameBack9);
        setRotationAngle(frameBack9, 0.0F, 0.0F, -0.8378F);
        frameBack9.cubeList.add(new ModelBox(frameBack9, 4, 34, 0.0F, 0.0F, 0.0F, 1, 19, 1, 0.0F, true));

        frameBack10 = new ModelRenderer(this);
        frameBack10.setRotationPoint(7.5F, -17.0F, -7.51F);
        bone2.addChild(frameBack10);
        setRotationAngle(frameBack10, 0.0F, 0.0F, 0.8378F);
        frameBack10.cubeList.add(new ModelBox(frameBack10, 4, 34, -1.0F, 0.0F, 0.0F, 1, 19, 1, 0.0F, true));

        tier = new ModelRenderer(this);
        tier.setRotationPoint(0.0F, 24.0F, 0.0F);


        motor = new ModelRenderer(this);
        motor.setRotationPoint(-3.0F, -29.0F, -3.0F);
        tier.addChild(motor);
        motor.cubeList.add(new ModelBox(motor, 80, 17, 0.0F, 0.0F, 0.0F, 6, 4, 6, 0.0F, true));

        top = new ModelRenderer(this);
        top.setRotationPoint(-8.0F, -32.0F, -8.0F);
        tier.addChild(top);
        top.cubeList.add(new ModelBox(top, 0, 0, 0.0F, 0.0F, 0.0F, 16, 2, 16, 0.0F, true));

        rivet11 = new ModelRenderer(this);
        rivet11.setRotationPoint(0.0F, -5.5F, 0.0F);
        setRotationAngle(rivet11, 0.0F, -1.5708F, 0.0F);
        rivet11.cubeList.add(new ModelBox(rivet11, 0, 0, -4.5F, 0.0F, 1.5F, 1, 1, 1, 0.0F, true));

        rivet12 = new ModelRenderer(this);
        rivet12.setRotationPoint(0.0F, -5.5F, 0.0F);
        setRotationAngle(rivet12, 0.0F, -1.5708F, 0.0F);
        rivet12.cubeList.add(new ModelBox(rivet12, 0, 0, 3.5F, 0.0F, 1.5F, 1, 1, 1, 0.0F, true));

        rivet13 = new ModelRenderer(this);
        rivet13.setRotationPoint(0.0F, -5.5F, 0.0F);
        setRotationAngle(rivet13, 0.0F, -1.5708F, 0.0F);
        rivet13.cubeList.add(new ModelBox(rivet13, 0, 0, 3.5F, 0.0F, -0.5F, 1, 1, 1, 0.0F, true));

        rivet14 = new ModelRenderer(this);
        rivet14.setRotationPoint(0.0F, -5.5F, 0.0F);
        setRotationAngle(rivet14, 0.0F, -1.5708F, 0.0F);
        rivet14.cubeList.add(new ModelBox(rivet14, 0, 0, -4.5F, 0.0F, -0.5F, 1, 1, 1, 0.0F, true));

        rivet15 = new ModelRenderer(this);
        rivet15.setRotationPoint(0.0F, -5.5F, 0.0F);
        setRotationAngle(rivet15, 0.0F, -1.5708F, 0.0F);
        rivet15.cubeList.add(new ModelBox(rivet15, 0, 0, 3.5F, 0.0F, -2.5F, 1, 1, 1, 0.0F, true));

        rivet16 = new ModelRenderer(this);
        rivet16.setRotationPoint(0.0F, -5.5F, 0.0F);
        setRotationAngle(rivet16, 0.0F, -1.5708F, 0.0F);
        rivet16.cubeList.add(new ModelBox(rivet16, 0, 0, -4.5F, 0.0F, -2.5F, 1, 1, 1, 0.0F, true));
    }

    public void renderTier(float size) {
        tier.render(size);
    }

    public void render(float size) {
        plate3.render(size);
        baseBack.render(size);
        pole4.render(size);
        shaft2.render(size);
        shaft1.render(size);
        plate2.render(size);
        pole3.render(size);
        frameRight5.render(size);
        baseRight.render(size);
        baseFront.render(size);
        baseLeft.render(size);
        frameRight3.render(size);
        pole1.render(size);
        frameRight4.render(size);
        frameRight1.render(size);
        frameRight2.render(size);
        frameLeft5.render(size);
        frameLeft4.render(size);
        frameLeft2.render(size);
        frameLeft1.render(size);
        pole2.render(size);
        frameLeft3.render(size);
        plate1.render(size);
        rivet10.render(size);
        rivet5.render(size);
        rivet1.render(size);
        rivet6.render(size);
        rivet2.render(size);
        rivet7.render(size);
        rivet3.render(size);
        rivet8.render(size);
        rivet4.render(size);
        rivet9.render(size);
        bone.render(size);
        bone2.render(size);
        rivet11.render(size);
        rivet12.render(size);
        rivet13.render(size);
        rivet14.render(size);
        rivet15.render(size);
        rivet16.render(size);
    }

    public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }
}
