/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.util;

import javax.annotation.Nullable;

import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.ArrayType;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.*;
import ucar.nc2.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;

import ucar.nc2.internal.iosp.hdf5.H5header;
import ucar.nc2.iosp.NetcdfFormatUtils;
import ucar.unidata.geoloc.Projection;

/**
 * Compare two NetcdfFile.
 * Doesnt fail (eg doesnt use assert), places results in Formatter.
 */
public class CompareNetcdf2 {
  public static final ObjFilter IDENTITY_FILTER = new ObjFilter() {};

  public interface ObjFilter {
    // if true, compare attribute, else skip comparision. Variable may be null.
    default boolean attCheckOk(Attribute att) {
      return true;
    }

    // override attribute comparision if needed
    default boolean attsAreEqual(Attribute att1, Attribute att2) {
      try {
        return att1.equals(att2);
      } catch (Exception e) {
        System.out.printf("att1 = %s != att2 = %s%n", att1, att2);
        return false;
      }
    }

    // override dimension comparision if needed
    default boolean dimsAreEqual(Dimension dim1, Dimension dim2) {
      return dim1.equals(dim2);
    }

    // override dimension comparision if needed
    default boolean enumsAreEqual(EnumTypedef enum1, EnumTypedef enum2) {
      return enum1.equals(enum2);
    }

    // if true, compare variable, else skip comparision
    default boolean varDataTypeCheckOk(Variable v) {
      return true;
    }

    // if true, compare dimension, else skip comparision
    default boolean checkDimensionsForFile(String filename) {
      return true;
    }

    // if true, compare transform, else skip comparision
    default boolean compareProjection(Projection ct1, Projection ct2) {
      if ((ct1 == null) != (ct2 == null)) {
        return false;
      }
      if (ct1 == null) {
        return true;
      }
      return ct1.equals(ct2);
    }
  }

  public static class Netcdf4ObjectFilter implements ObjFilter {

    public boolean attCheckOk(Attribute att) {
      // if (v != null && v.isMemberOfStructure()) return false;
      String name = att.getShortName();

      if (name.equals(H5header.HDF5_DIMENSION_LIST))
        return false;
      if (name.equals(H5header.HDF5_DIMENSION_SCALE))
        return false;
      if (name.equals(H5header.HDF5_DIMENSION_LABELS))
        return false;

      // added by cdm
      if (name.equals(CDM.CHUNK_SIZES))
        return false;
      if (name.equals(CDM.FILL_VALUE))
        return false;
      if (name.equals("_lastModified"))
        return false;

      if (CDM.NETCDF4_SPECIAL_ATTS.contains(name))
        return false;

      // hidden by nc4
      if (name.equals(NetcdfFormatUtils.NETCDF4_DIMID))
        return false; // preserve the order of the dimensions
      if (name.equals(NetcdfFormatUtils.NETCDF4_COORDINATES))
        return false; // ??
      if (name.equals(NetcdfFormatUtils.NETCDF4_STRICT))
        return false;

      return !name.startsWith("_");

      // not implemented yet
      // if (att.getDataType().isEnum()) return false;
    }

    @Override
    public boolean varDataTypeCheckOk(Variable v) {
      if (v.getArrayType() == ArrayType.CHAR)
        return false; // temp workaround
      return v.getArrayType() != ArrayType.STRING;
    }

    // override att comparision if needed
    public boolean attsAreEqual(Attribute att1, Attribute att2) {
      if (att1.getShortName().equalsIgnoreCase(CDM.UNITS) && att2.getShortName().equalsIgnoreCase(CDM.UNITS)) {
        return att1.getStringValue().trim().equals(att2.getStringValue().trim());
      }
      return att1.equals(att2);
    }

    // override dimension comparision if needed
    public boolean dimsAreEqual(Dimension dim1, Dimension dim2) {
      boolean unshared1 = dim1.getShortName() != null && (dim1.getShortName().contains("_Dim"));
      boolean unshared2 = dim2.getShortName() != null && (dim2.getShortName().contains("_Dim"));
      if (unshared1 || unshared2) { // only test length
        return dim1.getLength() == dim2.getLength();
      }
      return dim1.equals(dim2);
    }

