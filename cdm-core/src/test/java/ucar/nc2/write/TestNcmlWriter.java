package ucar.nc2.write;

import org.jdom2.Element;
import org.junit.Test;
import ucar.array.ArrayType;
import ucar.nc2.Variable;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.TestUtils.makeDummyGroup;

/** Test {@link NcmlWriter} */
public class TestNcmlWriter {

  @Test
  public void testMakeValuesElementFloat() throws IOException {
    Variable var = Variable.builder().setName("name").setArrayType(ArrayType.FLOAT)
        .setDimensionsAnonymous(new int[] {3}).setAutoGen(3.2, 2).build(makeDummyGroup());

    ucar.array.Array<?> data = var.readArray();
    System.out.printf("data = %s%n", data);

    NcmlWriter writer = new NcmlWriter();
    Element elem = writer.makeValuesElement(var, false);
    String ncml = writer.writeToString(elem);

    System.out.printf("ncml = %s%n", ncml);
    String expected =
        "<values xmlns=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\">3.200000 5.200000 7.200000</values>\n";
    assertThat(ncml).endsWith(expected);
  }

  @Test
  public void testMakeValuesElementDouble() throws IOException {
    Variable var = Variable.builder().setName("name").setArrayType(ArrayType.DOUBLE)
        .setDimensionsAnonymous(new int[] {3}).setAutoGen(3.2, 2).build(makeDummyGroup());

    ucar.array.Array<?> data = var.readArray();
    System.out.printf("data = %s%n", data);

    NcmlWriter writer = new NcmlWriter();
    Element elem = writer.makeValuesElement(var, false);
    String ncml = writer.writeToString(elem);

    System.out.printf("ncml = %s%n", ncml);
    String expected =
        "<values xmlns=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\">3.200000 5.200000 7.200000</values>\n";
    assertThat(ncml).endsWith(expected);
  }

  @Test
  public void testMakeValuesElementInteger() throws IOException {
    Variable var = Variable.builder().setName("name").setArrayType(ArrayType.INT).setDimensionsAnonymous(new int[] {3})
        .setAutoGen(3, 22).build(makeDummyGroup());

    ucar.array.Array<?> data = var.readArray();
    System.out.printf("data = %s%n", data);

    NcmlWriter writer = new NcmlWriter();
    Element elem = writer.makeValuesElement(var, false);
    String ncml = writer.writeToString(elem);

    System.out.printf("ncml = %s%n", ncml);
    String expected = "<values xmlns=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\">3 25 47</values>\n";
    assertThat(ncml).endsWith(expected);
  }

  @Test
  public void testMakeValuesElementUInteger() throws IOException {
    Variable var = Variable.builder().setName("name").setArrayType(ArrayType.UINT).setDimensionsAnonymous(new int[] {3})
        .setAutoGen(-33, 22).build(makeDummyGroup());

    ucar.array.Array<?> data = var.readArray();
    System.out.printf("data = %s%n", data);

    NcmlWriter writer = new NcmlWriter();
    Element elem = writer.makeValuesElement(var, false);
    String ncml = writer.writeToString(elem);

    System.out.printf("ncml = %s%n", ncml);
    String expected = "<values xmlns=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\">-33 -11 11</values>\n";
    assertThat(ncml).endsWith(expected);
  }

  @Test
  public void testMakeValuesElementAllowRegular() throws IOException {
    Variable var = Variable.builder().setName("name").setArrayType(ArrayType.DOUBLE)
        .setDimensionsAnonymous(new int[] {3}).setAutoGen(3.2, 2).build(makeDummyGroup());

    ucar.array.Array<?> data = var.readArray();
    System.out.printf("data = %s%n", data);

    NcmlWriter writer = new NcmlWriter();
    Element elem = writer.makeValuesElement(var, true);
    String ncml = writer.writeToString(elem);

    System.out.printf("ncml = %s%n", ncml);
    String expected =
        "<values xmlns=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\" start=\"3.200000\" increment=\"2.000000\" npts=\"3\" />\n";
    assertThat(ncml).endsWith(expected);
  }

}
