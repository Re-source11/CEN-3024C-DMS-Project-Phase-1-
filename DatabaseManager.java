import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the database side of the DMS. talks to MySQL with plain jdbc
 * (java.sql only) so the code compiles without any extra jar, the driver
 * only matters when the program actually runs. if theres no database the
 * program just stays in memory and keeps working like before.
 * <p>
 * note on ids: vial_id is the primary key and it never gets renumbered.
 * a real database doesnt reshuffle keys when a row is deleted so this
 * program doesnt either, deleted ids are just gone for good.
 * <p>
 * CEN 3024C - 31032
 *
 * @author Baker Legerme
 * @version 4.0
 */
public class DatabaseManager {
    private Connection connection;
    private String lastError = ""; // the raw reason the last connect failed

    /**
     * makes a manager with no connection yet, connect() is what opens one.
     */
    public DatabaseManager() {
    }

    /**
     * opens the connection and builds the peptides table if its not
     * there yet, so a brand new database works on the very first run
     * without typing any sql by hand
     *
     * @param url the jdbc url pointing at the server and database
     * @param user the database username
     * @param password the database password
     * @return true if it connected, false if not so the caller can fall back to memory
     */
    public boolean connect(String url, String user, String password) {
        try {
            connection = DriverManager.getConnection(url, user, password);
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS peptides (" +
                        "vial_id INT PRIMARY KEY," +
                        "compound_name VARCHAR(20) NOT NULL," +
                        "delivery_method VARCHAR(20) NOT NULL," +
                        "target_dose_mg FLOAT NOT NULL," +
                        "current_dose_mg FLOAT NOT NULL," +
                        "min_dose_mg FLOAT NOT NULL," +
                        "total_mass_mg FLOAT NOT NULL," +
                        "concentration_mg_per_ml FLOAT NOT NULL," +
                        "frequency VARCHAR(10) NOT NULL," +
                        "titration_needed BOOLEAN NOT NULL," +
                        "titration_increment_mg FLOAT NOT NULL," +
                        "days_per_step INT NOT NULL)");
            }
            return true;
        } catch (SQLException e) {
            // keep mysqls own words, access denied and unknown database and
            // connection refused all point at different fixes
            lastError = e.getMessage();
            connection = null;
            return false;
        }
    }

    /**
     * hands back the databases own words for why the connect failed,
     * so the status bar can show the real reason instead of a guess
     *
     * @return the last error text, or empty if there wasnt one
     */
    public String getLastError() {
        return lastError == null ? "" : lastError;
    }

    /**
     * quick check for whether database mode is actually on
     *
     * @return true if theres a live connection
     */
    public boolean isConnected() {
        return connection != null;
    }

    /**
     * pulls every row back out of the table and rebuilds them as
     * Peptide objects, ordered by id so the list reads top to bottom
     *
     * @return everything thats stored, or an empty list if theres nothing
     */
    public List<Peptide> loadAll() {
        List<Peptide> result = new ArrayList<>();
        if (!isConnected()) { return result; }
        String sql = "SELECT * FROM peptides ORDER BY vial_id";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new Peptide(
                        rs.getInt("vial_id"),
                        rs.getString("compound_name"),
                        rs.getString("delivery_method"),
                        rs.getFloat("target_dose_mg"),
                        rs.getFloat("current_dose_mg"),
                        rs.getFloat("min_dose_mg"),
                        rs.getFloat("total_mass_mg"),
                        rs.getFloat("concentration_mg_per_ml"),
                        rs.getString("frequency"),
                        rs.getBoolean("titration_needed"),
                        rs.getFloat("titration_increment_mg"),
                        rs.getInt("days_per_step")));
            }
        } catch (SQLException e) {
            // just return whatever got read before the error
        }
        return result;
    }

    /**
     * saves one new vial as a row, using the id the program already
     * generated for it
     *
     * @param p the Peptide to store
     * @return true if exactly one row went in
     */
    public boolean insert(Peptide p) {
        if (!isConnected()) { return false; }
        String sql = "INSERT INTO peptides VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, p);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * rewrites the whole row for this vial. one method covers every
     * field update instead of writing ten different ones
     *
     * @param p the Peptide whose current state should be saved
     * @return true if exactly one row changed
     */
    public boolean update(Peptide p) {
        if (!isConnected()) { return false; }
        String sql = "UPDATE peptides SET compound_name=?, delivery_method=?, target_dose_mg=?, " +
                "current_dose_mg=?, min_dose_mg=?, total_mass_mg=?, concentration_mg_per_ml=?, " +
                "frequency=?, titration_needed=?, titration_increment_mg=?, days_per_step=? WHERE vial_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p.getCompoundName());
            ps.setString(2, p.getDeliveryMethod());
            ps.setFloat(3, p.getTargetedDosemg());
            ps.setFloat(4, p.getCurrentDosemg());
            ps.setFloat(5, p.getMinTherapeuticDosemg());
            ps.setFloat(6, p.getTotalVialMassmg());
            ps.setFloat(7, p.getConcentrationmgPermL());
            ps.setString(8, p.getFrequency());
            ps.setBoolean(9, p.isTitrationNeeded());
            ps.setFloat(10, p.getTitrationIncrementmg());
            ps.setInt(11, p.getDaysNeeded());
            ps.setInt(12, p.getVialId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * removes the row with that id. the id is gone for good after this,
     * primary keys never get handed out twice
     *
     * @param vialId which row to delete
     * @return true if exactly one row was removed
     */
    public boolean delete(int vialId) {
        if (!isConnected()) { return false; }
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM peptides WHERE vial_id=?")) {
            ps.setInt(1, vialId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * lets go of the connection when the program shuts down
     */
    public void close() {
        try {
            if (connection != null) { connection.close(); }
        } catch (SQLException e) {
            // closing anyway, nothing to do about it
        }
        connection = null;
    }

    /**
     * helper that fills in all twelve columns of an insert in order,
     * so the column order only lives in one place
     *
     * @param ps the prepared insert
     * @param p the Peptide to bind
     */
    private void bind(PreparedStatement ps, Peptide p) throws SQLException {
        ps.setInt(1, p.getVialId());
        ps.setString(2, p.getCompoundName());
        ps.setString(3, p.getDeliveryMethod());
        ps.setFloat(4, p.getTargetedDosemg());
        ps.setFloat(5, p.getCurrentDosemg());
        ps.setFloat(6, p.getMinTherapeuticDosemg());
        ps.setFloat(7, p.getTotalVialMassmg());
        ps.setFloat(8, p.getConcentrationmgPermL());
        ps.setString(9, p.getFrequency());
        ps.setBoolean(10, p.isTitrationNeeded());
        ps.setFloat(11, p.getTitrationIncrementmg());
        ps.setInt(12, p.getDaysNeeded());
    }
}