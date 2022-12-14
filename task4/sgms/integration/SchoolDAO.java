package integration;

import model.Instrument;
import model.Lease;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class constitutes the integration layer, and is a Database Access Object.
 * We connect to the database, prepare SQL statements, and tell the database to run
 * these statements when so prompted.
 */
public class SchoolDAO {
    private Connection connection;

    private PreparedStatement findRentableStmt;
    private PreparedStatement createLeaseStmt;
    private PreparedStatement updateEndLeaseStmt;
    private PreparedStatement findActiveLeasesStudentStmt;
    private PreparedStatement findActiveRentalInstrumentStmt;

    /**
     * Constructs the Database Access Object by connecting to the database and
     * preparing the sql-statements. *
     *
     * @throws SchoolDBException
     */
    public SchoolDAO() throws SchoolDBException {
        try {
            connectToDB();
            prepareStatements();
        } catch (ClassNotFoundException | SQLException exception) {
            throw new SchoolDBException("Could not connect. ", exception);
        }
    }

    /**
     * Connects to the soundgood database and sets auto - commit to false.
     *
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private void connectToDB() throws ClassNotFoundException, SQLException {

        // connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "admin");
        connection =
        DriverManager.getConnection("jdbc:postgresql://localhost:5432/soundgood","postgres",
        "Ladrin123");
        connection.setAutoCommit(false);
    }

    /**
     * Finds all available instruments in the database of a certain type.
     *
     * @param type Specifies the type of instruments to search for.
     * @return A list of the Instrument objects found by the query.
     * @throws SchoolDBException
     */
    public List<Instrument> findRentableInstrumentsOfType(String type) throws SchoolDBException {
        String failMsg = "Could not fetch instruments";
        List<Instrument> instruments = new ArrayList<>();
        ResultSet result = null;
        try {
            findRentableStmt.setString(1, type);
            result = findRentableStmt.executeQuery();
            while (result.next()) {
                instruments.add(new Instrument(
                        result.getString(1), result.getString(2), result.getFloat(3)));
            }
            connection.commit();
        } catch (Exception e) {
            exceptionHandler(failMsg, e);
        } finally {
            closeResult(result, failMsg);
        }
        return instruments;
    }

    /**
     * Finds leases associated with a certain student
     *
     * @param studentID The id of the student
     * @return A list of active rentals that student currently has
     * @throws SchoolDBException
     */
    public List<Lease> findActiveLeasesForStudent(String studentID) throws SchoolDBException {
        String failMsg = "Could not fetch student rentals";
        ResultSet resultSet = null;
        List<Lease> result = new ArrayList<Lease>();
        try {
            findActiveLeasesStudentStmt.setInt(1, Integer.parseInt(studentID));

            resultSet = findActiveLeasesStudentStmt.executeQuery();

            while(resultSet.next()){
                result.add(new Lease(
                    resultSet.getString(1), 
                    resultSet.getString(2), 
                    resultSet.getString(3), 
                    resultSet.getString(4), 
                    resultSet.getString(5)));
            }
            // connection.commit();
        } catch (Exception e) {
            exceptionHandler(failMsg, e);
        } finally {
            closeResult(resultSet, failMsg);
        }
        return result;
    }

    /**
     * Finds the active rentals for a certain instrument. Should be zero (null) or one.
     *
     * @param instrumentID The ID of the instrument to check.
     * @return A Lease object with info about the active rental for the instrument or null.
     * @throws SchoolDBException
     */
    public Lease findActiveLeasesForInstrument(String instrumentID) throws SchoolDBException {
        String failMsg = "Could not check instrument";
        ResultSet resultSet = null;
        Lease result = null;
        try {
            // Following checks that instrument isn't already rented
            findActiveRentalInstrumentStmt.setInt(1, Integer.parseInt(instrumentID));

            resultSet = findActiveRentalInstrumentStmt.executeQuery();
            if (resultSet.next()) {
                result = new Lease(
                    resultSet.getString(1), 
                    resultSet.getString(2), 
                    resultSet.getString(3), 
                    resultSet.getString(4), 
                    resultSet.getString(5));
            }
            // connection.commit();
        } catch (Exception e) {
            exceptionHandler(failMsg, e);
        } finally {
            closeResult(resultSet, failMsg);
        }
        return result;
    }

