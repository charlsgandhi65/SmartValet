# SmartValet - Comprehensive Update Plan

## ✅ COMPLETED CHANGES

### 1. Dynamic Greeting (Issue #1)
**Status:** ✅ IMPLEMENTED
- Added time-based greetings in `CustomerActivity.java`
- 12am-11:59am: "Good Morning"
- 12pm-4pm: "Good Afternoon"  
- 4:01pm-11:59pm: "Good Evening"

### 2. My Bookings Privacy Fix (Issue #8)
**Status:** ✅ FIXED
- Updated `MyBookingsActivity.java` to use case-insensitive email matching
- Changed query from `customer_email=eq.` to `customer_email=ilike."`
- Customers now only see their own bookings

---

## 📋 REMAINING CHANGES NEEDED

### 3. Staff Filtering by Event (Issue #4)
**Status:** ⚠️ NEEDS INVESTIGATION
**Problem:** Shows all staff for every event selected
**Code Check:** Filtering logic is correct in `ManageStaffActivity.java` (line 164):
```java
staffCache = SupabaseClientInstance.getInstance()
    .selectFromTable("staff", "assigned_event_id=eq." + eventId, ...)
```

**Action Needed:**
1. Check if `assigned_event_id` column in staff table has correct UUID values matching event_id
2. Run this SQL query in Supabase to verify:
```sql
SELECT s.staff_id, s.full_name, s.assigned_event_id, e.event_name
FROM staff s
LEFT JOIN events e ON s.assigned_event_id = e.event_id
ORDER BY e.event_name;
```
3. If assigned_event_id is NULL or incorrect, update staff records manually

---

### 4. Staff Notifications for Valet Requests (Issue #2)
**Status:** ⏳ NOT IMPLEMENTED
**Complexity:** HIGH - Requires Firebase Cloud Messaging or similar

**Requirements:**
- Send notification to staff when customer requests service
- Remove request from ViewRequestsActivity when accepted
- Auto-generate QR code for customer on acceptance

**Implementation Steps:**
1. Add Firebase Cloud Messaging (FCM) to project
2. Store staff device tokens in database
3. Send notification when parking_requests row inserted
4. Update ViewRequestsActivity to filter out accepted requests
5. Create QR code generation on request acceptance

**Files to Modify:**
- Add FCM dependencies to `build.gradle`
- Create `FirebaseMessagingService` class
- Update `ValetRequestActivity.java`
- Update `ViewRequestsActivity.java`
- Create API endpoint or Cloud Function to send notifications

---

### 5. Booking/Request Count vs Available Spots (Issue #3)
**Status:** ⏳ NOT IMPLEMENTED
**Complexity:** MEDIUM

**Requirements:**
- Track occupied_spots per event (bookings + pending requests)
- Prevent booking/request if event is full

**Database Changes:**
```sql
-- Already in DATABASE_UPDATES.sql
ALTER TABLE events ADD COLUMN occupied_spots INTEGER DEFAULT 0;
```

**Implementation Steps:**
1. Run SQL to add occupied_spots column
2. Update `BookParkingActivity.java` to check availability before booking
3. Update `ValetRequestActivity.java` to check availability before request
4. Create function to update occupied_spots when:
   - Booking created: occupied_spots++
   - Request created: occupied_spots++
   - Booking cancelled: occupied_spots--
   - Request completed/rejected: occupied_spots--

---

### 6. Events History Feature (Issue #6)
**Status:** ⏳ NOT IMPLEMENTED  
**Complexity:** MEDIUM

**Requirements:**
- Auto-move past events to events_history table
- Add "Events History" button to AdminActivity
- Create EventsHistoryActivity with delete-only option
- Use history icon (📜 or clock/arrow icon)

**Database Changes:**
```sql
-- Already in DATABASE_UPDATES.sql
-- Creates events_history table and archive function
```

**Implementation Steps:**
1. Run SQL queries from DATABASE_UPDATES.sql
2. Add "Events History" button to AdminActivity
3. Create `EventsHistoryActivity.java`
4. Create `activity_events_history.xml` layout
5. Add history icon drawable: `ic_history.xml`
6. Schedule daily job to run `archive_past_events()` function

**Recommended Icon:**
Use Material Design icon: `history` or `access_time` with arrow

---

### 7. Separate Parking Spot Count Per Event (Issue #7)
**Status:** ⚠️ NEEDS INVESTIGATION
**Problem:** All events show same occupied count

**Likely Cause:** occupied_spots not tracked per event_id

**Action Needed:**
1. Verify occupied_spots column exists (run DATABASE_UPDATES.sql)
2. Check how occupied_spots is being updated
3. Ensure increment/decrement tied to specific event_id
4. Update display logic in event cards to show correct count

---

### 8. Customer Profile with Photo (Issue #9)
**Status:** ⏳ NOT IMPLEMENTED
**Complexity:** HIGH

**Requirements:**
- Profile page asking for full_name, contact_number (+91 prefix, 10 digits), photo
- Upload photo to Profile_Photos bucket in Supabase Storage
- Store photo path in customer.profile_photo column
- Replace logout button with profile photo/user icon
- Add "My Profile" button in Quick Access
- Default user icon if no photo uploaded

**Database Changes:**
```sql
-- Already in DATABASE_UPDATES.sql
ALTER TABLE customer ADD COLUMN contact_number VARCHAR(13);
ALTER TABLE customer ADD COLUMN profile_photo TEXT;
-- Creates Profile_Photos storage bucket with policies
```

