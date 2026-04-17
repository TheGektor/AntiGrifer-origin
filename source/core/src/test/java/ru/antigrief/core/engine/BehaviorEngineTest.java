package ru.antigrief.core.engine;

import org.junit.jupiter.api.Test;
import ru.antigrief.api.PlayerContext;
import ru.antigrief.common.action.ActionInfo;
import ru.antigrief.common.action.ActionType;
import ru.antigrief.common.data.PlatformLocation;
import ru.antigrief.common.data.PlatformPlayer;
import ru.antigrief.core.context.PlayerContextImpl;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BehaviorEngineTest {

    @Test
    void testScoreChange() {
        BehaviorEngine engine = new BehaviorEngine();
        PlayerContext context = new PlayerContextImpl(UUID.randomUUID());
        context.setTrustScore(10);
        
        ActionInfo action = new ActionInfo(new PlatformPlayer(context.getUniqueId(), "test"), ActionType.JOIN, new PlatformLocation("world", 0,0,0), null, new HashMap<>());
        
        engine.defineRule(ActionType.JOIN, 5);
        engine.processAction(context, action);
        
        assertEquals(15, context.getTrustScore());
    }
}
