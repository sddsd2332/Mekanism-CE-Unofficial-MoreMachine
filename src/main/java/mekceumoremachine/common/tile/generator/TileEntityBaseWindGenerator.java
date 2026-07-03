package mekceumoremachine.common.tile.generator;

import io.netty.buffer.ByteBuf;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.Coord4D;
import mekanism.api.IContentsListener;
import mekanism.api.TileNetworkList;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.ISpecialSelectionWireframeTile;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.config.MekanismConfig;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.common.tile.TileEntityGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

public abstract class TileEntityBaseWindGenerator extends TileEntityGenerator implements IBoundingBlock, ISpecialSelectionWireframeTile {

    public static final float SPEED = 32F;
    public static final float SPEED_SCALED = 256F / SPEED;
    static final String[] methods = new String[]{"getEnergy", "getOutput", "getMaxEnergy", "getEnergyNeeded", "getMultiplier"};
    private double angle;
    private float currentMultiplier;
    private boolean isBlacklistDimension = false;
    private EnergyInventorySlot energySlot;

    public TileEntityBaseWindGenerator(String name) {
        super("wind", name, 0, 0);
        initializeInventorySlots();
    }

    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = createInventorySlotHelper();
        energySlot = builder.addSlot(EnergyInventorySlot.drain(getMainEnergyContainer(listener), listener, 143, 35));
        energySlot.setSlotOverlay(SlotOverlay.PLUS);
        return builder.build();
    }


    @Override
    public void onLoad() {
        super.onLoad();
        isBlacklistDimension = MekanismConfig.current().generators.windGenerationDimBlacklist.val().contains(world.provider.getDimension());
        if (isBlacklistDimension) {
            setActive(false);
        }
    }

    @Override
    public void onAsyncUpdateServer() {
        super.onAsyncUpdateServer();
        energySlot.drainContainer();
        // If we're in a blacklisted dimension, there's nothing more to do
        if (isBlacklistDimension) {
            return;
        }

        if (ticker % 20 == 0) {
            currentMultiplier = getMultiplier();
            setActive(MekanismUtils.canFunction(this) && currentMultiplier > 0);
        }
        if (currentMultiplier > 0 && MekanismUtils.canFunction(this) && getEnergyContainer().getNeeded() > 0) {
            getEnergyContainer().insert(getEnergyAdd(), Action.EXECUTE, AutomationType.INTERNAL);
        }
    }

    public double getEnergyAdd() {
        return (MekanismConfig.current().generators.windGenerationMin.val() * currentMultiplier) * processes();
    }

    @Override
    public void onUpdateClient() {
        super.onUpdateClient();
        if (getActive()) {
            angle = (angle + (getPos().getY() + 4F) / SPEED_SCALED) % 360;
        }
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (isRemote()) {
            currentMultiplier = dataStream.readFloat();
            isBlacklistDimension = dataStream.readBoolean();

        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(currentMultiplier);
        data.add(isBlacklistDimension);
        return data;
    }


    /**
     * Determines the current output multiplier, taking sky visibility and height into account.
     **/
    public float getMultiplier() {
        BlockPos top = getPos().up(4);
        if (world.canSeeSky(top)) {
            int minY = MekanismConfig.current().generators.windGenerationMinY.val();
            int maxY = MekanismConfig.current().generators.windGenerationMaxY.val();
            float clampedY = Math.min(maxY, Math.max(minY, top.getY()));
            float minG = (float) MekanismConfig.current().generators.windGenerationMin.val();
            float maxG = (float) MekanismConfig.current().generators.windGenerationMax.val();
            //Prevents the possibility of writing opposite values; https://github.com/Thorfusion/Mekanism-Community-Edition/issues/150
            int rangeY = maxY < minY ? minY - maxY : maxY - minY;
            if (rangeY <= 0 || minG <= 0 || Float.isNaN(minG) || Float.isInfinite(minG) || Float.isNaN(maxG) || Float.isInfinite(maxG)) {
                return 0;
            }
            float rangG = maxG < minG ? minG - maxG : maxG - minG;
            float slope = rangG / rangeY;
            float toGen = minG + (slope * (clampedY - minY));
            float multiplier = toGen / minG;
            if (Float.isNaN(multiplier) || Float.isInfinite(multiplier)) {
                return 0;
            }
            return multiplier;
        }
        return 0;
    }

    @Override
    public String[] getMethods() {
        return methods;
    }

    @Override
    public Object[] invoke(int method, Object[] arguments) throws NoSuchMethodException {
        return switch (method) {
            case 0 -> new Object[]{getEnergy()};
            case 1 -> new Object[]{getMaxOutput()};
            case 2 -> new Object[]{getMaxEnergy()};
            case 3 -> new Object[]{getNeedEnergy()};
            case 4 -> new Object[]{getMultiplier()};
            default -> throw new NoSuchMethodException();
        };
    }

    @Override
    public boolean canOperate() {
        return getEnergyContainer().getNeeded() > 0 && getMultiplier() > 0 && MekanismUtils.canFunction(this);
    }

    @Override
    public void onPlace() {
        Coord4D current = Coord4D.get(this);
        MekanismUtils.makeBoundingBlock(world, getPos().up(), current);
        MekanismUtils.makeBoundingBlock(world, getPos().up(2), current);
        MekanismUtils.makeBoundingBlock(world, getPos().up(3), current);
        MekanismUtils.makeBoundingBlock(world, getPos().up(4), current);

        // Check to see if the placement is happening in a blacklisted dimension
        isBlacklistDimension = MekanismConfig.current().generators.windGenerationDimBlacklist.val().contains(world.provider.getDimension());
    }


    @Override
    public double getMaxOutput() {
        return MekanismConfig.current().generators.windGenerationMax.val() * 2 * processes();
    }

    @Override
    public double getMaxEnergy() {
        return MekanismConfig.current().generators.windGeneratorStorage.val() * processes();
    }

    @Override
    public void onBreak() {
        world.setBlockToAir(getPos().up());
        world.setBlockToAir(getPos().up(2));
        world.setBlockToAir(getPos().up(3));
        world.setBlockToAir(getPos().up(4));
        world.setBlockToAir(getPos());
    }


    @Override
    public boolean renderUpdate() {
        return false;
    }

    @Override
    public boolean lightUpdate() {
        return false;
    }

    public float getCurrentMultiplier() {
        return currentMultiplier;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public double getAngle() {
        return angle;
    }

    public boolean isBlacklistDimension() {
        return isBlacklistDimension;
    }

    public int processes() {
        return 1;
    }

    @Nonnull
    @Override
    public String getName() {
        return LangUtils.localize("tile." + fullName + ".name");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Class<?> getSelectionWireframeModelClass() {
        return mekceumoremachine.client.model.generator.ModelTierWindGenerator.class;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSelectionWireframeAnimationCacheKey(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (!MekanismConfig.current().client.windGeneratorRotating.val()) {
            return 0;
        }
        return Math.floorMod((int) Math.round(getSelectionWireframeAngle() * 2D), 720);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void applySelectionWireframeModelState(Object model, IBlockState state, IBlockAccess world, BlockPos pos) {
        if (model instanceof mekceumoremachine.client.model.generator.ModelTierWindGenerator windModel) {
            windModel.applySelectionBladeAngle(getSelectionWireframeAngle());
        }
    }

    @SideOnly(Side.CLIENT)
    private double getSelectionWireframeAngle() {
        double currentAngle = getAngle();
        if (getActive()) {
            float partial = Minecraft.getMinecraft().getRenderPartialTicks();
            currentAngle = (currentAngle + ((getPos().getY() + 4F) / SPEED_SCALED) * partial) % 360D;
        }
        return currentAngle < 0D ? currentAngle + 360D : currentAngle;
    }
}
