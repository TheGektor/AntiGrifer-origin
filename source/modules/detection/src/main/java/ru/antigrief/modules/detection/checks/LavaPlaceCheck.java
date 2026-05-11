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

public class LavaPlaceCheck implements Check {
    private final TrustConfig trustConfig;
    private final ConfigManager config;
    private final AlertSystem alertSystem;

    public LavaPlaceCheck(TrustConfig trustConfig, ConfigManager config, AlertSystem alertSystem) {
        this.trustConfig = trustConfig;
        this.config = config;
        this.alertSystem = alertSystem;
    }

    @Override
    public String getName() {
        return "LavaPlaceCheck";
    }

    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    public CheckResult check(PlayerContext context, ActionInfo action) {
        if (action.getType() == ActionType.BLOCK_PLACE && action.getTargetBlockOrEntity() != null) {
            if (action.getTargetBlockOrEntity().contains("LAVA")) {
                TrustTier tier = TrustTier.fromPlaytime(context.getPlaytimeSeconds(), trustConfig);
                if (tier.getLevel() < TrustTier.TIER_1.getLevel()) {
                    String msg = config.getString("messages.no_permission_lava",
                            "&cУ вас недостаточно доверия для размещения лавы! (Нужно 60 мин игры)");
                    alertSystem.dispatch("Попытка размещения лавы без доверия",
                            AlertSystem.AlertLevel.HIGH, action.getPlayer().getName());
                    return CheckResult.fail(40, true, msg);
                }
            }
        }
        return CheckResult.pass();
    }
}
