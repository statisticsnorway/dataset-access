package no.ssb.datasetaccess;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static net.logstash.logback.marker.Markers.appendEntries;

@Singleton
public class AccessRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AccessRepository.class);

    private static final String DOES_USER_HAVE_ACCESS = "" +
            "SELECT * FROM dataset_user_permission " +
            "WHERE dataset_user_id = ? " +
            "AND dataset_id = ?";

    private static final String CREATE_USER = "INSERT INTO dataset_user (id) VALUES (?) ON CONFLICT DO NOTHING";

    private static final String CREATE_DATASET = "INSERT INTO dataset (id) VALUES (?) ON CONFLICT DO NOTHING";

    private static final String CREATE_DATASET_USER_ACCESS = "" +
            "INSERT INTO dataset_user_permission (dataset_user_id, dataset_id) " +
            "VALUES (?, ?) ON CONFLICT DO NOTHING";

    private final Config config;

    private final DataSource dataSource;

    public AccessRepository(Config config, DataSource dataSource) {
        this.config = config;
        this.dataSource = dataSource;
    }

    public boolean doesUserHaveAccessToDataset(final User user, final Dataset dataset) throws AccessRepositoryException {

        boolean hasAccess = false;

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(DOES_USER_HAVE_ACCESS)
        ) {

            preparedStatement.setString(1, user.getId());
            preparedStatement.setString(2, dataset.getId());
            preparedStatement.setQueryTimeout(this.config.getQueryTimeout().toSecondsPart());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    hasAccess = true; // The result set has at least one entry

                    LOG.info(
                            appendEntries(Map.of("dataset_id", dataset.getId(), "user_id", user.getId())),
                            "User can access dataset"
                    );
                }
            }

        } catch (SQLException e) {
            throw new AccessRepositoryException("Error when checking user access", e);
        }

        return hasAccess;
    }

    private void addUserIfNotExists(final Connection connection, final User user) throws AccessRepositoryException {
        try (final PreparedStatement preparedStatement = connection.prepareStatement(CREATE_USER)) {

            preparedStatement.setString(1, user.getId());
            preparedStatement.setQueryTimeout(this.config.getQueryTimeout().toSecondsPart());

            final int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected == 0) {
                LOG.info(appendEntries(Map.of("user_id", user.getId())), "User already exists");
            }

        } catch (SQLException e) {
            throw new AccessRepositoryException("Could not create new user", e);
        }
    }

    public void addUserIfNotExists(final User user) throws AccessRepositoryException {

        try (final Connection connection = dataSource.getConnection()) {

            addUserIfNotExists(connection, user);

        } catch (SQLException e) {
            throw new AccessRepositoryException("Could not create new user", e);
        }
    }

    private void addDatasetIfNotExists(final Connection connection, final Dataset dataset) throws AccessRepositoryException {
        try (final PreparedStatement preparedStatement = connection.prepareStatement(CREATE_DATASET)) {

            preparedStatement.setString(1, dataset.getId());
            preparedStatement.setQueryTimeout(this.config.getQueryTimeout().toSecondsPart());

            final int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected == 0) {
                LOG.info(appendEntries(Map.of("dataset_id", dataset.getId())), "Dataset already exists");
            }

        } catch (SQLException e) {
            throw new AccessRepositoryException("Could not create new dataset", e);
        }
    }

    public void addDatasetIfNotExists(final Dataset dataset) throws AccessRepositoryException {
        try (final Connection connection = dataSource.getConnection()) {

            addDatasetIfNotExists(connection, dataset);

        } catch (SQLException e) {
            throw new AccessRepositoryException("Could not create new dataset", e);
        }
    }

    public void addDatasetUserAccessIfNotExists(final User user, final Dataset dataset) throws AccessRepositoryException {

        try (
                final Connection connection = dataSource.getConnection();
                final PreparedStatement preparedStatement = connection.prepareStatement(CREATE_DATASET_USER_ACCESS)
        ) {

            addUserIfNotExists(connection, user);
            addDatasetIfNotExists(connection, dataset);

            preparedStatement.setString(1, user.getId());
            preparedStatement.setString(2, dataset.getId());
            preparedStatement.setQueryTimeout(this.config.getQueryTimeout().toSecondsPart());

            final int rowsAffected = preparedStatement.executeUpdate();

            LOG.info(appendEntries(Map.of("rows_affected", rowsAffected)), "Created access");

        } catch (SQLException e) {
            throw new AccessRepositoryException("Failed creating dataset access", e);
        }
    }

    static class AccessRepositoryException extends Exception {
        AccessRepositoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
