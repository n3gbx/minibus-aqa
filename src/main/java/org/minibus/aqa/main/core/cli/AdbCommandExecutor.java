package org.minibus.aqa.main.core.cli;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.minibus.aqa.main.core.helpers.RandomHelper;
import org.minibus.aqa.main.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AdbCommandExecutor extends ShellCommandExecutor implements AdbCommand {
    private static final String ADB_DEVICES_PATTERN = "([a-zA-Z0-9\\\\-]+)\\t%s\\b";
    private static final String ADB_DEVICE_INFO_PATTERN = "(?:%s.*?)%s:(\\S+|$)";
    private static final String ADB_ALL_DEVICE_INFO_PATTERN = "(?:%s\\s+.*?\\s+)(.*)";

    public static File takeScreenshot() {
        return takeScreenshot(null);
    }

    public static File takeScreenshot(String udid) {
        String filePath = String.format("%s%s.%s", Constants.DEVICE_SDCARD, RandomHelper.temp(), Constants.PNG);
        adb(udid, SHELL, SCREENCAP, filePath);

        return new File(filePath);
    }

    public static File pull(String fileOrigin, String fileDest) {
        return pull(null, fileOrigin, fileDest);
    }

    public static File pull(String udid, String fromPath, String toPath) {
        adb(udid, PULL, fromPath, toPath);
        return new File(toPath);
    }

    public static boolean isDeviceConnected(String udid) {
        List<String> connectedDevices = getDevices(DeviceState.ONLINE);
        return connectedDevices.contains(udid);
    }

    public static int getDevicesCount() {
        String out = exec(ADB, DEVICES).getRawStdout();

        Pattern pattern = Pattern.compile(getAdbDevicesPattern());
        Matcher matcher = pattern.matcher(out);
        return (int) matcher.results().count();
    }

    public static int getDevicesCount(DeviceState state) {
        String out = exec(ADB, DEVICES).getRawStdout();

        Pattern pattern = Pattern.compile(getAdbDevicesPattern(state));
        Matcher matcher = pattern.matcher(out);
        return (int) matcher.results().count();
    }

    public static List<String> getDevices() {
        List<String> matches = new ArrayList<>();
        String out = exec(ADB, DEVICES).getRawStdout();

        Pattern pattern = Pattern.compile(getAdbDevicesPattern());
        Matcher matcher = pattern.matcher(out);

        while (matcher.find()) {
            matches.add(matcher.group(1));
        }

        return matches;
    }

    public static String getDevices(String delimiter) {
        return String.join(delimiter, getDevices());
    }

    public static List<String> getDevices(DeviceState state) {
        List<String> matches = new ArrayList<>();
        String out = exec(ADB, DEVICES).getRawStdout();

        Pattern pattern = Pattern.compile(getAdbDevicesPattern(state));
        Matcher matcher = pattern.matcher(out);

        while (matcher.find()) {
            matches.add(matcher.group(1));
        }

        return matches;
    }

    public static String getDevices(DeviceState state, String delimiter) {
        return String.join(delimiter, getDevices(state));
    }

    public static String getDeviceNameBySerial(String serial) {
        return adb(serial, "emu", "avd", "name")
                .getRawStdout()
                .split("\n")[0]
                .trim();
    }

    public static String getConnectedDeviceSerial(String deviceName) {
        List<String> connectedDevices = getDevices(DeviceState.ONLINE);
        return connectedDevices.stream().filter(s -> deviceName.equals(getDeviceNameBySerial(s))).findFirst().orElse(StringUtils.EMPTY);
    }

    public static DeviceState getDeviceState(String udid) {
        String state = adb(udid, STATE)
                .getRawStdout()
                .replace("\n", "")
                .trim();

        DeviceState deviceState = DeviceState.get(state);
        return deviceState != null ? deviceState : DeviceState.UNDEFINED;
    }

    public static String getDeviceInfo(String udid, DeviceInfo info) {
        String out = exec(ADB, DEVICES, "-l").getRawStdout();

        Pattern pattern = Pattern.compile(String.format(ADB_DEVICE_INFO_PATTERN, udid, info.get()));
        Matcher matcher = pattern.matcher(out);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return StringUtils.EMPTY;
        }
    }

    public static String getDeviceInfo(String udid) {
        String out = exec(ADB, DEVICES, "-l").getRawStdout();

        Pattern pattern = Pattern.compile(String.format(ADB_ALL_DEVICE_INFO_PATTERN, udid));
        Matcher matcher = pattern.matcher(out);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return StringUtils.EMPTY;
        }
    }

    public static String getDeviceVersion(String udid) {
        return adb(udid, SHELL, GETPROP, "ro.build.version.release")
                .getRawStdout()
                .replace("\n", "")
                .trim();
    }

    public static boolean startAdbd() {
        ShellCommandResult shellCommandResult = exec(ADB, START_ADBD);
        String out = shellCommandResult.getRawStdout();

        return shellCommandResult.getExitCode() != 1 && (out.isEmpty() || out.contains("successfully"));
    }

    public static boolean killAdbd() {
        ShellCommandResult shellCommandResult = exec(ADB, KILL_ADBD);
        return shellCommandResult.getExitCode() != 1 && !isAlive(ADB);
    }

    private static ShellCommandResult adb(String udid, String... cmdParts) {
        int devicesCount = getDevicesCount();

        if ((udid == null || udid.isEmpty()) && devicesCount == 1) {
            return exec(ArrayUtils.addAll(new String[] {ADB}, cmdParts));
        } else if ((udid == null || udid.isEmpty()) && devicesCount > 1) {
            String e = String.format("UDID is null, but number of connected devices is grater than 1: %s", getDevices(", "));
            throw new UnsupportedOperationException(e);
        } else if (devicesCount == 0) {
            throw new UnsupportedOperationException("Can't execute ADB, number of connected devices is 0");
        } else {
            return exec(ArrayUtils.addAll(new String[] {ADB, "-s", udid}, cmdParts));
        }
    }

    private static String getAdbDevicesPattern() {
        return String.format(ADB_DEVICES_PATTERN,
                Arrays.stream(DeviceState.values()).map(DeviceState::get).collect(Collectors.joining("|")));
    }

    private static String getAdbDevicesPattern(DeviceState state) {
        return String.format(ADB_DEVICES_PATTERN, state.get());
    }
}
