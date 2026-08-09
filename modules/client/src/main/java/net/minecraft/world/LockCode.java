package net.minecraft.world;

import net.minecraft.nbt.NBTTagCompound;

public record LockCode(String lock) {
    public static final LockCode EMPTY_CODE = new LockCode("");

    public boolean isEmpty() {
        return this.lock == null || this.lock.isEmpty();
    }

    public void toNBT(NBTTagCompound nbt) {
        nbt.setString("Lock", this.lock);
    }

    public static LockCode fromNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("Lock", 8)) {
            String s = nbt.getString("Lock");
            return new LockCode(s);
        } else {
            return EMPTY_CODE;
        }
    }
}
