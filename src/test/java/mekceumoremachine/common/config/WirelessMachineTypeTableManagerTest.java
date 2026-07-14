package mekceumoremachine.common.config;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WirelessMachineTypeTableManagerTest {

    @Test
    void connectionDataUsesWorldWideMoreMachineDirectory() {
        File worldDirectory = new File("test-world");

        File directory = WirelessMachineTypeTableManager.getConnectionsDirectory(worldDirectory);

        assertEquals(new File(worldDirectory, "data/mek/moremachine/wireless_energy/connections"), directory);
    }
}
