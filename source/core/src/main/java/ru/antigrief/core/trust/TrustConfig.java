package ru.antigrief.core.trust;

import java.util.ArrayList;
import java.util.List;

public class TrustConfig {
    private int tier1Minutes = 60;
    private int tier2Minutes = 120;
    private int tier3Minutes = 240;

    private List<String> fireMaterials = new ArrayList<>();
    private List<String> explosiveMaterials = new ArrayList<>();
    private List<String> redstoneMaterials = new ArrayList<>();

    public int getTier1Minutes() { return tier1Minutes; }
    public void setTier1Minutes(int tier1Minutes) { this.tier1Minutes = tier1Minutes; }

    public int getTier2Minutes() { return tier2Minutes; }
    public void setTier2Minutes(int tier2Minutes) { this.tier2Minutes = tier2Minutes; }

    public int getTier3Minutes() { return tier3Minutes; }
    public void setTier3Minutes(int tier3Minutes) { this.tier3Minutes = tier3Minutes; }

    public List<String> getFireMaterials() { return fireMaterials; }
    public void setFireMaterials(List<String> fireMaterials) { this.fireMaterials = fireMaterials; }

    public List<String> getExplosiveMaterials() { return explosiveMaterials; }
    public void setExplosiveMaterials(List<String> explosiveMaterials) { this.explosiveMaterials = explosiveMaterials; }

    public List<String> getRedstoneMaterials() { return redstoneMaterials; }
    public void setRedstoneMaterials(List<String> redstoneMaterials) { this.redstoneMaterials = redstoneMaterials; }
}
