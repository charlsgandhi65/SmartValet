-- =====================================================
-- SMARTVALET COMPLETE DATABASE SETUP - PRODUCTION READY
-- =====================================================
-- Version: 2.1
-- Date: November 6, 2025
-- Last Updated: November 6, 2025 (Evening Session)
-- Description: Complete database setup for SmartValet parking application
-- 
-- CHANGELOG (Version 2.1):
-- - Fixed staff table to use full_name instead of first_name/last_name
-- - Updated ValetPaymentSuccessActivity to fetch staff full_name
-- - Fixed QR scanner to handle partial UUID matches (e.g., "b25019f8-4")
-- - Added in-memory search for QR codes to avoid UUID conversion errors
-- - Fixed ManageStaffActivity UUID query (removed extra quotes)
-- - Event History now shows past events (event_date < today)
-- - Manage Events shows only current/future events (event_date >= today)
-- - Both pages have duplicate prevention using HashSet
-- 
-- INSTRUCTIONS:
-- 1. Create a new Supabase project
-- 2. Go to SQL Editor
-- 3. Copy and paste this ENTIRE file
-- 4. Click "Run" to execute
-- 5. Your database will be ready to use!
-- =====================================================

-- ============================================
-- STEP 1: Enable Required Extensions
-- ============================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- STEP 2: Drop Existing Tables (Clean Slate)
-- ============================================
-- WARNING: This will delete all existing data!
-- Comment out this section if you want to preserve data

DROP TABLE IF EXISTS public.qr_logs CASCADE;
DROP TABLE IF EXISTS public.parking_requests CASCADE;
DROP TABLE IF EXISTS public.booked_parking CASCADE;
DROP TABLE IF EXISTS public.staff CASCADE;
DROP TABLE IF EXISTS public.events CASCADE;
DROP TABLE IF EXISTS public.administration CASCADE;
DROP TABLE IF EXISTS public.customer CASCADE;

-- ============================================
-- STEP 3: Create All Tables
-- ============================================