**Implementation Steps:**
1. Run SQL queries from DATABASE_UPDATES.sql
2. Create Profile_Photos bucket in Supabase Storage (manually via UI)
3. Create `ProfileActivity.java`
4. Create `activity_profile.xml` layout
5. Add image picker functionality
6. Implement photo upload to Supabase Storage
7. Update CustomerActivity to show profile photo
8. Add "My Profile" card in customer dashboard
9. Add contact number validation (+91 + 10 digits)

**Files to Create:**
- `ProfileActivity.java`
- `activity_profile.xml`
- `ic_user_default.xml` (default user icon)

**Files to Modify:**
- `CustomerActivity.java` (replace back button with profile photo)
- `activity_customer.xml` (add My Profile card, change back button to ImageView)

---

### 9. Enhanced Valet Request Logic (Issue #10)
**Status:** ⏳ NOT IMPLEMENTED
**Complexity:** HIGH

**Requirements:**

**For customers WITH booking:**
- Request type: fetch_car ONLY
- Select booking: Show "Spot XXX - VehicleNumber" format
- On submit: Show existing QR code with message "Get your QR scanned at the exit, your car must be ready in few minutes"

**For customers WITHOUT booking:**
- Request type: valet_service ONLY
- Select booking: Dropdown of ACTIVE events only (not history/future)
- On submit: Show "Wait until the request is accepted"
- When accepted: Ask for car number
- After car number: Process payment and generate QR code
- Exit charges: Apply extra_charge_per_hour if exceeds event hours

**Implementation Steps:**
1. Check if customer has active booking
2. Conditionally show request types based on booking status
3. Create logic to filter active events (event_date = today)
4. Create new UI flow for non-booking valet service
5. Implement payment calculation with extra charges
6. Generate QR code after payment

**Files to Modify:**
- `ValetRequestActivity.java` (major overhaul)
- `activity_valet_request.xml`
- Create new activity for payment flow

---

### 10. Payment Page UI Fixes (Issue #11)
**Status:** ⏳ NOT IMPLEMENTED
**Complexity:** LOW

**Requirements:**
- Fix "Event: Vijay Sales" text visibility (change color)
- Use event's base_price for amount
- Replace $ with ₹ symbol
- Change "Your QR Code (preview)" to "UPI"
- Remove "Cash on Site" option

**Implementation Steps:**
1. Find `PaymentActivity.java` and `activity_payment.xml`
2. Update text colors to match UI theme
3. Fetch event.base_price from database
4. Replace all $ symbols with ₹
5. Change QR code label text
6. Remove cash payment option UI/logic

---

### 11. Required Staff Field Validation (Issue #12)
**Status:** ✅ ALREADY IMPLEMENTED
- All staff fields in `AddStaffToEventActivity.java` already have validation (line 140)
- No changes needed in code

**Database Enforcement:**
```sql
-- Already in DATABASE_UPDATES.sql
-- Makes all staff columns NOT NULL
```
Action: Run DATABASE_UPDATES.sql to enforce at database level

---

## 🗃️ DATABASE CHANGES SUMMARY

### Required SQL Queries
**File:** `DATABASE_UPDATES.sql` (already created)

**Run these in order:**
1. Add pricing columns to events (base_price, extra_charge_per_hour)
2. Add profile columns to customer (contact_number, profile_photo)
3. Add occupied_spots to events
4. Create events_history table
5. Create Profile_Photos storage bucket + policies
6. Make staff fields NOT NULL
7. Add contact_number format constraint
8. Create archive_past_events() function
9. Create indexes for performance
10. Initialize occupied_spots for existing events

---

## 📊 PRIORITY RECOMMENDATIONS

### HIGH PRIORITY (Fix These First)
1. ✅ My Bookings Privacy (DONE)
2. ✅ Dynamic Greeting (DONE)
3. Run DATABASE_UPDATES.sql queries
4. Fix parking spot count per event (Issue #7)
5. Investigate staff filtering (Issue #4)

### MEDIUM PRIORITY
6. Payment page UI fixes (Issue #11) - Quick win
7. Events history feature (Issue #6) - Good for long-term
8. Booking/request count validation (Issue #3)

### LOW PRIORITY (Complex Features)
9. Staff notifications (Issue #2) - Requires FCM setup
10. Enhanced valet request logic (Issue #10) - Major refactor
11. Customer profile with photo (Issue #9) - Multiple components

---

## 🔧 NEXT STEPS

1. **Immediate:** Run `DATABASE_UPDATES.sql` in Supabase SQL Editor
2. **Immediate:** Create Profile_Photos bucket in Supabase Storage UI
3. **Investigation:** Check staff.assigned_event_id values in database
4. **Investigation:** Check how occupied_spots is being calculated
5. **Development:** Start with Payment UI fixes (easiest)
6. **Development:** Implement Events History feature
7. **Development:** Build Customer Profile functionality
8. **Development:** Refactor Valet Request logic
9. **Development:** Add FCM for notifications

---

## 📝 NOTES

- Firebase Cloud Messaging (FCM) required for push notifications to staff
- Supabase Storage already included, just need to create bucket via UI
- Most database schema changes are provided in DATABASE_UPDATES.sql
- Staff filtering issue likely data problem, not code problem
- Profile photo feature requires Android image picker + Supabase Storage upload

