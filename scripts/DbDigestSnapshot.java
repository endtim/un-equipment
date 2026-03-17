import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DbDigestSnapshot {

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            throw new IllegalArgumentException(
                "Usage: DbDigestSnapshot <jdbcUrl> <username> <password> <limit> <outFile>");
        }
        String jdbcUrl = args[0];
        String username = args[1];
        String password = args[2];
        int limit = Integer.parseInt(args[3]);
        String outFile = args[4];

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            try (PrintWriter out = new PrintWriter(new FileWriter(outFile))) {
                out.println("digest,digest_text,count_star,sum_timer_wait,sum_lock_time,sum_rows_examined,sum_rows_sent");
                if (!isPerformanceSchemaEnabled(conn)) {
                    return;
                }
                snapshotDigest(conn, limit, out);
            }
        }
    }

    private static boolean isPerformanceSchemaEnabled(Connection conn) {
        String sql = "SHOW VARIABLES LIKE 'performance_schema'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String v = rs.getString("Value");
                return v != null && "ON".equalsIgnoreCase(v.trim());
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private static void snapshotDigest(Connection conn, int limit, PrintWriter out) throws Exception {
        String sql = "SELECT DIGEST, DIGEST_TEXT, COUNT_STAR, SUM_TIMER_WAIT, SUM_LOCK_TIME, "
            + "SUM_ROWS_EXAMINED, SUM_ROWS_SENT "
            + "FROM performance_schema.events_statements_summary_by_digest "
            + "WHERE DIGEST IS NOT NULL "
            + "ORDER BY SUM_TIMER_WAIT DESC "
            + "LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.print(csv(rs.getString("DIGEST")));
                    out.print(",");
                    out.print(csv(rs.getString("DIGEST_TEXT")));
                    out.print(",");
                    out.print(rs.getLong("COUNT_STAR"));
                    out.print(",");
                    out.print(rs.getLong("SUM_TIMER_WAIT"));
                    out.print(",");
                    out.print(rs.getLong("SUM_LOCK_TIME"));
                    out.print(",");
                    out.print(rs.getLong("SUM_ROWS_EXAMINED"));
                    out.print(",");
                    out.println(rs.getLong("SUM_ROWS_SENT"));
                }
            }
        }
    }

    private static String csv(String value) {
        if (value == null) {
            return "\"\"";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}