    public boolean enumsAreEqual(EnumTypedef enum1, EnumTypedef enum2) {
      String name1 = enum1.getShortName();
      String name2 = enum2.getShortName();
      if (name1.endsWith("_t")) {
        name1 = name1.substring(0, name1.length() - 2);
      }
      if (name2.endsWith("_t")) {
        name2 = name2.substring(0, name2.length() - 2);
      }
      return Objects.equals(name1, name2) && Objects.equals(enum1.getMap(), enum2.getMap())
          && enum1.getBaseArrayType() == enum2.getBaseArrayType();
    }

  }

  public static boolean compareData(String name, Array<?> data1, Array<?> data2) {
    return new CompareNetcdf2().compareData(name, data1, data2, false);
  }

  public static boolean compareData(String name, Array<?> data1, double[] data2) throws IOException {
    Array<?> data2a = Arrays.factory(ArrayType.DOUBLE, new int[] {data2.length}, data2);
    return compareData(name, data1, data2a);
  }

  public static boolean compareFiles(NetcdfFile org, NetcdfFile copy, Formatter f) {
    return compareFiles(org, copy, f, false, false, false);
  }

  public static boolean compareFiles(NetcdfFile org, NetcdfFile copy, Formatter f, boolean _compareData,
      boolean _showCompare, boolean _showEach) {
    CompareNetcdf2 tc = new CompareNetcdf2(f, _showCompare, _showEach, _compareData);
    return tc.compare(org, copy);
  }

  public static boolean compareLists(List org, List copy, Formatter f) {
    boolean ok1 = checkContains("first", org, copy, f);
    boolean ok2 = checkContains("second", copy, org, f);
    return ok1 && ok2;
  }

  public static boolean checkContains(String what, List<Object> container, List<Object> wantList, Formatter f) {
    boolean ok = true;

    for (Object want1 : wantList) {
      int index2 = container.indexOf(want1);
      if (index2 < 0) {
        f.format("  ** %s missing in %s %n", want1, what);
        ok = false;
      }
    }

    return ok;
  }

  /////////

  private final Formatter f;
  private boolean showCompare;
  private boolean showEach;
  private boolean compareData;

  public CompareNetcdf2() {
    this(new Formatter(System.out));
  }

  public CompareNetcdf2(Formatter f) {
    this(f, false, false, System.getProperty("allTests") != null);
  }

  public CompareNetcdf2(Formatter f, boolean showCompare, boolean showEach, boolean compareData) {
    this.f = f;
    this.compareData = compareData;
    this.showCompare = showCompare;
    this.showEach = showEach;
  }

  public boolean compare(NetcdfFile org, NetcdfFile copy) {
    return compare(org, copy, showCompare, showEach, compareData);
  }

  public boolean compare(NetcdfFile org, NetcdfFile copy, @Nullable ObjFilter filter) {
    return compare(org, copy, filter, showCompare, showEach, compareData);
  }

  private boolean compare(NetcdfFile org, NetcdfFile copy, boolean showCompare, boolean showEach, boolean compareData) {
    return compare(org, copy, null, showCompare, showEach, compareData);
  }

  private boolean compare(NetcdfFile org, NetcdfFile copy, @Nullable ObjFilter objFilter, boolean showCompare,
      boolean showEach, boolean compareData) {
    if (objFilter == null)
      objFilter = IDENTITY_FILTER;
    this.compareData = compareData;
    this.showCompare = showCompare;
    this.showEach = showEach;

    f.format(" First file = %s%n", org.getLocation());
    f.format(" Second file= %s%n", copy.getLocation());

    long start = System.currentTimeMillis();

    boolean ok = compareGroups(org.getRootGroup(), copy.getRootGroup(), objFilter);
    f.format(" Files are the same = %s%n", ok);

    long took = System.currentTimeMillis() - start;
    f.format(" Time to compare = %d msecs%n", took);

    // coordinate systems
    if (org instanceof NetcdfDataset && copy instanceof NetcdfDataset) {
      NetcdfDataset orgds = (NetcdfDataset) org;
      NetcdfDataset copyds = (NetcdfDataset) copy;

      // coordinate systems
      for (CoordinateSystem cs1 : orgds.getCoordinateSystems()) {
        CoordinateSystem cs2 = copyds.getCoordinateSystems().stream().filter(cs -> cs.getName().equals(cs1.getName()))
            .findFirst().orElse(null);
        if (cs2 == null) {
          ok = false;
          f.format("  ** Cant find CoordinateSystem '%s' in file2 %n", cs1.getName());
        } else {
          ok &= compareCoordinateSystem(cs1, cs2, objFilter);
        }
      }
    }

    return ok;
  }

