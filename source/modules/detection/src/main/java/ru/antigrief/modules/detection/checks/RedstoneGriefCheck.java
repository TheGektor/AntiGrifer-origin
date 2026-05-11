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

public class RedstoneGriefCheck implements Check {
    private final TrustConfig trustConfig;
    private final ConfigManager config;
    private final AlertSystem alertSystem;

    public RedstoneGriefCheck(TrustConfig trustConfig, ConfigManager config, AlertSystem alertSystem) {
        this.trustConfig = trustConfig;
        this.config = config;
        this.alertSystem = alertSystem;
    }

    @Override
    public String getName() {
        return "RedstoneGriefCheck";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public CheckResult check(PlayerContext context, ActionInfo action) {
        if (action.getType() == ActionType.REDSTONE_INTERACT) {
            TrustTier tier = TrustTier.resolve(context, trustConfig);
            
            if (tier.getLevel() < TrustTier.TIER_3.getLevel()) {
                // Check if the material is in the restricted list for Tier 3
                if (trustConfig.getRedstoneMaterials().contains(action.getTargetBlockOrEntity())) {
                    String msg = config.getString("messages.no_permission_redstone", "&cУ вас недостаточно доверия для взаимодействия с этими механизмами! (Нужно 240 мин игры)");
                    alertSystem.dispatch("Взаимодействие с редстоун-механизмами без доверия", AlertSystem.AlertLevel.MEDIUM, action.getPlayer().getName());
                    return CheckResult.fail(20, true, msg);
                }
            }
        }
        return CheckResult.pass();
    }
}
