-- =====================================================
-- SMARTVALET COMPLETE SUPABASE SETUP SCRIPT
-- =====================================================
-- Run this entire script in Supabase SQL Editor
-- Or run sections step by step as needed
-- =====================================================

-- ============================================
-- STEP 1: Enable Extensions
-- ============================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- STEP 2: Create All Tables
-- ============================================
-- Run these in order as some tables reference others

-- 1. CUSTOMER TABLE (MISSING FROM ORIGINAL SCHEMA - REQUIRED!)
CREATE TABLE IF NOT EXISTS public.customer (
    customer_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    email_id TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    full_name TEXT,
    contact_number TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. ADMINISTRATION TABLE
CREATE TABLE IF NOT EXISTS public.administration (
    admin_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    email_id TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    full_name TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. EVENTS TABLE
CREATE TABLE IF NOT EXISTS public.events (
    event_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    event_name TEXT NOT NULL,
    event_address TEXT NOT NULL,
    event_date DATE NOT NULL,
    hours_of_event INTEGER NOT NULL DEFAULT 6,
    total_vehicle_spots INTEGER NOT NULL DEFAULT 250,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by TEXT -- admin email_id
);

-- 4. STAFF TABLE
CREATE TABLE IF NOT EXISTS public.staff (
    staff_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    email_id TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    full_name TEXT NOT NULL,
    contact_number TEXT NOT NULL,
    location TEXT,
    age INTEGER,
    experience_level TEXT, -- "NEW COMER", "6-12 MONTHS EXPERIENCE", "MORE THAN 12 MONTHS EXPERIENCE"
    login_id TEXT UNIQUE,
    associated_parking TEXT,
    assigned_event_id UUID REFERENCES public.events(event_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 5. BOOKED_PARKING TABLE (References customer and events)
CREATE TABLE IF NOT EXISTS public.booked_parking (
    booking_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    customer_email TEXT NOT NULL REFERENCES public.customer(email_id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES public.events(event_id) ON DELETE CASCADE,
    parking_spot_number INTEGER NOT NULL,
    vehicle_number TEXT NOT NULL,
    vehicle_model TEXT,
    vehicle_color TEXT,
    booking_date DATE NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    payment_status TEXT DEFAULT 'pending', -- 'pending', 'paid', 'refunded'
    payment_amount DECIMAL(10,2),
    payment_method TEXT,
    qr_code TEXT UNIQUE, -- Generated QR code string
    qr_code_image_url TEXT, -- URL to QR image if stored
    status TEXT DEFAULT 'active', -- 'active', 'completed', 'cancelled'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 6. QR_LOGS TABLE
CREATE TABLE IF NOT EXISTS public.qr_logs (
    log_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES public.booked_parking(booking_id) ON DELETE CASCADE,
    qr_code TEXT NOT NULL,
    scanned_by_staff_id UUID REFERENCES public.staff(staff_id) ON DELETE SET NULL,
    scan_type TEXT NOT NULL, -- 'entry', 'exit', 'valet_request', 'car_fetch'
    scan_timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    vehicle_status TEXT, -- 'parked', 'retrieved', 'valet_in_transit', etc.
    notes TEXT
);

-- 7. PARKING_REQUESTS TABLE
CREATE TABLE IF NOT EXISTS public.parking_requests (
    request_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES public.booked_parking(booking_id) ON DELETE CASCADE,
    customer_email TEXT NOT NULL,
    request_type TEXT NOT NULL, -- 'valet_service', 'car_fetch', 'assistance'
    request_status TEXT DEFAULT 'pending', -- 'pending', 'assigned', 'in_progress', 'completed', 'cancelled'
    assigned_staff_id UUID REFERENCES public.staff(staff_id) ON DELETE SET NULL,
    request_details TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);

-- ============================================
-- STEP 3: Create Indexes
-- ============================================

-- Events indexes
CREATE INDEX IF NOT EXISTS idx_events_date ON public.events(event_date);
CREATE INDEX IF NOT EXISTS idx_events_created_by ON public.events(created_by);

-- Staff indexes
CREATE INDEX IF NOT EXISTS idx_staff_event ON public.staff(assigned_event_id);
CREATE INDEX IF NOT EXISTS idx_staff_email ON public.staff(email_id);

-- Customer indexes
CREATE INDEX IF NOT EXISTS idx_customer_email ON public.customer(email_id);

-- Administration indexes
CREATE INDEX IF NOT EXISTS idx_admin_email ON public.administration(email_id);

-- Booking indexes
CREATE INDEX IF NOT EXISTS idx_bookings_event ON public.booked_parking(event_id);
CREATE INDEX IF NOT EXISTS idx_bookings_customer ON public.booked_parking(customer_email);
CREATE INDEX IF NOT EXISTS idx_bookings_qr ON public.booked_parking(qr_code);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON public.booked_parking(status);
CREATE INDEX IF NOT EXISTS idx_bookings_payment_status ON public.booked_parking(payment_status);

-- QR logs indexes
CREATE INDEX IF NOT EXISTS idx_qr_logs_booking ON public.qr_logs(booking_id);
CREATE INDEX IF NOT EXISTS idx_qr_logs_staff ON public.qr_logs(scanned_by_staff_id);
CREATE INDEX IF NOT EXISTS idx_qr_logs_timestamp ON public.qr_logs(scan_timestamp);

-- Parking requests indexes
CREATE INDEX IF NOT EXISTS idx_parking_requests_status ON public.parking_requests(request_status);
CREATE INDEX IF NOT EXISTS idx_parking_requests_staff ON public.parking_requests(assigned_staff_id);
CREATE INDEX IF NOT EXISTS idx_parking_requests_customer ON public.parking_requests(customer_email);

-- ============================================
-- STEP 4: Enable Row Level Security (RLS)
-- ============================================

ALTER TABLE public.customer ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.administration ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.staff ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.booked_parking ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.qr_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.parking_requests ENABLE ROW LEVEL SECURITY;

-- ============================================
-- STEP 5: Create RLS Policies
-- ============================================
-- Drop existing policies first (if re-running)

DROP POLICY IF EXISTS "Allow public select on customer" ON public.customer;
DROP POLICY IF EXISTS "Allow public insert on customer" ON public.customer;
DROP POLICY IF EXISTS "Allow public update on customer" ON public.customer;

DROP POLICY IF EXISTS "Allow public select on administration" ON public.administration;
DROP POLICY IF EXISTS "Allow public insert on administration" ON public.administration;

DROP POLICY IF EXISTS "Allow public select on events" ON public.events;
DROP POLICY IF EXISTS "Allow public insert on events" ON public.events;
DROP POLICY IF EXISTS "Allow public update on events" ON public.events;
DROP POLICY IF EXISTS "Allow public delete on events" ON public.events;

DROP POLICY IF EXISTS "Allow public select on staff" ON public.staff;
DROP POLICY IF EXISTS "Allow public insert on staff" ON public.staff;
DROP POLICY IF EXISTS "Allow public update on staff" ON public.staff;
DROP POLICY IF EXISTS "Allow public delete on staff" ON public.staff;

DROP POLICY IF EXISTS "Allow public select on booked_parking" ON public.booked_parking;
DROP POLICY IF EXISTS "Allow public insert on booked_parking" ON public.booked_parking;
DROP POLICY IF EXISTS "Allow public update on booked_parking" ON public.booked_parking;
DROP POLICY IF EXISTS "Allow public delete on booked_parking" ON public.booked_parking;

DROP POLICY IF EXISTS "Allow public select on qr_logs" ON public.qr_logs;
DROP POLICY IF EXISTS "Allow public insert on qr_logs" ON public.qr_logs;
DROP POLICY IF EXISTS "Allow public update on qr_logs" ON public.qr_logs;

DROP POLICY IF EXISTS "Allow public select on parking_requests" ON public.parking_requests;
DROP POLICY IF EXISTS "Allow public insert on parking_requests" ON public.parking_requests;
DROP POLICY IF EXISTS "Allow public update on parking_requests" ON public.parking_requests;
DROP POLICY IF EXISTS "Allow public delete on parking_requests" ON public.parking_requests;

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

-- ADMINISTRATION TABLE POLICIES
CREATE POLICY "Allow public select on administration"
ON public.administration FOR SELECT
TO public
USING (true);

CREATE POLICY "Allow public insert on administration"
ON public.administration FOR INSERT
TO public
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
-- STEP 6: Create Functions & Triggers
-- ============================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for auto-updating updated_at

DROP TRIGGER IF EXISTS update_customer_updated_at ON public.customer;
CREATE TRIGGER update_customer_updated_at
    BEFORE UPDATE ON public.customer
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS update_events_updated_at ON public.events;
CREATE TRIGGER update_events_updated_at
    BEFORE UPDATE ON public.events
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS update_staff_updated_at ON public.staff;
CREATE TRIGGER update_staff_updated_at
    BEFORE UPDATE ON public.staff
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS update_booked_parking_updated_at ON public.booked_parking;
CREATE TRIGGER update_booked_parking_updated_at
    BEFORE UPDATE ON public.booked_parking
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS update_parking_requests_updated_at ON public.parking_requests;
CREATE TRIGGER update_parking_requests_updated_at
    BEFORE UPDATE ON public.parking_requests
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

-- ============================================
-- STEP 7: Insert Sample Data (Optional)
-- ============================================
-- Uncomment these if you want sample data for testing

/*
-- Insert sample admin
INSERT INTO public.administration (email_id, password, full_name)
VALUES ('admin@smartvalet.com', 'admin123', 'Admin User')
ON CONFLICT (email_id) DO NOTHING;

-- Insert sample customer
INSERT INTO public.customer (email_id, password, full_name, contact_number)
VALUES ('customer@example.com', 'customer123', 'Test Customer', '1234567890')
ON CONFLICT (email_id) DO NOTHING;

-- Insert sample event
INSERT INTO public.events (event_name, event_address, event_date, hours_of_event, total_vehicle_spots, created_by)
VALUES ('Summer Music Festival', '123 Main Street, City', '2024-06-15', 8, 500, 'admin@smartvalet.com')
ON CONFLICT DO NOTHING;
*/

-- ============================================
-- VERIFICATION QUERIES (Optional)
-- ============================================
-- Run these to verify setup

-- Check all tables exist
/*
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('customer', 'administration', 'events', 'staff', 'booked_parking', 'qr_logs', 'parking_requests')
ORDER BY table_name;
*/

-- Check RLS is enabled
/*
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE schemaname = 'public' 
AND tablename IN ('customer', 'administration', 'events', 'staff', 'booked_parking', 'qr_logs', 'parking_requests');
*/

-- Check policies exist
/*
SELECT schemaname, tablename, policyname, permissive, roles, cmd 
FROM pg_policies 
WHERE schemaname = 'public'
ORDER BY tablename, policyname;
*/

-- Check indexes exist
/*
SELECT tablename, indexname 
FROM pg_indexes 
WHERE schemaname = 'public' 
AND tablename IN ('customer', 'administration', 'events', 'staff', 'booked_parking', 'qr_logs', 'parking_requests')
ORDER BY tablename, indexname;
*/

-- ============================================
-- SETUP COMPLETE!
-- ============================================
-- Your Supabase database is now ready for SmartValet app
-- Test your Android app with these tables
-- ============================================

