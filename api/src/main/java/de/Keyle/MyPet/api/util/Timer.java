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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class Timer {
    private static final List<SchedulerManager.Task> timerIDs = new ArrayList<>();
    private static final List<Scheduler> tasksToSchedule = new ArrayList<>();

    // Folia ticks every pet and player on the thread that owns that entity, so each one needs its own task
    private static final Map<MyPet, SchedulerManager.Task> petTimers = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<MyPetPlayer, SchedulerManager.Task> playerTimers = Collections.synchronizedMap(new IdentityHashMap<>());

    private Timer() {
    }

    public static void stopTimer() {
        for (SchedulerManager.Task task : timerIDs) {
            task.cancel();
        }
        timerIDs.clear();
        synchronized (petTimers) {
            for (SchedulerManager.Task task : petTimers.values()) {
                task.cancel();
            }
            petTimers.clear();
        }
        synchronized (playerTimers) {
            for (SchedulerManager.Task task : playerTimers.values()) {
                task.cancel();
            }
            playerTimers.clear();
        }
    }

    public static void startTimer() {
        stopTimer();

        if (SchedulerManager.isFolia()) {
            // repository and skill tasks are global: they only touch their own state or the database
            timerIDs.add(SchedulerManager.runTimer(() -> {
                for (Scheduler skillTask : new ArrayList<>(tasksToSchedule)) {
                    skillTask.schedule();
                }
            }, 5L, 20L));
            for (MyPet myPet : MyPetApi.getMyPetManager().getAllActiveMyPets()) {
                startPetTimer(myPet);
            }
            for (MyPetPlayer player : MyPetApi.getPlayerManager().getMyPetPlayers()) {
                startPlayerTimer(player);
            }
            return;
        }

        timerIDs.add(SchedulerManager.runTimer(() -> {
            for (MyPet myPet : MyPetApi.getMyPetManager().getAllActiveMyPets()) {
                myPet.schedule();
            }
        }, 0L, 20L));
        timerIDs.add(SchedulerManager.runTimer(() -> {
            for (Scheduler task : tasksToSchedule) {
                task.schedule();
            }
        }, 5L, 20L));
        timerIDs.add(SchedulerManager.runTimer(() -> {
            for (MyPetPlayer player : MyPetApi.getPlayerManager().getMyPetPlayers()) {
                player.schedule();
            }
        }, 10L, 20L));
    }

    /**
     * Starts the one-second tick of a single pet. On Folia the tick runs on the region thread of the pet owner,
     * because the pet itself has no entity while it is despawned or dead but still needs to respawn.
     */
    public static void startPetTimer(MyPet myPet) {
        if (!SchedulerManager.isFolia() || myPet == null) {
            return;
        }
        MyPetPlayer owner = myPet.getOwner();
        Player player = owner != null && owner.isOnline() ? owner.getPlayer() : null;
        if (player == null) {
            return;
        }
        stopPetTimer(myPet);
        petTimers.put(myPet, SchedulerManager.runTimerAtEntity(player, myPet::schedule, 0L, 20L));
    }

    public static void stopPetTimer(MyPet myPet) {
        if (!petTimers.isEmpty()) {
            SchedulerManager.Task task = petTimers.remove(myPet);
            if (task != null) {
                task.cancel();
            }
        }
    }

    public static void startPlayerTimer(MyPetPlayer myPetPlayer) {
        if (!SchedulerManager.isFolia() || myPetPlayer == null || !myPetPlayer.isOnline()) {
            return;
        }
        Player player = myPetPlayer.getPlayer();
        if (player == null) {
            return;
        }
        stopPlayerTimer(myPetPlayer);
        playerTimers.put(myPetPlayer, SchedulerManager.runTimerAtEntity(player, myPetPlayer::schedule, 0L, 20L));
        if (myPetPlayer.hasMyPet()) {
            startPetTimer(myPetPlayer.getMyPet());
        }
    }

    public static void stopPlayerTimer(MyPetPlayer myPetPlayer) {
        if (!playerTimers.isEmpty()) {
            SchedulerManager.Task task = playerTimers.remove(myPetPlayer);
            if (task != null) {
                task.cancel();
            }
        }
    }

    public static void reset() {
        tasksToSchedule.clear();
        stopTimer();
    }

    public static void addTask(Scheduler task) {
        tasksToSchedule.add(task);
    }

    public static void removeTask(Scheduler task) {
        tasksToSchedule.remove(task);
    }
}
