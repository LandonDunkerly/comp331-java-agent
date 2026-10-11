package edu.comp331.agent;

/**
 *
 * 
 */
public final class DurationFormatter {
    private DurationFormatter() {}

    /**
     *
     *
     *
     * @throws IllegalArgumentException if totalSeconds is negative
     */
    public static String format(long totalSeconds) {
        if (totalSeconds < 0) {
            throw new IllegalArgumentException("totalSeconds must be >= 0, got " + totalSeconds);
        }
        if (totalSeconds == 0) {
            return "0s";
        }
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s ");
        return sb.toString().trim();
    }
}
