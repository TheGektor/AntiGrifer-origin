package ru.antigrief.core.rules;

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

class RuleEngineTest {

    @Test
    void testRuleEvaluation() {
        RuleEngine engine = new RuleEngine();
        PlayerContext context = new PlayerContextImpl(UUID.randomUUID());
        ActionInfo action = new ActionInfo(new PlatformPlayer(context.getUniqueId(), "test"), ActionType.BLOCK_PLACE, new PlatformLocation("world", 0,0,0), "TNT", new HashMap<>());

        engine.addRule("no-tnt", (ctx, act) -> act.getTargetBlockOrEntity().equals("TNT"), RuleEngine.RuleAction.CANCEL);
        
        RuleEngine.RuleAction result = engine.evaluate(context, action);
        assertEquals(RuleEngine.RuleAction.CANCEL, result);
    }
}
