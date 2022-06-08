/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory;

import thredds.client.catalog.TimeDuration;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.filesystem.MFileOS7;
import thredds.inventory.partition.DirectoryCollection;
import ucar.nc2.calendar.CalendarDate;
import ucar.unidata.util.StringUtil2;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.*;

/**
 * Abstract superclass for Collections of MFiles.
 * Deal with the collection element of feature collections:
 * 
 * <pre>
   <xsd:complexType name="collectionType">
     <xsd:attribute name="spec" type="xsd:string" use="required"/>
     <xsd:attribute name="name" type="xsd:token"/>
     <xsd:attribute name="olderThan" type="xsd:string" />
     <xsd:attribute name="dateFormatMark" type="xsd:string"/>
     <xsd:attribute name="timePartition" type="xsd:string"/>
   </xsd:complexType>
 * </pre>
 * 
 * where:
 * <li>
 * <ol>
 * spec: is handled by CollectionSpecParser. it provides the root directory, Pattern filter, and optionally a
 * dateExtractor
 * </ol>
 * <ol>
 * name: getCollectionName()
 * </ol>
 * <ol>
 * olderThan: getOlderThanMsecs()
 * </ol>
 * <ol>
 * dateFormatMark: dateExtractor
 * </ol>
 * <ol>
 * timePartition: CollectionGeneral, DirectoryPartition, FilePartition, TimePartition
 * </ol>
 * </li>
 *
 * @author caron
 * @since 11/20/13
 */
public abstract class CollectionAbstract implements MCollection {
  private static final org.slf4j.Logger defaultLog = org.slf4j.LoggerFactory.getLogger("featureCollectionScan");

  public static final String DIR = "directory:";
  public static final String FILE = "file:";
  public static final String LIST = "list:";
  public static final String GLOB = "glob:";

  // called from Aggregation, Fmrc, FeatureDatasetFactoryManager
  public static MCollection open(String collectionName, String collectionSpec, String olderThan, Formatter errlog)
      throws IOException {

    if (collectionSpec.startsWith(DIR))
      return new DirectoryCollection(collectionName, collectionSpec.substring(DIR.length()), true, olderThan, null);

    else if (collectionSpec.startsWith(FILE)) {
      MFile file = MFileOS7.getExistingFile(collectionSpec.substring(FILE.length()));
      if (file == null)
        throw new FileNotFoundException(collectionSpec.substring(FILE.length()));
      return new CollectionSingleFile(file, null);

    } else if (collectionSpec.startsWith(LIST))
      return new CollectionList(collectionName, collectionSpec.substring(LIST.length()), null);

    else if (collectionSpec.startsWith(GLOB))
      return new CollectionGlob(collectionName, collectionSpec.substring(GLOB.length()), null);

    else
      return MFileCollectionManager.open(collectionName, collectionSpec, olderThan, errlog);
  }

