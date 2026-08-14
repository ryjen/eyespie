Pod::Spec.new do |spec|
  spec.name = "EyespieImageEmbeddingCalibrationFixtures"
  spec.version = "1.0.0"
  spec.authors = "Eyespie contributors"
  spec.license = { :type => "Apache-2.0" }
  spec.homepage = "https://github.com/ryjen/eyespie"
  spec.summary = "Pinned MediaPipe fixtures for Eyespie image-embedding calibration"
  spec.description = "Packages generation-pinned, SHA-256-verified MediaPipe vision fixtures for explicit debug calibration runs."
  spec.ios.deployment_target = "15.0"
  spec.source = { :http => "" }
  spec.resources = "*.jpg"
end
