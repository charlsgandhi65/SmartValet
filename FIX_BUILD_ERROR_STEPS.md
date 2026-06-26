# Fix Build Error - Step by Step Instructions

## The Issue
The error `AttributePrefixUnbound?com.google.android.material.card` means Android Studio can't find the Material library even though it's in your dependencies.

## Solution Steps (Do These in Order)

### Step 1: Close Android Studio Completely
1. **File** → **Exit** (or close Android Studio completely)
2. Make sure Android Studio is NOT running

### Step 2: Clean Build Folders
Delete these folders (if they exist):
- `app/build`
- `.gradle` (in project root)
- `.idea` (optional - will be regenerated)

**Don't delete** `gradle` folder (the one with wrapper)

### Step 3: Invalidate Android Studio Cache
1. **Close** Android Studio completely
2. Open Android Studio
3. **File** → **Invalidate Caches...**
4. Check all boxes:
   - ✅ Invalidate and Restart
   - ✅ Clear file system cache and Local History
   - ✅ Clear downloaded shared indexes
5. Click **"Invalidate and Restart"**
6. Wait for Android Studio to restart

### Step 4: Open Project
1. After restart, **File** → **Open**
2. Select your SmartValet project folder
3. Wait for Gradle sync to complete

### Step 5: Sync Gradle
1. **File** → **Sync Project with Gradle Files**
2. Watch the bottom status bar for sync progress
3. Wait for "Gradle sync finished" message
4. If there are errors, check what they say

### Step 6: Clean and Rebuild
1. **Build** → **Clean Project** (wait for completion)
2. **Build** → **Rebuild Project** (wait for completion)

### Step 7: Run
1. **Run** → **Run 'app'**

---

## If Still Not Working After Above Steps

### Alternative: Check Material Library Version
1. Open `app/build.gradle`
2. Make sure line 38 shows:
   ```gradle
   implementation 'com.google.android.material:material:1.11.0'
   ```
3. Sync Gradle again

### Alternative: Try Different Material Version
If 1.11.0 doesn't work, try:
```gradle
implementation 'com.google.android.material:material:1.10.0'
```

---

## Check Your Internet Connection
Gradle needs to download dependencies. Make sure:
- You have internet connection
- No firewall is blocking Gradle
- If behind proxy, configure it in Android Studio settings

---

## Verify Dependencies
In `app/build.gradle`, make sure you have:
```gradle
implementation 'com.google.android.material:material:1.11.0'
```

This library includes MaterialCardView.

---

## Still Failing?
If after all these steps it still fails, share:
1. Full error message from Build output
2. Any errors shown in Gradle sync
3. Check Android Studio → Help → Show Log in Explorer for detailed logs

