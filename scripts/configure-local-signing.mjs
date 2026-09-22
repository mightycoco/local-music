import { existsSync } from "node:fs";
import { resolve } from "node:path";
import { spawnSync } from "node:child_process";
import { createInterface } from "node:readline/promises";

const keystorePath = ".github/local-music-release.p12";
const keyAlias = "local-music";
const absoluteKeystorePath = resolve(process.cwd(), keystorePath);

if (!existsSync(absoluteKeystorePath)) {
    console.error(`Signing keystore was not found: ${absoluteKeystorePath}`);
    process.exit(1);
}

if (!process.stdin.isTTY || !process.stdout.isTTY) {
    console.error("Run this command in an interactive terminal.");
    process.exit(1);
}

function runGitConfig(name, value) {
    const result = spawnSync(
        "git",
        ["config", "--local", "--replace-all", name, value],
        {
            cwd: process.cwd(),
            stdio: ["ignore", "inherit", "inherit"],
            windowsHide: true,
        },
    );
    if (result.error) {
        throw result.error;
    }
    if (result.status !== 0) {
        throw new Error(`git config failed for ${name}`);
    }
}

async function readHidden(prompt) {
    process.stdout.write(prompt);
    process.stdin.setRawMode(true);
    process.stdin.resume();
    process.stdin.setEncoding("utf8");

    return new Promise((resolvePassword, reject) => {
        let password = "";

        function finish() {
            process.stdin.setRawMode(false);
            process.stdin.pause();
            process.stdin.removeListener("data", onData);
            process.stdout.write("\n");
            resolvePassword(password);
        }

        function onData(character) {
            if (character === "\u0003") {
                process.stdin.setRawMode(false);
                process.stdin.pause();
                process.stdout.write("\n");
                reject(new Error("Configuration cancelled."));
                return;
            }
            if (character === "\r" || character === "\n") {
                finish();
                return;
            }
            if (character === "\u007f" || character === "\b") {
                password = password.slice(0, -1);
                return;
            }
            password += character;
        }

        process.stdin.on("data", onData);
    });
}

const readline = createInterface({ input: process.stdin, output: process.stdout });

try {
    readline.close();
    const password = await readHidden("Keystore password: ");
    if (!password) {
        throw new Error("The keystore password cannot be empty.");
    }
    const confirmation = await readHidden("Confirm password: ");
    if (password !== confirmation) {
        throw new Error("The passwords do not match.");
    }

    runGitConfig("localmusic.signing.keystorePath", keystorePath);
    runGitConfig("localmusic.signing.keyAlias", keyAlias);
    runGitConfig("localmusic.signing.password", password);
    console.log("Local signing configuration saved in .git/config.");
    console.log("Run npm run build:aab to build the signed release bundle.");
} catch (error) {
    console.error(error.message);
    process.exitCode = 1;
}