  public boolean compareVariables(NetcdfFile org, NetcdfFile copy) {
    f.format("Original = %s%n", org.getLocation());
    f.format("CompareTo= %s%n", copy.getLocation());
    boolean ok = true;

    for (Variable orgV : org.getVariables()) {
      // if (orgV.isCoordinateVariable()) continue;

      Variable copyVar = copy.findVariable(orgV.getShortName());
      if (copyVar == null) {
        f.format(" MISSING '%s' in 2nd file%n", orgV.getFullName());
        ok = false;
      } else {
        ok &= compareVariables(orgV, copyVar, null, compareData, true);
      }
    }

    f.format("%n");
    for (Variable orgV : copy.getVariables()) {
      // if (orgV.isCoordinateVariable()) continue;
      Variable copyVar = org.findVariable(orgV.getShortName());
      if (copyVar == null) {
        f.format(" MISSING '%s' in 1st file%n", orgV.getFullName());
        ok = false;
      }
    }

    return ok;
  }

  private boolean compareGroups(Group org, Group copy, ObjFilter filter) {
    if (showCompare)
      f.format("compare Group '%s' to '%s' %n", org.getShortName(), copy.getShortName());
    boolean ok = true;

    if (!org.getShortName().equals(copy.getShortName())) {
      f.format(" ** names are different %s != %s %n", org.getShortName(), copy.getShortName());
      ok = false;
    }

    // dimensions
    if (filter.checkDimensionsForFile(org.getNetcdfFile().getLocation())) {
      ok &= checkGroupDimensions(org, copy, "copy", filter);
      ok &= checkGroupDimensions(copy, org, "org", filter);
    }

    // attributes
    ok &= checkAttributes(org.getFullName(), org.attributes(), copy.attributes(), filter);

    // enums
    ok &= checkEnums(org, copy, filter);

    // variables
    // cant use object equality, just match on short name
    for (Variable orgV : org.getVariables()) {
      Variable copyVar = copy.findVariableLocal(orgV.getShortName());
      if (copyVar == null) {
        f.format(" ** cant find variable %s in 2nd file%n", orgV.getFullName());
        ok = false;
      } else {
        ok &= compareVariables(orgV, copyVar, filter, compareData, true);
      }
    }

    for (Variable copyV : copy.getVariables()) {
      Variable orgV = org.findVariableLocal(copyV.getShortName());
      if (orgV == null) {
        f.format(" ** cant find variable %s in 1st file%n", copyV.getFullName());
        ok = false;
      }
    }

    // nested groups
    List groups = new ArrayList();
    String name = org.isRoot() ? "root group" : org.getFullName();
    ok &= checkAll(name, org.getGroups(), copy.getGroups(), groups);
    for (int i = 0; i < groups.size(); i += 2) {
      Group orgGroup = (Group) groups.get(i);
      Group copyGroup = (Group) groups.get(i + 1);
      ok &= compareGroups(orgGroup, copyGroup, filter);
    }

    return ok;
  }


  public boolean compareVariable(Variable org, Variable copy, ObjFilter filter) {
    return compareVariables(org, copy, filter, compareData, true);
  }

