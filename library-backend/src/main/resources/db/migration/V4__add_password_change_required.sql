-- V4: track members who must replace a staff-issued temporary password.
--
-- Existing rows default to false, so nobody is forced to change a password by
-- this migration. Only an explicit staff reset sets the flag to true.
--
-- This migration is additive and does not modify or rewrite any existing data.

ALTER TABLE public.users
    ADD COLUMN password_change_required boolean NOT NULL DEFAULT false;
