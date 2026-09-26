{
  description = "eyespie development and CI toolchain";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
  }:
    flake-utils.lib.eachDefaultSystem (system: let
      isAndroidCiSystem = system == "x86_64-linux";

      pkgs = import nixpkgs {
        inherit system;
        config = nixpkgs.lib.optionalAttrs isAndroidCiSystem {
          allowUnfree = true;
          android_sdk.accept_license = true;
        };
      };

      # Keep the ordinary CI closure focused on compilation/package inspection.
      # Emulator/system-image work remains a separate hosted instrumentation
      # workload and must not inflate every Dubnium JIT worker.
      androidComposition =
        if isAndroidCiSystem
        then
          pkgs.androidenv.composeAndroidPackages {
            platformVersions = [ "36" ];
            buildToolsVersions = [ "35.0.0" ];
            includeNDK = false;
            includeEmulator = false;
            includeSystemImages = false;
          }
        else null;

      androidSdk =
        if isAndroidCiSystem
        then androidComposition.androidsdk
        else null;

      ciToolchainPackages = pkgs.lib.optionals isAndroidCiSystem [
        pkgs.jdk21_headless
        pkgs.python3
        androidSdk
      ];

      ciToolchain =
        if isAndroidCiSystem
        then
          pkgs.buildEnv {
            name = "eyespie-android-ci-toolchain";
            paths = ciToolchainPackages;
          }
        else null;

      ciEnvironment = pkgs.lib.optionalAttrs isAndroidCiSystem {
        ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
        ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
        JAVA_HOME = pkgs.jdk21_headless.home;
        LANG = "C.UTF-8";
        LC_ALL = "C.UTF-8";
      };
    in {
      packages = pkgs.lib.optionalAttrs isAndroidCiSystem {
        ci-toolchain = ciToolchain;
      };

      checks = pkgs.lib.optionalAttrs isAndroidCiSystem {
        ci-toolchain = pkgs.runCommand "eyespie-android-ci-toolchain-check" {
          nativeBuildInputs = [ ciToolchain ];
        } ''
          set -eu

          java -version 2>&1 | grep -Eq 'version "21\.'
          python3 --version | grep -Eq '^Python 3\.'

          grep -R -l -F "AndroidVersion.ApiLevel=36" \
            "${androidSdk}/libexec/android-sdk/platforms"/*/source.properties
          grep -R -l -F "Pkg.Revision=35.0.0" \
            "${androidSdk}/libexec/android-sdk/build-tools"/*/source.properties

          test -n "$(
            find "${androidSdk}/libexec/android-sdk/cmdline-tools" \
              -type f -name apkanalyzer -perm -u+x -print -quit
          )"

          test ! -d "${androidSdk}/libexec/android-sdk/emulator"
          test ! -d "${androidSdk}/libexec/android-sdk/ndk"
          test ! -d "${androidSdk}/libexec/android-sdk/system-images"

          touch "$out"
        '';
      };

      devShells =
        {
          # Preserve the fast repository shell: ordinary editing/linting should
          # not materialize the Android SDK closure.
          default = pkgs.mkShellNoCC {
            packages = [
              pkgs.actionlint
              pkgs.supabase-cli
            ];
          };
        }
        // pkgs.lib.optionalAttrs isAndroidCiSystem {
          ci = pkgs.mkShellNoCC (ciEnvironment
            // {
              packages = [ ciToolchain ];
            });
        };
    });
}
