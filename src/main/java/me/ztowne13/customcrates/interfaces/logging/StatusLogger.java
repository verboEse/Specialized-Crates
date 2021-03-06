package me.ztowne13.customcrates.interfaces.logging;

import me.ztowne13.customcrates.SettingsValue;
import me.ztowne13.customcrates.SpecializedCrates;
import me.ztowne13.customcrates.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusLogger {
    SpecializedCrates cc;

    int failures;

    Map<String, List<String>> completedEvents = new HashMap<>();
    Map<String, List<String>> failedEvents = new HashMap<>();

    public StatusLogger(SpecializedCrates cc) {
        this.cc = cc;
    }

    public void addEvent(boolean success, String section, String event, String cause) {
        failures += success ? 0 : 1;
        Map<String, List<String>> map = success ? getCompletedEvents() : getFailedEvents();
        List<String> list = map.getOrDefault(section, new ArrayList<>());
        list.add(event + "%CAUSE%" + cause);
        map.put(section, list);
    }

    public void logAll() {
        logAll(Bukkit.getConsoleSender(), false);
    }

    public void logAll(CommandSender sender, boolean forceOnlyFailures) {
        ArrayList<String> hasLogged = new ArrayList<>();
        for (String s : getCompletedEvents().keySet()) {
            if (!hasLogged.contains(s.toUpperCase())) {
                logSection(sender, s, forceOnlyFailures);
                hasLogged.add(s.toUpperCase());
            }
        }

        for (String s : getFailedEvents().keySet()) {
            if (!hasLogged.contains(s.toUpperCase())) {
                logSection(sender, s, forceOnlyFailures);
                hasLogged.add(s.toUpperCase());
            }
        }

        if (getFailedEvents().isEmpty() &&
                SettingsValue.LOG_SUCCESSES.getValue(getCc()).toString().equalsIgnoreCase("FAILURES")) {
            ChatUtils.log("  &a+&f Success: there were no issues.");
        }
    }

    public void logSection(CommandSender sender, String section, boolean forceOnlyFailures) {
        boolean hasLoggedHeader = false;
        String toLog = SettingsValue.LOG_SUCCESSES.getValue(getCc()).toString();

        if (!toLog.equalsIgnoreCase("NOTHING") || forceOnlyFailures) {
            if (!toLog.equalsIgnoreCase("FAILURES") && !forceOnlyFailures) {
                hasLoggedHeader = true;
                logValue(sender, "  " + section);
                for (String checkSec : getCompletedEvents().keySet()) {
                    if (checkSec.equalsIgnoreCase(section)) {
                        for (String s : getCompletedEvents().get(checkSec)) {
                            String[] split = s.split("%CAUSE%");
                            String event = split[0];
                            logValue(sender, "    &a+&f " + event);
                        }
                    }
                }
            }

            for (String checkSec : getFailedEvents().keySet()) {
                if (checkSec.equalsIgnoreCase(section)) {
                    if (!hasLoggedHeader) {
                        logValue(sender, "  " + section);
                        hasLoggedHeader = true;
                    }

                    for (String s : getFailedEvents().get(checkSec)) {
                        String[] parsedEvent = s.split("%CAUSE%");
                        String event = parsedEvent[0];
                        String cause = parsedEvent[1];

                        logValue(sender, "    &c-&f " + event);

                        if (cause.equalsIgnoreCase("NONE")) {
                            continue;
                        }

                        logValue(sender, "        &7CAUSE: " + cause);
                    }
                }
            }
        }
    }

    public void logValue(CommandSender sender, String s) {
        if (sender instanceof ConsoleCommandSender) {
            ChatUtils.log(s);
            return;
        }
        sender.sendMessage(ChatUtils.toChatColor(s));
    }

    public Map<String, List<String>> getCompletedEvents() {
        return completedEvents;
    }

    public void setCompletedEvents(Map<String, List<String>> completedEvents) {
        this.completedEvents = completedEvents;
    }

    public Map<String, List<String>> getFailedEvents() {
        return failedEvents;
    }

    public void setFailedEvents(Map<String, List<String>> failedEvents) {
        this.failedEvents = failedEvents;
    }

    public SpecializedCrates getCc() {
        return cc;
    }

    public void setCc(SpecializedCrates cc) {
        this.cc = cc;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }
}
