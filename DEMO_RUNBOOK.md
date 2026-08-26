# DORJA Demo Runbook

> **Last updated:** August 2026
> **For:** Judge demonstrations and live testing

---

## ⚡ AT A GLANCE — Run These Commands One by One

Copy-paste each line into PowerShell **one at a time**. Wait for each to finish before running the next.

```powershell
# 1. Install dependencies (first time only, or after code changes)
cd "C:\Projects\Dorja Homestation"; pnpm install

# 2. Start Docker (database + storage)
cd "C:\Projects\Dorja Homestation"; docker compose -f infra/docker-compose.yml up -d

# 3. Wait 10 seconds for Docker to be healthy, then check
docker ps --format "{{.Names}}: {{.Status}}"

# 4. Seed the database (first time only)
cd "C:\Projects\Dorja Homestation\apps\api"; npx tsx prisma/seed.ts

# 5. Start the API server (leave this terminal open)
cd "C:\Projects\Dorja Homestation\apps\api"; npx tsx src/server.ts

# 6. (NEW TERMINAL) Start the Web app
cd "C:\Projects\Dorja Homestation\apps\web"; npx next dev --port 3000

# 7. (NEW TERMINAL) Start the Mobile app
cd "C:\Projects\Dorja Homestation\apps\mobile"; npx expo start
```

**That's it.** Open http://localhost:3000 for the web app. Scan the QR code with Expo Go for mobile.

---

## Demo Credentials

| Username | Password | Role | Use on |
|----------|----------|------|--------|
| `seller` | `12345678` | Seller (OWNER) | Mobile app |
| `buyer` | `12345678` | Buyer (SEEKER) | Web app |

**Phone OTP:** Any phone number + any 6-digit code works (demo mode)

---

## Mobile App on Your Phone

### Quick Setup (5 minutes)

1. **Install Expo Go** on your phone:
   - iPhone: App Store → search "Expo Go"
   - Android: Play Store → search "Expo Go"

2. **Make sure phone and laptop are on the same WiFi**

3. **Run the mobile start command** (step 5 above)

4. **Scan the QR code:**
   - iPhone: Open Camera app → point at QR → tap notification
   - Android: Open Expo Go → tap "Scan QR Code" → point at QR

5. **The DORJA app loads on your phone**

### If QR Code Doesn't Work

```powershell
# Use tunnel mode (works across networks)
cd "C:\Projects\Dorja Homestation\apps\mobile"; npx expo start --tunnel
```

### Find Your Laptop's IP

```powershell
ipconfig | findstr "IPv4"
# Shows: IPv4 Address. . . . . . . : 192.168.1.100
```

Then on phone browser: `http://192.168.1.100:3000`

### Windows Firewall Fix

```powershell
# Run PowerShell as Administrator
New-NetFirewallRule -DisplayName "Expo Dev Server" -Direction Inbound -LocalPort 8081 -Protocol TCP -Action Allow
```

---

## Changing the Backend URL (Without Rebuilding)

The mobile APK remembers the last backend URL you entered. **No rebuild needed** when you move to a different WiFi network.

### On the Phone (Error Screen)

If the app shows **"Server not found"**:

1. Tap **"Change server URL"**
2. Enter the new backend URL (e.g. `http://192.168.1.100:4000`)
3. Tap **"Save & Test"**
4. The app re-checks the connection and loads if successful

### How to Find the Server's IP

#### Option A: You're on YOUR OWN machine (easiest)

**PowerShell:**

```powershell
ipconfig | findstr "IPv4"
# Example output:
#   IPv4 Address. . . . . . . : 192.168.68.101
```

Use that IP as: `http://192.168.68.101:4000`

#### Option B: You're on SOMEONE ELSE's WiFi (you need their IP)

Ask the person whose WiFi you're on to run this on their machine:

```powershell
ipconfig | findstr "IPv4"
```

They'll see something like:

```text
   IPv4 Address. . . . . . . : 192.168.1.42
```

