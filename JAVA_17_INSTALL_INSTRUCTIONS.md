# Java 17 Installation Instructions

## Problem
Your system has Java 8, 23, and 24, but Android Gradle Plugin 8.2.2 requires **Java 11-17**.

## Solution: Install Java 17

### Step 1: Download Java 17
1. Visit: https://adoptium.net/temurin/releases/?version=17
2. Select:
   - **Version**: 17 - LTS
   - **Operating System**: Windows
   - **Architecture**: x64
   - **Package Type**: JDK
3. Download the `.msi` installer

### Step 2: Install
1. Run the downloaded `.msi` file
2. Check **"Set JAVA_HOME variable"** during installation
3. Check **"Add to PATH"** during installation
4. Complete the installation

### Step 3: Verify Installation
Open PowerShell and run:
```powershell
java -version
```
Should show: `openjdk version "17.x.x"`

### Step 4: Set JAVA_HOME (if not set automatically)
1. Open System Properties → Advanced → Environment Variables
2. Under "System Variables", click "New"
3. Variable name: `JAVA_HOME`
4. Variable value: `C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot\`
5. Click OK

### Step 5: Rebuild Project
In VS Code terminal:
```powershell
cd "C:\Users\Asus\Desktop\Navrachana University\Mobile Application Development\Charls\SmartValet"
.\gradlew clean assembleDebug
```

## Alternative: Use Java 11
If you prefer Java 11 instead:
- Download from: https://adoptium.net/temurin/releases/?version=11
- Follow same steps as above

## After Installation
Once Java 17 is installed, your project will build successfully!
