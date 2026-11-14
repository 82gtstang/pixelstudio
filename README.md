# Pixel Studio — Falcon F16V5 Edition (v1 source)

Android Studio Java/Gradle project. Open the **PixelStudio_Falcon_v1** directory in Android Studio, allow Gradle sync, then Build > Build APK(s). JDK 17 is recommended. Android SDK 35 required (install via SDK Manager). APK is not prebuilt.

## Falcon setup
- Phone and Falcon must be on a network that permits phone-to-controller UDP packets.
- Enable DDP input on the Falcon and verify the DDP channel mapping and output configuration in its web UI. Firmware UI terminology can vary.
- Enter the Falcon IP, number of RGB pixels, and the **zero-based DDP byte/channel offset** (a multiple of 3). RGB 8-bit output is assumed.
- Start with 10–30 pixels and low brightness to verify mapping before attempting thousands.
- Stop sends a one-time black frame; if another show player is transmitting concurrently, that player may immediately overwrite it.
- DDP is connectionless UDP. The app's “Streaming” status confirms local transmission attempts, **not** that the Falcon received or displayed packets.

## Limitations
- This is an initial live-control prototype, not an exact Govee clone.
- No per-port discovery, individual pixel editor, music sync, saved scenes, or xLights sequence editing in v1.
- Does not automatically discover Falcon configuration or guarantee a 20,000-pixel sustained frame rate. 20,000 pixels at 25 FPS is high UDP traffic; performance depends on network/controller.
- A Falcon configured for another live protocol, a conflicting show source, or a mismatched channel map will not respond correctly.
- DDP packetization is 1440 RGB bytes per UDP datagram with 10-byte headers, using UDP port 4048.
- Build has not been run in this environment (Android SDK/Gradle unavailable here).

## GitHub Actions APK build
Push this project's **contents** (including `.github/workflows/android.yml`) to the root of `main` on `82gtstang/pixelstudio`. Under Actions, select **Build Android APK** and **Run workflow** (or push to main). Download the `pixel-studio-debug-apk` artifact from the completed run. This build has not been verified yet.

Enter your controller's local IP address in the app. DDP uses UDP port 4048. Confirm the Falcon's DDP channel mapping and avoid concurrent xLights playback during testing.