  public static String cleanName(String name) {
    if (name == null)
      return null;
    return StringUtil2.replace(name.trim(), ' ', "_"); // TODO must be ok in URL - probably not sufficient here
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected String collectionName;
  protected String root;
  protected final org.slf4j.Logger logger;

  protected FeatureCollectionConfig.ProtoChoice protoChoice = FeatureCollectionConfig.ProtoChoice.Penultimate; // default

  protected Map<String, Object> auxInfo; // lazy init

  protected DateExtractor dateExtractor;
  protected CalendarDate startCollection;
  protected long lastModified;
  protected DirectoryStream.Filter<Path> sfilter;

  protected CollectionAbstract(String collectionName, org.slf4j.Logger logger) {
    this.collectionName = cleanName(collectionName);
    this.logger = logger != null ? logger : defaultLog;
  }

  @Override
  public String getCollectionName() {
    return collectionName;
  }

  @Override
  public String getIndexFilename(String suffix) {
    return getRoot() + "/" + collectionName + suffix;
  }

  public void setStreamFilter(DirectoryStream.Filter<Path> filter) {
    this.sfilter = filter;
  }

  @Override
  public String getRoot() {
    return root;
  }

  protected void setRoot(String root) {
    this.root = root;
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public MFile getLatestFile() throws IOException {
    MFile result = null;
    for (MFile f : getFilesSorted()) // only have an Iterable
      result = f;
    return result;
  }

  @Override
  public List<String> getFilenames() throws IOException {
    List<String> result = new ArrayList<>();
    for (MFile f : getFilesSorted())
      result.add(f.getPath());
    return result;
  }

  @Override
  public CalendarDate extractDate(MFile mfile) {
    return (dateExtractor == null) ? null : dateExtractor.getCalendarDate(mfile);
  }

  @Override
  public boolean hasDateExtractor() {
    return (dateExtractor != null);
  }

  public void setDateExtractor(DateExtractor dateExtractor) {
    this.dateExtractor = dateExtractor;
  }

  @Override
  public CalendarDate getPartitionDate() {
    return startCollection;
  }

  ////////////////////////////////////////////////////
  // ability to pass arbitrary information in. kind of a kludge

  @Override
  public Object getAuxInfo(String key) {
    return auxInfo == null ? null : auxInfo.get(key);
  }

  @Override
  public void putAuxInfo(String key, Object value) {
    if (auxInfo == null)
      auxInfo = new HashMap<>();
    auxInfo.put(key, value);
  }

  ////////////////////////////////////////////////////
  // proto dataset choosing

  @Override
  public int getProtoIndex(int n) {
    if (n < 2)
      return 0;

    int protoIdx = 0;
    switch (protoChoice) {
      case First:
        protoIdx = 0;
        break;
      case Random:
        Random r = new Random(System.currentTimeMillis());
        protoIdx = r.nextInt(n - 1);
        break;
      case Run:
      case Penultimate:
        protoIdx = Math.max(n - 2, 0);
        break;
      case Latest:
        protoIdx = Math.max(n - 1, 0);
        break;
    }
    return protoIdx;
  }

  public class DateSorter implements Comparator<MFile> {
    public int compare(MFile m1, MFile m2) {
      CalendarDate cd1 = extractRunDateWithError(m1);
      CalendarDate cd2 = extractRunDateWithError(m2);
      if ((cd1 == null) || (cd2 == null)) {
        // cd1 = extractRunDateWithError(m1); //debug
        // cd2 = extractRunDateWithError(m2);
        throw new IllegalStateException();
      }
      return cd1.compareTo(cd2);
    }
  }

  CalendarDate extractRunDateWithError(MFile mfile) {
    CalendarDate result = extractDate(mfile);
    if (result == null)
      logger.error("Failed to extract date from file {} with Extractor {}", mfile.getPath(), dateExtractor);
    return result;
  }


  /////////////////////////////////////////////////////////////////////////

  public class MyStreamFilter implements DirectoryStream.Filter<Path> {
    public boolean accept(Path entry) throws IOException {
      return sfilter == null || sfilter.accept(entry);
    }
  }

  protected List<MFile> makeFileListSorted() throws IOException {
    List<MFile> list = new ArrayList<>(100);
    try (CloseableIterator<MFile> iter = getFileIterator()) {
      if (iter == null)
        return list;

      while (iter.hasNext())
        list.add(iter.next());
    }
    if (hasDateExtractor()) {
      list.sort(new DateSorter()); // sort by date
    } else {
      Collections.sort(list); // sort by name
    }
    return list;
  }

  ////////////////////////////////////////////

  /**
   * parse the "olderThan" TimeDuration, meaning files must not have been modified since this amount of time
   * 
   * @param olderThan TimeDuration string
   * @return TimeDuration in millisecs
   */
  protected long parseOlderThanString(String olderThan) {
    if (olderThan != null) {
      try {
        // TODO use nc2.calendar
        TimeDuration tu = TimeDuration.parse(olderThan);
        return (long) (1000 * tu.getValueInSeconds());
      } catch (Exception e) {
        logger.error(collectionName + ": Invalid time unit for olderThan = {}", olderThan);
      }
    }
    return -1;
  }


}

