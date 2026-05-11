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

public class FireUseCheck implements Check {
    private final TrustConfig trustConfig;
    private final ConfigManager config;
    private final AlertSystem alertSystem;

    public FireUseCheck(TrustConfig trustConfig, ConfigManager config, AlertSystem alertSystem) {
        this.trustConfig = trustConfig;
        this.config = config;
        this.alertSystem = alertSystem;
    }

    @Override
    public String getName() {
        return "FireUseCheck";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public CheckResult check(PlayerContext context, ActionInfo action) {
        if (action.getType() == ActionType.FIRE_USE) {
            TrustTier tier = TrustTier.resolve(context, trustConfig);
            
            if (tier.getLevel() < TrustTier.TIER_1.getLevel()) {
                String msg = config.getString("messages.no_permission_fire", "&cУ вас недостаточно доверия для использования огнеопасных предметов! (Нужно 60 мин игры)");
                alertSystem.dispatch("Попытка использования огня без доверия", AlertSystem.AlertLevel.MEDIUM, action.getPlayer().getName());
                return CheckResult.fail(30, true, msg);
            }
        }
        return CheckResult.pass();
    }
}
