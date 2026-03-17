import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class DbMetricsSnapshot {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            throw new IllegalArgumentException("Usage: DbMetricsSnapshot <jdbcUrl> <username> <password> <outFile>");
        }
        String jdbcUrl = args[0];
        String username = args[1];
        String password = args[2];
        String outFile = args[3];

        Class.forName("com.mysql.cj.jdbc.Driver");
        Map<String, String> metrics = new LinkedHashMap<>();
        metrics.put("snapshot_time", String.valueOf(System.currentTimeMillis()));

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            queryStatus(conn, metrics, "Threads_connected");
            queryStatus(conn, metrics, "Threads_running");
            queryStatus(conn, metrics, "Questions");
            queryStatus(conn, metrics, "Slow_queries");
            queryStatus(conn, metrics, "Innodb_row_lock_time");
            queryStatus(conn, metrics, "Innodb_row_lock_waits");
            queryStatus(conn, metrics, "Innodb_buffer_pool_reads");
            queryStatus(conn, metrics, "Innodb_buffer_pool_read_requests");
            queryStatus(conn, metrics, "Com_select");
            queryStatus(conn, metrics, "Com_insert");
            queryStatus(conn, metrics, "Com_update");
            queryStatus(conn, metrics, "Com_delete");
            queryStatus(conn, metrics, "Created_tmp_disk_tables");
            queryStatus(conn, metrics, "Created_tmp_tables");
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(outFile))) {
            out.println("metric,value");
            for (Map.Entry<String, String> entry : metrics.entrySet()) {
                out.println(entry.getKey() + "," + entry.getValue());
            }
        }
    }

    private static void queryStatus(Connection conn, Map<String, String> metrics, String key) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SHOW GLOBAL STATUS LIKE ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    metrics.put(rs.getString("Variable_name"), rs.getString("Value"));
                } else {
                    metrics.put(key, "0");
                }
            }
        }
    }
}

