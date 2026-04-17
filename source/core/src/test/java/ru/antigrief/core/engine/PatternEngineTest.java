package ru.antigrief.core.engine;

import org.junit.jupiter.api.Test;
import ru.antigrief.api.ActionPattern;
import ru.antigrief.api.PlayerContext;
import ru.antigrief.common.action.ActionInfo;
import ru.antigrief.common.action.ActionType;
import ru.antigrief.common.data.PlatformLocation;
import ru.antigrief.common.data.PlatformPlayer;
import ru.antigrief.core.context.PlayerContextImpl;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class PatternEngineTest {

    @Test
    void testPatternMatch() {
        PatternEngine engine = new PatternEngine();
        PlayerContext context = new PlayerContextImpl(UUID.randomUUID());
        
        final boolean[] matched = {false};
        ActionPattern pattern = new ActionPattern() {
            @Override public String getName() { return "test"; }
            @Override public List<ActionType> getSequence() { return List.of(ActionType.OPEN_CONTAINER, ActionType.BLOCK_BREAK); }
            @Override public void onMatch(PlayerContext context) { matched[0] = true; }
        };
        
        engine.registerPattern(pattern);
        
        PlatformPlayer player = new PlatformPlayer(context.getUniqueId(), "test");
        PlatformLocation loc = new PlatformLocation("world", 0,0,0);
        
        ActionInfo a1 = new ActionInfo(player, ActionType.OPEN_CONTAINER, loc, "CHEST", new HashMap<>());
        ActionInfo a2 = new ActionInfo(player, ActionType.BLOCK_BREAK, loc, "CHEST", new HashMap<>());
        
        context.getRecentActions().add(a1);
        engine.feedAction(context, a2);
        
        assertTrue(matched[0]);
    }
}
