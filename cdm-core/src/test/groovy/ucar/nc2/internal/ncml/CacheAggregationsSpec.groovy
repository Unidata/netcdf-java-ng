/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 *  See LICENSE for license information.
 */

package ucar.nc2.internal.ncml

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification
import ucar.array.Section
import ucar.array.Array
import ucar.array.Arrays
import ucar.nc2.Variable
import ucar.nc2.dataset.DatasetUrl
import ucar.nc2.dataset.NetcdfDatasets

/**
 * Tests acquiring aggregated datasets from a file cache.
 *
 * @author cwardgar
 * @since 2015-12-29
 */
class CacheAggregationsSpec extends Specification {
    private static final Logger logger = LoggerFactory.getLogger(CacheAggregationsSpec)
    
    def setupSpec() {
        // All datasets, once opened, will be added to this cache.
        // Config values copied from CdmInit.
        NetcdfDatasets.initNetcdfFileCache(100, 150, 12 * 60);

        // Force NetcdfDataset to reacquire underlying file in order to read a Variable's data, instead of being
        // able to retrieve that data from the Variable's internal cache. OPV-470285 does not manifest on variables
        // with cached data.
        Variable.permitCaching = false;
    }

    def cleanupSpec() {
        // Undo global changes we made in setupSpec() so that they do not affect subsequent test classes.
        NetcdfDatasets.shutdown();
        Variable.permitCaching = true;
    }

    // The number of times each dataset will be acquired.
    // Failure, if it occurs, is expected to start happening on the 2nd trial.
    int numTrials = 2

    // Demonstrates eSupport ticket OPV-470285.
    def "union"() {
        setup:
        String filename = "file:./" + TestNcmlRead.topDir + "aggUnion.xml"
        def expecteds = [5.0, 10.0, 15.0, 20.0]
        def actuals

        (1..numTrials).each {
            when:
            NetcdfDatasets.acquireDataset(DatasetUrl.findDatasetUrl(filename), false, null).withCloseable {
                Variable var = it.findVariable('Temperature')
                Array array = var.readArray(new Section('1,1,:'))  // Prior to fix, failure happened here on 2nd trial.
                actuals = Arrays.copyPrimitiveArray(array) as List
            }

            then:
            expecteds == actuals
        }
    }

    def "joinExisting"() {
        setup:
        String filename = "file:./"+TestNcmlRead.topDir + "aggExisting.xml";
        def expecteds = [8420.0, 8422.0, 8424.0, 8426.0]
        def actuals

        (1..numTrials).each {
            when:
            NetcdfDatasets.acquireDataset(DatasetUrl.findDatasetUrl(filename), false, null).withCloseable {
                Variable var = it.findVariable('P')
                Array array = var.readArray(new Section('42,1,:'))
                actuals = Arrays.copyPrimitiveArray(array) as List
            }

            then:
            expecteds == actuals
        }
    }

    def "joinNew"() {
        setup:
        String filename = "file:./"+TestNcmlRead.topDir + "aggSynthetic.xml";
        def expecteds = [110.0, 111.0, 112.0, 113.0]
        def actuals

        (1..numTrials).each {
            when:
            NetcdfDatasets.acquireDataset(DatasetUrl.findDatasetUrl(filename), false, null).withCloseable {
                Variable var = it.findVariable('T')
                Array array = var.readArray(new Section('1,1,:'))
                actuals = Arrays.copyPrimitiveArray(array) as List
            }

            then:
            expecteds == actuals
        }
    }
}
