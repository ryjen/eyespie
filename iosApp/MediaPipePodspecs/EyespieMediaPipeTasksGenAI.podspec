Pod::Spec.new do |spec|
  spec.name = "EyespieMediaPipeTasksGenAI"
  spec.version = ENV.fetch("POD_VERSION", "0.10.26.1")
  spec.authors = "Google Inc."
  spec.license = { :type => "Apache", :file => "LICENSE" }
  spec.homepage = "https://github.com/ryjen/mediapipe"
  spec.summary = "MediaPipe Task Library - Gen AI"
  spec.description = "The Gen AI APIs of the MediaPipe Task Library, built from upstream v0.10.26."
  spec.ios.deployment_target = "15.0"
  spec.swift_version = "6.0"
  spec.module_name = "MediaPipeTasksGenAI"
  spec.static_framework = true

  tag = ENV.fetch("POD_RELEASE_TAG", "eyespie-ios-v0.10.26.1")
  archive = "MediaPipeTasksGenAI-#{spec.version}.tar.gz"
  spec.source = {
    :http => "https://github.com/ryjen/mediapipe/releases/download/#{tag}/#{archive}",
    :sha256 => "55c866f8c878e3e3fc302218a346fd4fbf8921d0f305ee505e0aa73ffe96bebe"
  }

  spec.pod_target_xcconfig = {
    "FRAMEWORK_SEARCH_PATHS[sdk=iphonesimulator*]" => "$(inherited) \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksGenAI.xcframework/ios-arm64_x86_64-simulator\" \"$(PODS_ROOT)/EyespieMediaPipeTasksGenAIC/frameworks/MediaPipeTasksGenAIC.xcframework/ios-arm64_x86_64-simulator\" \"$(PODS_ROOT)/EyespieMediaPipeTasksCommon/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64_x86_64-simulator\"",
    "FRAMEWORK_SEARCH_PATHS[sdk=iphoneos*]" => "$(inherited) \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksGenAI.xcframework/ios-arm64\" \"$(PODS_ROOT)/EyespieMediaPipeTasksGenAIC/frameworks/MediaPipeTasksGenAIC.xcframework/ios-arm64\" \"$(PODS_ROOT)/EyespieMediaPipeTasksCommon/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64\""
  }
  spec.dependency "EyespieMediaPipeTasksGenAIC", "= #{spec.version}"
  spec.vendored_frameworks = "frameworks/MediaPipeTasksGenAI.xcframework"
end
