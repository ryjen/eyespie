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
      pkgs = import nixpkgs {
        inherit system;
        config = {
          allowUnfree = true;
          android_sdk.accept_license = true;
        };
      };

      isLinux = pkgs.stdenv.isLinux;

      # Keep the ordinary CI closure focused on compilation/package inspection.
      # Emulator/system-image work remains a separate hosted instrumentation
      # workload and must not inflate every Dubnium JIT worker.
      androidComposition =
        if isLinux
        then
          pkgs.androidenv.composeAndroidPackages {
            platformVersions = [ "36" ];
            buildToolsVersions = [ "36.0.0" ];
            includeNDK = false;
            includeEmulator = false;
            includeSystemImages = false;
          }
        else null;

      androidSdk =
        if isLinux
        then androidComposition.androidsdk
        else null;

      ciToolchainPackages =
        [
          pkgs.jdk21
          pkgs.python3
        ]
        ++ pkgs.lib.optionals isLinux [
          androidSdk
        ];

      ciToolchain =
        if isLinux
        then
          pkgs.buildEnv {
            name = "eyespie-android-ci-toolchain";
            paths = ciToolchainPackages;
          }
        else null;

      androidEnvironment = pkgs.lib.optionalAttrs isLinux {
        ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
        ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
      };

      shellEnvironment = {
        JAVA_HOME = pkgs.jdk21.home;
        LANG = "C.UTF-8";
        LC_ALL = "C.UTF-8";
      };
    in {
      packages = pkgs.lib.optionalAttrs isLinux {
        ci-toolchain = ciToolchain;
      };

      checks = pkgs.lib.optionalAttrs isLinux {
        ci-toolchain = pkgs.runCommand "eyespie-android-ci-toolchain-check" {
          nativeBuildInputs = ciToolchainPackages;
        } ''
          set -eu

          java -version 2>&1 | grep -Eq 'version "21\\.'
          python3 --version | grep -Eq '^Python 3\\.'

          grep -R -l -F "AndroidVersion.ApiLevel=36" \
            "${androidSdk}/libexec/android-sdk/platforms"/*/source.properties
          grep -R -l -F "Pkg.Revision=36.0.0" \
            "${androidSdk}/libexec/android-sdk/build-tools"/*/source.properties

          test -n "$(
            find "${androidSdk}/libexec/android-sdk/cmdline-tools" \
              -type f -name apkanalyzer -perm -u+x -print -quit
          )"

          touch "$out"
        '';
      };

      devShells =
        {
          default = pkgs.mkShellNoCC (shellEnvironment
            // androidEnvironment
            // {
              packages =
                ciToolchainPackages
                ++ [
                  pkgs.actionlint
                  pkgs.supabase-cli
                ];
            });
        }
        // pkgs.lib.optionalAttrs isLinux {
          ci = pkgs.mkShellNoCC (shellEnvironment
            // androidEnvironment
            // {
              packages = [ ciToolchain ];
            });
        };
    });
}
