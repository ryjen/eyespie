# Eyespie-pinned MediaPipe iOS distribution built from the immutable 0.10.26.2 release.
Pod::Spec.new do |spec|
  spec.name = "EyespieMediaPipeTasksCommon"
  spec.version = ENV.fetch("POD_VERSION", "0.10.26.2")
  spec.authors = "Google Inc."
  spec.license = { :type => "Apache", :file => "LICENSE" }
  spec.homepage = "https://github.com/ryjen/mediapipe"
  spec.summary = "MediaPipe Task Library - Common"
  spec.description = "The common libraries of the MediaPipe Task Library, built from upstream v0.10.26."
  spec.ios.deployment_target = "15.0"
  spec.module_name = "MediaPipeTasksCommon"
  spec.static_framework = true

  tag = ENV.fetch("POD_RELEASE_TAG", "eyespie-ios-v0.10.26.2")
  archive = "MediaPipeTasksCommon-#{spec.version}.tar.gz"
  spec.source = {
    :http => "https://github.com/ryjen/mediapipe/releases/download/#{tag}/#{archive}",
    :sha256 => "caa9b7000ccba761751ebbfb0b5cee1dbba4717ab69a6f64a0fb46d7d8e40c62"
  }

  spec.pod_target_xcconfig = {
    "FRAMEWORK_SEARCH_PATHS" => "$(inherited) \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64_x86_64-simulator\" \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64\"",
    "FRAMEWORK_SEARCH_PATHS[sdk=iphonesimulator*]" => "$(inherited) \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64_x86_64-simulator\"",
    "FRAMEWORK_SEARCH_PATHS[sdk=iphoneos*]" => "$(inherited) \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64\"",
    "OTHER_CFLAGS[sdk=iphonesimulator*]" => "$(inherited) -fmodules -F\"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64_x86_64-simulator\"",
    "OTHER_CFLAGS[sdk=iphoneos*]" => "$(inherited) -fmodules -F\"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64\""
  }
  spec.user_target_xcconfig = {
    "OTHER_LDFLAGS[sdk=iphonesimulator*]" => "$(inherited) -force_load \"$(PODS_ROOT)/EyespieMediaPipeTasksCommon/frameworks/graph_libraries/libMediaPipeTasksCommon_simulator_graph.a\"",
    "OTHER_LDFLAGS[sdk=iphoneos*]" => "$(inherited) -force_load \"$(PODS_ROOT)/EyespieMediaPipeTasksCommon/frameworks/graph_libraries/libMediaPipeTasksCommon_device_graph.a\""
  }
  spec.frameworks = "Accelerate", "CoreMedia", "AssetsLibrary", "CoreFoundation", "CoreGraphics", "CoreImage", "QuartzCore", "AVFoundation", "CoreVideo", "UIKit"
  spec.preserve_paths = "frameworks/graph_libraries/*.a"
  spec.library = "c++"
  spec.vendored_frameworks = "frameworks/MediaPipeTasksCommon.xcframework"
end