    /**
     * Creates a new database entry for the rental of a specified instrument to a
     * student.
     *
     * @param instrumentID Specifies the ID of the instrument to be rented.
     * @param studentID    Specifies the ID of the student the instrument should be
     *                     rented to.
     * @throws SchoolDBException
     */
    public void createInstrumentLease(String instrumentID, String studentID) throws SchoolDBException {
        String failMsg = "Could not rent instrument";
        int affectedRows = 0;
        try {
            int studentToRentID = Integer.parseInt(studentID);
            int instrumentToRentID = Integer.parseInt(instrumentID);

            // rents the instrument
            createLeaseStmt.setInt(1, studentToRentID);
            createLeaseStmt.setInt(2, instrumentToRentID);

            affectedRows = createLeaseStmt.executeUpdate();
            if (affectedRows != 1) {
                exceptionHandler(failMsg, null);
            }
            connection.commit();

        } catch (Exception e) {
            exceptionHandler(failMsg, e);
        }
    }

    /**
     * Terminates a rental of an instrument by updating the lease end date to the
     * current date.
     *
     * @param instrumentID Specifies the id of the instrument
     * @param studentID    Specifies the id of the student
     * @throws SQLException
     */
    public void updateInstrumentLeaseAsTerminated(String instrumentID, String studentID) throws SchoolDBException {
        String failMsg = "Could not terminate rental";
        int affectedRows = 0;
        try {
            updateEndLeaseStmt.setInt(1, Integer.parseInt(studentID));
            updateEndLeaseStmt.setInt(2, Integer.parseInt(instrumentID));

            affectedRows = updateEndLeaseStmt.executeUpdate();
            if (affectedRows != 1) {
                exceptionHandler(failMsg, null);
            }
            connection.commit();
        } catch (Exception e) {
            exceptionHandler(failMsg, e);
        }
    }

    /**
     * Initialises the prepared sql-statements.
     *
     * @throws SQLException
     */
    private void prepareStatements() throws SQLException {

        findActiveRentalInstrumentStmt = connection.prepareStatement(
                "SELECT * FROM instrument_lease WHERE instruments_id = ? AND lease_end IS NULL FOR UPDATE");

        findActiveLeasesStudentStmt = connection.prepareStatement(
                "SELECT * FROM instrument_lease WHERE student_id = ? AND lease_end IS NULL FOR UPDATE");

        createLeaseStmt = connection.prepareStatement(
                "INSERT INTO instrument_lease (lease_start, lease_end, student_id, instruments_id) " +
                        "VALUES (now(),null,?,?)");

        findRentableStmt = connection.prepareStatement(
                "SELECT instruments.id, instruments.brand, instruments.fee " +
                        "FROM instruments, instrument_type " +
                        "WHERE " +
                        "(instruments.instrument_type_id = instrument_type.id AND instrument_type.name = ?) " +
                        "AND instruments.id NOT IN " +
                        "(SELECT DISTINCT instruments_id FROM instrument_lease WHERE lease_end IS NULL)");

        updateEndLeaseStmt = connection.prepareStatement(
                "UPDATE instrument_lease SET lease_end = NOW() " +
                        "WHERE student_id = ? AND instruments_id = ? AND lease_end IS NULL");
    }

    private void exceptionHandler(String failMsg, Exception causeOfFail) throws SchoolDBException {
        try {
            connection.rollback();
        } catch (SQLException e) {
            failMsg += ". Failed to rollback: " + e.getMessage();
        }

        if (causeOfFail != null) {
            throw new SchoolDBException(failMsg, causeOfFail);
        } else {
            throw new SchoolDBException(failMsg);
        }
    }

    private void closeResult(ResultSet result, String failMsg) throws SchoolDBException {
        try {
            result.close();
        } catch (Exception e) {
            throw new SchoolDBException(failMsg + ". Could not close stmt: ", e);
        }
    }
}
