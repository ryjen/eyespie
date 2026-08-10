Pod::Spec.new do |spec|
  spec.name = "EyespieMediaPipeTasksGenAIC"
  spec.version = ENV.fetch("POD_VERSION", "0.10.26.2")
  spec.authors = "Google Inc."
  spec.license = { :type => "Apache", :file => "LICENSE" }
  spec.homepage = "https://github.com/ryjen/mediapipe"
  spec.summary = "MediaPipe Task Library - Gen AI C API"
  spec.description = "The Gen AI C APIs of the MediaPipe Task Library, built from upstream v0.10.26."
  spec.ios.deployment_target = "15.0"
  spec.module_name = "MediaPipeTasksGenAIC"
  spec.static_framework = true

  tag = ENV.fetch("POD_RELEASE_TAG", "eyespie-ios-v0.10.26.2")
  archive = "MediaPipeTasksGenAIC-#{spec.version}.tar.gz"
  spec.source = {
    :http => "https://github.com/ryjen/mediapipe/releases/download/#{tag}/#{archive}",
    :sha256 => "a13676a9b8ef50192c98c5fafc685f34cbaa8f195d7a3c83a883f71426d55bf6"
  }

  spec.pod_target_xcconfig = {
    "FRAMEWORK_SEARCH_PATHS" => "$(inherited) \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksGenAIC.xcframework/ios-arm64_x86_64-simulator\" \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksGenAIC.xcframework/ios-arm64\" \"$(PODS_ROOT)/EyespieMediaPipeTasksCommon/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64_x86_64-simulator\" \"$(PODS_ROOT)/EyespieMediaPipeTasksCommon/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64\"",
    "FRAMEWORK_SEARCH_PATHS[sdk=iphonesimulator*]" => "$(inherited) \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksGenAIC.xcframework/ios-arm64_x86_64-simulator\" \"$(PODS_ROOT)/EyespieMediaPipeTasksCommon/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64_x86_64-simulator\"",
    "FRAMEWORK_SEARCH_PATHS[sdk=iphoneos*]" => "$(inherited) \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksGenAIC.xcframework/ios-arm64\" \"$(PODS_ROOT)/EyespieMediaPipeTasksCommon/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64\""
  }
  spec.dependency "EyespieMediaPipeTasksCommon", "= #{spec.version}"
  spec.frameworks = "Accelerate", "CoreVideo", "Metal", "OpenGLES"
  spec.library = "c++"
  spec.vendored_frameworks = "frameworks/MediaPipeTasksGenAIC.xcframework"
end
