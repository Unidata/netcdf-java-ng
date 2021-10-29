/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import java.io.IOException;
import java.util.Iterator;
import javax.annotation.concurrent.Immutable;

import ucar.array.ArrayType;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.array.StructureData;

/**
 * A one-dimensional Structure with indeterminate length, possibly 0.
 * The only data access is through getStructureIterator().
 */
@Immutable
public class Sequence extends Structure implements Iterable<ucar.array.StructureData> {

  /** An iterator over all the data in the sequence. */
  @Override
  public Iterator<ucar.array.StructureData> iterator() {
    if (cache.getData() != null) {
      ucar.array.Array<?> array = cache.getData();
      if (array instanceof ucar.array.StructureDataArray) {
        return (Iterator<ucar.array.StructureData>) array;
      }
    }
    try {
      return ncfile.getSequenceIterator(this, -1);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** @throws UnsupportedOperationException always */
  @Override
  public Variable section(Section subsection) throws InvalidRangeException {
    throw new UnsupportedOperationException();
  }

  /** @throws UnsupportedOperationException always */
  @Override
  public Variable slice(int dim, int value) {
    throw new UnsupportedOperationException();
  }

  /** @throws UnsupportedOperationException always */
  @Override
  public StructureData readRecord(int recno) throws IOException, InvalidRangeException {
    throw new UnsupportedOperationException();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  protected Sequence(Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  /**
   * Get Builder for this class that allows subclassing.
   * 
   * @see "https://community.oracle.com/blogs/emcmanus/2010/10/24/using-builder-pattern-subclasses"
   */
  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends Structure.Builder<T> {
    private boolean built;

    protected abstract T self();

    public Sequence build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      // LOOK mutable!
      this.setArrayType(ArrayType.SEQUENCE);
      return new Sequence(this, parentGroup);
    }
  }

}
