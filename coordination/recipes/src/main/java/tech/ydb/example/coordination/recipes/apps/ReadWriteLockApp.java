package tech.ydb.example.coordination.recipes.apps;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.example.coordination.recipes.lib.locks.ReadWriteInterProcessLock;

import java.time.Duration;
import java.util.Scanner;

public class ReadWriteLockApp {
    ReadWriteInterProcessLock lock;

    public ReadWriteLockApp(CoordinationClient client) {
        client.createNode("examples/app").join().expectSuccess("cannot create coordination path");
        lock = new ReadWriteInterProcessLock(
                client,
                "examples/app",
                "default_lock"
        );
    }

    public void readAcquire(Duration timeout) {
        try {
            if (timeout == null) {
                lock.readLock().acquire();
            } else {
                lock.readLock().acquire(timeout);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void releaseRead() {
        try {
            lock.readLock().release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isAcquiredRead() {
        return lock.readLock().isAcquiredInThisProcess();
    }

    public void writeAcquire(Duration timeout) {
        try {
            if (timeout == null) {
                lock.writeLock().acquire();
            } else {
                lock.writeLock().acquire(timeout);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void releaseWrite() {
        try {
            lock.writeLock().release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isAcquiredWrite() {
        return lock.writeLock().isAcquiredInThisProcess();
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter commands: r/w lock [seconds] | release | ?");

        while (scanner.hasNextLine()) {
            String commandLine = scanner.nextLine().trim();
            String[] commandParts = commandLine.split("\\s+");
            if (commandParts.length < 2) {
                System.out.println("Not enough arguments");
                continue;
            }
            String type = commandParts[0];
            String command = commandParts[1];

            switch (command.toLowerCase()) {
                case "lock":
                    int seconds = -1;
                    if (commandParts.length > 2) {
                        try {
                            seconds = Integer.parseInt(commandParts[2]);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid number format, defaulting to 0 seconds");
                        }
                    }
                    if (type.equals("r")) {
                        if (seconds == -1) {
                            readAcquire(null);
                        } else {
                            readAcquire(Duration.ofSeconds(seconds));
                        }
                    } else if (type.equals("w")) {
                        if (seconds == -1) {
                            writeAcquire(null);
                        } else {
                            writeAcquire(Duration.ofSeconds(seconds));
                        }
                    } else {
                        System.out.println("Unknown type: " + type);
                    }
                    break;
                case "release":
                    if (type.equals("r")) {
                        releaseRead();
                    } else if (type.equals("w")) {
                        releaseWrite();
                    } else {
                        System.out.println("Unknown type: " + type);
                    }
                    break;
                case "?":
                    if (type.equals("r")) {
                        System.out.println("Read lock is acquired: " + isAcquiredRead());
                    } else if (type.equals("w")) {
                        System.out.println("Write lock is acquired: " + isAcquiredWrite());
                    } else {
                        System.out.println("Unknown type: " + type);
                    }
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
