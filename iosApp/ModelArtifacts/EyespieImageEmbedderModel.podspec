Pod::Spec.new do |spec|
  spec.name = "EyespieImageEmbedderModel"
  spec.version = "1.0.0"
  spec.authors = "Eyespie contributors"
  spec.license = { :type => "Apache-2.0" }
  spec.homepage = "https://github.com/ryjen/eyespie"
  spec.summary = "Pinned MediaPipe image-embedder model resource for Eyespie"
  spec.description = "Packages the SHA-256-verified image embedder selected by models/image-embedder.json."
  spec.ios.deployment_target = "15.0"
  spec.source = { :http => "" }
  spec.resources = "mobilenet_v3_small_100_224_embedder.tflite"
end
