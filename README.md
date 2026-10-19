# SignalMeter

SignalMeter is an Android cellular signal monitor focused on LTE/5G diagnostics.

## Current features

- Ongoing notification with available LTE or 5G NR RSRP.
- Dark, green-accented diagnostics dashboard.
- RSRQ and SINR / SS-SINR when exposed by the device and Android telephony APIs.
- Session-only chart of up to 60 recent RSRP samples.
- Clear explanations of what the radio measurements mean.

## Notes and limitations

- Metrics are provided by Android's telephony APIs and modem. Availability and refresh cadence vary by device, carrier, network mode, and Android version.
- The dashboard history is a lightweight in-app session view, not a persistent exportable log.
- Signal-quality labels are approximate guidance; they are not a guarantee of throughput or connection quality.
- Android does not allow ordinary apps to insert arbitrary text directly into the system clock/signal area; SignalMeter uses an ongoing notification instead.

## Build

The GitHub Actions workflow builds a debug APK on pushes to `main` and manual workflow runs. Download the APK from the workflow run's Artifacts section.
