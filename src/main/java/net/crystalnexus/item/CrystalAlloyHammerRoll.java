package net.crystalnexus.item;

final class CrystalAlloyHammerRoll {
    private CrystalAlloyHammerRoll() {
    }

    static boolean createsNode(float roll, float chance) {
        return roll < chance;
    }
}
