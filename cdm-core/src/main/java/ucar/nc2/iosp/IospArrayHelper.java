/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp;

import ucar.array.ArrayType;
import ucar.unidata.io.PositioningDataInputStream;
import ucar.unidata.io.RandomAccessFile;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/** Helper methods for IOSP's for reading data. */
public class IospArrayHelper {
  private static final boolean showLayoutTypes = false;

  /**
   * Read data subset from RandomAccessFile, create primitive array of size Layout.getTotalNelems.
   * Reading is controlled by the Layout object.
   *
   * @param raf read from here.
   * @param index handles skipping around in the file.
   * @param arrayType ArrayType of the variable
   * @param fillValue must be Number if ArrayType.isNumeric(), or String for STRING, byte[] for Structure, or null for
   *        none
   * @param byteOrder if equal to RandomAccessFile.ORDER_XXXX, set the byte order just before reading
   * @return primitive array with data read in
   * @throws IOException on read error
   */
  public static Object readDataFill(RandomAccessFile raf, Layout index, ArrayType arrayType, Object fillValue,
      ByteOrder byteOrder) throws IOException {
    Object arr = (fillValue == null) ? makePrimitiveArray((int) index.getTotalNelems(), arrayType)
        : makePrimitiveArray((int) index.getTotalNelems(), arrayType, fillValue);
    return readData(raf, index, arrayType, arr, byteOrder);
  }

