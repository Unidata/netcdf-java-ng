/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory.filter;

import thredds.inventory.MFile;
import thredds.inventory.MFileFilter;

/**
 * return the negation of the wrapped filter
 *
 * @author caron
 * @since 1/22/2015
 */
public class FilterNegate implements MFileFilter {
  private final MFileFilter filter;

  public FilterNegate(MFileFilter filter) {
    this.filter = filter;
  }

  @Override
  public boolean accept(MFile mfile) {
    return !filter.accept(mfile);
  }
}
