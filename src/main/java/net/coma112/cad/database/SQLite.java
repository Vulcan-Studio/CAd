package net.coma112.cad.database;

import lombok.Getter;
import net.coma112.cad.CAd;
import net.coma112.cad.events.AdCreateEvent;
import net.coma112.cad.events.AdRemoveEvent;
import net.coma112.cad.manager.Advertisement;
import net.coma112.cad.utils.AdLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class SQLite extends AbstractDatabase {
    private final Connection connection;

    public SQLite() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        File dataFolder = new File(CAd.getInstance().getDataFolder(), "ads.db");
        String url = "jdbc:sqlite:" + dataFolder;
        connection = DriverManager.getConnection(url);
    }

    @Override
    public void createTable() {
        String query = "CREATE TABLE IF NOT EXISTS ads (ID INTEGER PRIMARY KEY, PLAYER VARCHAR(255) NOT NULL, TITLE VARCHAR(2048) NOT NULL, DESCRIPTION VARCHAR(2048) NOT NULL, STARTING_DATE DATETIME, ENDING_DATE DATETIME)";

        try (PreparedStatement preparedStatement = getConnection().prepareStatement(query)) {
            preparedStatement.execute();
        } catch (SQLException exception) {
            AdLogger.error(exception.getMessage());
        }
    }

    @Override
    public void createAdvertisement(@NotNull Player player, @NotNull String title, @NotNull String description, @NotNull String startingDate, @NotNull String endingDate) {
        String insertQuery = "INSERT INTO ads (PLAYER, TITLE, DESCRIPTION, STARTING_DATE, ENDING_DATE) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = getConnection().prepareStatement(insertQuery)) {
            preparedStatement.setString(1, player.getName());
            preparedStatement.setString(2, title);
            preparedStatement.setString(3, description);
            preparedStatement.setString(4, startingDate);
            preparedStatement.setString(5, endingDate);
            preparedStatement.execute();

            CAd.getInstance().getServer().getPluginManager().callEvent(new AdCreateEvent(player.getName(), title, description, endingDate));
        } catch (SQLException exception) {
            AdLogger.error(exception.getMessage());
        }
    }

    @Override
    public int getAdvertisementsCount(@NotNull Player player) {
        String query = "SELECT COUNT(*) AS count FROM ads WHERE PLAYER = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, player.getName());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) return resultSet.getInt("count");
        } catch (SQLException exception) {
            AdLogger.error(exception.getMessage());
        }
        return 0;
    }

    @Override
    public boolean exists(int id) {
        String query = "SELECT * FROM ads WHERE ID = ?";

        try {
            try (PreparedStatement preparedStatement = getConnection().prepareStatement(query)) {
                preparedStatement.setInt(1, id);

                ResultSet resultSet = preparedStatement.executeQuery();
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void deleteAdvertisement(int id) {
        String selectQuery = "SELECT * FROM ads WHERE ID = ?";
        String deleteQuery = "DELETE FROM ads WHERE ID = ?";

        try (PreparedStatement selectStatement = getConnection().prepareStatement(selectQuery)) {
            selectStatement.setInt(1, id);
            ResultSet resultSet = selectStatement.executeQuery();

            if (resultSet.next()) {
                int adID = resultSet.getInt("ID");
                String player = resultSet.getString("PLAYER");
                String title = resultSet.getString("TITLE");
                String description = resultSet.getString("DESCRIPTION");
                String startingDate = resultSet.getString("STARTING_DATE");
                String endingDate = resultSet.getString("ENDING_DATE");

                CAd.getInstance().getServer().getPluginManager().callEvent(new AdRemoveEvent(adID, player, title, description, startingDate, endingDate));
            }

            try (PreparedStatement deleteStatement = getConnection().prepareStatement(deleteQuery)) {
                deleteStatement.setInt(1, id);
                deleteStatement.executeUpdate();
            }

        } catch (SQLException exception) {
            AdLogger.error(exception.getMessage());
        }
    }

    @Override
    public void deleteExpiredAdvertisements() {
        String deleteQuery = "DELETE FROM ads WHERE ENDING_DATE <= CURRENT_TIMESTAMP";

        try (PreparedStatement preparedStatement = getConnection().prepareStatement(deleteQuery)) {
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            AdLogger.error(exception.getMessage());
        }
    }

    @Override
    public List<Advertisement> getAdvertisements() {
        List<Advertisement> advertisements = new ArrayList<>();
        String query = "SELECT * FROM ads";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                int id = resultSet.getInt("ID");
                String player = resultSet.getString("PLAYER");
                String title = resultSet.getString("TITLE");
                String description = resultSet.getString("DESCRIPTION");
                String startingDate = resultSet.getString("STARTING_DATE");
                String endingDate = resultSet.getString("ENDING_DATE");

                advertisements.add(new Advertisement(id, player, title, description, startingDate, endingDate));
            }
        } catch (SQLException exception) {
            AdLogger.error(exception.getMessage());
        }

        return advertisements;
    }

    @Override
    public List<Advertisement> getAdvertisementsFromPlayer(@NotNull Player playerToGet) {
        List<Advertisement> advertisements = new ArrayList<>();
        String query = "SELECT * FROM ads WHERE PLAYER = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, playerToGet.getName());

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                int id = resultSet.getInt("ID");
                String player = resultSet.getString("PLAYER");
                String title = resultSet.getString("TITLE");
                String description = resultSet.getString("DESCRIPTION");
                String startingDate = resultSet.getString("STARTING_DATE");
                String endingDate = resultSet.getString("ENDING_DATE");

                advertisements.add(new Advertisement(id, player, title, description, startingDate, endingDate));
            }
        } catch (SQLException exception) {
            AdLogger.error(exception.getMessage());
        }

        return advertisements;
    }

    @Override
    public boolean isConnected() {
        return connection != null;
    }

    @Override
    public void disconnect() {
        if (isConnected()) {
            try {
                connection.close();
            } catch (SQLException exception) {
                AdLogger.error(exception.getMessage());
            }
        }
    }
}