  /**
   * Read data subset from RandomAccessFile, place in given primitive array.
   * Reading is controlled by the Layout object.
   *
   * @param raf read from here.
   * @param layout handles skipping around in the file.
   * @param arrayType ArrayType of the variable
   * @param arr primitive array to read data into
   * @param byteOrder if equal to RandomAccessFile.ORDER_XXXX, set the byte order just before reading
   * @return primitive array with data read in
   * @throws IOException on read error
   */
  public static Object readData(RandomAccessFile raf, Layout layout, ArrayType arrayType, Object arr,
      ByteOrder byteOrder) throws IOException {
    if (showLayoutTypes)
      System.out.println("***RAF LayoutType=" + layout.getClass().getName());

    if (arrayType.getPrimitiveClass() == Byte.class || arrayType == ArrayType.CHAR) {
      byte[] pa = (byte[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readFully(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (arrayType.getPrimitiveClass() == Short.class) {
      short[] pa = (short[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readShort(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (arrayType.getPrimitiveClass() == Integer.class) {
      int[] pa = (int[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readInt(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (arrayType == ArrayType.FLOAT) {
      float[] pa = (float[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readFloat(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (arrayType == ArrayType.DOUBLE) {
      double[] pa = (double[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readDouble(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (arrayType.getPrimitiveClass() == Long.class) {
      long[] pa = (long[]) arr;
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readLong(pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (arrayType == ArrayType.STRUCTURE) {
      byte[] pa = (byte[]) arr;
      int recsize = layout.getElemSize();
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        raf.order(byteOrder);
        raf.seek(chunk.getSrcPos());
        raf.readFully(pa, (int) chunk.getDestElem() * recsize, chunk.getNelems() * recsize);
      }
      return pa;

    } else if (arrayType == ArrayType.STRING) {
      int size = (int) layout.getTotalNelems();
      int elemSize = layout.getElemSize();
      StringBuilder sb = new StringBuilder(size);
      while (layout.hasNext()) {
        Layout.Chunk chunk = layout.next();
        if (chunk == null) {
          continue;
        }
        for (int i = 0; i < chunk.getNelems(); i++) {
          sb.append(raf.readString(elemSize));
        }
      }
      return sb.toString();
    }

    throw new IllegalStateException("unknown type= " + arrayType);
  }

  /**
   * Read data subset from PositioningDataInputStream, create primitive array of size Layout.getTotalNelems.
   * Reading is controlled by the Layout object.
   *
   * @param is read from here.
   * @param index handles skipping around in the file.
   * @param ArrayType ArrayType of the variable
   * @param fillValue must be Number if ArrayType.isNumeric(), or String for STRING, byte[] for Structure, or null for
   *        none
   * @return primitive array with data read in
   * @throws IOException on read error
   */
  public static Object readDataFill(PositioningDataInputStream is, Layout index, ArrayType ArrayType, Object fillValue)
      throws IOException {
    Object arr = (fillValue == null) ? makePrimitiveArray((int) index.getTotalNelems(), ArrayType)
        : makePrimitiveArray((int) index.getTotalNelems(), ArrayType, fillValue);
    return readData(is, index, ArrayType, arr);
  }

  /**
   * Read data subset from PositioningDataInputStream, place in given primitive array.
   * Reading is controlled by the Layout object.
   *
   * @param raf read from here.
   * @param index handles skipping around in the file.
   * @param arrayType ArrayType of the variable
   * @param arr primitive array to read data into
   * @return primitive array with data read in
   * @throws IOException on read error
   */
  public static Object readData(PositioningDataInputStream raf, Layout index, ArrayType arrayType, Object arr)
      throws IOException {
    if (showLayoutTypes)
      System.out.println("***PositioningDataInputStream LayoutType=" + index.getClass().getName());

    if (arrayType.getPrimitiveClass() == Byte.class || arrayType == ArrayType.CHAR) {
      byte[] pa = (byte[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.read(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (arrayType.getPrimitiveClass() == Short.class) {
      short[] pa = (short[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readShort(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (arrayType.getPrimitiveClass() == Integer.class) {
      int[] pa = (int[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readInt(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (arrayType == ArrayType.FLOAT) {
      float[] pa = (float[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readFloat(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (arrayType == ArrayType.DOUBLE) {
      double[] pa = (double[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readDouble(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (arrayType.getPrimitiveClass() == Long.class) {
      long[] pa = (long[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.readLong(chunk.getSrcPos(), pa, (int) chunk.getDestElem(), chunk.getNelems());
      }
      return pa;

    } else if (arrayType == ArrayType.STRUCTURE) {
      int recsize = index.getElemSize();
      byte[] pa = (byte[]) arr;
      while (index.hasNext()) {
        Layout.Chunk chunk = index.next();
        raf.read(chunk.getSrcPos(), pa, (int) chunk.getDestElem() * recsize, chunk.getNelems() * recsize);
      }
      return pa;
    }

    throw new IllegalStateException();
  } //

  /**
   * Read data subset from ByteBuffer, create primitive array of size Layout.getTotalNelems.
   * Reading is controlled by the Layout object.
   *
   * @param layout handles skipping around in the file, provide ByteBuffer to read from
   * @param arrayType ArrayType of the variable
   * @param fillValue must be Number if ArrayType.isNumeric(), or String for STRING, byte[] for Structure, or null for
   *        none
   * @return primitive array with data read in
   */
  public static Object readDataFill(LayoutBB layout, ArrayType arrayType, Object fillValue) {
    long size = layout.getTotalNelems();
    if (arrayType == ArrayType.STRUCTURE) {
      size *= layout.getElemSize();
    }
    if (size >= Integer.MAX_VALUE) {
      throw new RuntimeException("Read request too large");
    }
    Object arr = (fillValue == null) ? makePrimitiveArray((int) size, arrayType)
        : makePrimitiveArray((int) size, arrayType, fillValue);
    return readData(layout, arrayType, arr);
  }

  /**
   * Read data subset from ByteBuffer, place in given primitive array.
   * Reading is controlled by the LayoutBB object.
   *
   * @param layout handles skipping around in the file, privide ByteBuffer to read from
   * @param arrayType ArrayType of the variable
   * @param arr primitive array to read data into
   * @return the primitive array with data read in
   */
  public static Object readData(LayoutBB layout, ArrayType arrayType, Object arr) {
    if (showLayoutTypes)
      System.out.println("***BB LayoutType=" + layout.getClass().getName());

    if (arrayType.getPrimitiveClass() == Byte.class || (arrayType == ArrayType.CHAR)) {
      byte[] pa = (byte[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        ByteBuffer bb = chunk.getByteBuffer();
        bb.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = bb.get();
      }
      return pa;

    } else if (arrayType.getPrimitiveClass() == Short.class) {
      short[] pa = (short[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        ShortBuffer buff = chunk.getShortBuffer();
        buff.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (arrayType.getPrimitiveClass() == Integer.class) {
      int[] pa = (int[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        IntBuffer buff = chunk.getIntBuffer();
        buff.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (arrayType == ArrayType.FLOAT) {
      float[] pa = (float[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        FloatBuffer buff = chunk.getFloatBuffer();
        buff.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (arrayType == ArrayType.DOUBLE) {
      double[] pa = (double[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        DoubleBuffer buff = chunk.getDoubleBuffer();
        buff.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (arrayType.getPrimitiveClass() == Long.class) {
      long[] pa = (long[]) arr;
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        LongBuffer buff = chunk.getLongBuffer();
        buff.position(chunk.getSrcElem());
        int pos = (int) chunk.getDestElem();
        for (int i = 0; i < chunk.getNelems(); i++)
          pa[pos++] = buff.get();
      }
      return pa;

    } else if (arrayType == ArrayType.STRUCTURE) {
      byte[] pa = (byte[]) arr;
      int recsize = layout.getElemSize();
      while (layout.hasNext()) {
        LayoutBB.Chunk chunk = layout.next();
        ByteBuffer bb = chunk.getByteBuffer();
        bb.position(chunk.getSrcElem() * recsize);
        int pos = (int) chunk.getDestElem() * recsize;
        for (int i = 0; i < chunk.getNelems() * recsize; i++)
          pa[pos++] = bb.get();
      }
      return pa;
    }

    throw new IllegalStateException();
  }

  /**
   * Create 1D primitive array of the given size and type
   *
   * @param size the size of the array to create
   * @param arrayType ArrayType of the variable
   * @return primitive array with all zeroes
   */
  public static Object makePrimitiveArray(int size, ArrayType arrayType) {
    Object arr = null;

    if ((arrayType.getPrimitiveClass() == Byte.class) || (arrayType == ArrayType.CHAR)
        || (arrayType == ArrayType.OPAQUE) || (arrayType == ArrayType.STRUCTURE)) {
      arr = new byte[size];

    } else if (arrayType.getPrimitiveClass() == Short.class) {
      arr = new short[size];

    } else if (arrayType.getPrimitiveClass() == Integer.class) {
      arr = new int[size];

    } else if (arrayType.getPrimitiveClass() == Long.class) {
      arr = new long[size];

    } else if (arrayType == ArrayType.FLOAT) {
      arr = new float[size];

    } else if (arrayType == ArrayType.DOUBLE) {
      arr = new double[size];

    } else if (arrayType == ArrayType.STRING) {
      arr = new String[size];
    }

    return arr;
  }


  /**
   * Create 1D primitive array of the given size and type, fill it with the given value
   *
   * @param size the size of the array to create
   * @param arrayType ArrayType of the variable
   * @param fillValue must be Number if ArrayType.isNumeric(), or String for STRING, byte[] for Structure, or null for
   *        none
   * @return primitive array with data read in
   */
  public static Object makePrimitiveArray(int size, ArrayType arrayType, Object fillValue) {

    if (arrayType.getPrimitiveClass() == Byte.class || (arrayType == ArrayType.CHAR)) {
      byte[] pa = new byte[size];
      byte val = ((Number) fillValue).byteValue();
      if (val != 0) {
        for (int i = 0; i < size; i++) {
          pa[i] = val;
        }
      }
      // if (ArrayType == ArrayType.CHAR) return convertByteToChar(pa);
      return pa;

    } else if (arrayType == ArrayType.OPAQUE) {
      return new byte[size];

    } else if (arrayType.getPrimitiveClass() == Short.class) {
      short[] pa = new short[size];
      short val = ((Number) fillValue).shortValue();
      if (val != 0) {
        for (int i = 0; i < size; i++) {
          pa[i] = val;
        }
      }
      return pa;

    } else if (arrayType.getPrimitiveClass() == Integer.class) {
      int[] pa = new int[size];
      int val = ((Number) fillValue).intValue();
      if (val != 0) {
        for (int i = 0; i < size; i++) {
          pa[i] = val;
        }
      }
      return pa;

    } else if (arrayType.getPrimitiveClass() == Long.class) {
      long[] pa = new long[size];
      long val = ((Number) fillValue).longValue();
      if (val != 0) {
        for (int i = 0; i < size; i++) {
          pa[i] = val;
        }
      }
      return pa;

    } else if (arrayType == ArrayType.FLOAT) {
      float[] pa = new float[size];
      float val = ((Number) fillValue).floatValue();
      if (val != 0.0) {
        for (int i = 0; i < size; i++) {
          pa[i] = val;
        }
      }
      return pa;

    } else if (arrayType == ArrayType.DOUBLE) {
      double[] pa = new double[size];
      double val = ((Number) fillValue).doubleValue();
      if (val != 0.0) {
        for (int i = 0; i < size; i++) {
          pa[i] = val;
        }
      }
      return pa;

    } else if (arrayType == ArrayType.STRING) {
      String[] pa = new String[size];
      for (int i = 0; i < size; i++) {
        pa[i] = (String) fillValue;
      }
      return pa;

    } else if (arrayType == ArrayType.STRUCTURE) {
      byte[] pa = new byte[size];
      if (fillValue != null) {
        byte[] val = (byte[]) fillValue;
        int count = 0;
        while (count < size && count < val.length) {
          for (byte aVal : val) {
            pa[count++] = aVal;
          }
        }
      }
      return pa;
    }

    throw new IllegalStateException();
  }
}
