/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.bufr;

import ucar.nc2.Sequence;
import ucar.nc2.Structure;

import java.io.IOException;
import java.util.Formatter;

public class MessageBitCounter {
  final Structure struct;
  final Message proto;
  final Message message;
  final Formatter out;

  int msg_nbits;

  // LOOK why do we need Structure struct and Message proto?
  public MessageBitCounter(Sequence struct, Message proto, Message message, Formatter out) {
    this.struct = struct;
    this.proto = proto;
    this.message = message;
    this.out = out;

    this.msg_nbits = calcTotalBits();
  }

  public int getCountedDataBits() {
    return msg_nbits;
  }

  public boolean isBitCountOk() {
    message.getRootDataDescriptor(); // make sure root is calculated
    // int nbitsGiven = 8 * (dataSection.getDataLength() - 4);
    int nbytesCounted = getCountedDataBytes();
    int nbytesGiven = message.dataSection.getDataLength();
    return Math.abs(nbytesCounted - nbytesGiven) <= 1; // radiosondes dataLen not even number of bytes
  }

  public int getCountedDataBytes() {
    int msg_nbytes = msg_nbits / 8;
    if (msg_nbits % 8 != 0) {
      msg_nbytes++;
    }
    msg_nbytes += 4;
    if (msg_nbytes % 2 != 0) {
      msg_nbytes++; // LOOK seems to be violated by some messages
    }
    return msg_nbytes;
  }

  // only have one raf, so cant do multiple messages in parellel
  private int calcTotalBits() {
    try {
      if (!message.dds.isCompressed()) {
        MessageArrayUncompressedReader reader2 =
            new MessageArrayUncompressedReader(struct, proto, message, message.raf(), out);
        reader2.readEntireMessage();

      } else {
        MessageArrayCompressedReader reader2 =
            new MessageArrayCompressedReader(struct, proto, message, message.raf(), out);
        reader2.readEntireMessage();
      }
    } catch (IOException ioe) {
      return 0;
    }

    return message.msg_nbits;
  }


}