**You then use:** `http://192.168.1.42:4000`

> **Important:** Both your phone AND the server machine must be on the **same WiFi network**. If you're on `Friends_WiFi` and the server is on `Home_WiFi`, it won't work — join the same network first.

#### Option C: The server isn't on your network at all

If the API server is on a different network entirely (e.g. someone's house and you're elsewhere), you can't use a LAN IP. Options:

1. **Expose the port** — use ngrok or Cloudflare Tunnel on the server machine:
   ```powershell
   # On the server machine:
   ngrok http 4000
   # Gives you a public URL like: https://abc123.ngrok-free.app
   ```
   Then enter that URL in the app.

2. **Run your own server** — clone the repo and run the stack locally on your machine.

3. **Use a VPN** — if both machines are on the same VPN (e.g. Tailscale), use the VPN IP.

#### Option D: You're presenting at a venue and don't know the network

1. Run the API server on your laptop
2. Connect your laptop to the venue WiFi
3. Run `ipconfig | findstr "IPv4"` to get your laptop's IP on that network
4. Enter `http://YOUR_LAPTOP_IP:4000` in the app
5. Make sure your phone is also on the venue WiFi

> **Pro tip:** Before the demo, always test the connection from your phone to your laptop on the target network. Open the DORJA app → if you see "Server not found", tap "Change server URL" and enter the correct IP.

### Where the URL is Stored

- The URL is saved in **AsyncStorage** on the device (`@dorja/api_url`)
- It persists across app restarts — you only need to enter it once per network
- The app checks this saved URL first, then falls back to the build-time default

### Setting the URL Before First Launch (EAS Build)

For cloud-built APKs, set `EXPO_PUBLIC_API_URL` as an EAS secret so the APK ships with the correct URL:

```powershell
cd apps/mobile
easel secrets:create EXPO_PUBLIC_API_URL --value http://YOUR_IP:4000
```

Then rebuild:

eas build --platform android --profile preview

On subsequent networks, the user can change it from the error screen.

### Quick Reference: Common URLs

| Network | Server IP | URL |
|---------|-----------|-----|
| Home WiFi | Find with `ipconfig` | `http://192.168.x.x:4000` |
| Coffee shop | Your laptop hotspot | `http://192.168.43.1:4000` |
| Expo tunnel | Auto-assigned | Use `npx expo start --tunnel` instead |

---

## Building the Mobile App for Submission

**NEVER build with Gradle locally.** Use EAS Build:

```powershell
# Install EAS CLI (one time)
npm install -g eas-cli

# Login to Expo (one time)
cd "C:\Projects\Dorja Homestation\apps\mobile"
eas login

# Build for Android
eas build --platform android --profile preview

# Build for iOS
eas build --platform ios --profile preview
```

EAS builds in the cloud — no local Android SDK or Xcode needed.

---

## Demo Flow for Judges

### Setup (before judges arrive)

1. Run all 5 commands from "At a Glance" above
2. Open http://localhost:3000 on the presentation PC (**Buyer view**)
3. Have your phone ready with the DORJA app (**Seller view**)

### Live Demo Script

#### Part 1: Sign In (show the auth system)

**On Phone (Seller):**
1. Open DORJA app → tap **Account** tab → tap **Sign In**
2. Enter username: `seller`, password: `12345678`
3. Tap **Sign In** → redirects to Explore

**On PC (Buyer):**
1. Open http://localhost:3000/auth
2. Enter username: `buyer`, password: `12345678`
3. Click **Sign In** → redirects to Explore

**Tell judges:** "We have role-based access — sellers list properties, buyers explore them."

#### Part 2: Create a Listing (Seller — Phone)

1. On the phone, tap **Capture** tab → **"Add your property"**
2. Fill in your ACTUAL house details:
   - Title: "My Actual House"
   - Intent: For Rent
   - Type: Apartment
   - Area: (your area name)
   - Price: (your price)
   - Exact Address: (your real address — it gets encrypted!)
