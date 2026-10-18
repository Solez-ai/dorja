# Final QA Test Plan - Rome Submission

## Pre-Submission Testing

### Functional Testing
- [ ] Scanner: Complete 12-frame scan on all 5 devices
- [ ] Camera: Binding succeeds within 2 seconds
- [ ] Zoom: All zoom levels apply smoothly
- [ ] Stitching: Completes without crashes
- [ ] AR Ring: Color transitions work smoothly
- [ ] Haptic: All feedback patterns work
- [ ] Calculator: All calculations accurate
- [ ] Chat: Send/receive messages work
- [ ] Passport: Add/view promises work
- [ ] Relocation: Country selection works

### Performance Testing
- [ ] Scan time: < 2.5 minutes
- [ ] Memory: < 200MB peak
- [ ] CPU: < 40% average
- [ ] Battery: < 30% drain per 10-minute scan
- [ ] Startup: App launch < 2 seconds
- [ ] Stitching: < 30 seconds timeout

### Compatibility Testing
- [ ] Pixel 5: All features work
- [ ] Samsung S21: All features work
- [ ] OnePlus 9: All features work
- [ ] Xiaomi Mi 11: Fallback mode works
- [ ] Motorola G20: Works (slower stitching expected)

### Crash Testing
- [ ] No crashes during 5-minute continuous scan
- [ ] No crashes on permission denial
- [ ] No crashes when camera detached
- [ ] No crashes when device rotated
- [ ] No crashes on low memory

### Security Testing
- [ ] No plaintext frame data stored
- [ ] No logs contain sensitive data
- [ ] No hardcoded API keys
- [ ] Certificate pinning verified

### Accessibility Testing
- [ ] TalkBack: All controls readable
- [ ] Large text: Works at 200%
- [ ] High contrast: All visible at night
- [ ] Touch targets: All >= 48dp