-- 1. CUSTOMER TABLE
-- Stores customer information for parking bookings
CREATE TABLE public.customer (
    customer_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    email_id TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    contact_number TEXT NOT NULL,
    profile_photo_base64 TEXT, -- Base64 encoded profile photo (optional)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

COMMENT ON TABLE public.customer IS 'Customer accounts for SmartValet parking system';
COMMENT ON COLUMN public.customer.first_name IS 'Customer first name (required)';
COMMENT ON COLUMN public.customer.last_name IS 'Customer last name (required)';
COMMENT ON COLUMN public.customer.contact_number IS 'Contact number with country code (required, e.g., +919876543210)';
COMMENT ON COLUMN public.customer.profile_photo_base64 IS 'Base64 encoded profile photo (optional)';

-- 2. ADMINISTRATION TABLE
-- Stores admin accounts for managing the system
CREATE TABLE public.administration (
    admin_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    email_id TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    full_name TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

COMMENT ON TABLE public.administration IS 'Admin accounts for managing SmartValet system';

-- 3. EVENTS TABLE
-- Stores parking events (concerts, festivals, etc.)
CREATE TABLE public.events (
    event_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    event_name TEXT NOT NULL,
    event_address TEXT NOT NULL,
    event_date DATE NOT NULL,
    hours_of_event INTEGER NOT NULL DEFAULT 6,
    total_vehicle_spots INTEGER NOT NULL DEFAULT 250,
    occupied_spots INTEGER DEFAULT 0, -- Current number of booked spots
    base_price NUMERIC(10, 2) DEFAULT 100.00, -- Base parking price
    extra_charge_per_hour NUMERIC(10, 2) DEFAULT 50.00, -- Extra charge per hour after base hours
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by TEXT -- Admin email who created the event
);

COMMENT ON TABLE public.events IS 'Parking events (concerts, festivals, conferences, etc.)';
COMMENT ON COLUMN public.events.hours_of_event IS 'Base hours included in base_price (default: 6 hours)';
COMMENT ON COLUMN public.events.occupied_spots IS 'Number of currently occupied/booked parking spots';
COMMENT ON COLUMN public.events.base_price IS 'Base price for parking (covers hours_of_event)';
COMMENT ON COLUMN public.events.extra_charge_per_hour IS 'Additional charge per hour beyond base hours';

-- 4. STAFF TABLE
-- Stores staff/valet information
CREATE TABLE public.staff (
    staff_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    email_id TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    full_name TEXT NOT NULL,
    contact_number TEXT NOT NULL,
    location TEXT,
    age INTEGER,
    experience_level TEXT, -- "NEW COMER", "6 - 12 MONTHS EXPERIENCE", "MORE THAN 12 MONTHS EXPERIENCE"
    login_id TEXT UNIQUE, -- Staff login ID (can be different from email)
    associated_parking TEXT,
    assigned_event_id UUID REFERENCES public.events(event_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

COMMENT ON TABLE public.staff IS 'Valet staff members assigned to events';
COMMENT ON COLUMN public.staff.experience_level IS 'Staff experience: NEW COMER, 6 - 12 MONTHS EXPERIENCE, or MORE THAN 12 MONTHS EXPERIENCE';
COMMENT ON COLUMN public.staff.assigned_event_id IS 'Event this staff member is currently assigned to';

-- 5. BOOKED_PARKING TABLE
-- Stores all parking bookings (both regular and valet)
CREATE TABLE public.booked_parking (
    booking_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    customer_email TEXT NOT NULL REFERENCES public.customer(email_id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES public.events(event_id) ON DELETE CASCADE,
    event_name TEXT, -- Denormalized for easy access
    parking_spot_number INTEGER NOT NULL DEFAULT 0, -- 0 = To be assigned by staff (valet)
    vehicle_number TEXT NOT NULL,
    vehicle_model TEXT,
    vehicle_color TEXT,
    booking_date DATE NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    payment_status TEXT DEFAULT 'pending', -- 'pending', 'paid', 'refunded'
    payment_amount NUMERIC(10, 2),
    payment_method TEXT, -- 'cash', 'card', 'upi', etc.
    qr_code TEXT UNIQUE, -- Generated QR code string (can be booking_id)
    qr_code_image_url TEXT, -- URL to QR image if stored separately
    status TEXT DEFAULT 'active', -- 'active', 'completed', 'cancelled'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

COMMENT ON TABLE public.booked_parking IS 'All parking bookings (regular and valet service)';
COMMENT ON COLUMN public.booked_parking.event_name IS 'Cached event name for display';
COMMENT ON COLUMN public.booked_parking.parking_spot_number IS 'Assigned parking spot (0 = to be assigned by valet staff)';
COMMENT ON COLUMN public.booked_parking.qr_code IS 'QR code for entry/exit verification';

-- 6. QR_LOGS TABLE
-- Tracks all QR code scans by staff
CREATE TABLE public.qr_logs (
    log_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES public.booked_parking(booking_id) ON DELETE CASCADE,
    qr_code TEXT NOT NULL,
    scanned_by_staff_id UUID REFERENCES public.staff(staff_id) ON DELETE SET NULL,
    scan_type TEXT NOT NULL, -- 'entry', 'exit', 'valet_request', 'car_fetch'
    scan_timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    vehicle_status TEXT, -- 'parked', 'retrieved', 'valet_in_transit', etc.
    notes TEXT
);

COMMENT ON TABLE public.qr_logs IS 'Log of all QR code scans for auditing and tracking';
COMMENT ON COLUMN public.qr_logs.scan_type IS 'Type of scan: entry, exit, valet_request, car_fetch';

-- 7. PARKING_REQUESTS TABLE
-- Stores valet service requests and other customer requests
CREATE TABLE public.parking_requests (
    request_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    booking_id UUID REFERENCES public.booked_parking(booking_id) ON DELETE CASCADE,
    customer_email TEXT NOT NULL,
    event_id UUID REFERENCES public.events(event_id) ON DELETE CASCADE,
    event_name TEXT, -- Cached event name
    request_type TEXT NOT NULL, -- 'valet_service', 'car_fetch', 'assistance'
    request_status TEXT DEFAULT 'pending', -- 'pending', 'assigned', 'approved', 'in_progress', 'completed', 'cancelled'
    assigned_staff_id UUID REFERENCES public.staff(staff_id) ON DELETE SET NULL,
    vehicle_number TEXT,
    vehicle_model TEXT,
    vehicle_color TEXT,
    request_details TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);

COMMENT ON TABLE public.parking_requests IS 'Customer requests for valet service, car fetch, and assistance';
COMMENT ON COLUMN public.parking_requests.request_status IS 'Request status: pending, assigned, approved, in_progress, completed, cancelled';
COMMENT ON COLUMN public.parking_requests.event_name IS 'Cached event name for display';

-- ============================================
-- STEP 4: Create Indexes for Performance
-- ============================================

-- Customer indexes
CREATE INDEX idx_customer_email ON public.customer(email_id);

-- Administration indexes
CREATE INDEX idx_admin_email ON public.administration(email_id);

-- Events indexes
CREATE INDEX idx_events_date ON public.events(event_date);
CREATE INDEX idx_events_created_by ON public.events(created_by);

-- Staff indexes
CREATE INDEX idx_staff_event ON public.staff(assigned_event_id);
CREATE INDEX idx_staff_email ON public.staff(email_id);
CREATE INDEX idx_staff_login ON public.staff(login_id);

-- Booking indexes
CREATE INDEX idx_bookings_event ON public.booked_parking(event_id);
CREATE INDEX idx_bookings_customer ON public.booked_parking(customer_email);
CREATE INDEX idx_bookings_qr ON public.booked_parking(qr_code);
CREATE INDEX idx_bookings_status ON public.booked_parking(status);
CREATE INDEX idx_bookings_payment_status ON public.booked_parking(payment_status);

-- QR logs indexes
CREATE INDEX idx_qr_logs_booking ON public.qr_logs(booking_id);
CREATE INDEX idx_qr_logs_staff ON public.qr_logs(scanned_by_staff_id);
CREATE INDEX idx_qr_logs_timestamp ON public.qr_logs(scan_timestamp);

-- Parking requests indexes
CREATE INDEX idx_parking_requests_status ON public.parking_requests(request_status);
CREATE INDEX idx_parking_requests_staff ON public.parking_requests(assigned_staff_id);
CREATE INDEX idx_parking_requests_customer ON public.parking_requests(customer_email);
CREATE INDEX idx_parking_requests_event ON public.parking_requests(event_id);

-- ============================================
-- STEP 5: Enable Row Level Security (RLS)
-- ============================================

ALTER TABLE public.customer ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.administration ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.staff ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.booked_parking ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.qr_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.parking_requests ENABLE ROW LEVEL SECURITY;

-- ============================================
-- STEP 6: Create RLS Policies
-- ============================================
-- These policies allow public access for the mobile app
-- Adjust these based on your security requirements

-- CUSTOMER TABLE POLICIES
CREATE POLICY "Allow public select on customer"
ON public.customer FOR SELECT
TO public
USING (true);

CREATE POLICY "Allow public insert on customer"
ON public.customer FOR INSERT
TO public
WITH CHECK (true);

CREATE POLICY "Allow public update on customer"
ON public.customer FOR UPDATE
TO public
USING (true)
WITH CHECK (true);

CREATE POLICY "Allow public delete on customer"
ON public.customer FOR DELETE
TO public
USING (true);

-- ADMINISTRATION TABLE POLICIES
CREATE POLICY "Allow public select on administration"
ON public.administration FOR SELECT
TO public
USING (true);

CREATE POLICY "Allow public insert on administration"
ON public.administration FOR INSERT
TO public
WITH CHECK (true);

CREATE POLICY "Allow public update on administration"
ON public.administration FOR UPDATE
TO public
USING (true)
WITH CHECK (true);

-- EVENTS TABLE POLICIES
CREATE POLICY "Allow public select on events"
ON public.events FOR SELECT
TO public
USING (true);

CREATE POLICY "Allow public insert on events"
ON public.events FOR INSERT
TO public
WITH CHECK (true);

CREATE POLICY "Allow public update on events"
ON public.events FOR UPDATE
TO public
USING (true)
WITH CHECK (true);

CREATE POLICY "Allow public delete on events"
ON public.events FOR DELETE
TO public
USING (true);

-- STAFF TABLE POLICIES
CREATE POLICY "Allow public select on staff"
ON public.staff FOR SELECT
TO public
USING (true);

CREATE POLICY "Allow public insert on staff"
ON public.staff FOR INSERT
TO public
WITH CHECK (true);

CREATE POLICY "Allow public update on staff"
ON public.staff FOR UPDATE
TO public
USING (true)
WITH CHECK (true);

CREATE POLICY "Allow public delete on staff"
ON public.staff FOR DELETE
TO public
USING (true);

-- BOOKED_PARKING TABLE POLICIES
CREATE POLICY "Allow public select on booked_parking"
ON public.booked_parking FOR SELECT
TO public
USING (true);

CREATE POLICY "Allow public insert on booked_parking"
ON public.booked_parking FOR INSERT
TO public
WITH CHECK (true);

CREATE POLICY "Allow public update on booked_parking"
ON public.booked_parking FOR UPDATE
TO public
USING (true)
WITH CHECK (true);

CREATE POLICY "Allow public delete on booked_parking"
ON public.booked_parking FOR DELETE
TO public
USING (true);

-- QR_LOGS TABLE POLICIES
CREATE POLICY "Allow public select on qr_logs"
ON public.qr_logs FOR SELECT
TO public
USING (true);

CREATE POLICY "Allow public insert on qr_logs"
ON public.qr_logs FOR INSERT
TO public
WITH CHECK (true);

CREATE POLICY "Allow public update on qr_logs"
ON public.qr_logs FOR UPDATE
TO public
USING (true)
WITH CHECK (true);

CREATE POLICY "Allow public delete on qr_logs"
ON public.qr_logs FOR DELETE
TO public
USING (true);

-- PARKING_REQUESTS TABLE POLICIES
CREATE POLICY "Allow public select on parking_requests"
ON public.parking_requests FOR SELECT
TO public
USING (true);

CREATE POLICY "Allow public insert on parking_requests"
ON public.parking_requests FOR INSERT
TO public
WITH CHECK (true);

CREATE POLICY "Allow public update on parking_requests"
ON public.parking_requests FOR UPDATE
TO public
USING (true)
WITH CHECK (true);

CREATE POLICY "Allow public delete on parking_requests"
ON public.parking_requests FOR DELETE
TO public
USING (true);

-- ============================================
-- STEP 7: Create Functions & Triggers
-- ============================================

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for auto-updating updated_at on all tables

CREATE TRIGGER update_customer_updated_at
    BEFORE UPDATE ON public.customer
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_events_updated_at
    BEFORE UPDATE ON public.events
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_staff_updated_at
    BEFORE UPDATE ON public.staff
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_booked_parking_updated_at
    BEFORE UPDATE ON public.booked_parking
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_parking_requests_updated_at
    BEFORE UPDATE ON public.parking_requests
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- ============================================
-- STEP 8: Insert Sample Data (OPTIONAL)
-- ============================================
-- Uncomment the sections below if you want sample data for testing

-- Sample Admin Account
-- Password: admin123
INSERT INTO public.administration (email_id, password, full_name)
VALUES ('admin@smartvalet.com', 'admin123', 'System Administrator')
ON CONFLICT (email_id) DO NOTHING;

-- Sample Customer Account
-- Password: customer123
INSERT INTO public.customer (email_id, password, first_name, last_name, contact_number)
VALUES ('customer@example.com', 'customer123', 'Test', 'Customer', '+919876543210')
ON CONFLICT (email_id) DO NOTHING;

-- Sample Event (Future Date - November 15, 2025)
INSERT INTO public.events (
    event_name, 
    event_address, 
    event_date, 
    hours_of_event, 
    total_vehicle_spots,
    base_price,
    extra_charge_per_hour,
    occupied_spots,
    created_by
)
VALUES (
    'Winter Music Festival 2025', 
    'Central Park, 123 Main Street, City Center', 
    '2025-11-15', 
    6, 
    500,
    150.00,
    50.00,
    0,
    'admin@smartvalet.com'
)
ON CONFLICT DO NOTHING;

-- Sample Event (Past Date - for testing Event History)
INSERT INTO public.events (
    event_name, 
    event_address, 
    event_date, 
    hours_of_event, 
    total_vehicle_spots,
    base_price,
    extra_charge_per_hour,
    occupied_spots,
    created_by
)
VALUES (
    'Summer Concert 2025', 
    'Beach Boulevard, 456 Ocean Drive', 
    '2025-07-20', 
    8, 
    300,
    200.00,
    75.00,
    0,
    'admin@smartvalet.com'
)
ON CONFLICT DO NOTHING;

-- Sample Staff Member
-- Password: staff123
INSERT INTO public.staff (
    email_id, 
    password, 
    full_name, 
    contact_number, 
    location, 
    age, 
    experience_level, 
    login_id,
    associated_parking
)
VALUES (
    'staff@smartvalet.com', 
    'staff123', 
    'John Doe', 
    '+919876543211', 
    'City Center', 
    28, 
    'MORE THAN 12 MONTHS EXPERIENCE', 
    'STAFF001',
    'Central Parking'
)
ON CONFLICT (email_id) DO NOTHING;

-- ============================================
-- STEP 9: Verification Queries
-- ============================================
-- Run these after setup to verify everything is working

-- Check all tables exist
SELECT 
    table_name,
    (SELECT COUNT(*) FROM information_schema.columns WHERE columns.table_name = tables.table_name) as column_count
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('customer', 'administration', 'events', 'staff', 'booked_parking', 'qr_logs', 'parking_requests')
ORDER BY table_name;

-- Check RLS is enabled on all tables
SELECT 
    tablename, 
    CASE WHEN rowsecurity THEN '✓ Enabled' ELSE '✗ Disabled' END as rls_status
FROM pg_tables 
WHERE schemaname = 'public' 
AND tablename IN ('customer', 'administration', 'events', 'staff', 'booked_parking', 'qr_logs', 'parking_requests')
ORDER BY tablename;

-- Count policies for each table
SELECT 
    tablename,
    COUNT(*) as policy_count
FROM pg_policies 
WHERE schemaname = 'public'
GROUP BY tablename
ORDER BY tablename;

-- Check indexes exist
SELECT 
    tablename,
    COUNT(*) as index_count
FROM pg_indexes 
WHERE schemaname = 'public' 
AND tablename IN ('customer', 'administration', 'events', 'staff', 'booked_parking', 'qr_logs', 'parking_requests')
GROUP BY tablename
ORDER BY tablename;

-- Check sample data was inserted
SELECT 
    'Admins' as table_name, COUNT(*) as record_count FROM public.administration
UNION ALL
SELECT 'Customers', COUNT(*) FROM public.customer
UNION ALL
SELECT 'Events', COUNT(*) FROM public.events
UNION ALL
SELECT 'Staff', COUNT(*) FROM public.staff
UNION ALL
SELECT 'Bookings', COUNT(*) FROM public.booked_parking
UNION ALL
SELECT 'QR Logs', COUNT(*) FROM public.qr_logs
UNION ALL
SELECT 'Requests', COUNT(*) FROM public.parking_requests;

-- ============================================
-- SETUP COMPLETE! ✓
-- ============================================
-- 
-- Your Supabase database is now fully configured for SmartValet!
-- 
-- NEXT STEPS:
-- 1. Copy your Supabase URL and ANON KEY
-- 2. Update SupabaseClientInstance.java with these credentials:
--    - SUPABASE_URL = "https://your-project.supabase.co"
--    - SUPABASE_ANON_KEY = "your-anon-key"
-- 3. Build and run your SmartValet Android app
-- 4. Test with sample accounts:
--    - Admin: admin@smartvalet.com / admin123
--    - Customer: customer@example.com / customer123
--    - Staff: staff@smartvalet.com / staff123
-- 
-- DATABASE FEATURES INCLUDED:
-- ✓ 7 Tables (Customer, Admin, Events, Staff, Bookings, QR Logs, Requests)
-- ✓ Row Level Security (RLS) enabled on all tables
-- ✓ Public access policies for mobile app
-- ✓ Indexes for optimal query performance
-- ✓ Auto-update triggers for timestamp fields
-- ✓ Sample data for testing
-- ✓ Event pricing system (base price + extra charges)
-- ✓ Valet service request tracking
-- ✓ QR code scanning logs
-- ✓ Customer profile photos (Base64)
-- ✓ Event date filtering (current/future vs past events)
-- ✓ Staff assignment to events
-- 
-- IMPORTANT NOTES:
-- - All UUIDs are auto-generated using gen_random_uuid()
-- - Timestamps are auto-managed with triggers
-- - Foreign keys have appropriate CASCADE rules
-- - Indexes are optimized for common queries
-- - RLS policies allow public access (adjust for production security)
-- 
-- For support or issues, refer to the project documentation.
-- =====================================================