3. Add rooms (Living Room, Bedroom, Kitchen, etc.)
4. For each room, tap **"Add photo"** and label it with a custom name
5. Tap **"Create Listing"**

**Tell judges:** "The exact address is AES-256-GCM encrypted. Only visible after SafeView confirms a visit."

#### Part 3: 3D Room Scanner (Seller — Phone)

1. On the phone, tap **Capture** tab → **"3D Room Scanner"**
2. Select a room type (e.g., "Living Room")
3. Tap **"Start Scanning"**
4. **Point your phone camera at the room** — the scanner captures frames
5. Watch the coverage dots fill the screen and the progress bar grow
6. Tap the **stop button** when done (or it auto-completes at 95%)
7. Shows scan results: frames captured, duration, data points

**Tell judges:** "This uses the same approach as rumahku — ARCore-style capture with spatial reconstruction. The scan data is stored and used to generate the 3D walkthrough."

#### Part 4: Listing Appears on Web (Buyer — PC)

1. On the PC, refresh http://localhost:3000
2. **The new listing appears immediately!** Point this out
3. Click on the listing → shows **Property Passport**
4. The encrypted address is stored securely

**Tell judges:** "Both platforms share the same PostgreSQL database. The seller's listing is instantly visible to the buyer."

#### Part 3b: 3D Tour Walkthrough

1. On the Property Passport, click **"Open 3D Tour"**
2. **On PC:** Use mouse drag to look around, W/S to zoom, click doorways
3. **On Phone:** Use the joystick to look around, tap doorways to walk

#### Part 4: Protected Chat

1. On the PC web app, click **Inbox** in the nav rail
2. Shows the conversation list
3. Click on a conversation → shows the chat messages
4. Type a message and send it

**Tell judges:** "Messages are end-to-end encrypted. Phone numbers are never shared between parties."

#### Part 5: Handover Passport (for sale properties)

1. Navigate to a property → click **"Open Handover Passport"**
2. Show the **Promise Line** — developer promises with categories
3. Click on a promise to expand → shows original text + evidence
4. Switch to **Remedy Clock** → shows issues being tracked
5. Switch to **Evidence Pack** → printable/exportable dossier

**Tell judges:** "For purchased properties, every developer promise is recorded with evidence. The Remedy Clock tracks issues. The Evidence Pack is a neutral, printable dossier."

---

## What to Tell the Judges

### DORJA solves four problems in Bangladesh real estate:

1. **Scam prevention** — Exact addresses are AES-256-GCM encrypted. Only visible after a verified SafeView appointment. No more fake listings.

2. **Accountability** — Every listing has a Live Pulse (real-time availability), Reality Passport (seller-captured proof), and Authority Review (owner verification). If a listing is fake, there's a trail.

3. **Safe transactions** — Structured offers, encrypted messaging (phone numbers masked), SafeView appointments with timed QR passes, check-in/out with missed checkout alerts.

4. **Promise-to-Proof tracking** — For purchased properties, every developer promise is recorded with evidence. The Remedy Clock tracks issues. The Evidence Pack is a neutral, printable dossier.

### Technical highlights:

- **Monorepo:** pnpm workspaces, 4 packages, TypeScript end-to-end
- **Encryption:** AES-256-GCM envelope encryption for sensitive data
- **Database:** PostgreSQL with 40+ models, all relations normalized
- **Real-time:** Live Pulse auto-expires after 48h, must be reconfirmed
- **Cross-platform:** Shared API, mobile + web from same database
- **3D Scanner:** Camera-based room scanning with spatial reconstruction
- **Auth:** Role-based (Seller/Buyer) + phone OTP with demo bypass

