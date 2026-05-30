package dev.lu15.voicechat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * Per-player leaky-bucket rate limiter for the Minecraft-channel voice chat
 * packets (group create/join/leave, state changes). Mirrors upstream Simple
 * Voice Chat. Does not apply to UDP voice packets, which are legitimately
 * high-frequency.
 */
final class PacketRateLimiter {

    private static final int TIME_WINDOW_SECONDS = 5;

    private final @NotNull Map<UUID, Bucket> buckets = new ConcurrentHashMap<>();
    private final int maxPacketsPerSecond;

    PacketRateLimiter(int maxPacketsPerSecond) {
        this.maxPacketsPerSecond = maxPacketsPerSecond;
    }

    /** @return {@code true} if the player may send another packet right now */
    boolean allow(@NotNull UUID player) {
        if (this.maxPacketsPerSecond <= 0) return true; // disabled
        Bucket bucket = this.buckets.computeIfAbsent(player,
                id -> new Bucket(this.maxPacketsPerSecond * TIME_WINDOW_SECONDS, 1000L * TIME_WINDOW_SECONDS));
        return bucket.tryAcquire();
    }

    void remove(@NotNull UUID player) {
        this.buckets.remove(player);
    }

    private static final class Bucket {
        private final int threshold;
        private final long timePerTokenNanos;
        private long lastLeakNanos;
        private long amount;

        Bucket(int threshold, long windowMillis) {
            this.threshold = threshold;
            this.timePerTokenNanos = (windowMillis * 1_000_000L) / threshold;
            this.lastLeakNanos = System.nanoTime();
        }

        synchronized boolean tryAcquire() {
            long now = System.nanoTime();
            long leaked = (now - this.lastLeakNanos) / this.timePerTokenNanos;
            if (leaked > 0) {
                this.amount = Math.max(0L, this.amount - leaked);
                this.lastLeakNanos += leaked * this.timePerTokenNanos;
            }
            if (this.amount >= this.threshold) return false;
            this.amount++;
            return true;
        }
    }

}