  private boolean compareVariables(Variable org, Variable copy, ObjFilter filter, boolean compareData,
      boolean justOne) {
    boolean ok = true;

    if (showCompare)
      f.format("compare Variable %s to %s %n", org.getFullName(), copy.getFullName());
    if (!org.getFullName().equals(copy.getFullName())) {
      f.format(" ** names are different %s != %s %n", org.getFullName(), copy.getFullName());
      ok = false;
    }
    if (filter.varDataTypeCheckOk(org) && (org.getArrayType() != copy.getArrayType())) {
      f.format(" ** %s dataTypes are different %s != %s %n", org.getFullName(), org.getArrayType(),
          copy.getArrayType());
      ok = false;
    }

    // dimensions
    ok &= checkDimensions(org.getDimensions(), copy.getDimensions(), copy.getFullName() + " copy", filter);
    ok &= checkDimensions(copy.getDimensions(), org.getDimensions(), org.getFullName() + " org", filter);

    // attributes
    ok &= checkAttributes(org.getFullName(), org.attributes(), copy.attributes(), filter);

    // data !!
    if (compareData) {
      ok &= CompareArrayToArray.compareVariableData(f, org, copy, false);
    }

    // coordinate systems
    if (org instanceof VariableEnhanced && copy instanceof VariableEnhanced) {
      VariableEnhanced orge = (VariableEnhanced) org;
      VariableEnhanced copye = (VariableEnhanced) copy;

      for (CoordinateSystem cs1 : orge.getCoordinateSystems()) {
        CoordinateSystem cs2 = copye.getCoordinateSystems().stream().filter(cs -> cs.getName().equals(cs1.getName()))
            .findFirst().orElse(null);
        if (cs2 == null) {
          ok = false;
          f.format("  ** Cant find CoordinateSystem '%s' in file2 for var %s %n", cs1.getName(), org.getShortName());
        } else {
          ok &= compareCoordinateSystem(cs1, cs2, filter);
        }
      }
    }

    // f.format(" Variable '%s' ok %s %n", org.getName(), ok);
    return ok;
  }

  private boolean compareCoordinateSystem(CoordinateSystem cs1, CoordinateSystem cs2, ObjFilter filter) {
    if (showCompare)
      f.format("compare CoordinateSystem '%s' to '%s' %n", cs1.getName(), cs2.getName());

    boolean ok = true;
    for (CoordinateAxis ct1 : cs1.getCoordinateAxes()) {
      CoordinateAxis ct2 = cs2.getCoordinateAxes().stream().filter(ct -> ct.getFullName().equals(ct1.getFullName()))
          .findFirst().orElse(null);
      if (ct2 == null) {
        ok = false;
        f.format("  ** Cant find coordinateAxis %s in file2 %n", ct1.getFullName());
      } else {
        ok &= compareCoordinateAxis(ct1, ct2, filter);
      }
    }

    Projection cp1 = cs1.getProjection();
    Projection cp2 = cs2.getProjection();
    boolean ctOk = filter.compareProjection(cp1, cp2);
    ok = ok && ctOk;

    return ok;
  }

  private boolean compareCoordinateAxis(CoordinateAxis a1, CoordinateAxis a2, ObjFilter filter) {
    if (showCompare)
      f.format("  compare CoordinateAxis '%s' to '%s' %n", a1.getShortName(), a2.getShortName());

    compareVariable(a1, a2, filter);
    return true;
  }


  // make sure each object in wantList is contained in container, using equals().

  // make sure each object in each list are in the other list, using equals().
  // return an arrayList of paired objects.

  private boolean checkAttributes(String name, AttributeContainer list1, AttributeContainer list2,
      ObjFilter objFilter) {
    boolean ok = true;

    for (Attribute att1 : list1) {
      if (objFilter.attCheckOk(att1)) {
        ok &= checkAtt(name, att1, "file1", list1, "file2", list2, objFilter);
      }
    }

    for (Attribute att2 : list2) {
      if (objFilter.attCheckOk(att2)) {
        ok &= checkAtt(name, att2, "file2", list2, "file1", list1, objFilter);
      }
    }

    return ok;
  }

