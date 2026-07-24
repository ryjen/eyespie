plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("model_pack")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}

val verifyModelArtifact by tasks.registering(Exec::class) {
    group = "verification"
    description = "Verify the staged MediaPipe model against its manifest and Android descriptor."
    workingDir = rootDir
    commandLine(
        "python3",
        "scripts/stage_android_model_artifact.py",
        "verify",
        "--require-artifact",
    )
}

gradle.projectsEvaluated {
    project(":app").tasks.matching {
        name.startsWith("bundle") && !name.contains("Debug", ignoreCase = true)
    }.configureEach {
        dependsOn(verifyModelArtifact)
    }
}
