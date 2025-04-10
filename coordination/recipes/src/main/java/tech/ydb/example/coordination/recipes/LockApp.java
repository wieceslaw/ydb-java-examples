package tech.ydb.example.coordination.recipes;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.example.coordination.recipes.lib.locks.LockInternals;

import java.time.Duration;
import java.util.Scanner;

public class LockApp {

    LockInternals lock;

    LockApp(CoordinationClient client) {
        client.createNode("examples/app").join().expectSuccess("cannot create coordination path");
        lock = new LockInternals(
                client,
                "examples/app",
                "default_lock"
        );
        lock.start();
    }

    public void lock(Duration timeout, boolean exclusive) {
        try {
            lock.tryAcquire(
                    null,
                    exclusive,
                    null
            );
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

    private boolean isAcquired() {
        return lock.isAcquired();
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
                    boolean exclusive = false;
                    if (commandParts.length > 1) {
                        try {
                            seconds = Integer.parseInt(commandParts[1]);
                            exclusive = Boolean.parseBoolean(commandParts[2]);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid number format, defaulting to 0 seconds");
                        }
                    }
                    if (seconds == -1) {
                        lock(null, exclusive);
                    } else {
                        lock(Duration.ofSeconds(seconds), exclusive);
                    }
                    break;
                case "release":
                    release();
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

    public void close() {
        lock.close();
    }
}