  // Theres a bug in old HDF4 (eg "MOD021KM.A2004328.1735.004.2004329164007.hdf) where dimensions
  // are not properly moved up (eg dim BAND_250M is in both root and Data_Fields).
  // So we are going to allow that to be ok (until proven otherwise) but we have to adjust
  // dimension comparision. Currently Dimension.equals() checks the Group.
  private boolean checkDimensions(List<Dimension> list1, List<Dimension> list2, String where, ObjFilter filter) {
    boolean ok = true;

    for (Dimension d1 : list1) {
      if (d1.isShared()) {
        boolean hasit = listContains(list2, d1, filter);
        if (!hasit) {
          f.format("  ** Missing Variable dim '%s' not in %s %n", d1, where);
        }
        ok &= hasit;
      }
    }

    return ok;
  }

  // Check contains not using Group
  private boolean listContains(List<Dimension> list, Dimension d2, ObjFilter filter) {
    for (Dimension d1 : list) {
      if (equalInValue(d1, d2, filter)) {
        return true;
      }
    }
    return false;
  }

  public Dimension findDimension(Group g, Dimension dim, ObjFilter filter) {
    if (dim == null) {
      return null;
    }
    for (Dimension d : g.getDimensions()) {
      if (equalInValue(d, dim, filter)) {
        return d;
      }
    }
    Group parent = g.getParentGroup();
    if (parent != null) {
      return findDimension(parent, dim, filter);
    }
    return null;
  }

  public EnumTypedef findEnum(Group g, EnumTypedef typedef, ObjFilter filter) {
    if (typedef == null) {
      return null;
    }
    for (EnumTypedef other : g.getEnumTypedefs()) {
      if (filter.enumsAreEqual(typedef, other)) {
        return other;
      }
    }
    Group parent = g.getParentGroup();
    if (parent != null) {
      return findEnum(parent, typedef, filter);
    }
    return null;
  }

  private boolean equalInValue(Dimension d1, Dimension other, ObjFilter filter) {
    return filter.dimsAreEqual(d1, other);
  }

  // values equal, not using Group
  private boolean equalInValueOld(Dimension d1, Dimension other) {
    if ((d1.getShortName() == null) && (other.getShortName() != null))
      return false;
    if ((d1.getShortName() != null) && !d1.getShortName().equals(other.getShortName()))
      return false;
    return (d1.getLength() == other.getLength()) && (d1.isUnlimited() == other.isUnlimited())
        && (d1.isVariableLength() == other.isVariableLength()) && (d1.isShared() == other.isShared());
  }

  private boolean checkGroupDimensions(Group group1, Group group2, String where, ObjFilter filter) {
    boolean ok = true;
    for (Dimension d1 : group1.getDimensions()) {
      if (d1.isShared()) {
        if (!group2.getDimensions().contains(d1)) {
          // not in local, is it in a parent?
          if (findDimension(group2, d1, filter) != null) {
            f.format("  ** Dimension '%s' found in parent group of %s %s%n", d1, where, group2.getFullName());
          } else {
            boolean unshared1 = d1.getShortName() != null && (d1.getShortName().contains("_Dim"));
            if (!unshared1) {
              f.format("  ** Missing Group dim '%s' not in %s %s%n", d1, where, group2.getFullName());
              ok = false;
            }
          }
        }
      }
    }
    return ok;
  }

  private boolean checkEnums(Group org, Group copy, ObjFilter filter) {
    boolean ok = true;

    for (EnumTypedef enum1 : org.getEnumTypedefs()) {
      if (showCompare)
        f.format("compare Enum %s%n", enum1.getShortName());
      EnumTypedef enum2 = findEnum(copy, enum1, filter);
      if (enum2 == null) {
        findEnum(org, enum1, filter);
        f.format("  ** Enum %s not in file2 %n", enum1.getShortName());
        ok = false;
      }
    }

    for (EnumTypedef enum2 : copy.getEnumTypedefs()) {
      EnumTypedef enum1 = findEnum(org, enum2, filter);
      if (enum1 == null) {
        findEnum(org, enum2, filter);
        f.format("  ** Enum %s not in file1 %n", enum2.getShortName());
        ok = false;
      }
    }
    return ok;
  }

