# macOS Code Signing and Notarization

The tagged-release workflow signs the application and its bundled Java runtime
with a Developer ID Application certificate. It then signs, notarizes, staples,
and verifies the DMG before uploading it as a release artifact.

The certificate and credentials must never be committed to this repository.

## 1. Create a Developer ID Application certificate

Developer ID certificates require an active Apple Developer Program membership.
The Account Holder creates the certificate.

1. Open **Keychain Access** on the Mac.
2. Select **Keychain Access → Certificate Assistant → Request a Certificate
   From a Certificate Authority**.
3. Enter the Apple Account email and a descriptive common name, leave the CA
   email blank, select **Saved to disk**, and save the
   `.certSigningRequest` file.
4. Open [Certificates, Identifiers & Profiles](https://developer.apple.com/account/resources/certificates/list),
   click **+**, select **Developer ID**, then select
   **Developer ID Application**.
5. Upload the certificate request, download the resulting `.cer`, and
   double-click it to install it in Keychain Access.
6. Under **My Certificates**, confirm that the Developer ID Application
   certificate expands to show its private key.

Apple's instructions are available in
[Create Developer ID certificates](https://developer.apple.com/help/account/certificates/create-developer-id-certificates/)
and
[Create a certificate signing request](https://developer.apple.com/help/account/certificates/create-a-certificate-signing-request/).

Verify the installed identity:

```bash
security find-identity -p codesigning -v
```

The output must contain a valid `Developer ID Application` identity.

## 2. Export the signing identity

In Xcode, open **Xcode → Settings → Accounts**, select the Apple Account and
team, then open **Manage Certificates**. Control-click the Developer ID
Application certificate, select **Export Certificate**, and protect the
exported `.p12` with a strong, unique password.

The `.p12` contains the private key. Anyone with both this file and its password
can sign software as this developer.

Convert the `.p12` to text for the GitHub secret:

```bash
base64 -i DeveloperIDApplication.p12 | pbcopy
```

## 3. Create notarization credentials

The simplest setup uses an app-specific password:

1. Sign in at [account.apple.com](https://account.apple.com/).
2. Open **Sign-In and Security → App-Specific Passwords**.
3. Generate a password named `notarytool-climate-school` and save it in the
   password manager used for the project.
4. Find the ten-character Team ID on the
   [Apple Developer membership page](https://developer.apple.com/account/).

The app-specific password is not the normal Apple Account password.

Credentials can be tested locally without putting the password in shell
history:

```bash
xcrun notarytool store-credentials "climate-school-notary" \
  --apple-id "APPLE_ACCOUNT_EMAIL" \
  --team-id "TEAM_ID"

xcrun notarytool history \
  --keychain-profile "climate-school-notary"
```

`store-credentials` prompts securely for the app-specific password.

## 4. Configure GitHub Actions secrets

Open the GitHub repository, then select **Settings → Secrets and variables →
Actions → New repository secret**. Create these five secrets:

| Secret | Value |
| --- | --- |
| `MACOS_CERTIFICATE_BASE64` | Clipboard contents produced by the `base64` command above |
| `MACOS_CERTIFICATE_PASSWORD` | Password used when exporting the `.p12` |
| `APPLE_ID` | Apple Account email used for notarization |
| `APPLE_TEAM_ID` | Ten-character Apple Developer Team ID |
| `APPLE_APP_SPECIFIC_PASSWORD` | App-specific password generated for `notarytool` |

The workflow creates a random temporary keychain on the GitHub-hosted macOS
runner and deletes the imported signing material after the build. A normal
local `mvn package` remains unsigned. The macOS GitHub Actions build activates
the `mac-sign` Maven profile for manual runs and tagged releases.

The signed DMG is an Apple Silicon (`arm64`) package. During that build, the
profile removes three obsolete Intel-only `.jnilib` entries embedded in the
jblas and GlueGen dependency JARs. Those binaries cannot run in the bundled
Apple Silicon JVM, but Apple still inspects them inside the JAR during
notarization. The Java dependencies themselves remain in the application.

## 5. Test without creating a release

After the workflow changes are on GitHub, open **Actions → Build & Release
(tag) → Run workflow**. A manual run builds and verifies all three platform
artifacts, including the signed and notarized DMG, but skips the GitHub Release
job. Download the `dist-macOS` workflow artifact to inspect the result.

## 6. Verify a released DMG

After downloading a release DMG, verify its signature and notarization ticket:

```bash
codesign --verify --strict --verbose=2 "Climate.School.Exercises-VERSION.dmg"
xcrun stapler validate "Climate.School.Exercises-VERSION.dmg"
spctl -a -t open -vvv \
  --context context:primary-signature \
  "Climate.School.Exercises-VERSION.dmg"
```

The Gatekeeper assessment should report `accepted` and
`source=Notarized Developer ID`.

For more detail, see Apple's
[notarization documentation](https://developer.apple.com/documentation/security/notarizing-macos-software-before-distribution)
and GitHub's
[certificate-import example](https://docs.github.com/actions/how-tos/deploy/deploy-to-third-party-platforms/sign-xcode-applications).
