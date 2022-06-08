/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.util.prefs;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.nc2.util.Misc;
import java.io.IOException;
import java.util.prefs.Preferences;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class TestXMLStore {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  static {
    System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");
  }

  private String storeFile;

  @Before
  public void setup() throws IOException {
    storeFile = tempFolder.newFile().getAbsolutePath();
    XMLStore store = XMLStore.createFromFile(storeFile, null);
    PreferencesExt prefs = store.getPreferences();
    prefs.putDouble("testD", 3.14157);
    prefs.putFloat("testF", 1.23456F);
    prefs.putLong("testL", 12345678900L);
    prefs.putInt("testI", 123456789);
    prefs.put("testS", "youdBeDeadbyNow");
    prefs.putBoolean("testB", true);

    byte[] barr = new byte[3];
    barr[0] = 1;
    barr[1] = 2;
    barr[2] = 3;
    prefs.putByteArray("testBA", barr);

    Preferences subnode = prefs.node("SemperUbi");
    subnode.putDouble("testD", 3.14158);
    subnode.putFloat("testF", 1.23457F);
    subnode.putLong("testL", 12345678901L);
    subnode.putInt("testI", 123456780);
    subnode.put("testS", "youdBeLivebyNow");
    subnode.putBoolean("testB", false);

    byte[] barr2 = new byte[3];
    barr2[0] = 2;
    barr2[1] = 3;
    barr2[2] = 4;
    subnode.putByteArray("testBA", barr2);

    store.save();
  }

  @Test
  public void testPersistence() throws IOException {
    XMLStore store = XMLStore.createFromFile(storeFile, null);

    PreferencesExt prefs = store.getPreferences();

    double d = prefs.getDouble("testD", 0.0);
    assertThat(Misc.nearlyEquals(d, 3.14157)).isTrue();

    float f = prefs.getFloat("testF", 0.0F);
    assertThat(Misc.nearlyEquals(f, 1.23456F)).isTrue();

    long ll = prefs.getLong("testL", 0);
    assertThat(ll).isEqualTo(12345678900L);

    int ii = prefs.getInt("testI", 0);
    assertThat(ii).isEqualTo(123456789);

    String s = prefs.get("testS", "");
    assertThat(s).isEqualTo("youdBeDeadbyNow");

    boolean b = prefs.getBoolean("testB", false);
    assertThat(b).isTrue();

    byte[] barr = new byte[3];
    byte[] barr2 = new byte[3];
    barr[0] = 1;
    barr[1] = 2;
    barr[2] = 3;
    byte[] ba = prefs.getByteArray("testBA", barr2);
    for (int i = 0; i < 3; i++)
      assertThat(ba[i]).isEqualTo(barr[i]);
  }

  @Test
  public void testPersistenceSubnode() throws IOException {
    XMLStore store = XMLStore.createFromFile(storeFile, null);
    Preferences prefs = store.getPreferences().node("SemperUbi");

    double d = prefs.getDouble("testD", 0.0);
    assertThat(Misc.nearlyEquals(d, 3.14158)).isTrue();

    float f = prefs.getFloat("testF", 0.0F);
    assertThat(Misc.nearlyEquals(f, 1.23457F)).isTrue();

    long ll = prefs.getLong("testL", 0);
    assertThat(ll).isEqualTo(12345678901L);

    int ii = prefs.getInt("testI", 0);
    assertThat(ii).isEqualTo(123456780);

    String s = prefs.get("testS", "");
    assertThat(s).isEqualTo("youdBeLivebyNow");

    boolean b = prefs.getBoolean("testB", true);
    assertThat(!b).isTrue();

    byte[] barr = new byte[3];
    byte[] barr2 = new byte[3];
    barr[0] = 2;
    barr[1] = 3;
    barr[2] = 4;
    byte[] ba = prefs.getByteArray("testBA", barr2);
    for (int i = 0; i < 3; i++)
      assertThat(ba[i]).isEqualTo(barr[i]);
  }

  @Test
  public void testPersistenceChange() throws IOException {
    XMLStore store = XMLStore.createFromFile(storeFile, null);
    Preferences prefs = store.getPreferences().node("SemperUbi");

    String s = prefs.get("testS", "");
    assertThat(s).isEqualTo("youdBeLivebyNow");

    prefs.put("testS", "NewBetter");
    store.save();

    XMLStore store2 = XMLStore.createFromFile(storeFile, null);
    Preferences prefs2 = store2.getPreferences().node("SemperUbi");

    s = prefs2.get("testS", "");
    assertThat(s).isEqualTo("NewBetter");

    prefs.put("testS", "youdBeDeadbyNow");
    store.save();

    XMLStore store3 = XMLStore.createFromFile(storeFile, null);
    Preferences prefs3 = store3.getPreferences().node("SemperUbi");

    s = prefs3.get("testS", "");
    assertThat(s).isEqualTo("youdBeDeadbyNow");
  }

  @Test
  public void testPersistenceAddRemove() throws IOException {
    XMLStore store = XMLStore.createFromFile(storeFile, null);
    Preferences prefs = store.getPreferences().node("SemperUbi");

    String s = prefs.get("testS2", "def");
    assertThat(s).isEqualTo("def");

    prefs.put("testS2", "WayBetter");
    store.save();

    XMLStore store2 = XMLStore.createFromFile(storeFile, null);
    Preferences prefs2 = store2.getPreferences().node("SemperUbi");

    s = prefs2.get("testS2", "");
    assertThat(s).isEqualTo("WayBetter");

    prefs.remove("testS2");
    store.save();

    XMLStore store3 = XMLStore.createFromFile("E:/dev/prefs/test/store/prefs2.xml", null);
    Preferences prefs3 = store3.getPreferences().node("SemperUbi");

    s = prefs3.get("testS2", "deff");
    assertThat(s).isEqualTo("deff");
  }

  @Test
  public void testPersistenceDefaults() throws IOException {
    XMLStore store = XMLStore.createFromFile(storeFile, null);
    Preferences newNode = store.getPreferences().node("SemperUbi/SubSemperUbi2");

    String s = newNode.get("testS2", "def");
    assertThat(s).isEqualTo("def");

    s = newNode.get("testS2", "def2");
    assertThat(s).isEqualTo("def2");
  }

  @Test
  public void testPersistenceAddRemoveNode() throws Exception {
    XMLStore store = XMLStore.createFromFile(storeFile, null);
    Preferences newNode = store.getPreferences().node("SemperUbi/SubSemperUbi2");

    String s = newNode.get("testS2", "def");
    assertThat(s).isEqualTo("def");

    newNode.put("testS2", "WayBetterValue");
    store.save();

    XMLStore store2 = XMLStore.createFromFile(storeFile, null);
    Preferences prefs2 = store2.getPreferences().node("SemperUbi/SubSemperUbi2");

    s = prefs2.get("testS2", "");
    assertThat(s).isEqualTo("WayBetterValue");

    prefs2.removeNode();
    store2.save();

    XMLStore store3 = XMLStore.createFromFile(storeFile, null);
    Preferences prefs3 = store3.getPreferences().node("SemperUbi/SubSemperUbi2");

    s = prefs3.get("testS2", "deff");
    assertThat(s).isEqualTo("deff");
  }

  @Test
  public void testXMLencoding() throws IOException {
    String bad = "><';&\r\"\n";

    XMLStore store = XMLStore.createFromFile(storeFile, null);
    Preferences prefs = store.getPreferences().node("badchars");

    prefs.put("baddog", bad);
    store.save();

    XMLStore store2 = XMLStore.createFromFile(storeFile, null);
    Preferences pref2 = store2.getPreferences().node("badchars");

    String s = pref2.get("baddog", null);
    assertThat(s).isEqualTo(bad);
  }
}