  private boolean checkAll(String what, List list1, List list2, List result) {
    boolean ok = true;

    for (Object aList1 : list1) {
      ok &= checkEach(what, aList1, "file1", list1, "file2", list2, result);
    }

    for (Object aList2 : list2) {
      ok &= checkEach(what, aList2, "file2", list2, "file1", list1, null);
    }

    return ok;
  }

  // check that want is in both list1 and list2, using object.equals()
  private boolean checkEach(String what, Object want1, String name1, List list1, String name2, List list2,
      List result) {
    boolean ok = true;
    try {
      int index2 = list2.indexOf(want1);
      if (index2 < 0) {
        f.format("  ** %s: %s 0x%x (%s) not in %s %n", what, want1, want1.hashCode(), name1, name2);
        ok = false;
      } else { // found it in second list
        Object want2 = list2.get(index2);
        int index1 = list1.indexOf(want2);
        if (index1 < 0) { // can this happen ??
          f.format("  ** %s: %s 0x%x (%s) not in %s %n", what, want2, want2.hashCode(), name2, name1);
          ok = false;

        } else { // found it in both lists
          Object want = list1.get(index1);
          if (!want.equals(want1)) {
            f.format("  ** %s: %s 0x%x (%s) not equal to %s 0x%x (%s) %n", what, want1, want1.hashCode(), name1, want2,
                want2.hashCode(), name2);
            ok = false;
          } else {
            if (showEach)
              f.format("  OK <%s> equals <%s>%n", want1, want2);
            if (result != null) {
              result.add(want1);
              result.add(want2);
            }
          }
        }
      }

    } catch (Throwable t) {
      t.printStackTrace();
      f.format(" *** Throwable= %s %n", t.getMessage());
      ok = false;
    }

    return ok;
  }

  // check that want is in both list1 and list2, using object.equals()
  private boolean checkAtt(String what, Attribute want, String name1, AttributeContainer list1, String name2,
      AttributeContainer list2, ObjFilter objFilter) {
    boolean ok = true;
    Attribute found = list2.findAttributeIgnoreCase(want.getShortName());
    if (found == null) {
      boolean check = objFilter.attCheckOk(want);
      f.format("  ** Attribute %s: %s (%s) not in %s %n", what, want, name1, name2);
      ok = false;
    } else {
      if (!objFilter.attsAreEqual(want, found)) {
        f.format("  ** Attribute %s: %s 0x%x (%s) not equal to %s 0x%x (%s) %n", what, want, want.hashCode(), name1,
            found, found.hashCode(), name2);
        ok = false;
      } else if (showEach) {
        f.format("  OK <%s> equals <%s>%n", want, found);
      }
    }
    return ok;
  }

  private boolean compareVariableData(Variable var1, Variable var2, boolean showCompare, boolean justOne)
      throws IOException {
    // TODO prevent trying to read > 2 gb
    try {
      Array<?> data1 = var1.readArray();
      Array<?> data2 = var2.readArray();
      if (showCompare) {
        f.format(" compareArrays %s unlimited=%s size=%d%n", var1.getNameAndDimensions(), var1.isUnlimited(),
            data1.getSize());
      }
      boolean ok = compareData(var1.getFullName(), data1, data2, justOne);
      if (showCompare) {
        f.format("   ok=%s%n", ok);
      }
      return ok;
    } catch (RuntimeException e) {
      if (e.getMessage() != null && e.getMessage().contains("Read request too large"))
        return true;
      throw e;
    }
  }

  public boolean compareData(String name, double[] data1, double[] data2) {
    Array<?> data1a = Arrays.factory(ArrayType.DOUBLE, new int[] {data1.length}, data1);
    Array<?> data2a = Arrays.factory(ArrayType.DOUBLE, new int[] {data2.length}, data2);
    return compareData(name, data1a, data2a, false);
  }

  public boolean compareData(String name, Array<?> data1, Array<?> data2, boolean justOne) {
    return CompareArrayToArray.compareData(f, name, data1, data2, justOne, true);
  }

}
