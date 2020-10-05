/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import ucar.array.StructureMembers.Member;
import ucar.ma2.DataType;
import ucar.nc2.iosp.IospHelper;

/**
 * Storage for Array<StructureData> with all data in a single ByteBuffer, member offsets and ByteOrder,
 * and a heap for vlen data such as Strings, Vlens, and Sequences. Mimics ArrayStructureBB.
 * The StructureData are manufactured on the fly, referencing the ByteBuffer and heap for data.
 */
public final class StructureDataStorageBB implements Storage<StructureData> {
  private final StructureMembers members;
  private final ByteBuffer bbuffer;
  private final int nelems;
  private final int offset;
  private final ArrayList<Object> heap = new ArrayList<>();

  private boolean structuresOnHeap = false;

  public StructureDataStorageBB(StructureMembers members, ByteBuffer bbuffer, int nelems) {
    this.members = members;
    this.bbuffer = bbuffer;
    this.nelems = nelems;
    this.offset = 0;
  }

  StructureDataStorageBB(StructureMembers members, ByteBuffer bbuffer, int nelems, int offset) {
    this.members = members;
    this.bbuffer = bbuffer;
    this.nelems = nelems;
    this.offset = offset;
  }

  public StructureDataStorageBB setStructuresOnHeap(boolean structuresOnHeap) {
    this.structuresOnHeap = structuresOnHeap;
    return this;
  }

  /** Put the object on the heap, return heap index. */
  public int putOnHeap(Object s) {
    heap.add(s);
    return heap.size() - 1;
  }

  @Override
  public long getLength() {
    return nelems;
  }

  @Override
  public StructureData get(long elem) {
    return new StructureDataBB((int) elem);
  }

  @Override
  public void arraycopy(int srcPos, Object dest, int destPos, long length) {
    // TODO
  }

  public int getStructureSize() {
    return members.getStorageSizeBytes();
  }

  // TODO go away in version 7 I hope
  ByteBuffer buffer() {
    return bbuffer;
  }

  /** Copy Array data into ByteBuffer. Not sure if this is useful. */
  public void setMemberData(int row, Member member, Array<?> data) {
    setMemberData(0, members.getStorageSizeBytes(), row, member, data);
  }

  private void setMemberData(int offset, int structSize, int row, Member member, Array<?> data) {
    int pos = offset + row * structSize + member.getOffset();
    bbuffer.position(pos);
    if (member.isVariableLength()) {
      // LOOK not making a copy
      int index = this.putOnHeap(data);
      bbuffer.putInt(index);
      return;
    }

    DataType dataType = data.getDataType();
    switch (dataType) {
      case ENUM1:
      case UBYTE:
      case BYTE: {
        Array<Byte> bdata = (Array<Byte>) data;
        for (byte val : bdata) {
          bbuffer.put(val);
        }
        return;
      }
      case OPAQUE: {
        int index = this.putOnHeap(data);
        bbuffer.putInt(index);
        return;
      }
      case CHAR: {
        Array<Character> cdata = (Array<Character>) data;
        for (char val : cdata) {
          bbuffer.put((byte) val);
        }
        return;
      }
      case ENUM2:
      case USHORT:
      case SHORT: {
        Array<Short> sdata = (Array<Short>) data;
        for (short val : sdata) {
          bbuffer.putShort(val);
        }
        return;
      }
      case ENUM4:
      case UINT:
      case INT: {
        Array<Integer> idata = (Array<Integer>) data;
        for (int val : idata) {
          bbuffer.putInt(val);
        }
        return;
      }
      case ULONG:
      case LONG: {
        Array<Long> ldata = (Array<Long>) data;
        for (long val : ldata) {
          bbuffer.putLong(val);
        }
        return;
      }
      case FLOAT: {
        Array<Float> fdata = (Array<Float>) data;
        for (float val : fdata) {
          bbuffer.putFloat(val);
        }
        return;
      }
      case DOUBLE: {
        Array<Double> ddata = (Array<Double>) data;
        for (double val : ddata) {
          bbuffer.putDouble(val);
        }
        return;
      }
      case STRING: {
        // LOOK could put Array<Sting> onto the heap
        String[] vals = new String[(int) data.length()];
        Array<String> sdata = (Array<String>) data;
        int idx = 0;
        for (String val : sdata) {
          vals[idx++] = val;
        }
        int index = this.putOnHeap(vals);
        bbuffer.putInt(index);
        return;
      }
      case STRUCTURE: {
        Preconditions.checkArgument(member.getStructureMembers() != null);
        StructureDataArray orgArray = (StructureDataArray) data;
        StructureMembers nestedMembers = orgArray.getStructureMembers();
        int length = (int) orgArray.length();
        for (int nrow = 0; nrow < length; nrow++) {
          StructureData orgData = orgArray.get(nrow);
          for (StructureMembers.Member nmember : nestedMembers) {
            setMemberData(pos, nestedMembers.getStorageSizeBytes(), nrow, nmember, orgData.getMemberData(nmember));
          }
        }
        return;
      }
      default:
        throw new IllegalStateException("Unkown datatype " + dataType);
    }
  }

