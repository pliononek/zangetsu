package com.zangetsu.init;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class ReiatsuData {
    public static final Codec<ReiatsuData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("current").forGetter(ReiatsuData::getCurrent),
            Codec.FLOAT.fieldOf("max").forGetter(ReiatsuData::getMax),
            Codec.BOOL.fieldOf("infusion").forGetter(ReiatsuData::isInfusionActive),
            Codec.STRING.optionalFieldOf("returnDim", "minecraft:overworld").forGetter(ReiatsuData::getReturnDim),
            Codec.DOUBLE.optionalFieldOf("returnX", 0.0).forGetter(ReiatsuData::getReturnX),
            Codec.DOUBLE.optionalFieldOf("returnY", 100.0).forGetter(ReiatsuData::getReturnY),
            Codec.DOUBLE.optionalFieldOf("returnZ", 0.0).forGetter(ReiatsuData::getReturnZ),
            Codec.BOOL.optionalFieldOf("bankaiUnlocked", false).forGetter(ReiatsuData::isBankaiUnlocked)
    ).apply(instance, ReiatsuData::new));

    private float current;
    private float max;
    private boolean infusionActive;
    private String returnDim;
    private double returnX;
    private double returnY;
    private double returnZ;
    private boolean bankaiUnlocked;

    public ReiatsuData() {
        this(1000.0f, 1000.0f, false, "minecraft:overworld", 0.0, 100.0, 0.0, false);
    }

    public ReiatsuData(float current, float max, boolean infusionActive, String returnDim, double returnX, double returnY, double returnZ, boolean bankaiUnlocked) {
        this.current = current;
        this.max = max;
        this.infusionActive = infusionActive;
        this.returnDim = returnDim != null ? returnDim : "minecraft:overworld";
        this.returnX = returnX;
        this.returnY = returnY;
        this.returnZ = returnZ;
        this.bankaiUnlocked = bankaiUnlocked;
    }

    public float getCurrent() {
        return current;
    }

    public void setCurrent(float current) {
        this.current = Math.max(0.0f, Math.min(this.max, current));
    }

    public float getMax() {
        return max;
    }

    public void setMax(float max) {
        this.max = max;
    }

    public boolean isInfusionActive() {
        return infusionActive;
    }

    public void setInfusionActive(boolean infusionActive) {
        this.infusionActive = infusionActive;
    }

    public String getReturnDim() {
        return returnDim;
    }

    public void setReturnDim(String returnDim) {
        this.returnDim = returnDim;
    }

    public double getReturnX() {
        return returnX;
    }

    public void setReturnX(double returnX) {
        this.returnX = returnX;
    }

    public double getReturnY() {
        return returnY;
    }

    public void setReturnY(double returnY) {
        this.returnY = returnY;
    }

    public double getReturnZ() {
        return returnZ;
    }

    public void setReturnZ(double returnZ) {
        this.returnZ = returnZ;
    }

    public void setReturnLocation(String dim, double x, double y, double z) {
        this.returnDim = dim;
        this.returnX = x;
        this.returnY = y;
        this.returnZ = z;
    }

    public boolean consume(float amount) {
        if (this.current >= amount) {
            this.current -= amount;
            return true;
        }
        return false;
    }

    public void restore(float amount) {
        if (this.infusionActive) return; // Zero regeneration while Infusion is active!
        this.current = Math.min(this.max, this.current + amount);
    }

    public void checkAndMigrateMax() {
        if (this.max < 1000.0f) {
            float ratio = this.max > 0 ? (this.current / this.max) : 1.0f;
            this.max = 1000.0f;
            this.current = Math.min(1000.0f, Math.max(100.0f, ratio * 1000.0f));
        }
    }

    public boolean isBankaiUnlocked() {
        return bankaiUnlocked;
    }

    public void setBankaiUnlocked(boolean bankaiUnlocked) {
        this.bankaiUnlocked = bankaiUnlocked;
    }
}
