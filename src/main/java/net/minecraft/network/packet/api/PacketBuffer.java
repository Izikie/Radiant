package net.minecraft.network.packet.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.chat.IChatComponent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PacketBuffer extends ByteBuf {
    private final ByteBuf buf;

    public PacketBuffer(ByteBuf wrapped) {
        this.buf = wrapped;
    }

    public static int getVarIntSize(int input) {
        for (int i = 1; i < 5; ++i) {
            if ((input & -1 << i * 7) == 0) {
                return i;
            }
        }

        return 5;
    }

    public void writeByteArray(byte[] array) {
        this.writeVarIntToBuffer(array.length);
        this.writeBytes(array);
    }

    public byte[] readByteArray() {
        byte[] bytes = new byte[this.readVarIntFromBuffer()];
        this.readBytes(bytes);
        return bytes;
    }

    public BlockPos readBlockPos() {
        return BlockPos.fromLong(this.readLong());
    }

    public void writeBlockPos(BlockPos pos) {
        this.writeLong(pos.toLong());
    }

    public IChatComponent readChatComponent() {
        return IChatComponent.Serializer.jsonToComponent(this.readStringFromBuffer(32767));
    }

    public void writeChatComponent(IChatComponent component) {
        this.writeString(IChatComponent.Serializer.componentToJson(component));
    }

    public <T extends Enum<T>> T readEnumValue(Class<T> enumClass) {
        return enumClass.getEnumConstants()[this.readVarIntFromBuffer()];
    }

    public void writeEnumValue(Enum<?> value) {
        this.writeVarIntToBuffer(value.ordinal());
    }

    public int readVarIntFromBuffer() {
        int result = 0;
        int shift = 0;

        while (true) {
            byte currentByte = this.readByte();
            result |= (currentByte & 127) << shift++ * 7;

            if (shift > 5) {
                throw new RuntimeException("VarInt too big");
            }

            if ((currentByte & 128) != 128) {
                break;
            }
        }

        return result;
    }

    public long readVarLong() {
        long result = 0L;
        int shift = 0;

        while (true) {
            byte currentByte = this.readByte();
            result |= (long) (currentByte & 127) << shift++ * 7;

            if (shift > 10) {
                throw new RuntimeException("VarLong too big");
            }

            if ((currentByte & 128) != 128) {
                break;
            }
        }

        return result;
    }

    public void writeUuid(UUID uuid) {
        this.writeLong(uuid.getMostSignificantBits());
        this.writeLong(uuid.getLeastSignificantBits());
    }

    public UUID readUuid() {
        return new UUID(this.readLong(), this.readLong());
    }

    public void writeVarIntToBuffer(int input) {
        while ((input & -128) != 0) {
            this.writeByte(input & 127 | 128);
            input >>>= 7;
        }

        this.writeByte(input);
    }

    public void writeVarLong(long value) {
        while ((value & -128L) != 0L) {
            this.writeByte((int) (value & 127L) | 128);
            value >>>= 7;
        }

        this.writeByte((int) value);
    }

    public void writeNBTTagCompoundToBuffer(NBTTagCompound nbt) {
        if (nbt == null) {
            this.writeByte(0);
        } else {
            try {
                CompressedStreamTools.write(nbt, new ByteBufOutputStream(this));
            } catch (IOException exception) {
                throw new EncoderException(exception);
            }
        }
    }

    public NBTTagCompound readNBTTagCompoundFromBuffer() throws IOException {
        int i = this.readerIndex();
        byte b0 = this.readByte();

        if (b0 == 0) {
            return null;
        } else {
            this.readerIndex(i);
            return CompressedStreamTools.read(new ByteBufInputStream(this), new NBTSizeTracker(2097152L));
        }
    }

    public void writeItemStackToBuffer(ItemStack stack) {
        if (stack == null) {
            this.writeShort(-1);
        } else {
            this.writeShort(Item.getIdFromItem(stack.getItem()));
            this.writeByte(stack.stackSize);
            this.writeShort(stack.getMetadata());
            NBTTagCompound nbttagcompound = null;

            if (stack.getItem().isDamageable() || stack.getItem().getShareTag()) {
                nbttagcompound = stack.getTagCompound();
            }

            this.writeNBTTagCompoundToBuffer(nbttagcompound);
        }
    }

    public ItemStack readItemStackFromBuffer() throws IOException {
        ItemStack itemstack = null;
        int i = this.readShort();

        if (i >= 0) {
            int j = this.readByte();
            int k = this.readShort();
            itemstack = new ItemStack(Item.getItemById(i), j, k);
            itemstack.setTagCompound(this.readNBTTagCompoundFromBuffer());
        }

        return itemstack;
    }

    public String readStringFromBuffer(int maxLength) {
        int i = this.readVarIntFromBuffer();

        if (i > maxLength * 4) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + i + " > " + maxLength * 4 + ")");
        } else if (i < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        } else {
            // Handle both direct and heap buffers safely
            ByteBuf byteBuf = this.readBytes(i);
            String s;

            try {
                if (byteBuf.hasArray()) {
                    // Use array() for heap buffers
                    s = new String(byteBuf.array(), byteBuf.arrayOffset() + byteBuf.readerIndex(), byteBuf.readableBytes(), StandardCharsets.UTF_8);
                } else {
                    // Use getBytes() for direct buffers
                    byte[] bytes = new byte[byteBuf.readableBytes()];
                    byteBuf.getBytes(byteBuf.readerIndex(), bytes);
                    s = new String(bytes, StandardCharsets.UTF_8);
                }
            } finally {
                byteBuf.release(); // Always release the buffer to prevent memory leaks
            }

            if (s.length() > maxLength) {
                throw new DecoderException("The received string length is longer than maximum allowed (" + i + " > " + maxLength + ")");
            } else {
                return s;
            }
        }
    }

    public PacketBuffer writeString(String string) {
        byte[] abyte = string.getBytes(StandardCharsets.UTF_8);

        if (abyte.length > 32767) {
            throw new EncoderException("String too big (was " + string.length() + " bytes encoded, max " + 32767 + ")");
        } else {
            this.writeVarIntToBuffer(abyte.length);
            this.writeBytes(abyte);
            return this;
        }
    }

    @Override
    public int capacity() {
        return this.buf.capacity();
    }

    @Override
    public ByteBuf capacity(int newCapacity) {
        return this.buf.capacity(newCapacity);
    }

    @Override
    public int maxCapacity() {
        return this.buf.maxCapacity();
    }

    @Override
    public ByteBufAllocator alloc() {
        return this.buf.alloc();
    }

    @Override
    public ByteOrder order() {
        return this.buf.order();
    }

    @Override
    public ByteBuf order(ByteOrder endianness) {
        return this.buf.order(endianness);
    }

    @Override
    public ByteBuf unwrap() {
        return this.buf.unwrap();
    }

    @Override
    public boolean isDirect() {
        return this.buf.isDirect();
    }

    @Override
    public int readerIndex() {
        return this.buf.readerIndex();
    }

    @Override
    public ByteBuf readerIndex(int readerIndex) {
        return this.buf.readerIndex(readerIndex);
    }

    @Override
    public int writerIndex() {
        return this.buf.writerIndex();
    }

    @Override
    public ByteBuf writerIndex(int writerIndex) {
        return this.buf.writerIndex(writerIndex);
    }

    @Override
    public ByteBuf setIndex(int readerIndex, int writerIndex) {
        return this.buf.setIndex(readerIndex, writerIndex);
    }

    @Override
    public int readableBytes() {
        return this.buf.readableBytes();
    }

    @Override
    public int writableBytes() {
        return this.buf.writableBytes();
    }

    @Override
    public int maxWritableBytes() {
        return this.buf.maxWritableBytes();
    }

    @Override
    public boolean isReadable() {
        return this.buf.isReadable();
    }

    @Override
    public boolean isReadable(int size) {
        return this.buf.isReadable(size);
    }

    @Override
    public boolean isWritable() {
        return this.buf.isWritable();
    }

    @Override
    public boolean isWritable(int size) {
        return this.buf.isWritable(size);
    }

    @Override
    public ByteBuf clear() {
        return this.buf.clear();
    }

    @Override
    public ByteBuf markReaderIndex() {
        return this.buf.markReaderIndex();
    }

    @Override
    public ByteBuf resetReaderIndex() {
        return this.buf.resetReaderIndex();
    }

    @Override
    public ByteBuf markWriterIndex() {
        return this.buf.markWriterIndex();
    }

    @Override
    public ByteBuf resetWriterIndex() {
        return this.buf.resetWriterIndex();
    }

    @Override
    public ByteBuf discardReadBytes() {
        return this.buf.discardReadBytes();
    }

    @Override
    public ByteBuf discardSomeReadBytes() {
        return this.buf.discardSomeReadBytes();
    }

    @Override
    public ByteBuf ensureWritable(int minWritableBytes) {
        return this.buf.ensureWritable(minWritableBytes);
    }

    @Override
    public int ensureWritable(int minWritableBytes, boolean force) {
        return this.buf.ensureWritable(minWritableBytes, force);
    }

    @Override
    public boolean getBoolean(int index) {
        return this.buf.getBoolean(index);
    }

    @Override
    public byte getByte(int index) {
        return this.buf.getByte(index);
    }

    @Override
    public short getUnsignedByte(int index) {
        return this.buf.getUnsignedByte(index);
    }

    @Override
    public short getShort(int index) {
        return this.buf.getShort(index);
    }

    @Override
    public int getUnsignedShort(int index) {
        return this.buf.getUnsignedShort(index);
    }

    @Override
    public int getMedium(int index) {
        return this.buf.getMedium(index);
    }

    @Override
    public int getUnsignedMedium(int index) {
        return this.buf.getUnsignedMedium(index);
    }

    @Override
    public int getInt(int index) {
        return this.buf.getInt(index);
    }

    @Override
    public long getUnsignedInt(int index) {
        return this.buf.getUnsignedInt(index);
    }

    @Override
    public long getLong(int index) {
        return this.buf.getLong(index);
    }

    @Override
    public char getChar(int index) {
        return this.buf.getChar(index);
    }

    @Override
    public float getFloat(int index) {
        return this.buf.getFloat(index);
    }

    @Override
    public double getDouble(int index) {
        return this.buf.getDouble(index);
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst) {
        return this.buf.getBytes(index, dst);
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int length) {
        return this.buf.getBytes(index, dst, length);
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        return this.buf.getBytes(index, dst, dstIndex, length);
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst) {
        return this.buf.getBytes(index, dst);
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
        return this.buf.getBytes(index, dst, dstIndex, length);
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuffer dst) {
        return this.buf.getBytes(index, dst);
    }

    @Override
    public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
        return this.buf.getBytes(index, out, length);
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        return this.buf.getBytes(index, out, length);
    }

    @Override
    public ByteBuf setBoolean(int index, boolean value) {
        return this.buf.setBoolean(index, value);
    }

    @Override
    public ByteBuf setByte(int index, int value) {
        return this.buf.setByte(index, value);
    }

    @Override
    public ByteBuf setShort(int index, int value) {
        return this.buf.setShort(index, value);
    }

    @Override
    public ByteBuf setMedium(int index, int value) {
        return this.buf.setMedium(index, value);
    }

    @Override
    public ByteBuf setInt(int index, int value) {
        return this.buf.setInt(index, value);
    }

    @Override
    public ByteBuf setLong(int index, long value) {
        return this.buf.setLong(index, value);
    }

    @Override
    public ByteBuf setChar(int index, int value) {
        return this.buf.setChar(index, value);
    }

    @Override
    public ByteBuf setFloat(int index, float value) {
        return this.buf.setFloat(index, value);
    }

    @Override
    public ByteBuf setDouble(int index, double value) {
        return this.buf.setDouble(index, value);
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src) {
        return this.buf.setBytes(index, src);
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int length) {
        return this.buf.setBytes(index, src, length);
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
        return this.buf.setBytes(index, src, srcIndex, length);
    }

    @Override
    public ByteBuf setBytes(int index, byte[] src) {
        return this.buf.setBytes(index, src);
    }

    @Override
    public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
        return this.buf.setBytes(index, src, srcIndex, length);
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuffer src) {
        return this.buf.setBytes(index, src);
    }

    @Override
    public int setBytes(int index, InputStream in, int length) throws IOException {
        return this.buf.setBytes(index, in, length);
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        return this.buf.setBytes(index, in, length);
    }

    @Override
    public ByteBuf setZero(int index, int length) {
        return this.buf.setZero(index, length);
    }

    @Override
    public boolean readBoolean() {
        return this.buf.readBoolean();
    }

    @Override
    public byte readByte() {
        return this.buf.readByte();
    }

    @Override
    public short readUnsignedByte() {
        return this.buf.readUnsignedByte();
    }

    @Override
    public short readShort() {
        return this.buf.readShort();
    }

    @Override
    public int readUnsignedShort() {
        return this.buf.readUnsignedShort();
    }

    @Override
    public int readMedium() {
        return this.buf.readMedium();
    }

    @Override
    public int readUnsignedMedium() {
        return this.buf.readUnsignedMedium();
    }

    @Override
    public int readInt() {
        return this.buf.readInt();
    }

    @Override
    public long readUnsignedInt() {
        return this.buf.readUnsignedInt();
    }

    @Override
    public long readLong() {
        return this.buf.readLong();
    }

    @Override
    public char readChar() {
        return this.buf.readChar();
    }

    @Override
    public float readFloat() {
        return this.buf.readFloat();
    }

    @Override
    public double readDouble() {
        return this.buf.readDouble();
    }

    @Override
    public ByteBuf readBytes(int length) {
        return this.buf.readBytes(length);
    }

    @Override
    public ByteBuf readSlice(int length) {
        return this.buf.readSlice(length);
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst) {
        return this.buf.readBytes(dst);
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst, int length) {
        return this.buf.readBytes(dst, length);
    }

    @Override
    public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
        return this.buf.readBytes(dst, dstIndex, length);
    }

    @Override
    public ByteBuf readBytes(byte[] dst) {
        return this.buf.readBytes(dst);
    }

    @Override
    public ByteBuf readBytes(byte[] dst, int dstIndex, int length) {
        return this.buf.readBytes(dst, dstIndex, length);
    }

    @Override
    public ByteBuf readBytes(ByteBuffer dst) {
        return this.buf.readBytes(dst);
    }

    @Override
    public ByteBuf readBytes(OutputStream out, int length) throws IOException {
        return this.buf.readBytes(out, length);
    }

    @Override
    public int readBytes(GatheringByteChannel out, int length) throws IOException {
        return this.buf.readBytes(out, length);
    }

    @Override
    public ByteBuf skipBytes(int length) {
        return this.buf.skipBytes(length);
    }

    @Override
    public ByteBuf writeBoolean(boolean value) {
        return this.buf.writeBoolean(value);
    }

    @Override
    public ByteBuf writeByte(int value) {
        return this.buf.writeByte(value);
    }

    @Override
    public ByteBuf writeShort(int value) {
        return this.buf.writeShort(value);
    }

    @Override
    public ByteBuf writeMedium(int value) {
        return this.buf.writeMedium(value);
    }

    @Override
    public ByteBuf writeInt(int value) {
        return this.buf.writeInt(value);
    }

    @Override
    public ByteBuf writeLong(long value) {
        return this.buf.writeLong(value);
    }

    @Override
    public ByteBuf writeChar(int value) {
        return this.buf.writeChar(value);
    }

    @Override
    public ByteBuf writeFloat(float value) {
        return this.buf.writeFloat(value);
    }

    @Override
    public ByteBuf writeDouble(double value) {
        return this.buf.writeDouble(value);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src) {
        return this.buf.writeBytes(src);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src, int length) {
        return this.buf.writeBytes(src, length);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
        return this.buf.writeBytes(src, srcIndex, length);
    }

    @Override
    public ByteBuf writeBytes(byte[] src) {
        return this.buf.writeBytes(src);
    }

    @Override
    public ByteBuf writeBytes(byte[] src, int srcIndex, int length) {
        return this.buf.writeBytes(src, srcIndex, length);
    }

    @Override
    public ByteBuf writeBytes(ByteBuffer src) {
        return this.buf.writeBytes(src);
    }

    @Override
    public int writeBytes(InputStream in, int length) throws IOException {
        return this.buf.writeBytes(in, length);
    }

    @Override
    public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
        return this.buf.writeBytes(in, length);
    }

    @Override
    public ByteBuf writeZero(int length) {
        return this.buf.writeZero(length);
    }

    @Override
    public int indexOf(int fromIndex, int toIndex, byte value) {
        return this.buf.indexOf(fromIndex, toIndex, value);
    }

    @Override
    public int bytesBefore(byte value) {
        return this.buf.bytesBefore(value);
    }

    @Override
    public int bytesBefore(int length, byte value) {
        return this.buf.bytesBefore(length, value);
    }

    @Override
    public int bytesBefore(int index, int length, byte value) {
        return this.buf.bytesBefore(index, length, value);
    }

    @Override
    public int forEachByte(ByteProcessor processor) {
        return this.buf.forEachByte(processor);
    }

    @Override
    public int forEachByte(int index, int length, ByteProcessor processor) {
        return this.buf.forEachByte(index, length, processor);
    }

    @Override
    public int forEachByteDesc(ByteProcessor processor) {
        return this.buf.forEachByteDesc(processor);
    }

    @Override
    public int forEachByteDesc(int index, int length, ByteProcessor processor) {
        return this.buf.forEachByteDesc(index, length, processor);
    }

    @Override
    public ByteBuf copy() {
        return this.buf.copy();
    }

    @Override
    public ByteBuf copy(int index, int length) {
        return this.buf.copy(index, length);
    }

    @Override
    public ByteBuf slice() {
        return this.buf.slice();
    }

    @Override
    public ByteBuf slice(int index, int length) {
        return this.buf.slice(index, length);
    }

    @Override
    public ByteBuf duplicate() {
        return this.buf.duplicate();
    }

    @Override
    public int nioBufferCount() {
        return this.buf.nioBufferCount();
    }

    @Override
    public ByteBuffer nioBuffer() {
        return this.buf.nioBuffer();
    }

    @Override
    public ByteBuffer nioBuffer(int index, int length) {
        return this.buf.nioBuffer(index, length);
    }

    @Override
    public ByteBuffer internalNioBuffer(int index, int length) {
        return this.buf.internalNioBuffer(index, length);
    }

    @Override
    public ByteBuffer[] nioBuffers() {
        return this.buf.nioBuffers();
    }

    @Override
    public ByteBuffer[] nioBuffers(int index, int length) {
        return this.buf.nioBuffers(index, length);
    }

    @Override
    public boolean hasArray() {
        return this.buf.hasArray();
    }

    @Override
    public byte[] array() {
        if (this.buf.hasArray()) {
            return this.buf.array();
        } else {
            // For direct buffers, create a copy of the data
            byte[] array = new byte[this.buf.readableBytes()];
            this.buf.getBytes(this.buf.readerIndex(), array);
            return array;
        }
    }

    @Override
    public int arrayOffset() {
        if (this.buf.hasArray()) {
            return this.buf.arrayOffset();
        } else {
            // For direct buffers, return 0 since we create a new array
            return 0;
        }
    }

    @Override
    public boolean hasMemoryAddress() {
        return this.buf.hasMemoryAddress();
    }

    @Override
    public long memoryAddress() {
        return this.buf.memoryAddress();
    }

    @Override
    public String toString(Charset charset) {
        return this.buf.toString(charset);
    }

    @Override
    public String toString(int index, int length, Charset charset) {
        return this.buf.toString(index, length, charset);
    }

    public int hashCode() {
        return this.buf.hashCode();
    }

    public boolean equals(Object object) {
        return buf.equals(object);
    }

    @Override
    public int compareTo(ByteBuf buffer) {
        return this.buf.compareTo(buffer);
    }

    public String toString() {
        return this.buf.toString();
    }

    @Override
    public ByteBuf retain(int increment) {
        return this.buf.retain(increment);
    }

    @Override
    public ByteBuf retain() {
        return this.buf.retain();
    }

    @Override
    public int refCnt() {
        return this.buf.refCnt();
    }

    @Override
    public boolean release() {
        return this.buf.release();
    }

    @Override
    public boolean release(int decrement) {
        return this.buf.release(decrement);
    }

    @Override
    public ByteBuf touch() {
        return this.buf.touch();
    }

    @Override
    public ByteBuf touch(Object hint) {
        return this.buf.touch(hint);
    }

    @Override
    public ByteBuf retainedDuplicate() {
        return this.buf.retainedDuplicate();
    }

    @Override
    public ByteBuf retainedSlice() {
        return this.buf.retainedSlice();
    }

    @Override
    public ByteBuf retainedSlice(int index, int length) {
        return this.buf.retainedSlice(index, length);
    }

    // New methods added in Netty 4.1.x
    @Override
    public ByteBuf readRetainedSlice(int length) {
        return this.buf.readRetainedSlice(length);
    }

    @Override
    public CharSequence getCharSequence(int index, int length, Charset charset) {
        return this.buf.getCharSequence(index, length, charset);
    }

    @Override
    public int setCharSequence(int index, CharSequence sequence, Charset charset) {
        return this.buf.setCharSequence(index, sequence, charset);
    }

    @Override
    public CharSequence readCharSequence(int length, Charset charset) {
        return this.buf.readCharSequence(length, charset);
    }

    @Override
    public int writeCharSequence(CharSequence sequence, Charset charset) {
        return this.buf.writeCharSequence(sequence, charset);
    }

    @Override
    public int getBytes(int index, FileChannel out, long position, int length) throws IOException {
        return this.buf.getBytes(index, out, position, length);
    }

    @Override
    public int setBytes(int index, FileChannel in, long position, int length) throws IOException {
        return this.buf.setBytes(index, in, position, length);
    }

    @Override
    public int readBytes(FileChannel out, long position, int length) throws IOException {
        return this.buf.readBytes(out, position, length);
    }

    @Override
    public int writeBytes(FileChannel in, long position, int length) throws IOException {
        return this.buf.writeBytes(in, position, length);
    }

    @Override
    public boolean isReadOnly() {
        return this.buf.isReadOnly();
    }

    @Override
    public ByteBuf asReadOnly() {
        return this.buf.asReadOnly();
    }

    // Little Endian methods added in Netty 4.1.x
    @Override
    public short getShortLE(int index) {
        return this.buf.getShortLE(index);
    }

    @Override
    public int getUnsignedShortLE(int index) {
        return this.buf.getUnsignedShortLE(index);
    }

    @Override
    public int getMediumLE(int index) {
        return this.buf.getMediumLE(index);
    }

    @Override
    public int getUnsignedMediumLE(int index) {
        return this.buf.getUnsignedMediumLE(index);
    }

    @Override
    public int getIntLE(int index) {
        return this.buf.getIntLE(index);
    }

    @Override
    public long getUnsignedIntLE(int index) {
        return this.buf.getUnsignedIntLE(index);
    }

    @Override
    public long getLongLE(int index) {
        return this.buf.getLongLE(index);
    }

    @Override
    public float getFloatLE(int index) {
        return this.buf.getFloatLE(index);
    }

    @Override
    public double getDoubleLE(int index) {
        return this.buf.getDoubleLE(index);
    }

    @Override
    public ByteBuf setShortLE(int index, int value) {
        return this.buf.setShortLE(index, value);
    }

    @Override
    public ByteBuf setMediumLE(int index, int value) {
        return this.buf.setMediumLE(index, value);
    }

    @Override
    public ByteBuf setIntLE(int index, int value) {
        return this.buf.setIntLE(index, value);
    }

    @Override
    public ByteBuf setLongLE(int index, long value) {
        return this.buf.setLongLE(index, value);
    }

    @Override
    public ByteBuf setFloatLE(int index, float value) {
        return this.buf.setFloatLE(index, value);
    }

    @Override
    public ByteBuf setDoubleLE(int index, double value) {
        return this.buf.setDoubleLE(index, value);
    }

    @Override
    public short readShortLE() {
        return this.buf.readShortLE();
    }

    @Override
    public int readUnsignedShortLE() {
        return this.buf.readUnsignedShortLE();
    }

    @Override
    public int readMediumLE() {
        return this.buf.readMediumLE();
    }

    @Override
    public int readUnsignedMediumLE() {
        return this.buf.readUnsignedMediumLE();
    }

    @Override
    public int readIntLE() {
        return this.buf.readIntLE();
    }

    @Override
    public long readUnsignedIntLE() {
        return this.buf.readUnsignedIntLE();
    }

    @Override
    public long readLongLE() {
        return this.buf.readLongLE();
    }

    @Override
    public float readFloatLE() {
        return this.buf.readFloatLE();
    }

    @Override
    public double readDoubleLE() {
        return this.buf.readDoubleLE();
    }

    @Override
    public ByteBuf writeShortLE(int value) {
        return this.buf.writeShortLE(value);
    }

    @Override
    public ByteBuf writeMediumLE(int value) {
        return this.buf.writeMediumLE(value);
    }

    @Override
    public ByteBuf writeIntLE(int value) {
        return this.buf.writeIntLE(value);
    }

    @Override
    public ByteBuf writeLongLE(long value) {
        return this.buf.writeLongLE(value);
    }

    @Override
    public ByteBuf writeFloatLE(float value) {
        return this.buf.writeFloatLE(value);
    }

    @Override
    public ByteBuf writeDoubleLE(double value) {
        return this.buf.writeDoubleLE(value);
    }
}
