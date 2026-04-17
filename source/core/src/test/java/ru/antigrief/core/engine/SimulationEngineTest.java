package ru.antigrief.core.engine;

import org.junit.jupiter.api.Test;
import ru.antigrief.common.action.ActionInfo;
import ru.antigrief.common.action.ActionType;
import ru.antigrief.common.data.PlatformLocation;
import ru.antigrief.common.data.PlatformPlayer;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SimulationEngineTest {

    @Test
    void testLavaSimulation() {
        SimulationEngine engine = new SimulationEngine();
        ActionInfo lavaAction = new ActionInfo(new PlatformPlayer(UUID.randomUUID(), "test"), ActionType.BLOCK_PLACE, new PlatformLocation("world", 0,0,0), "LAVA", new HashMap<>());
        
        SimulationEngine.SimulationResult result = engine.simulate(lavaAction);
        assertTrue(result.dangerous());
        assertEquals(0.8, result.blockDamageEstimate());
    }
}
