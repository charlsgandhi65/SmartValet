# "Unable to Resolve Host" Error - Fix Guide

## Changes Made

### 1. Added Network Security Configuration
**File**: `app/src/main/res/xml/network_security_config.xml`
- Allows cleartext traffic (though Supabase uses HTTPS)
- Trusts system and user certificates
- Specifically allows supabase.co domain

### 2. Updated AndroidManifest.xml
**Added**:
- `ACCESS_NETWORK_STATE` permission
- `usesCleartextTraffic="true"`
- `networkSecurityConfig="@xml/network_security_config"`

### 3. Added Debug Logging
**File**: `SupabaseClientInstance.java`
- Logs URL being used
- Logs response codes
- Logs response data

## Steps to Fix "Unable to Resolve Host"

### Step 1: Sync & Rebuild
```
1. File → Sync Project with Gradle Files
2. Build → Clean Project
3. Build → Rebuild Project
```

### Step 2: Check Your Device/Emulator Internet
**On Physical Device:**
- Make sure WiFi or Mobile Data is ON
- Try opening a browser and visiting https://google.com
- Check if you can access https://smyeqsobctlhwoqcards.supabase.co in Chrome

**On Emulator:**
- Sometimes emulator network gets stuck
- Close emulator completely
- Restart Android Studio
- Start emulator again
- In emulator settings, make sure network is enabled

### Step 3: Check Logcat for Detailed Error
1. Open Logcat (View → Tool Windows → Logcat)
2. Try to login
3. Look for messages starting with "SupabaseClientInstance:"
4. Share the exact error message

**Common Errors:**

**Error**: `java.net.UnknownHostException: Unable to resolve host "smyeqsobctlhwoqcards.supabase.co"`
**Cause**: No internet connection or DNS issue
**Fix**: 
- Check internet connection
- Restart emulator/device
- Try switching between WiFi and Mobile Data

**Error**: `javax.net.ssl.SSLHandshakeException`
**Cause**: Certificate/SSL issue
**Fix**: Network security config should fix this (already added)

**Error**: `java.net.SocketTimeoutException`
**Cause**: Slow or unstable connection
**Fix**: Try again with better internet

### Step 4: Test Supabase URL Directly
Open this URL in your phone/computer browser:
```
https://smyeqsobctlhwoqcards.supabase.co/rest/v1/
```

**Expected**: Should show a JSON error (this is normal - means server is reachable)
**If error loading page**: Your network can't reach Supabase

### Step 5: Alternative - Use Mobile Hotspot
If emulator can't connect:
1. Enable mobile hotspot on your phone
2. Connect your computer to that hotspot
3. Run emulator - it should now have internet

### Step 6: Check Firewall/Antivirus
Sometimes firewall blocks emulator internet:
- **Windows Defender**: Allow Android Studio and ADB through firewall
- **Antivirus**: Temporarily disable and test

## Diagnostic Checklist

Run through these checks:

1. ✅ Internet permission in manifest? → YES (already added)
2. ✅ Network security config exists? → YES (just created)
3. ✅ Manifest references network config? → YES (just added)
4. ✅ Device/emulator has internet? → CHECK THIS
5. ✅ Can access Supabase URL in browser? → CHECK THIS
6. ✅ Firewall blocking emulator? → CHECK THIS
7. ✅ Project synced and rebuilt? → DO THIS NOW

## What to Share if Still Broken

If the error persists after trying above steps, share:

1. **Full error from Logcat** (copy the entire stack trace)
2. **Device type**: Physical phone or emulator? Which Android version?
3. **Browser test result**: Can you open the Supabase URL in browser?
4. **Network type**: WiFi, mobile data, or computer ethernet?
5. **Logcat output** starting with "SupabaseClientInstance:"

## Quick Test

After rebuilding, try this test:

1. Open app
2. Go to login screen
3. Enter any email/password
4. Click login
5. Immediately open Logcat
6. Look for these lines:
   ```
   SupabaseClientInstance: Attempting login for: [email]
   SupabaseClientInstance: URL: https://smyeqsobctlhwoqcards.supabase.co
   SupabaseClientInstance: Got response, code: [number]
   ```

**If you see "Attempting login" but NOT "Got response":**
→ Network issue (no internet or firewall blocking)

**If you see "Got response, code: 400" or "200":**
→ Network is working! Issue is something else (credentials, database, etc.)
