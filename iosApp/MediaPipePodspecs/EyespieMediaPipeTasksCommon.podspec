# Eyespie-pinned MediaPipe iOS distribution built from the immutable 0.10.26.1 release.
Pod::Spec.new do |spec|
  spec.name = "EyespieMediaPipeTasksCommon"
  spec.version = ENV.fetch("POD_VERSION", "0.10.26.1")
  spec.authors = "Google Inc."
  spec.license = { :type => "Apache", :file => "LICENSE" }
  spec.homepage = "https://github.com/ryjen/mediapipe"
  spec.summary = "MediaPipe Task Library - Common"
  spec.description = "The common libraries of the MediaPipe Task Library, built from upstream v0.10.26."
  spec.ios.deployment_target = "15.0"
  spec.module_name = "MediaPipeTasksCommon"
  spec.static_framework = true

  tag = ENV.fetch("POD_RELEASE_TAG", "eyespie-ios-v0.10.26.1")
  archive = "MediaPipeTasksCommon-#{spec.version}.tar.gz"
  spec.source = {
    :http => "https://github.com/ryjen/mediapipe/releases/download/#{tag}/#{archive}",
    :sha256 => "f884a5f47e0bbc4c53a7c1b440fb2b21966d0977b2f60ac15f2ce26eadfd8b88"
  }

  spec.pod_target_xcconfig = {
    "FRAMEWORK_SEARCH_PATHS[sdk=iphonesimulator*]" => "$(inherited) \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64_x86_64-simulator\"",
    "FRAMEWORK_SEARCH_PATHS[sdk=iphoneos*]" => "$(inherited) \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64\""
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