  @Override
  public Iterator<StructureData> iterator() {
    return new Iter();
  }

  private final class Iter implements Iterator<StructureData> {
    private int count = 0;

    @Override
    public final boolean hasNext() {
      return count < nelems;
    }

    @Override
    public final StructureData next() {
      return new StructureDataBB(count++);
    }
  }

  private final class StructureDataBB extends StructureData {
    private final int recno;

    private StructureDataBB(int recno) {
      super(StructureDataStorageBB.this.members);
      this.recno = recno;
    }

    @Override
    public Array<?> getMemberData(Member m) {
      if (m.isVariableLength()) {
        return getMemberVlenData(m);
      }

      DataType dataType = m.getDataType();
      bbuffer.order(m.getByteOrder());
      int length = m.length();
      int pos = offset + recno * members.getStorageSizeBytes() + m.getOffset();

      switch (dataType) {
        case BOOLEAN:
        case UBYTE:
        case ENUM1:
        case BYTE: {
          byte[] array = new byte[length];
          for (int count = 0; count < length; count++) {
            array[count] = bbuffer.get(pos + count);
          }
          return new ArrayByte(dataType, m.getShape(), new ucar.array.ArrayByte.StorageS(array));
        }

        case CHAR: {
          // char is stored in a single byte
          byte[] array = new byte[length];
          for (int count = 0; count < length; count++) {
            array[count] = bbuffer.get(pos + count);
          }
          return new ArrayChar(m.getShape(), new ucar.array.ArrayChar.StorageS(IospHelper.convertByteToChar(array)));
        }

        case DOUBLE: {
          double[] darray = new double[length];
          for (int count = 0; count < length; count++) {
            darray[count] = bbuffer.getDouble(pos + 8 * count);
          }
          return new ArrayDouble(m.getShape(), new ucar.array.ArrayDouble.StorageD(darray));
        }

        case FLOAT: {
          float[] farray = new float[length];
          for (int count = 0; count < length; count++) {
            farray[count] = bbuffer.getFloat(pos + 4 * count);
          }
          return new ArrayFloat(m.getShape(), new ucar.array.ArrayFloat.StorageF(farray));
        }

        case UINT:
        case ENUM4:
        case INT: {
          int[] array = new int[length];
          for (int count = 0; count < length; count++) {
            array[count] = bbuffer.getInt(pos + 4 * count);
          }
          return new ArrayInteger(dataType, m.getShape(), new ucar.array.ArrayInteger.StorageS(array));
        }

        case ULONG:
        case LONG: {
          long[] array = new long[length];
          for (int count = 0; count < length; count++) {
            array[count] = bbuffer.getLong(pos + 8 * count);
          }
          return new ArrayLong(dataType, m.getShape(), new ucar.array.ArrayLong.StorageS(array));
        }

        case USHORT:
        case ENUM2:
        case SHORT: {
          short[] array = new short[length];
          for (int count = 0; count < length; count++) {
            array[count] = bbuffer.getShort(pos + 2 * count);
          }
          return new ArrayShort(dataType, m.getShape(), new ucar.array.ArrayShort.StorageS(array));
        }

        case STRING: {
          int heapIdx = bbuffer.getInt(pos);
          String[] array = (String[]) heap.get(heapIdx);
          return new ArrayString(m.getShape(), new ucar.array.ArrayString.StorageS(array));
        }

        case SEQUENCE: {
          int heapIdx = bbuffer.getInt(pos);
          // System.out.printf("getMemberData get seq %s at heap = %d pos = %d%n", m.getName(), heapIdx, pos);
          return (StructureDataArray) heap.get(heapIdx);
        }

        case STRUCTURE:
          if (structuresOnHeap) {
            int heapIdx = bbuffer.getInt(pos);
            StructureDataArray structArray = (StructureDataArray) heap.get(heapIdx);
            return structArray;
          } else {
            StructureMembers nestedMembers = Preconditions.checkNotNull(m.getStructureMembers());
            // System.out.printf("getMemberData get struct %s at offset = %d%n", m.getName(), pos);
            Storage<StructureData> nestedStorage =
                new ucar.array.StructureDataStorageBB(nestedMembers, bbuffer, length, pos);
            return new StructureDataArray(nestedMembers, m.getShape(), nestedStorage);
          }

        default:
          throw new RuntimeException("unknown dataType " + dataType);
      }
    }

    private Array<?> getMemberVlenData(Member m) {
      bbuffer.order(m.getByteOrder());
      int pos = offset + recno * members.getStorageSizeBytes() + m.getOffset();

      int heapIdx = bbuffer.getInt(pos);
      ArrayVlen<?> vlenArray = (ArrayVlen<?>) heap.get(heapIdx);

      return vlenArray.length() == 1 ? vlenArray.get(vlenArray.getIndex()) : vlenArray;
    }

  }
}
