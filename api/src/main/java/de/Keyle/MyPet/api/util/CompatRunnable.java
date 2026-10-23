/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.util;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Drop-in replacement for {@link BukkitRunnable} that schedules through {@link SchedulerManager} instead of the
 * Bukkit scheduler, so the same code keeps working on regionised servers (Folia) where {@code BukkitScheduler}
 * does not exist. On a regular Paper server it behaves exactly like {@link BukkitRunnable}.
 */
public abstract class CompatRunnable extends BukkitRunnable {

    private final List<SchedulerManager.Task> handles = new ArrayList<>(1);
    /** Entity whose region thread should run this task on Folia; {@code null} means the global region. */
    private final Entity anchor;

    public CompatRunnable() {
        this.anchor = null;
    }

    public CompatRunnable(Entity anchor) {
        this.anchor = anchor;
    }

    @Override
    public BukkitTask runTask(Plugin plugin) {
        if (!SchedulerManager.isFolia()) {
            return super.runTask(plugin);
        }
        return track(anchor != null ? SchedulerManager.runAtEntity(anchor, this) : SchedulerManager.run(this), plugin, true);
    }

    @Override
    public BukkitTask runTaskLater(Plugin plugin, long delay) {
        if (!SchedulerManager.isFolia()) {
            return super.runTaskLater(plugin, delay);
        }
        return track(anchor != null ? SchedulerManager.runAtEntityLater(anchor, this, delay) : SchedulerManager.runLater(this, delay), plugin, true);
    }

    @Override
    public BukkitTask runTaskTimer(Plugin plugin, long delay, long period) {
        if (!SchedulerManager.isFolia()) {
            return super.runTaskTimer(plugin, delay, period);
        }
        return track(anchor != null ? SchedulerManager.runTimerAtEntity(anchor, this, delay, period) : SchedulerManager.runTimer(this, delay, period), plugin, true);
    }

    @Override
    public BukkitTask runTaskAsynchronously(Plugin plugin) {
        if (!SchedulerManager.isFolia()) {
            return super.runTaskAsynchronously(plugin);
        }
        return track(SchedulerManager.runAsync(this), plugin, false);
    }

    @Override
    public BukkitTask runTaskLaterAsynchronously(Plugin plugin, long delay) {
        if (!SchedulerManager.isFolia()) {
            return super.runTaskLaterAsynchronously(plugin, delay);
        }
        return track(SchedulerManager.runAsyncLater(this, delay), plugin, false);
    }

    @Override
    public BukkitTask runTaskTimerAsynchronously(Plugin plugin, long delay, long period) {
        if (!SchedulerManager.isFolia()) {
            return super.runTaskTimerAsynchronously(plugin, delay, period);
        }
        return track(SchedulerManager.runAsyncTimer(this, delay, period), plugin, false);
    }

    @Override
    public void cancel() {
        if (!SchedulerManager.isFolia()) {
            super.cancel();
            return;
        }
        synchronized (handles) {
            for (SchedulerManager.Task handle : handles) {
                handle.cancel();
            }
            handles.clear();
        }
    }

    private BukkitTask track(SchedulerManager.Task handle, Plugin plugin, boolean sync) {
        CompatTask task = new CompatTask(handle, plugin, sync);
        synchronized (handles) {
            handles.add(handle);
        }
        return task;
    }

    /**
     * Task handle returned on Folia, where no {@link BukkitTask} is produced by the scheduler.
     */
    private static final class CompatTask implements BukkitTask {
        private final SchedulerManager.Task handle;
        private final Plugin owner;
        private final boolean sync;
        private boolean cancelled = false;

        private CompatTask(SchedulerManager.Task handle, Plugin owner, boolean sync) {
            this.handle = handle;
            this.owner = owner;
            this.sync = sync;
        }

        @Override
        public int getTaskId() {
            return -1;
        }

        @Override
        public Plugin getOwner() {
            return owner;
        }

        @Override
        public boolean isSync() {
            return sync;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void cancel() {
            cancelled = true;
            handle.cancel();
        }

        public void run() {
            // the wrapped task is executed by the server scheduler, nothing to do here
        }
    }
}
