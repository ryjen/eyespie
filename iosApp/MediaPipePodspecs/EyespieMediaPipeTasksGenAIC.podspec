Pod::Spec.new do |spec|
  spec.name = "EyespieMediaPipeTasksGenAIC"
  spec.version = ENV.fetch("POD_VERSION", "0.10.26.1")
  spec.authors = "Google Inc."
  spec.license = { :type => "Apache", :file => "LICENSE" }
  spec.homepage = "https://github.com/ryjen/mediapipe"
  spec.summary = "MediaPipe Task Library - Gen AI C API"
  spec.description = "The Gen AI C APIs of the MediaPipe Task Library, built from upstream v0.10.26."
  spec.ios.deployment_target = "15.0"
  spec.module_name = "MediaPipeTasksGenAIC"
  spec.static_framework = true

  tag = ENV.fetch("POD_RELEASE_TAG", "eyespie-ios-v0.10.26.1")
  archive = "MediaPipeTasksGenAIC-#{spec.version}.tar.gz"
  spec.source = {
    :http => "https://github.com/ryjen/mediapipe/releases/download/#{tag}/#{archive}",
    :sha256 => "4fcc412d63b2da9e8e67188bccede6561b7f114dfa6b75171918b3b4cc10887e"
  }

  spec.pod_target_xcconfig = {
    "FRAMEWORK_SEARCH_PATHS[sdk=iphonesimulator*]" => "$(inherited) \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksGenAIC.xcframework/ios-arm64_x86_64-simulator\" \"$(PODS_ROOT)/EyespieMediaPipeTasksCommon/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64_x86_64-simulator\"",
    "FRAMEWORK_SEARCH_PATHS[sdk=iphoneos*]" => "$(inherited) \"$(PODS_TARGET_SRCROOT)/frameworks/MediaPipeTasksGenAIC.xcframework/ios-arm64\" \"$(PODS_ROOT)/EyespieMediaPipeTasksCommon/frameworks/MediaPipeTasksCommon.xcframework/ios-arm64\""
  }
  spec.dependency "EyespieMediaPipeTasksCommon", "= #{spec.version}"
  spec.frameworks = "Accelerate", "CoreVideo", "Metal", "OpenGLES"
  spec.library = "c++"
  spec.vendored_frameworks = "frameworks/MediaPipeTasksGenAIC.xcframework"
end
