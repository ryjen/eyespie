Pod::Spec.new do |spec|
  spec.name = "EyespieMediaPipeTasksVision"
  spec.version = ENV.fetch("POD_VERSION", "0.10.26.2")
  spec.authors = "Google Inc."
  spec.license = { :type => "Apache", :file => "LICENSE" }
  spec.homepage = "https://github.com/ryjen/mediapipe"
  spec.summary = "MediaPipe Task Library - Vision"
  spec.description = "The Vision APIs of the MediaPipe Task Library, built from upstream v0.10.26."
  spec.ios.deployment_target = "15.0"
  spec.module_name = "MediaPipeTasksVision"
  spec.static_framework = true

  tag = ENV.fetch("POD_RELEASE_TAG", "eyespie-ios-v0.10.26.2")
  archive = "MediaPipeTasksVision-#{spec.version}.tar.gz"
  spec.source = {
    :http => "https://github.com/ryjen/mediapipe/releases/download/#{tag}/#{archive}",
    :sha256 => "ac042903d16efacb3de9a52c9bd0ca03a09197ef208b1b7f5ed6248b9ea4160c"
  }

  spec.pod_target_xcconfig = {
    "FRAMEWORK_SEARCH_PATHS" => "$(inherited) \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksVision.xcframework/ios-arm64_x86_64-simulator\" \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksVision.xcframework/ios-arm64\" \"$(PODS_ROOT)/EyespieMediaPipeTasksCommon/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64_x86_64-simulator\" \"$(PODS_ROOT)/EyespieMediaPipeTasksCommon/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64\"",
    "FRAMEWORK_SEARCH_PATHS[sdk=iphonesimulator*]" => "$(inherited) \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksVision.xcframework/ios-arm64_x86_64-simulator\" \"$(PODS_ROOT)/EyespieMediaPipeTasksCommon/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64_x86_64-simulator\"",
    "FRAMEWORK_SEARCH_PATHS[sdk=iphoneos*]" => "$(inherited) \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksVision.xcframework/ios-arm64\" \"$(PODS_ROOT)/EyespieMediaPipeTasksCommon/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64\""
  }
  spec.dependency "EyespieMediaPipeTasksCommon", "= #{spec.version}"
  spec.library = "c++"
  spec.vendored_frameworks = "frameworks/MediaPipeTasksVision.xcframework"
end
