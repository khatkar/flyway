package org.flywaydb.core;

import org.flywaydb.core.internal.util.jdbc.DriverDataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

@RunWith(Parameterized.class)
public class PerformanceTestcase {

    private static Logger LOG = Logger.getLogger( PerformanceTestcase.class.getName() );
    private static final String SCHEMA_TABLE_NAME = "test_versions";
    private static final String SKRIPT_NAME_FORMAT = "%s.%05d__test.sql";
    private static final String SKRIPT_CONTENT = "select %05d from dual;";
    private static final String FILESYSTEM = "filesystem:";
    private static final String BASE_SKRIPT_NAME = "V00.00.00";
    private static final String BASE_DIR = "/tmp/performanceTest";

    @Parameterized.Parameter
    public int noOfScripts;

    @Before
    public void generateLotsOfInstallerSkripts() throws IOException {
        LOG.log(Level.INFO, "generating {0} skripts", noOfScripts);
        Path baseVersion = getBaseVersionPath();
        generateSkripts( noOfScripts, baseVersion, BASE_SKRIPT_NAME );
    }

    @Test
    public void testPerformance() throws IOException, SQLException {
        // this one does not scale well with increasing noOfScripts
        migrate();
    }


    private void migrate() throws SQLException {
        Flyway flyway = new Flyway();
        flyway.setDataSource( getDataSource() );
        flyway.setLocations( FILESYSTEM + BASE_DIR );
        flyway.setTable( SCHEMA_TABLE_NAME );
        flyway.setBaselineVersionAsString(BASE_SKRIPT_NAME.substring(1) );
        flyway.setBaselineOnMigrate(true);
        flyway.setValidateOnMigrate(false);

        flyway.migrate();
    }

    @Parameterized.Parameters(name="noOfScripts={0}")
    public static Collection<Object[]> data() {
        Collection<Object[]> retval = new ArrayList<>();
        for ( int i = 1; i < 16000; i += 1000 ) {
            retval.add(new Object[] { i });
        }
        return retval;
    }

    private Path getBaseDirPath() throws IOException {
        Path base = Paths.get(BASE_DIR);
        if ( !Files.exists(base) ) {
            Files.createDirectory(base);
        }
        return base;
    }

    private Path getBaseVersionPath() throws IOException {
        Path base = getBaseDirPath();
        Path baseVersion = base.resolve(BASE_SKRIPT_NAME);
        if ( !Files.exists(baseVersion) ) {
            Files.createDirectories(baseVersion);
        }
        return baseVersion;
    }

    private void generateSkripts( int numberOfSkripts, Path baseDir, String baseName ) throws IOException {
        for (int i = 0; i < numberOfSkripts; i++) {
            Path file = baseDir.resolve( String.format(SKRIPT_NAME_FORMAT, baseName, i) );
            Files.write( file
                    , Arrays.asList( new String[] { String.format( SKRIPT_CONTENT, i ) } )
                    , StandardOpenOption.CREATE
                    , StandardOpenOption.TRUNCATE_EXISTING
            );
        }

    }

    private DataSource getDataSource() throws SQLException {
        DriverDataSource ds =
            new DriverDataSource(Thread.currentThread().getContextClassLoader(), null, "jdbc:h2:mem:flyway_db_info;DB_CLOSE_DELAY=-1", "sa", null);
        return ds;
    }
}
