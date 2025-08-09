package qr.encoding;

import java.util.BitSet;

public interface IEncoder {
  BitSet encode(String input);
}
