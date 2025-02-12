package tech.ydb.coordination.recipes.example;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.recipes.example.lib.lock.InterProcessLock;
import tech.ydb.coordination.recipes.example.lib.lock.InterProcessMutex;

import java.time.Duration;
import java.util.Scanner;

public class LockApp {

    CoordinationSession session;
    InterProcessLock lock;

    LockApp(CoordinationClient client) {
        client.createNode("examples/app").join().expectSuccess("cannot create coordination path");
        session = client.createSession("examples/app");
        session.connect().join().expectSuccess("cannot start coordination session");
        lock = new InterProcessMutex(
                session,
                "data".getBytes(),
                "default_lock"
        );
    }

    public void lock(Duration duration) {
        try {
            if (duration == null) {
                lock.acquire();
            } else {
                lock.acquire(duration);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void release() {
        try {
            lock.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reconnect() {
        session.connect().join().expectSuccess("cannot start coordination session");
    }

    private boolean isAcquired() {
        return lock.isAcquiredInThisProcess();
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter commands: lock [seconds] | release | reconnect | ?");

        while (scanner.hasNextLine()) {
            String commandLine = scanner.nextLine().trim();
            String[] commandParts = commandLine.split("\\s+");
            String command = commandParts[0];

            switch (command.toLowerCase()) {
                case "lock":
                    int seconds = -1;
                    if (commandParts.length > 1) {
                        try {
                            seconds = Integer.parseInt(commandParts[1]);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid number format, defaulting to 0 seconds");
                        }
                    }
                    if (seconds == -1) {
                        lock(null);
                    } else {
                        lock(Duration.ofSeconds(seconds));
                    }
                    break;
                case "release":
                    release();
                    break;
                case "reconnect":
                    reconnect();
                    break;
                case "?":
                    System.out.println("Lock is acquired: " + isAcquired());
                    break;
                default:
                    System.out.println("Unknown command: " + command);
            }
        }

        scanner.close();
    }

}

