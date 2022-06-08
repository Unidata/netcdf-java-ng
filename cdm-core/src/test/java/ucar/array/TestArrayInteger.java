/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

/** Test {@link ArrayInteger} */
public class TestArrayInteger {

  @Test
  public void testBasics() {
    int[] shape = new int[] {1, 2, 3};
    int[] parray = new int[] {1, 2, 3, 4, 5, 6};
    Storage<Integer> store = new ArrayInteger.StorageS(parray);
    ArrayInteger array = new ArrayInteger(ArrayType.INT, shape, store);

    assertThat(array.get(0, 0, 0)).isEqualTo(1);
    assertThat(array.get(0, 0, 1)).isEqualTo(2);
    assertThat(array.get(0, 0, 2)).isEqualTo(3);
    assertThat(array.get(0, 1, 0)).isEqualTo(4);
    assertThat(array.get(0, 1, 1)).isEqualTo(5);
    assertThat(array.get(0, 1, 2)).isEqualTo(6);

    int count = 0;
    for (int val : array) {
      assertThat(val).isEqualTo(count + 1);
      count++;
    }

    assertThrows(IllegalArgumentException.class, () -> array.get(0, 2, 2));
    assertThrows(IllegalArgumentException.class, () -> array.get(0, 1));

    int[] result = new int[3];
    array.arraycopy(1, result, 0, 3);
    assertThat(result).isEqualTo(new int[] {2, 3, 4});

    assertThat(array.storage()).isEqualTo(store);
    assertThat(Arrays.copyPrimitiveArray(array)).isEqualTo(parray);
  }

  @Test
  public void testNonCanonicalOrder() {
    int[] shape = new int[] {1, 2, 3};
    Storage<Integer> store = new ArrayInteger.StorageS(new int[] {1, 2, 3, 4, 5, 6});
    Array<Integer> array = new ArrayInteger(ArrayType.INT, shape, store);
    array = Arrays.flip(array, 1);
    int[] expected = new int[] {4, 5, 6, 1, 2, 3};
    int count = 0;
    for (int val : array) {
      assertThat(val).isEqualTo(expected[count]);
      count++;
    }

    int[] result = new int[3];
    array.arraycopy(1, result, 0, 3);
    assertThat(result).isEqualTo(new int[] {5, 6, 1});
  }

  @Test
  public void testCombine() {
    int[] shape1 = new int[] {1, 2, 3};
    Array<Integer> array1 = Arrays.factory(ArrayType.INT, shape1, new int[] {1, 2, 3, 4, 5, 6});
    Array<Integer> array2 = Arrays.factory(ArrayType.INT, shape1, new int[] {7, 8, 9, 10, 11, 12});

    int[] shape = new int[] {2, 2, 3};
    Array<Integer> array = Arrays.combine(ArrayType.INT, shape, ImmutableList.of(array1, array2));

    assertThat(array.get(0, 0, 0)).isEqualTo(1);
    assertThat(array.get(0, 0, 1)).isEqualTo(2);
    assertThat(array.get(0, 0, 2)).isEqualTo(3);
    assertThat(array.get(0, 1, 0)).isEqualTo(4);
    assertThat(array.get(0, 1, 1)).isEqualTo(5);
    assertThat(array.get(0, 1, 2)).isEqualTo(6);
    assertThat(array.get(1, 0, 0)).isEqualTo(7);
    assertThat(array.get(1, 0, 1)).isEqualTo(8);
    assertThat(array.get(1, 0, 2)).isEqualTo(9);
    assertThat(array.get(1, 1, 0)).isEqualTo(10);
    assertThat(array.get(1, 1, 1)).isEqualTo(11);
    assertThat(array.get(1, 1, 2)).isEqualTo(12);

    int count = 0;
    for (int val : array) {
      assertThat(val).isEqualTo(count + 1);
      count++;
    }
  }

  @Test
  public void testMisc() {
    int[] shape1 = new int[] {1, 2, 3};
    Array<Integer> array = Arrays.factoryFill(ArrayType.INT, shape1, 2);
    Index index = array.getIndex();
    assertThat(array.get(index.set(0, 1, 2))).isEqualTo(2);
  }

  @Test
  public void testFactoryFill() {
    int[] shape = new int[] {1, 2, 3};
    Array<Integer> array = Arrays.factoryFill(ArrayType.INT, shape, -9);

    assertThat(array.getSize()).isEqualTo(Arrays.computeSize(shape));
    assertThat(array.getShape()).isEqualTo(shape);

    for (int val : array) {
      assertThat(val).isEqualTo(-9);
    }
  }

  @Test
  public void testMakeArray() {
    Array<Integer> array = Arrays.makeArray(ArrayType.INT, 1000, 0.0, 1, 10, 10, 10);
    int count = 0;
    for (int val : array) {
      assertThat(val).isEqualTo(count);
      count++;
    }
  }

}
