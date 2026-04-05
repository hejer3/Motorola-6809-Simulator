package projet;

public class Memory {
    private byte[] data = new byte[65536];

    public Memory() {
        reset();
    }

    public void reset() {
        for (int i = 0; i < data.length; i++) {
            // RAM (0000-7FFF) à 00, ROM (8000-FFFF) à FF
            data[i] = (i < 0x8000) ? (byte) 0x00 : (byte) 0xFF;
        }
    }

    public int read(int address) {
        return data[address & 0xFFFF] & 0xFF;
    }

    public void write(int address, int value) {
        data[address & 0xFFFF] = (byte) value;
    }
}