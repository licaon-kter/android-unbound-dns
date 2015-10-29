/*
    Copyright (c) 2015 Marek Sebera <marek.sebera@gmail.com>

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package cz.msebera.unbound.dns;

import android.util.Log;

import java.io.File;
import java.util.Map;

public final class RunnableThread extends Thread {

    private final File workDir;
    private final File binDir;
    private final String binDirPath;
    private final String binName;
    private final String libDir;
    private final File binFile;
    private final String TAG;
    private final UnboundService.UnboundServiceCallback callback;

    public RunnableThread(UnboundService.UnboundServiceCallback callback, File workDir, String binName, String libDir) {
        this.workDir = workDir;
        this.binName = binName;
        this.libDir = libDir;
        this.binFile = new File(workDir, "bin/" + binName);
        this.TAG = "RunnableThread:" + binName;
        this.binDir = this.binFile.getParentFile();
        this.binDirPath = this.binDir.getAbsolutePath();
        this.callback = callback;
    }

    @Override
    public void run() {
        if (!binFile.exists()) {
            Log.e(TAG, "Bin File does not exist: " + binFile.getAbsolutePath());
            return;
        }
        if (!binFile.setExecutable(true, true)) {
            Log.e(TAG, "Could not set the binary as executable: " + binFile.getAbsolutePath());
            return;
        }
        if (this.binDir.isDirectory()) {
            for (File binary : this.binDir.listFiles()) {
                if (!binary.setExecutable(true)) {
                    Log.d(TAG, "Could not set the binary as executable: " + binary.getAbsolutePath());
                }
            }
        }

        ProcessBuilder pb = new ProcessBuilder(new File(this.binDir, "unbound-control-setup").getAbsolutePath());
        pb.redirectErrorStream(true);
        pb.directory(this.binDir);
        Map<String, String> env = pb.environment();
        env.put("PATH", env.get("PATH") + ":" + this.binDirPath);
        env.put("HOME", this.binDirPath);
        Process javap = null;
        try {
            javap = pb.start();
            StreamGobbler inputGobbler = new StreamGobbler(javap.getInputStream(), TAG);
            inputGobbler.start();
            javap.waitFor();
            inputGobbler.interrupt();
        } catch (Throwable t) {
            Log.e(TAG, "Error while executing", t);
        } finally {
            if (javap != null) {
                javap.destroy();
            }
        }

        pb = new ProcessBuilder(new File(this.binDir, "unbound-anchor").getAbsolutePath());
        pb.redirectErrorStream(true);
        pb.directory(this.binDir);
        env = pb.environment();
        env.put("PATH", env.get("PATH") + ":" + this.binDirPath);
        env.put("HOME", this.binDirPath);
        env.put("LD_LIBRARY_PATH", new File(workDir, libDir).getAbsolutePath());
        for (Map.Entry<String, String> envEntry : env.entrySet()) {
            Log.d(TAG, String.format("%s = %s", envEntry.getKey(), envEntry.getValue()));
        }
        try {
            javap = pb.start();
            StreamGobbler inputGobbler = new StreamGobbler(javap.getInputStream(), TAG);
            inputGobbler.start();
            javap.waitFor();
            inputGobbler.interrupt();
        } catch (Throwable t) {
            Log.e(TAG, "Error while executing", t);
        } finally {
            if (javap != null) {
                javap.destroy();
            }
        }

        pb = new ProcessBuilder(binFile.getAbsolutePath(), "-d", "-v", "-c", String.format("%s/unbound.conf", this.binDirPath));
        pb.redirectErrorStream(true);
        pb.directory(this.binDir);
        env = pb.environment();
        env.put("PATH", env.get("PATH") + ":" + this.binDirPath);
        env.put("HOME", this.binDirPath);
        for (Map.Entry<String, String> envEntry : env.entrySet()) {
            Log.d(TAG, String.format("%s = %s", envEntry.getKey(), envEntry.getValue()));
        }
        try {
            javap = pb.start();
            StreamGobbler inputGobbler = new StreamGobbler(javap.getInputStream(), TAG);
            inputGobbler.start();
            javap.waitFor();
            inputGobbler.interrupt();
        } catch (Throwable t) {
            Log.e(TAG, "Error while executing", t);
        } finally {
            if (javap != null) {
                javap.destroy();
            }
        }
        Log.e(TAG, "Process ended");
        if (callback != null) {
            callback.threadFinished();
        }
    }
}
