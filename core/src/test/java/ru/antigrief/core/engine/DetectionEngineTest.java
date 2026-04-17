package ru.antigrief.core.engine;

import org.junit.jupiter.api.Test;
import ru.antigrief.api.Check;
import ru.antigrief.api.CheckResult;
import ru.antigrief.api.PlayerContext;
import ru.antigrief.common.action.ActionInfo;
import ru.antigrief.common.action.ActionType;
import ru.antigrief.common.data.PlatformLocation;
import ru.antigrief.common.data.PlatformPlayer;
import ru.antigrief.core.context.PlayerContextImpl;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DetectionEngineTest {

    @Test
    void testDetectionExecution() {
        DetectionEngine engine = new DetectionEngine();
        PlayerContext context = new PlayerContextImpl(UUID.randomUUID());
        ActionInfo action = new ActionInfo(new PlatformPlayer(context.getUniqueId(), "test"), ActionType.BLOCK_BREAK, new PlatformLocation("world", 0,0,0), "DIRT", new HashMap<>());

        engine.registerCheck(new Check() {
            @Override public String getName() { return "TestCheck"; }
            @Override public int getPriority() { return 1; }
            @Override public CheckResult check(PlayerContext context, ActionInfo action) {
                return CheckResult.fail(10, false, "Caught!");
            }
        });

        CheckResult result = engine.runChecks(context, action);
        assertEquals(10, result.getViolationScore());
        assertFalse(result.isCancelAction());
    }
}
