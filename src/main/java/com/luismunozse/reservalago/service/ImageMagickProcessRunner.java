package com.luismunozse.reservalago.service;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class ImageMagickProcessRunner {

    public CommandResult run(List<String> command, Duration timeout) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).start();
            Process runningProcess = process;
            CompletableFuture<String> stdout = CompletableFuture.supplyAsync(() -> readStream(runningProcess.getInputStream()));
            CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> readStream(runningProcess.getErrorStream()));

            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CommandResult(-1, stdout.join(), stderr.join(), true);
            }

            return new CommandResult(process.exitValue(), stdout.join(), stderr.join(), false);
        } catch (IOException ex) {
            return new CommandResult(-1, "", ex.getMessage(), false);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            if (process != null) process.destroyForcibly();
            return new CommandResult(-1, "", "Interrupted", true);
        }
    }

    private String readStream(InputStream input) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            input.transferTo(output);
            return output.toString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    public record CommandResult(int exitCode, String stdout, String stderr, boolean timedOut) {
    }
}