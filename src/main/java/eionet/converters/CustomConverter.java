package eionet.converters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

/**
 * @author George Sofianos
 *
 */
public class CustomConverter implements Converter {

    private static final Logger log = LoggerFactory.getLogger(CustomConverter.class);

    private String url;
    private String input;
    private final int batchSize;
    private Statement st;
    private Connection connection;
    private String query;

    public CustomConverter(int batchSize) {
        this.batchSize = batchSize;
    }


    private Connection getConnection(String url) throws SQLException {
        connection = DriverManager.getConnection(url);
        return connection;
    }

    public void convert() throws SQLException {
        try {
            executeStatements();
        } catch (SQLException ex) {
            log.error("Error while executing statements, trying to rollback: " + ex);
            rollback();
        } catch (IOException ex) {
            log.error("Error while executing statements, trying to rollback: " + ex);
            rollback();
        } finally {
            cleanup();
        }
    }

    private void rollback() throws SQLException {
        connection.rollback();
        log.debug("Changes to the database have been reverted");
    }

    private void executeStatements() throws SQLException, IOException {
        connection = this.getConnection(url);
        log.debug("Connected to database");
        log.info("Executing SQL statements now...");
        long duration = System.currentTimeMillis();
        connection.setAutoCommit(false);
        st = connection.createStatement();
        if (connection != null) {
            BufferedReader inputFile = null;
            try {
                inputFile = getFile(input);
                int count = 0;
                while ((query = inputFile.readLine()) != null) {
                    query = query.trim();
                    if (query.isEmpty()) {
                        log.debug("Ignored empty line");
                        continue;
                    }
                    st.addBatch(query);
                    if (++count % batchSize == 0) {
                        st.executeBatch();
                        log.debug("Flushed last " + batchSize + " statements");
                    }
                }
                st.executeBatch();
                log.debug("Flushed remanining statements");

                connection.commit();
                log.debug("Transaction committed successfully");
                duration = System.currentTimeMillis() - duration;
                log.info("SQL statements executed successfully, time it took to finish: " + String.format("%d minutes, %d seconds",
                        TimeUnit.MILLISECONDS.toMinutes(duration), TimeUnit.MILLISECONDS.toSeconds(duration)
                                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)))
                );
            } finally {
                inputFile.close();
            }
        }
    }

    /**
     * Cleans connection and other objects.
     */
    private void cleanup() {
        try {
            connection.close();
            log.debug("Connection to database closed");
        } catch (SQLException ex) {
            log.error("An error has occured while closing the connection: " + ex);
        }
    }

    private BufferedReader getFile(String filePath) throws FileNotFoundException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(filePath), Charset.forName("UTF-8")));
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setInput(String input) {
        this.input = input;
    }
}
