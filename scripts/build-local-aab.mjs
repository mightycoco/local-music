import { existsSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { isAbsolute, join, resolve } from "node:path";
import { spawnSync } from "node:child_process";

const requiredVariables = [
    "ANDROID_KEYSTORE_BASE64",
    "ANDROID_KEYSTORE_PASSWORD",
    "ANDROID_KEY_ALIAS",
    "ANDROID_KEY_PASSWORD",
];
const gradleWrapper = process.platform === "win32" ? "gradlew.bat" : "./gradlew";
const additionalTasks = process.argv.slice(2);
const environmentIsComplete = requiredVariables.every(
    (variable) => process.env[variable]?.trim(),
);
const environmentIsPartial = requiredVariables.some(
    (variable) => process.env[variable]?.trim(),
);

function readLocalGitConfig(name, fallback = "") {
    const result = spawnSync("git", ["config", "--local", "--get", name], {
        cwd: process.cwd(),
        encoding: "utf8",
        windowsHide: true,
    });
    return result.status === 0 ? result.stdout.trim() : fallback;
}

let temporaryDirectory;
let signingEnvironment;
const localPassword = readLocalGitConfig("localmusic.signing.password");

if (environmentIsComplete && !localPassword) {
    const encodedKeystore = process.env.ANDROID_KEYSTORE_BASE64.replace(/\s/g, "");
    const keystore = Buffer.from(encodedKeystore, "base64");

    if (
        keystore.length === 0 ||
        keystore.toString("base64").replace(/=+$/, "") !==
        encodedKeystore.replace(/=+$/, "")
    ) {
        console.error("ANDROID_KEYSTORE_BASE64 is not valid base64.");
        process.exit(1);
    }

    temporaryDirectory = mkdtempSync(join(tmpdir(), "local-music-signing-"));
    const keystorePath = join(temporaryDirectory, "local-music-release.p12");
    writeFileSync(keystorePath, keystore, { mode: 0o600 });
    signingEnvironment = {
        ANDROID_KEYSTORE_PATH: keystorePath,
        ANDROID_KEYSTORE_PASSWORD: process.env.ANDROID_KEYSTORE_PASSWORD,
        ANDROID_KEY_ALIAS: process.env.ANDROID_KEY_ALIAS,
        ANDROID_KEY_PASSWORD: process.env.ANDROID_KEY_PASSWORD,
    };
} else {
    if (!localPassword && environmentIsPartial) {
        const missingVariables = requiredVariables.filter(
            (variable) => !process.env[variable]?.trim(),
        );
        console.error(
            `Signing environment is incomplete; missing: ${missingVariables.join(", ")}`,
        );
        process.exit(1);
    }

    const configuredPath = readLocalGitConfig(
        "localmusic.signing.keystorePath",
        ".github/local-music-release.p12",
    );
    const keystorePath = isAbsolute(configuredPath)
        ? configuredPath
        : resolve(process.cwd(), configuredPath);
    const password = localPassword;
    const alias = readLocalGitConfig("localmusic.signing.keyAlias", "local-music");

    if (!existsSync(keystorePath)) {
        console.error(`Local signing keystore was not found: ${keystorePath}`);
        process.exit(1);
    }
    if (!password) {
        console.error(
            "Local signing password is not configured. Run: npm run configure:signing",
        );
        process.exit(1);
    }

    signingEnvironment = {
        ANDROID_KEYSTORE_PATH: keystorePath,
        ANDROID_KEYSTORE_PASSWORD: password,
        ANDROID_KEY_ALIAS: alias,
        ANDROID_KEY_PASSWORD: password,
    };
}

try {
    const result = spawnSync(
        gradleWrapper,
        ["testDebugUnitTest", "bundleRelease", ...additionalTasks],
        {
            cwd: process.cwd(),
            env: {
                ...process.env,
                ...signingEnvironment,
            },
            stdio: "inherit",
            shell: process.platform === "win32",
        },
    );

    if (result.error) {
        throw result.error;
    }

    process.exitCode = result.status ?? 1;
} finally {
    if (temporaryDirectory) {
        rmSync(temporaryDirectory, { recursive: true, force: true });
    }
}