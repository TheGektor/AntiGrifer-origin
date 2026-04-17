package ru.antigrief.modules.detection.checks;

import ru.antigrief.api.Check;
import ru.antigrief.api.CheckResult;
import ru.antigrief.api.PlayerContext;
import ru.antigrief.common.action.ActionInfo;
import ru.antigrief.common.action.ActionType;
import ru.antigrief.core.alert.AlertSystem;
import ru.antigrief.core.config.ConfigManager;
import ru.antigrief.core.trust.TrustConfig;
import ru.antigrief.core.trust.TrustTier;

public class ExplosiveCheck implements Check {
    private final TrustConfig trustConfig;
    private final ConfigManager config;
    private final AlertSystem alertSystem;

    public ExplosiveCheck(TrustConfig trustConfig, ConfigManager config, AlertSystem alertSystem) {
        this.trustConfig = trustConfig;
        this.config = config;
        this.alertSystem = alertSystem;
    }

    @Override
    public String getName() {
        return "ExplosiveCheck";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public CheckResult check(PlayerContext context, ActionInfo action) {
        if (action.getType() == ActionType.EXPLOSIVE_PLACE) {
            TrustTier tier = TrustTier.fromPlaytime(context.getPlaytimeSeconds(), trustConfig);
            
            if (tier.getLevel() < TrustTier.TIER_2.getLevel()) {
                String msg = config.getString("messages.no_permission_explosive", "&cУ вас недостаточно доверия для использования взрывчатки! (Нужно 120 мин игры)");
                alertSystem.dispatch("Попытка установки взрывчатки без доверия", AlertSystem.AlertLevel.HIGH, action.getPlayer().getName());
                return CheckResult.fail(50, true, msg);
            }
        }
        return CheckResult.pass();
    }
}
