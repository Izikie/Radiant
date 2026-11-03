package net.minecraft.network.packet.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.chat.IChatComponent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PacketBuffer {
    private static final int MAX_STRING_LENGTH = 32767;
    private final ByteBuf buf;

    public PacketBuffer(ByteBuf buf) {
        this.buf = buf;
    }

    /**
     * Retrieves the underlying Netty ByteBuf.
     */
    public ByteBuf getByteBuf() {
        return this.buf;
    }

    public static int getVarIntSize(int input) {
        for (int i = 1; i < 5; ++i) {
            if ((input & -1 << i * 7) == 0) {
                return i;
            }
        }
        return 5;
    }

    public int readVarInt() {
        int result = 0;
        int shift = 0;

        while (true) {
            byte currentByte = this.buf.readByte();
            result |= (currentByte & 127) << shift++ * 7;

            if (shift > 5) {
                throw new DecoderException("VarInt too big (" + shift + " > 5)");
            }

            if ((currentByte & 128) != 128) {
                break;
            }
        }

        return result;
    }

    public void writeVarInt(int input) {
        while ((input & -128) != 0) {
            this.buf.writeByte(input & 127 | 128);
            input >>>= 7;
        }

        this.buf.writeByte(input);
    }

    public long readVarLong() {
        long result = 0L;
        int shift = 0;

        while (true) {
            byte currentByte = this.buf.readByte();
            result |= (long) (currentByte & 127) << shift++ * 7;

            if (shift > 10) {
                throw new DecoderException("VarLong too big (" + shift + " > 10)");
            }

            if ((currentByte & 128) != 128) {
                break;
            }
        }

        return result;
    }

    public void writeVarLong(long value) {
        while ((value & -128L) != 0L) {
            this.buf.writeByte((int) (value & 127L) | 128);
            value >>>= 7;
        }

        this.buf.writeByte((int) value);
    }

    public byte[] readByteArray() {
        byte[] bytes = new byte[this.readVarInt()];
        this.buf.readBytes(bytes);
        return bytes;
    }

    public void writeByteArray(byte[] array) {
        this.writeVarInt(array.length);
        this.buf.writeBytes(array);
    }

    public String readString(int maxLength) {
        int length = this.readVarInt();
        int checkMaxLength = maxLength * 4;

        if (length > checkMaxLength) {
            throw new DecoderException("String too big (" + length + " > " + maxLength + " bytes)");
        } else if (length < 0) {
            throw new DecoderException("String less than zero! Weird string!");
        }

        ByteBuf byteBuf = this.buf.readBytes(length);
        try {
            String result = byteBuf.toString(StandardCharsets.UTF_8);

            if (result.length() > maxLength) {
                throw new DecoderException("String too big (" + result.length() + " > " + maxLength + " chars)");
            }

            return result;
        } finally {
            byteBuf.release();
        }
    }

    public PacketBuffer writeString(String string) {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);

        if (bytes.length > MAX_STRING_LENGTH) {
            throw new EncoderException("String too big (" + bytes.length + " > " + MAX_STRING_LENGTH + " bytes)");
        }

        this.writeVarInt(bytes.length);
        this.buf.writeBytes(bytes);

        return this;
    }

    public BlockPos readBlockPos() {
        return BlockPos.fromLong(this.buf.readLong());
    }

    public void writeBlockPos(BlockPos pos) {
        this.buf.writeLong(pos.toLong());
    }

    public IChatComponent readChatComponent() {
        return IChatComponent.Serializer.jsonToComponent(this.readString(MAX_STRING_LENGTH));
    }

    public void writeChatComponent(IChatComponent component) {
        this.writeString(IChatComponent.Serializer.componentToJson(component));
    }

    public <T extends Enum<T>> T readEnum(Class<T> enumClass) {
        return enumClass.getEnumConstants()[this.readVarInt()];
    }

    public void writeEnum(Enum<?> value) {
        this.writeVarInt(value.ordinal());
    }

    public UUID readUuid() {
        return new UUID(this.buf.readLong(), this.buf.readLong());
    }

    public void writeUuid(UUID uuid) {
        this.buf.writeLong(uuid.getMostSignificantBits());
        this.buf.writeLong(uuid.getLeastSignificantBits());
    }

    public NBTTagCompound readNBTTagCompound() throws IOException {
        int i = this.buf.readerIndex();
        byte byt = this.buf.readByte();

        if (byt == 0) {
            return null;
        }

        this.buf.readerIndex(i);
        return CompressedStreamTools.read(new ByteBufInputStream(this.buf), new NBTSizeTracker(2097152L));
    }

    public void writeNBTTagCompound(NBTTagCompound nbt) {
        if (nbt == null) {
            this.buf.writeByte(0);
            return;
        }

        try {
            CompressedStreamTools.write(nbt, new ByteBufOutputStream(this.buf));
        } catch (IOException exception) {
            throw new EncoderException("Failed to write NBT tag", exception);
        }
    }

    public ItemStack readItemStack() throws IOException {
        ItemStack stack = null;
        int id = this.buf.readShort();

        if (id >= 0) {
            int count = this.buf.readByte();
            int meta = this.buf.readShort();
            stack = new ItemStack(Item.getItemById(id), count, meta);
            stack.setTagCompound(this.readNBTTagCompound());
        }

        return stack;
    }

    public void writeItemStack(ItemStack stack) {
        if (stack == null) {
            this.writeShort(-1);
            return;
        }

        this.writeShort(Item.getIdFromItem(stack.getItem()));
        this.writeByte(stack.stackSize);
        this.writeShort(stack.getMetadata());

        NBTTagCompound tag = null;

        if (stack.getItem().isDamageable() || stack.getItem().getShareTag()) {
            tag = stack.getTagCompound();
        }

        this.writeNBTTagCompound(tag);
    }

    // --- Netty Delegators & Helpers ---

    public PacketBuffer writeBoolean(boolean value) {
        this.buf.writeBoolean(value);
        return this;
    }

    public PacketBuffer writeByte(int value) {
        this.buf.writeByte(value);
        return this;
    }

    public PacketBuffer writeShort(int value) {
        this.buf.writeShort(value);
        return this;
    }

    public PacketBuffer writeInt(int value) {
        this.buf.writeInt(value);
        return this;
    }

    public PacketBuffer writeLong(long value) {
        this.buf.writeLong(value);
        return this;
    }

    public PacketBuffer writeFloat(float value) {
        this.buf.writeFloat(value);
        return this;
    }

    public PacketBuffer writeDouble(double value) {
        this.buf.writeDouble(value);
        return this;
    }

    public boolean readBoolean() {
        return this.buf.readBoolean();
    }

    public byte readByte() {
        return this.buf.readByte();
    }

    public short readUnsignedByte() {
        return this.buf.readUnsignedByte();
    }

    public short readShort() {
        return this.buf.readShort();
    }

    public int readUnsignedShort() {
        return this.buf.readUnsignedShort();
    }

    public int readInt() {
        return this.buf.readInt();
    }

    public long readLong() {
        return this.buf.readLong();
    }

    public float readFloat() {
        return this.buf.readFloat();
    }

    public double readDouble() {
        return this.buf.readDouble();
    }

    public boolean release() {
        return this.buf.release();
    }

    public int readableBytes() {
        return this.buf.readableBytes();
    }

    public int writerIndex() {
        return this.buf.writerIndex();
    }

    public PacketBuffer writerIndex(int writerIndex) {
        this.buf.writerIndex(writerIndex);
        return this;
    }

    public ByteBuf readBytes(int length) {
        return this.buf.readBytes(length);
    }

    public PacketBuffer readBytes(byte[] dst) {
        this.buf.readBytes(dst);
        return this;
    }

    public PacketBuffer readBytes(byte[] dst, int dstIndex, int length) {
        this.buf.readBytes(dst, dstIndex, length);
        return this;
    }

    public PacketBuffer writeBytes(PacketBuffer src) {
        this.buf.writeBytes(src.getByteBuf());
        return this;
    }

    public PacketBuffer writeBytes(ByteBuf src) {
        this.buf.writeBytes(src);
        return this;
    }

    public PacketBuffer writeBytes(byte[] src) {
        this.buf.writeBytes(src);
        return this;
    }

    public PacketBuffer writeBytes(ByteBuf src, int srcIndex, int length) {
        this.buf.writeBytes(src, srcIndex, length);
        return this;
    }

    public PacketBuffer writeBytes(byte[] src, int srcIndex, int length) {
        this.buf.writeBytes(src, srcIndex, length);
        return this;
    }

    public boolean isReadable() {
        return this.buf.isReadable();
    }

    public PacketBuffer ensureWritable(int minWritableBytes) {
        this.buf.ensureWritable(minWritableBytes);
        return this;
    }
}
