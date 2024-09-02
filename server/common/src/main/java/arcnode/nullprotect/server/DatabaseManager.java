package arcnode.nullprotect.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;

public class DatabaseManager {
    private final HikariDataSource src;

    public DatabaseManager(String jdbcUrl, String username, String password) {
        HikariConfig conf = new HikariConfig();
        conf.setJdbcUrl(jdbcUrl);
        conf.setUsername(username);
        conf.setPassword(password);

        this.src = configureHikari(conf);
        this.setup();
    }

    public DatabaseManager(String jdbcUrl) {
        HikariConfig conf = new HikariConfig();
        conf.setJdbcUrl(jdbcUrl);
        this.src = configureHikari(conf);
        this.setup();
    }

    private void setup() {
        try (Connection con = this.src.getConnection()) {
            Statement stmt = con.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS nullprot_hwid_bans(hwid varchar(32) PRIMARY KEY, exec varchar(24))");
        } catch (SQLException t) {
            throw new RuntimeException("Error initializing database", t);
        }
    }

    public void insertBan(String hwid, String by) throws SQLException {
        try (Connection con = this.src.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement("REPLACE INTO nullprot_hwid_bans(hwid, exec) VALUES(?, ?)")) {
                stmt.setString(1, hwid);
                stmt.setString(2, by);
                stmt.executeUpdate();
            }
        }
    }

    public String bannedBy(String hwid) throws SQLException {
        try (Connection con = this.src.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement("SELECT exec FROM nullprot_hwid_bans WHERE hwid = ?")) {
                stmt.setString(1, hwid);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next())
                        return rs.getString("exec");
                    else return null;
                }
            }
        }
    }

    private static HikariDataSource configureHikari(HikariConfig conf) {
        return new HikariDataSource(conf);
    }
}
