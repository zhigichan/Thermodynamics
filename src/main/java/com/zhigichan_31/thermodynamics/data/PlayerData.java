package com.zhigichan_31.thermodynamics.data;

public class PlayerData {
    private float stamina;
    private float weight;
    private boolean isExhausted;

    public PlayerData() {
        this.stamina = 100.0f;
        this.weight = 0.0f;
        this.isExhausted = false;
    }

    public PlayerData(float stamina, float weight, boolean isExhausted) {
        this.stamina = stamina;
        this.weight = weight;
        this.isExhausted = isExhausted;
    }

    public float getStamina() { return stamina; }
    public void setStamina(float stamina) { this.stamina = Math.clamp(stamina, 0.0f, 100.0f); }

    public float getWeight() { return weight; }
    public void setWeight(float weight) { this.weight = Math.max(0.0f, weight); }

    public boolean isExhausted() { return isExhausted; }
    public void setExhausted(boolean exhausted) { this.isExhausted = exhausted; }
}
