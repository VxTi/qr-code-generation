package qr;

public enum MaskPattern {

    /**
     * No mask. This should never be used in regular QR codes
     */
    MASK0(0b000, (x, y) -> false),
    MASK1(0b001, (x, y) -> y % 2 == 0),
    MASK2(0b010, (x, y) -> x % 3 == 0),
    MASK3(0b011, (x, y) -> (x + y) % 3 == 0),
    MASK4(0b100, (x, y) -> (x / 3 + y / 2) % 2 == 0),
    MASK5(0b101, (x, y) -> (x * y) % 2 + (x * y) / 3 == 0),
    MASK6(0b110, (x, y) -> ((x * y) % 2 + (x * y) % 3) == 0),
    MASK7(0b111, (x, y) -> ((x + y) / 2 + (x * y) % 3) % 2 == 0);


    private final int bitmask;
    private final Generator generator;

    public Generator getGenerator() {
        return this.generator;
    }

    public int getMask() {
        return this.bitmask;
    }

    public static Generator fromBitmask(int bitmask) {
        for (final MaskPattern pattern : values()) {
            if (pattern.bitmask != bitmask) continue;

            return pattern.generator;
        }
        return MASK0.generator;
    }

    MaskPattern(int bitmask, Generator generator) {
        this.bitmask = bitmask;
        this.generator = generator;
    }

    public interface Generator {
        boolean mask(int x, int y);
    }
}
