package mekceumoremachine.common.util;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MachineStackTypeResolverTest {

    @Test
    void fingerprintIsIndependentOfCompoundInsertionOrder() {
        NBTTagCompound first = new NBTTagCompound();
        first.setInteger("Level", 3);
        NBTTagCompound firstNested = new NBTTagCompound();
        firstNested.setString("mode", "fast");
        firstNested.setLong("capacity", 8_000L);
        first.setTag("machine", firstNested);

        NBTTagCompound second = new NBTTagCompound();
        NBTTagCompound secondNested = new NBTTagCompound();
        secondNested.setLong("capacity", 8_000L);
        secondNested.setString("mode", "fast");
        second.setTag("machine", secondNested);
        second.setInteger("Level", 3);

        assertEquals(MachineStackTypeResolver.fingerprint(first), MachineStackTypeResolver.fingerprint(second));
    }

    @Test
    void fingerprintChangesWhenTierDataChanges() {
        NBTTagCompound basic = new NBTTagCompound();
        basic.setInteger("Level", 0);
        NBTTagCompound ultimate = new NBTTagCompound();
        ultimate.setInteger("Level", 3);

        assertNotEquals(MachineStackTypeResolver.fingerprint(basic), MachineStackTypeResolver.fingerprint(ultimate));
    }
}