---

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Mobile     │     │   Web App   │     │   API       │
│   (Expo)     │     │   (Next.js) │     │   (Fastify) │
│              │     │              │     │              │
│  5-tab app   │     │  11 pages   │     │  10 modules  │
│  Scanner     │     │  Explore    │     │  Auth        │
│  Chat        │     │  Chat       │     │  Chat        │
│  Listing     │     │  Passport   │     │  Listings    │
│  3D Tour     │     │  Handover   │     │  Handover    │
└──────┬───────┘     └──────┬───────┘     └──────┬───────┘
       │                    │                    │
       └────────────────────┼────────────────────┘
                            │
                   ┌────────▼────────┐
                   │   PostgreSQL    │
                   │   Redis         │
                   │   MinIO         │
                   └─────────────────┘
```

---

## 🚨 CRITICAL: Fix "Server not found" on Phone (Do This First)

If your phone shows **"Server not found"** and you've confirmed the API server is running on your PC, the problem is **Windows Firewall blocking port 4000**. This is the #1 reason LAN connections fail on Windows.

### Step 1: Open Windows Firewall (run ONCE)

Open **PowerShell as Administrator** (right-click → "Run as administrator") and run:

```powershell
New-NetFirewallRule -DisplayName "DORJA API" -Direction Inbound -LocalPort 4000 -Protocol TCP -Action Allow
```

This creates a permanent firewall rule. You only need to do this **once**.

### Step 2: Verify the server is reachable from your phone

On your phone, open a browser and go to:

```
http://YOUR_PC_IP:4000/v1/health
```

Replace `YOUR_PC_IP` with your PC's LAN IP (find it with `ipconfig | findstr "IPv4"`).

- ✅ If you see `{"status":"ok"}` → server is reachable, the firewall fix worked
- ❌ If it times out → double-check you ran the firewall command as Administrator

### Step 3: Make sure phone and PC are on the same WiFi

Your phone **must** be connected to the **same WiFi network** as your PC. If your phone is on mobile data or a different WiFi, it can't reach your PC.

### Step 4: Rebuild the APK (one time only)

Your current APK was built **before** the runtime URL editor was added. You need **one rebuild** to get the "Change server URL" button on the error screen.

```powershell
cd apps/mobile

# Set the EAS secret with your home IP
easel secrets:create EXPO_PUBLIC_API_URL --value http://YOUR_HOME_IP:4000

# Build the APK
eas build --platform android --profile preview
```

After this **one rebuild**, the APK will:
1. Ship with your home URL pre-configured
2. Have the "Change server URL" button on the error screen
3. Let you change the URL at runtime (no more rebuilds needed)

### Step 5: When you go somewhere else

1. Open the app → "Server not found" appears
2. Tap **"Change server URL"**
3. Enter `http://NEW_SERVER_IP:4000`
4. Tap **"Save & Test"** → connected ✅

The URL persists on the device — you only need to enter it once per network.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Docker won't start | Open Docker Desktop manually, wait for "running" |
| API won't start | Check Docker is running: `docker ps` |
| "ECONNREFUSED" on DB | `docker compose -f infra/docker-compose.yml up -d` |
| Listing not showing | Check API is running, refresh the page |
| 3D view black | Click a room tab to select a room |
| Mobile can't reach API | **Windows Firewall fix** (see section above). Then use `http://<your-ip>:4000` |
| Mobile "Server not found" | 1) Run firewall command as Admin. 2) Verify phone + PC on same WiFi. 3) Tap "Change server URL" → enter correct IP |
| Phone can reach API in browser but not in app | Rebuild the APK once to get the runtime URL editor (see Step 4 above) |
| Prisma errors | `cd apps/api && npx prisma generate` |
| Port 3000 busy | `npx next dev --port 3001` |
| Mobile Expo won't start | `npx expo start --tunnel` |
| Phone can't scan QR | Make sure same WiFi, or use tunnel mode |
| Build APK locally failing | Use `eas build` instead — never Gradle locally |
| APK URL is wrong | Tap "Change server URL" on the error screen — no rebuild needed |
| Changed networks | Same as above — enter the new server IP from the error screen |
| Still can't connect after firewall fix | Try `telnet YOUR_IP 4000` from another device on the same network to verify port is open |
