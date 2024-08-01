import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TimeDifferenceCalculator {

    public static void main(String[] args) {
        // Sample records
        List<Record> records = List.of(
            new Record("Receiver1", "Dispatcher1", LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1)),
            new Record("Receiver1", "Dispatcher1", LocalDateTime.now().minusHours(4), LocalDateTime.now().minusHours(2)),
            new Record("Receiver1", "Dispatcher2", LocalDateTime.now().minusHours(6), LocalDateTime.now().minusHours(3)),
            new Record("Receiver2", "Dispatcher1", LocalDateTime.now().minusHours(3), LocalDateTime.now()),
            new Record("Receiver2", "Dispatcher1", LocalDateTime.now().minusHours(5), LocalDateTime.now().minusHours(1))
        );

        // Calculate min and max time differences for each (receiver, dispatcher) pair
        Map<String, Map<String, TimeStats>> timeDifferences = records.stream()
            .collect(Collectors.groupingBy(
                Record::getReceiver,
                Collectors.groupingBy(
                    Record::getDispatcher,
                    Collectors.collectingAndThen(
                        Collectors.mapping(
                            record -> Duration.between(record.getAvailableAt(), record.getDispatchedAt()).toMinutes(),
                            Collectors.teeing(
                                Collectors.minBy(Long::compareTo),
                                Collectors.maxBy(Long::compareTo),
                                (min, max) -> new TimeStats(min.orElse(0L), max.orElse(0L))
                            )
                        ),
                        stats -> stats
                    )
                )
            ));

        // Print results
        timeDifferences.forEach((receiver, dispatchers) -> {
            dispatchers.forEach((dispatcher, stats) -> {
                System.out.println("Receiver: " + receiver + ", Dispatcher: " + dispatcher +
                    ", Min Time: " + stats.getMinTime() + " minutes, Max Time: " + stats.getMaxTime() + " minutes");
            });
        });
    }
}

class TimeStats {
    private final long minTime;
    private final long maxTime;

    public TimeStats(long minTime, long maxTime) {
        this.minTime = minTime;
        this.maxTime = maxTime;
    }

    public long getMinTime() {
        return minTime;
    }

    public long getMaxTime() {
        return maxTime;
    }
}